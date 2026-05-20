package io.github.ssaengji17.pocketcam

import android.app.Activity
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Camera
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableException
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

data class ArCoreStatus(
    val availability: String,
    val surfaceReady: Boolean = false,
    val sessionResumed: Boolean = false,
    val tracking: String = "lost",
    val error: String? = null,
)

class ArCoreTracker(
    private val activity: Activity,
    private val surfaceView: GLSurfaceView,
    private val listener: (PosePacket) -> Unit,
    private val onStatus: (ArCoreStatus) -> Unit,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val renderer = ArCoreRenderer()
    private var session: Session? = null
    private var installRequested = false
    private var startRequested = false

    @Volatile
    private var running = false

    @Volatile
    private var sessionResumed = false

    @Volatile
    private var lastTracking = "lost"

    init {
        surfaceView.preserveEGLContextOnPause = true
        surfaceView.setEGLContextClientVersion(2)
        surfaceView.setRenderer(renderer)
        surfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
    }

    val isAvailable: Boolean
        get() = checkAvailability().isSupported

    val availabilityLabel: String
        get() = availabilityToLabel(checkAvailability())

    fun start(): Boolean {
        val availability = checkAvailability()
        val availabilityText = availabilityToLabel(availability)
        Log.d(TAG, "Starting ARCore tracker; availability=${availability.name}")
        emitStatus(availability = availabilityText)

        if (availability.isTransient) {
            emitStatus(
                availability = availabilityText,
                error = "ARCore availability is still checking",
            )
            return false
        }

        if (!availability.isSupported) {
            emitStatus(
                availability = availabilityText,
                error = "ARCore is not supported on this device",
            )
            return false
        }

        try {
            when (ArCoreApk.getInstance().requestInstall(activity, !installRequested)) {
                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                    installRequested = true
                    emitStatus(
                        availability = availabilityText,
                        error = "Install or update Google Play Services for AR, then start again",
                    )
                    return false
                }

                ArCoreApk.InstallStatus.INSTALLED -> Unit
            }

            val arSession = session ?: Session(activity).also { session = it }
            val config = Config(arSession).apply {
                updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                planeFindingMode = Config.PlaneFindingMode.DISABLED
                lightEstimationMode = Config.LightEstimationMode.DISABLED
            }
            arSession.configure(config)

            startRequested = true
            running = false
            sessionResumed = false
            lastTracking = "limited"
            renderer.session = arSession
            renderer.resetFrameState()

            Log.d(TAG, "ARCore session configured on main thread")
            Log.d(TAG, "Calling GLSurfaceView.onResume before Session.resume")
            surfaceView.onResume()

            emitStatus(availability = "available")
            mainHandler.post { resumeSessionIfSurfaceReady() }
            return true
        } catch (exception: UnavailableException) {
            reportStartupFailure(exception)
        } catch (exception: RuntimeException) {
            reportStartupFailure(exception)
        }

        return false
    }

    fun stop() {
        Log.d(TAG, "Stopping ARCore tracker")
        startRequested = false
        running = false

        if (sessionResumed) {
            try {
                session?.pause()
            } catch (exception: RuntimeException) {
                Log.w(TAG, "Ignoring ARCore pause failure", exception)
            }
        }

        sessionResumed = false
        surfaceView.onPause()
        renderer.session = null
        emitStatus(availability = availabilityLabel, tracking = "lost")
    }

    fun close() {
        stop()
        session?.close()
        session = null
    }

    private fun resumeSessionIfSurfaceReady() {
        if (!startRequested || sessionResumed) {
            return
        }

        if (!renderer.isSurfaceReady) {
            Log.d(TAG, "Waiting for ARCore preview surface before Session.resume; surface=${renderer.surfaceSizeLabel}")
            emitStatus(availability = "available")
            return
        }

        val arSession = session ?: return

        try {
            Log.d(TAG, "Calling ARCore Session.resume on main thread; surface=${renderer.surfaceSizeLabel}")
            arSession.resume()
            sessionResumed = true
            running = true
            renderer.markTextureNeedsAttach()
            emitStatus(availability = "available")
            Log.d(TAG, "ARCore Session.resume completed")
        } catch (exception: UnavailableException) {
            reportStartupFailure(exception)
        } catch (exception: CameraNotAvailableException) {
            reportStartupFailure(exception)
        } catch (exception: RuntimeException) {
            reportStartupFailure(exception)
        }
    }

    private fun checkAvailability(): ArCoreApk.Availability {
        return ArCoreApk.getInstance().checkAvailability(activity)
    }

    private fun reportStartupFailure(exception: Exception) {
        Log.e(TAG, "ARCore startup failed", exception)
        startRequested = false
        running = false
        sessionResumed = false
        emitStatus(
            availability = availabilityLabel,
            tracking = "lost",
            error = formatExceptionForUi(exception),
        )
    }

    private fun formatExceptionForUi(exception: Exception): String {
        val message = exception.message ?: "No exception message"
        val detail = "${exception.javaClass.name}: $message"
        return if (isSensorQueueFailure(exception)) {
            "$SENSOR_ACCESS_ERROR $detail"
        } else {
            detail
        }
    }

    private fun isSensorQueueFailure(exception: Exception): Boolean {
        var current: Throwable? = exception
        while (current != null) {
            val message = current.message.orEmpty()
            if (
                message.contains("Failed to register sensor to queue", ignoreCase = true) ||
                message.contains("register sensor", ignoreCase = true)
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }

    private fun emitStatus(
        availability: String,
        tracking: String = lastTracking,
        error: String? = null,
    ) {
        onStatus(
            ArCoreStatus(
                availability = availability,
                surfaceReady = renderer.isSurfaceReady,
                sessionResumed = sessionResumed,
                tracking = tracking,
                error = error,
            ),
        )
    }

    private fun availabilityToLabel(availability: ArCoreApk.Availability): String {
        return when {
            availability == ArCoreApk.Availability.SUPPORTED_INSTALLED -> "available"
            availability.isSupported -> "available (${availability.name.lowercase().replace('_', ' ')})"
            availability.isTransient -> "checking"
            else -> "unavailable (${availability.name.lowercase().replace('_', ' ')})"
        }
    }

    private fun trackingToProtocol(state: TrackingState): String {
        return when (state) {
            TrackingState.TRACKING -> "normal"
            TrackingState.PAUSED -> "limited"
            TrackingState.STOPPED -> "lost"
        }
    }

    private fun posePacketFromCamera(camera: Camera): PosePacket {
        val pose = camera.pose
        val translation = pose.translation
        val rotation = pose.rotationQuaternion

        return PosePacket(
            mode = "arcore",
            position = PositionVector(
                x = translation[0],
                y = translation[1],
                z = translation[2],
            ),
            rotation = RotationQuaternion(
                x = rotation[0],
                y = rotation[1],
                z = rotation[2],
                w = rotation[3],
            ),
            tracking = trackingToProtocol(camera.trackingState),
        )
    }

    private inner class ArCoreRenderer : GLSurfaceView.Renderer {
        @Volatile
        var session: Session? = null

        @Volatile
        private var surfaceCreated = false

        @Volatile
        private var viewportWidth = 0

        @Volatile
        private var viewportHeight = 0

        private var cameraTextureId = 0
        private var textureAttached = false
        private var lastFrameTimestamp = 0L
        private var viewportChanged = false

        val isSurfaceReady: Boolean
            get() = surfaceCreated && viewportWidth > 1 && viewportHeight > 1

        val surfaceSizeLabel: String
            get() = "${viewportWidth}x${viewportHeight}"

        fun resetFrameState() {
            textureAttached = false
            lastFrameTimestamp = 0L
            viewportChanged = true
        }

        fun markTextureNeedsAttach() {
            textureAttached = false
        }

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            cameraTextureId = createExternalTexture()
            textureAttached = false
            surfaceCreated = true
            GLES20.glClearColor(0f, 0f, 0f, 1f)
            Log.d(TAG, "GLSurfaceView surface created")
            notifySurfaceStateChanged()
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            viewportWidth = width.coerceAtLeast(0)
            viewportHeight = height.coerceAtLeast(0)
            viewportChanged = true
            GLES20.glViewport(0, 0, viewportWidth, viewportHeight)
            Log.d(TAG, "GLSurfaceView surface changed: ${surfaceSizeLabel}")
            notifySurfaceStateChanged()
        }

        override fun onDrawFrame(gl: GL10?) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

            val arSession = session ?: return
            if (!running || !sessionResumed) {
                return
            }

            try {
                if (cameraTextureId == 0) {
                    cameraTextureId = createExternalTexture()
                    textureAttached = false
                }

                if (!textureAttached) {
                    arSession.setCameraTextureNames(intArrayOf(cameraTextureId))
                    textureAttached = true
                }

                if (viewportChanged) {
                    arSession.setDisplayGeometry(activity.windowManager.defaultDisplay.rotation, viewportWidth, viewportHeight)
                    viewportChanged = false
                }

                val frame = arSession.update()
                if (frame.timestamp == 0L || frame.timestamp == lastFrameTimestamp) {
                    return
                }

                lastFrameTimestamp = frame.timestamp
                val camera = frame.camera
                val tracking = trackingToProtocol(camera.trackingState)
                lastTracking = tracking
                onStatus(
                    ArCoreStatus(
                        availability = "available",
                        surfaceReady = isSurfaceReady,
                        sessionResumed = sessionResumed,
                        tracking = tracking,
                    ),
                )
                listener(posePacketFromCamera(camera))
            } catch (exception: CameraNotAvailableException) {
                mainHandler.post { reportStartupFailure(exception) }
            } catch (exception: RuntimeException) {
                mainHandler.post { reportStartupFailure(exception) }
            }
        }

        private fun notifySurfaceStateChanged() {
            mainHandler.post {
                emitStatus(availability = availabilityLabel)
                resumeSessionIfSurfaceReady()
            }
        }

        private fun createExternalTexture(): Int {
            val textureIds = IntArray(1)
            GLES20.glGenTextures(1, textureIds, 0)
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureIds[0])
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            return textureIds[0]
        }
    }

    private companion object {
        private const val TAG = "PocketCamARCore"
        private const val SENSOR_ACCESS_ERROR = "ARCore failed to access device sensors."
    }
}

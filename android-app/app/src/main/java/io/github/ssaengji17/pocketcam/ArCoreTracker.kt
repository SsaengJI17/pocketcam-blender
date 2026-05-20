package io.github.ssaengji17.pocketcam

import android.app.Activity
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
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
    val tracking: String,
    val error: String? = null,
)

class ArCoreTracker(
    private val activity: Activity,
    private val surfaceView: GLSurfaceView,
    private val listener: (PosePacket) -> Unit,
    private val onStatus: (ArCoreStatus) -> Unit,
) {
    private val renderer = ArCoreRenderer()
    private var session: Session? = null
    private var installRequested = false

    @Volatile
    private var running = false

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
        onStatus(ArCoreStatus(availabilityToLabel(availability), "lost"))

        if (availability.isTransient) {
            onStatus(ArCoreStatus(availabilityToLabel(availability), "lost", "ARCore availability is still checking"))
            return false
        }

        if (!availability.isSupported) {
            onStatus(ArCoreStatus(availabilityToLabel(availability), "lost", "ARCore is not supported on this device"))
            return false
        }

        try {
            when (ArCoreApk.getInstance().requestInstall(activity, !installRequested)) {
                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                    installRequested = true
                    onStatus(ArCoreStatus(availabilityToLabel(availability), "lost", "Install or update Google Play Services for AR, then start again"))
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
            renderer.session = arSession
            renderer.resetFrameState()
            arSession.resume()
            running = true
            surfaceView.onResume()
            onStatus(ArCoreStatus("available", "limited"))
            return true
        } catch (exception: UnavailableException) {
            onStatus(ArCoreStatus(availabilityToLabel(availability), "lost", exception.message ?: exception.javaClass.simpleName))
        } catch (exception: CameraNotAvailableException) {
            onStatus(ArCoreStatus(availabilityToLabel(availability), "lost", exception.message ?: exception.javaClass.simpleName))
        } catch (exception: RuntimeException) {
            onStatus(ArCoreStatus(availabilityToLabel(availability), "lost", exception.message ?: exception.javaClass.simpleName))
        }

        running = false
        return false
    }

    fun stop() {
        running = false
        surfaceView.onPause()
        session?.pause()
        renderer.session = null
        onStatus(ArCoreStatus(availabilityLabel, "lost"))
    }

    fun close() {
        stop()
        session?.close()
        session = null
    }

    private fun checkAvailability(): ArCoreApk.Availability {
        return ArCoreApk.getInstance().checkAvailability(activity)
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

        private var cameraTextureId = 0
        private var textureAttached = false
        private var lastFrameTimestamp = 0L
        private var viewportWidth = 1
        private var viewportHeight = 1
        private var viewportChanged = false

        fun resetFrameState() {
            textureAttached = false
            lastFrameTimestamp = 0L
            viewportChanged = true
        }

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            cameraTextureId = createExternalTexture()
            textureAttached = false
            GLES20.glClearColor(0f, 0f, 0f, 1f)
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            viewportWidth = width.coerceAtLeast(1)
            viewportHeight = height.coerceAtLeast(1)
            viewportChanged = true
            GLES20.glViewport(0, 0, viewportWidth, viewportHeight)
        }

        override fun onDrawFrame(gl: GL10?) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

            val arSession = session ?: return
            if (!running) {
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
                onStatus(ArCoreStatus("available", tracking))
                listener(posePacketFromCamera(camera))
            } catch (exception: CameraNotAvailableException) {
                onStatus(ArCoreStatus(availabilityLabel, "lost", exception.message ?: exception.javaClass.simpleName))
                running = false
            } catch (exception: RuntimeException) {
                onStatus(ArCoreStatus(availabilityLabel, "lost", exception.message ?: exception.javaClass.simpleName))
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
}

package io.github.ssaengji17.pocketcam

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView

private enum class TrackingMode {
    ROTATION_SENSOR,
    ARCORE_6DOF,
}

class MainActivity : Activity() {
    private lateinit var sensorTracker: RotationSensorTracker
    private lateinit var arCoreTracker: ArCoreTracker
    private lateinit var poseSender: UdpPoseSender
    private lateinit var arCoreSurfaceView: GLSurfaceView

    private lateinit var hostInput: EditText
    private lateinit var portInput: EditText
    private lateinit var modeGroup: RadioGroup
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var sensorStatus: TextView
    private lateinit var arCoreStatus: TextView
    private lateinit var surfaceStatus: TextView
    private lateinit var sessionStatus: TextView
    private lateinit var trackingStatus: TextView
    private lateinit var sendingStatus: TextView
    private lateinit var packetsSentStatus: TextView
    private lateinit var lastErrorStatus: TextView

    private var selectedMode = TrackingMode.ROTATION_SENSOR
    private var packetsSent = 0L
    private var lastError = "None"
    private var arCoreAvailability = "checking"
    private var arCoreSurfaceReady = false
    private var arCoreSessionResumed = false
    private var arCoreTracking = "lost"
    private var pendingArCoreStart = false
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arCoreSurfaceView = GLSurfaceView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(320),
            ).apply {
                setMargins(0, dpToPx(8), 0, dpToPx(16))
            }
        }

        poseSender = UdpPoseSender(
            onPacketSent = {
                runOnUiThread {
                    packetsSent += 1
                    updateStatus()
                }
            },
            onError = { message ->
                runOnUiThread {
                    lastError = message
                    updateStatus()
                }
            },
        )

        sensorTracker = RotationSensorTracker(
            context = this,
            listener = { rotation ->
                poseSender.send(rotation)
            },
        )

        arCoreTracker = ArCoreTracker(
            activity = this,
            surfaceView = arCoreSurfaceView,
            listener = { packet ->
                poseSender.send(packet)
            },
            onStatus = { status ->
                runOnUiThread {
                    arCoreAvailability = status.availability
                    arCoreSurfaceReady = status.surfaceReady
                    arCoreSessionResumed = status.sessionResumed
                    arCoreTracking = status.tracking
                    status.error?.let { lastError = it }
                    if (status.error != null && selectedMode == TrackingMode.ARCORE_6DOF) {
                        poseSender.stop()
                    }
                    updateStatus()
                }
            },
        )
        arCoreAvailability = arCoreTracker.availabilityLabel

        setContentView(buildContentView())
        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        arCoreAvailability = arCoreTracker.availabilityLabel
        updateStatus()
    }

    override fun onPause() {
        super.onPause()
        pendingArCoreStart = false
        stopSending()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSending()
        arCoreTracker.close()
        poseSender.close()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode != CAMERA_PERMISSION_REQUEST) {
            return
        }

        val granted = grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            pendingArCoreStart = false
            lastError = "Camera permission is required for ARCore 6DoF mode"
            updateStatus()
            return
        }

        if (pendingArCoreStart) {
            pendingArCoreStart = false
            startSending()
        } else {
            updateStatus()
        }
    }

    private fun buildContentView(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 0)
        }

        header.addView(title("PocketCam"))
        header.addView(label("Tracking mode"))
        modeGroup = RadioGroup(this).apply {
            orientation = RadioGroup.VERTICAL
            addView(
                RadioButton(this@MainActivity).apply {
                    id = MODE_ROTATION_SENSOR
                    text = "Rotation Sensor mode"
                    isChecked = true
                },
            )
            addView(
                RadioButton(this@MainActivity).apply {
                    id = MODE_ARCORE_6DOF
                    text = "ARCore 6DoF mode"
                },
            )
            setOnCheckedChangeListener { _, checkedId ->
                val newMode = if (checkedId == MODE_ARCORE_6DOF) {
                    TrackingMode.ARCORE_6DOF
                } else {
                    TrackingMode.ROTATION_SENSOR
                }

                if (newMode != selectedMode) {
                    stopSending()
                    selectedMode = newMode
                    updateStatus()
                }
            }
        }
        header.addView(modeGroup)
        header.addView(label("ARCore preview"))
        header.addView(arCoreSurfaceView)
        root.addView(header)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 0, 32, 32)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }

        content.addView(label("Target host"))
        hostInput = EditText(this).apply {
            setText(DEFAULT_HOST)
            inputType = InputType.TYPE_CLASS_TEXT
            isSingleLine = true
        }
        content.addView(hostInput)

        content.addView(label("Target port"))
        portInput = EditText(this).apply {
            setText(DEFAULT_PORT.toString())
            inputType = InputType.TYPE_CLASS_NUMBER
            isSingleLine = true
        }
        content.addView(portInput)

        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 24)
        }

        startButton = Button(this).apply {
            text = "Start Sending"
            setOnClickListener { startSending() }
        }
        stopButton = Button(this).apply {
            text = "Stop Sending"
            setOnClickListener { stopSending() }
        }
        buttonRow.addView(startButton, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        buttonRow.addView(stopButton, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        content.addView(buttonRow)

        sensorStatus = statusLine()
        arCoreStatus = statusLine()
        surfaceStatus = statusLine()
        sessionStatus = statusLine()
        trackingStatus = statusLine()
        sendingStatus = statusLine()
        packetsSentStatus = statusLine()
        lastErrorStatus = statusLine()
        content.addView(sensorStatus)
        content.addView(arCoreStatus)
        content.addView(surfaceStatus)
        content.addView(sessionStatus)
        content.addView(trackingStatus)
        content.addView(sendingStatus)
        content.addView(packetsSentStatus)
        content.addView(lastErrorStatus)

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f,
            )
            addView(content)
        }
        root.addView(scrollView)
        return root
    }

    private fun startSending() {
        val host = hostInput.text.toString().trim()
        val port = portInput.text.toString().toIntOrNull()

        if (host.isBlank()) {
            lastError = "Host is required"
            updateStatus()
            return
        }

        if (port == null || port !in 1..65535) {
            lastError = "Port must be between 1 and 65535"
            updateStatus()
            return
        }

        when (selectedMode) {
            TrackingMode.ROTATION_SENSOR -> startRotationSensorMode(host, port)
            TrackingMode.ARCORE_6DOF -> startArCoreMode(host, port)
        }
    }

    private fun startRotationSensorMode(host: String, port: Int) {
        if (!sensorTracker.isAvailable) {
            lastError = "Rotation Vector Sensor is not available"
            updateStatus()
            return
        }

        packetsSent = 0
        lastError = "None"
        poseSender.start(host, port)
        sensorTracker.start()
        updateStatus()
    }

    private fun startArCoreMode(host: String, port: Int) {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            pendingArCoreStart = true
            lastError = "Camera permission is required for ARCore 6DoF mode"
            requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST)
            updateStatus()
            return
        }

        Log.d(TAG, "Preparing ARCore mode; stopping all app-managed sensors first")
        sensorTracker.stop()
        Log.d(TAG, "Rotation sensor stop requested before ARCore startup")
        arCoreTracker.stop()

        packetsSent = 0
        lastError = "None"
        arCoreTracking = "limited"
        poseSender.start(host, port)
        updateStatus()

        Log.d(TAG, "Delaying ARCore tracker start until app-managed sensors are released")
        mainHandler.postDelayed(
            {
                if (selectedMode != TrackingMode.ARCORE_6DOF || !poseSender.isSending) {
                    Log.d(TAG, "Skipping delayed ARCore start because mode or sender state changed")
                    return@postDelayed
                }

                Log.d(TAG, "Starting ARCore tracker after sensor release delay")
                if (!arCoreTracker.start()) {
                    poseSender.stop()
                }
                updateStatus()
            },
            SENSOR_RELEASE_DELAY_MS,
        )
    }

    private fun stopSending() {
        sensorTracker.stop()
        arCoreTracker.stop()
        poseSender.stop()
        updateStatus()
    }

    private fun updateStatus() {
        if (!poseSender.isSending) {
            arCoreAvailability = arCoreTracker.availabilityLabel
        }

        val modeText = when (selectedMode) {
            TrackingMode.ROTATION_SENSOR -> "Rotation Sensor"
            TrackingMode.ARCORE_6DOF -> "ARCore 6DoF"
        }
        val trackingText = when (selectedMode) {
            TrackingMode.ROTATION_SENSOR -> "normal"
            TrackingMode.ARCORE_6DOF -> arCoreTracking
        }
        sensorStatus.text = "Rotation sensor availability: ${if (sensorTracker.isAvailable) "available" else "unavailable"}"
        arCoreStatus.text = "ARCore availability: $arCoreAvailability"
        surfaceStatus.text = "ARCore surface ready: ${if (arCoreSurfaceReady) "yes" else "no"}"
        sessionStatus.text = "ARCore session resumed: ${if (arCoreSessionResumed) "yes" else "no"}"
        trackingStatus.text = "Tracking mode/state: $modeText / $trackingText"
        sendingStatus.text = "Sending state: ${if (poseSender.isSending) "sending" else "stopped"}"
        packetsSentStatus.text = "Packets sent: $packetsSent"
        lastErrorStatus.text = "Last error: $lastError"
        startButton.isEnabled = !poseSender.isSending && isSelectedModeStartable()
        stopButton.isEnabled = poseSender.isSending
    }

    private fun isSelectedModeStartable(): Boolean {
        return when (selectedMode) {
            TrackingMode.ROTATION_SENSOR -> sensorTracker.isAvailable
            TrackingMode.ARCORE_6DOF -> !arCoreAvailability.startsWith("unavailable")
        }
    }

    private fun title(text: String): TextView =
        TextView(this).apply {
            this.text = text
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 24)
        }

    private fun label(text: String): TextView =
        TextView(this).apply {
            this.text = text
            textSize = 14f
            setPadding(0, 16, 0, 4)
        }

    private fun statusLine(): TextView =
        TextView(this).apply {
            textSize = 16f
            setPadding(0, 8, 0, 8)
        }

    private fun dpToPx(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private companion object {
        private const val DEFAULT_HOST = "127.0.0.1"
        private const val DEFAULT_PORT = 8765
        private const val MODE_ROTATION_SENSOR = 1001
        private const val MODE_ARCORE_6DOF = 1002
        private const val CAMERA_PERMISSION_REQUEST = 2001
        private const val SENSOR_RELEASE_DELAY_MS = 150L
        private const val TAG = "PocketCamMain"
    }
}

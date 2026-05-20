package io.github.ssaengji17.pocketcam

import android.app.Activity
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var sensorTracker: RotationSensorTracker
    private lateinit var poseSender: UdpPoseSender

    private lateinit var hostInput: EditText
    private lateinit var portInput: EditText
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var sensorStatus: TextView
    private lateinit var sendingStatus: TextView
    private lateinit var packetsSentStatus: TextView
    private lateinit var lastErrorStatus: TextView

    private var packetsSent = 0L
    private var lastError = "None"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        setContentView(buildContentView())
        updateStatus()
    }

    override fun onPause() {
        super.onPause()
        stopSending()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSending()
        poseSender.close()
    }

    private fun buildContentView(): ScrollView {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }

        content.addView(title("PocketCam"))
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
        sendingStatus = statusLine()
        packetsSentStatus = statusLine()
        lastErrorStatus = statusLine()
        content.addView(sensorStatus)
        content.addView(sendingStatus)
        content.addView(packetsSentStatus)
        content.addView(lastErrorStatus)

        return ScrollView(this).apply {
            addView(content)
        }
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

    private fun stopSending() {
        sensorTracker.stop()
        poseSender.stop()
        updateStatus()
    }

    private fun updateStatus() {
        sensorStatus.text = "Sensor availability: ${if (sensorTracker.isAvailable) "available" else "unavailable"}"
        sendingStatus.text = "Sending state: ${if (poseSender.isSending) "sending" else "stopped"}"
        packetsSentStatus.text = "Packets sent: $packetsSent"
        lastErrorStatus.text = "Last error: $lastError"
        startButton.isEnabled = sensorTracker.isAvailable && !poseSender.isSending
        stopButton.isEnabled = poseSender.isSending
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

    private companion object {
        private const val DEFAULT_HOST = "127.0.0.1"
        private const val DEFAULT_PORT = 8765
    }
}

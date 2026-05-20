package io.github.ssaengji17.pocketcam

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class RotationSensorTracker(
    context: Context,
    private val listener: (RotationQuaternion) -> Unit,
) : SensorEventListener {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    val isAvailable: Boolean
        get() = rotationSensor != null

    fun start() {
        val sensor = rotationSensor ?: return
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) {
            return
        }

        val quaternion = FloatArray(4)
        SensorManager.getQuaternionFromVector(quaternion, event.values)

        // Android returns [w, x, y, z]. Protocol v1 sends [x, y, z, w].
        listener(
            RotationQuaternion(
                x = quaternion[1],
                y = quaternion[2],
                z = quaternion[3],
                w = quaternion[0],
            ),
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

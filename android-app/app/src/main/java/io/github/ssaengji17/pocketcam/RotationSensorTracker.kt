package io.github.ssaengji17.pocketcam

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

class RotationSensorTracker(
    context: Context,
    private val listener: (RotationQuaternion) -> Unit,
) {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private var sensorEventListener: SensorEventListener? = null

    val isAvailable: Boolean
        get() = rotationSensor != null

    fun start() {
        val sensor = rotationSensor ?: return
        stop()

        val eventListener = createSensorEventListener()
        sensorEventListener = eventListener
        Log.d(TAG, "Registering Rotation Vector Sensor listener")
        sensorManager.registerListener(eventListener, sensor, SensorManager.SENSOR_DELAY_GAME)
        Log.d(TAG, "Rotation Vector Sensor listener registered")
    }

    fun stop() {
        val eventListener = sensorEventListener
        if (eventListener == null) {
            Log.d(TAG, "Rotation Vector Sensor listener already stopped")
            return
        }

        Log.d(TAG, "Unregistering Rotation Vector Sensor listener")
        sensorManager.unregisterListener(eventListener)
        sensorEventListener = null
        Log.d(TAG, "Rotation Vector Sensor listener unregistered and released")
    }

    private fun createSensorEventListener(): SensorEventListener {
        return object : SensorEventListener {
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
    }

    private companion object {
        private const val TAG = "PocketCamRotation"
    }
}

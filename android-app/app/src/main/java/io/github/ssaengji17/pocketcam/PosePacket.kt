package io.github.ssaengji17.pocketcam

import org.json.JSONArray
import org.json.JSONObject

data class RotationQuaternion(
    val x: Float,
    val y: Float,
    val z: Float,
    val w: Float,
)

fun RotationQuaternion.toPoseJson(timestampSeconds: Double): String {
    return JSONObject()
        .put("type", "pose")
        .put("version", 1)
        .put("timestamp", timestampSeconds)
        .put("mode", "rotation")
        .put("rotation", JSONArray(listOf(x.toDouble(), y.toDouble(), z.toDouble(), w.toDouble())))
        .put("tracking", "normal")
        .toString()
}

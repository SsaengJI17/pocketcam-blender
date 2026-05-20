package io.github.ssaengji17.pocketcam

import org.json.JSONArray
import org.json.JSONObject

data class RotationQuaternion(
    val x: Float,
    val y: Float,
    val z: Float,
    val w: Float,
)

data class PositionVector(
    val x: Float,
    val y: Float,
    val z: Float,
)

data class PosePacket(
    val mode: String,
    val rotation: RotationQuaternion,
    val position: PositionVector? = null,
    val tracking: String = "normal",
)

fun RotationQuaternion.toPoseJson(timestampSeconds: Double): String {
    return PosePacket(
        mode = "rotation",
        rotation = this,
        tracking = "normal",
    ).toPoseJson(timestampSeconds)
}

fun PosePacket.toPoseJson(timestampSeconds: Double): String {
    val json = JSONObject()
        .put("type", "pose")
        .put("version", 1)
        .put("timestamp", timestampSeconds)
        .put("mode", mode)
        .put("rotation", rotation.toJsonArray())
        .put("tracking", tracking)

    position?.let {
        json.put("position", JSONArray(listOf(it.x.toDouble(), it.y.toDouble(), it.z.toDouble())))
    }

    return json.toString()
}

private fun RotationQuaternion.toJsonArray(): JSONArray {
    return JSONArray(listOf(x.toDouble(), y.toDouble(), z.toDouble(), w.toDouble()))
}

import math
from typing import Any, Dict, Optional, Tuple


RotationXYZW = Tuple[float, float, float, float]
PositionXYZ = Tuple[float, float, float]


class PosePacketError(ValueError):
    """Raised when a received packet does not match protocol v1 requirements."""


def parse_pose_packet(
    packet: Dict[str, Any],
) -> Tuple[RotationXYZW, Optional[PositionXYZ], Optional[float], Optional[str], Optional[str]]:
    if not isinstance(packet, dict):
        raise PosePacketError("packet is not a JSON object")

    if packet.get("type") != "pose":
        raise PosePacketError("packet type is not pose")

    if packet.get("version") != 1:
        raise PosePacketError("unsupported protocol version")

    rotation = _parse_rotation(packet.get("rotation"))
    position = _parse_optional_position(packet.get("position"))
    timestamp = _parse_optional_number(packet.get("timestamp"))
    mode = _parse_optional_string(packet.get("mode"))
    tracking = _parse_optional_string(packet.get("tracking"))
    return rotation, position, timestamp, mode, tracking


def to_blender_quaternion_wxyz(rotation_xyzw: RotationXYZW) -> Tuple[float, float, float, float]:
    x, y, z, w = rotation_xyzw

    # MVP behavior: incoming rotation is treated as already being in Blender space.
    # TODO: Add Android Rotation Vector Sensor and ARCore basis conversion here.
    return (w, x, y, z)


def to_blender_position_xyz(position_xyz: PositionXYZ) -> PositionXYZ:
    x, y, z = position_xyz

    # ARCore camera pose uses an OpenGL-like basis: X right, Y up, -Z forward.
    # Blender uses X right, Y forward, Z up.
    # TODO: Refine Android/ARCore coordinate conversion after device calibration tests.
    return (x, -z, y)


def _parse_rotation(value: Any) -> RotationXYZW:
    if not isinstance(value, list) or len(value) != 4:
        raise PosePacketError("rotation must be an array of four numbers")

    values = []
    for component in value:
        if isinstance(component, bool) or not isinstance(component, (int, float)):
            raise PosePacketError("rotation components must be numbers")
        number = float(component)
        if not math.isfinite(number):
            raise PosePacketError("rotation components must be finite")
        values.append(number)

    length = math.sqrt(sum(component * component for component in values))
    if length <= 1e-8:
        raise PosePacketError("rotation quaternion has zero length")

    return (values[0], values[1], values[2], values[3])


def _parse_optional_position(value: Any) -> Optional[PositionXYZ]:
    if value is None:
        return None

    if not isinstance(value, list) or len(value) != 3:
        raise PosePacketError("position must be an array of three numbers")

    values = []
    for component in value:
        if isinstance(component, bool) or not isinstance(component, (int, float)):
            raise PosePacketError("position components must be numbers")
        number = float(component)
        if not math.isfinite(number):
            raise PosePacketError("position components must be finite")
        values.append(number)

    return (values[0], values[1], values[2])


def _parse_optional_number(value: Any) -> Optional[float]:
    if value is None:
        return None
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        return None
    number = float(value)
    return number if math.isfinite(number) else None


def _parse_optional_string(value: Any) -> Optional[str]:
    return value if isinstance(value, str) else None

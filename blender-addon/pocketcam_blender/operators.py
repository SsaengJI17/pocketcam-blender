from typing import Optional

import bpy
from mathutils import Quaternion, Vector

from .coordinate import PositionXYZ, to_blender_position_xyz, to_blender_quaternion_wxyz
from .receiver import UDPReceiver
from .state import STATE


DEFAULT_HOST = "0.0.0.0"
DEFAULT_PORT = 8765
TIMER_INTERVAL_SECONDS = 1.0 / 60.0

_receiver: Optional[UDPReceiver] = None
_position_origin_xyz: Optional[PositionXYZ] = None
_camera_origin_location: Optional[PositionXYZ] = None
_rotation_origin_inverse: Optional[Quaternion] = None
_camera_origin_rotation: Optional[Quaternion] = None


def _select_camera(context):
    scene_camera = context.scene.camera
    if scene_camera is not None:
        return scene_camera

    active_object = context.view_layer.objects.active
    if active_object is not None and active_object.type == "CAMERA":
        return active_object

    return None


def _apply_latest_pose():
    if not STATE.is_server_running():
        return None

    pose = STATE.get_latest_pose()
    if pose is None:
        return TIMER_INTERVAL_SECONDS

    camera = _select_camera(bpy.context)
    if camera is None:
        STATE.set_last_error("No active scene camera found")
        return TIMER_INTERVAL_SECONDS

    quaternion = Quaternion(to_blender_quaternion_wxyz(pose.rotation_xyzw))
    quaternion.normalize()
    if _rotation_origin_inverse is not None and _camera_origin_rotation is not None:
        quaternion = _camera_origin_rotation @ (_rotation_origin_inverse @ quaternion)
        quaternion.normalize()

    camera.rotation_mode = "QUATERNION"
    camera.rotation_quaternion = quaternion

    if pose.position_xyz is not None:
        position = to_blender_position_xyz(pose.position_xyz)
        scale = getattr(bpy.context.scene, "pocketcam_position_scale", 1.0)
        if _position_origin_xyz is not None and _camera_origin_location is not None:
            origin = Vector(_position_origin_xyz)
            camera_origin = Vector(_camera_origin_location)
            camera.location = camera_origin + ((Vector(position) - origin) * scale)
        else:
            camera.location = Vector(position) * scale

    return TIMER_INTERVAL_SECONDS


def _ensure_timer_registered():
    if not bpy.app.timers.is_registered(_apply_latest_pose):
        bpy.app.timers.register(_apply_latest_pose, first_interval=0.0)


def _stop_receiver():
    global _receiver

    if _receiver is not None:
        _receiver.stop()
        _receiver = None
    STATE.set_server_running(False, DEFAULT_HOST, DEFAULT_PORT)


class POCKETCAM_OT_start_server(bpy.types.Operator):
    bl_idname = "pocketcam.start_server"
    bl_label = "Start Server"
    bl_description = "Start the PocketCam UDP pose receiver"

    def execute(self, context):
        global _receiver

        if STATE.is_server_running():
            self.report({"INFO"}, "PocketCam server is already running")
            return {"FINISHED"}

        _receiver = UDPReceiver(STATE, host=DEFAULT_HOST, port=DEFAULT_PORT)
        try:
            _receiver.start()
        except OSError as exc:
            _receiver = None
            STATE.set_last_error(str(exc))
            self.report({"ERROR"}, f"Failed to start PocketCam server: {exc}")
            return {"CANCELLED"}

        _ensure_timer_registered()
        self.report({"INFO"}, f"PocketCam UDP server started on port {DEFAULT_PORT}")
        return {"FINISHED"}


class POCKETCAM_OT_stop_server(bpy.types.Operator):
    bl_idname = "pocketcam.stop_server"
    bl_label = "Stop Server"
    bl_description = "Stop the PocketCam UDP pose receiver"

    def execute(self, context):
        _stop_receiver()
        self.report({"INFO"}, "PocketCam UDP server stopped")
        return {"FINISHED"}


class POCKETCAM_OT_recenter_pose(bpy.types.Operator):
    bl_idname = "pocketcam.recenter_pose"
    bl_label = "Recenter"
    bl_description = "Use the current incoming PocketCam pose as the local origin"

    def execute(self, context):
        global _position_origin_xyz, _camera_origin_location, _rotation_origin_inverse, _camera_origin_rotation

        pose = STATE.get_latest_pose()
        if pose is None:
            self.report({"WARNING"}, "No incoming pose is available to recenter")
            return {"CANCELLED"}

        camera = _select_camera(context)
        if camera is None:
            STATE.set_last_error("No active scene camera found")
            self.report({"ERROR"}, "No active scene camera found")
            return {"CANCELLED"}

        if pose.position_xyz is not None:
            _position_origin_xyz = to_blender_position_xyz(pose.position_xyz)
            _camera_origin_location = tuple(camera.location)

        rotation = Quaternion(to_blender_quaternion_wxyz(pose.rotation_xyzw))
        rotation.normalize()
        _rotation_origin_inverse = rotation.inverted()
        _camera_origin_rotation = _camera_rotation_quaternion(camera)
        _camera_origin_rotation.normalize()

        self.report({"INFO"}, "PocketCam pose origin recentered")
        return {"FINISHED"}


def _camera_rotation_quaternion(camera) -> Quaternion:
    if camera.rotation_mode == "QUATERNION":
        return camera.rotation_quaternion.copy()
    return camera.rotation_euler.to_quaternion()


CLASSES = (
    POCKETCAM_OT_start_server,
    POCKETCAM_OT_stop_server,
    POCKETCAM_OT_recenter_pose,
)


def register():
    bpy.types.Scene.pocketcam_position_scale = bpy.props.FloatProperty(
        name="Position Scale",
        description="Scale incoming PocketCam position before applying it to the active camera",
        default=1.0,
        min=0.01,
        max=100.0,
    )

    for cls in CLASSES:
        bpy.utils.register_class(cls)


def unregister():
    _stop_receiver()
    if bpy.app.timers.is_registered(_apply_latest_pose):
        bpy.app.timers.unregister(_apply_latest_pose)

    for cls in reversed(CLASSES):
        bpy.utils.unregister_class(cls)

    if hasattr(bpy.types.Scene, "pocketcam_position_scale"):
        del bpy.types.Scene.pocketcam_position_scale

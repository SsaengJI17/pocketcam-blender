from typing import Optional

import bpy
from mathutils import Quaternion

from .coordinate import to_blender_quaternion_wxyz
from .receiver import UDPReceiver
from .state import STATE


DEFAULT_HOST = "0.0.0.0"
DEFAULT_PORT = 8765
TIMER_INTERVAL_SECONDS = 1.0 / 60.0

_receiver: Optional[UDPReceiver] = None


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
    camera.rotation_mode = "QUATERNION"
    camera.rotation_quaternion = quaternion
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


CLASSES = (
    POCKETCAM_OT_start_server,
    POCKETCAM_OT_stop_server,
)


def register():
    for cls in CLASSES:
        bpy.utils.register_class(cls)


def unregister():
    _stop_receiver()
    if bpy.app.timers.is_registered(_apply_latest_pose):
        bpy.app.timers.unregister(_apply_latest_pose)

    for cls in reversed(CLASSES):
        bpy.utils.unregister_class(cls)

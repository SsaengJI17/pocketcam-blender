import bpy

from .state import STATE


class POCKETCAM_PT_receiver(bpy.types.Panel):
    bl_label = "PocketCam Receiver"
    bl_idname = "POCKETCAM_PT_receiver"
    bl_space_type = "VIEW_3D"
    bl_region_type = "UI"
    bl_category = "PocketCam"

    def draw(self, context):
        layout = self.layout
        snapshot = STATE.snapshot()

        row = layout.row(align=True)
        start = row.row(align=True)
        start.enabled = not snapshot.server_running
        start.operator("pocketcam.start_server", text="Start Server", icon="PLAY")

        stop = row.row(align=True)
        stop.enabled = snapshot.server_running
        stop.operator("pocketcam.stop_server", text="Stop Server", icon="PAUSE")

        status = "Running" if snapshot.server_running else "Stopped"
        layout.label(text=f"Status: {status}")
        layout.label(text=f"UDP: {snapshot.host}:{snapshot.port}")
        layout.label(text=f"Valid packets: {snapshot.valid_packets}")
        layout.label(text=f"Invalid packets: {snapshot.invalid_packets}")

        layout.separator()
        layout.prop(context.scene, "pocketcam_position_scale")
        layout.operator("pocketcam.recenter_pose", text="Recenter", icon="EMPTY_ARROWS")

        if snapshot.latest_pose is not None:
            if snapshot.latest_pose.mode:
                layout.label(text=f"Mode: {snapshot.latest_pose.mode}")
            if snapshot.latest_pose.tracking:
                layout.label(text=f"Tracking: {snapshot.latest_pose.tracking}")

            x, y, z, w = snapshot.latest_pose.rotation_xyzw
            layout.label(text=f"Rotation: [{x:.3f}, {y:.3f}, {z:.3f}, {w:.3f}]")
            if snapshot.latest_pose.position_xyz is not None:
                px, py, pz = snapshot.latest_pose.position_xyz
                layout.label(text=f"Position: [{px:.3f}, {py:.3f}, {pz:.3f}]")

        if snapshot.last_error:
            layout.label(text=f"Last issue: {snapshot.last_error}", icon="ERROR")


CLASSES = (POCKETCAM_PT_receiver,)


def register():
    for cls in CLASSES:
        bpy.utils.register_class(cls)


def unregister():
    for cls in reversed(CLASSES):
        bpy.utils.unregister_class(cls)

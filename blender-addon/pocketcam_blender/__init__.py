bl_info = {
    "name": "PocketCam Blender Receiver",
    "author": "PocketCam Blender contributors",
    "version": (0, 1, 0),
    "blender": (4, 0, 0),
    "location": "View3D > Sidebar > PocketCam",
    "description": "Receive PocketCam protocol v1 UDP pose packets and apply rotation to the active camera.",
    "category": "Camera",
}

_operators = None
_ui = None


def register():
    global _operators, _ui

    from . import operators
    from . import ui

    _operators = operators
    _ui = ui

    operators.register()
    ui.register()


def unregister():
    if _ui is not None:
        _ui.unregister()
    if _operators is not None:
        _operators.unregister()

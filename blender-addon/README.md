# PocketCam Blender Add-on

This folder contains the Version 0.1.0 Blender Receiver MVP.

## What it does

- Starts and stops a UDP server on port `8765`.
- Receives UTF-8 JSON pose packets using protocol v1.
- Parses `rotation` values in `[x, y, z, w]` quaternion order.
- Stores only the latest valid pose.
- Applies the latest quaternion to the active Blender camera.
- Adds Start Server and Stop Server controls to the 3D View sidebar.

For this first MVP, incoming quaternions are treated as Blender-space rotations. Android Rotation Vector Sensor and ARCore coordinate conversion will be added later.

## Install for development

1. Open Blender 4.x.
2. Copy `blender-addon/pocketcam_blender` into Blender's user add-ons directory.
   - Windows example: `%APPDATA%\Blender Foundation\Blender\4.0\scripts\addons\pocketcam_blender`
3. In Blender, open `Edit > Preferences > Add-ons`.
4. Search for `PocketCam Blender Receiver`.
5. Enable the add-on.

## Test with the local sender

1. Open a Blender scene with a camera.
2. In the 3D View, open the sidebar with `N`.
3. Open the `PocketCam` tab.
4. Click `Start Server`.
5. From the repository root, run:

   ```bash
   python scripts/send_test_pose.py --host 127.0.0.1 --port 8765 --duration 10
   ```

The active scene camera should rotate while packets are being sent.

To send a single identity rotation packet:

```bash
python scripts/send_test_pose.py --once
```

## Troubleshooting

- If the server fails to start, check whether another process is already using UDP port `8765`.
- If the packet counter increases but the camera does not move, make sure the scene has an active camera.
- If invalid packets increase, check that the sender is emitting UTF-8 JSON with `type`, `version`, and `rotation`.

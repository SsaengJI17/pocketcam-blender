# PocketCam Blender Add-on

This folder contains the Blender Receiver MVP.

## What it does

- Starts and stops a UDP server on port `8765`.
- Receives UTF-8 JSON pose packets using protocol v1.
- Parses `rotation` values in `[x, y, z, w]` quaternion order.
- Parses optional `position` values in `[x, y, z]` order.
- Stores only the latest valid pose.
- Applies the latest quaternion to the active Blender camera.
- Applies incoming position to the active Blender camera when present.
- Exposes a `Position Scale` setting from `0.01` to `100.0`.
- Adds a `Recenter` button that uses the current incoming pose as the local origin.
- Adds Start Server and Stop Server controls to the 3D View sidebar.

For the 6DoF MVP, incoming ARCore positions use a minimal conversion from ARCore/OpenGL-style `[x, y, z]` to Blender `[x, -z, y]`. Incoming quaternions are still treated as Blender-space rotations. TODO comments in the code mark the coordinate conversion points that need refinement after real device calibration.

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

## ARCore 6DoF controls

1. Start the PocketCam UDP server from the sidebar.
2. Run the Android app in `ARCore 6DoF mode`.
3. Set `Position Scale` to control how far ARCore meter-scale movement moves the Blender camera.
   - Default: `1.0`
   - Range: `0.01` to `100.0`
4. Click `Recenter` after ARCore tracking begins to treat the current incoming pose as the local origin.

Rotation-only packets continue to work as before. The add-on applies position only when a packet includes `position`.

## Troubleshooting

- If the server fails to start, check whether another process is already using UDP port `8765`.
- If the packet counter increases but the camera does not move, make sure the scene has an active camera.
- If invalid packets increase, check that the sender is emitting UTF-8 JSON with `type`, `version`, and `rotation`.
- If ARCore position feels too large or too small, adjust `Position Scale` and click `Recenter` again.

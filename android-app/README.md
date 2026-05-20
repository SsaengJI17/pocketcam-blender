# PocketCam Android Rotation Sender

This folder contains the Version 0.2.0 Android Rotation Sender MVP.

## What it does

- Reads the Android Rotation Vector Sensor.
- Converts sensor quaternion data to PocketCam protocol v1 rotation order: `[x, y, z, w]`.
- Sends UTF-8 JSON pose packets over UDP.
- Defaults to host `127.0.0.1` and port `8765`.
- Provides editable host and port fields.
- Shows sensor availability, sending state, packets sent, and the last error.

This MVP does not include ARCore, 6DoF position tracking, binary protocol support, smoothing, recording, or take management.

## Build

1. Open `android-app/` in Android Studio.
2. Let Android Studio sync the Gradle project.
3. Connect an Android device with Developer Options and USB debugging enabled.
4. Run the `app` configuration.

The project is a native Android app written in Kotlin and uses only Android platform APIs for the UI, sensor tracking, JSON, and UDP networking.

## Blender receiver setup

1. Open Blender and enable the PocketCam Blender Receiver add-on.
2. Open the 3D View sidebar with `N`.
3. Open the `PocketCam` tab.
4. Click `Start Server`.

## USB debugging and ADB reverse

Enable USB debugging on the Android device, then run:

```bash
adb reverse tcp:8765 tcp:8765
```

The app defaults to `127.0.0.1:8765` for the intended USB reverse workflow.

Important: this MVP currently sends UDP packets, while `adb reverse` forwards TCP ports. If UDP packets do not reach Blender over USB, use Wi-Fi/LAN testing by setting the app host field to the computer's local IP address, or add a TCP transport in a later iteration.

## Wi-Fi/LAN test path

1. Put the Android device and computer on the same network.
2. Start the Blender UDP receiver.
3. Set the app host field to the computer's local IP address.
4. Keep the port as `8765`.
5. Tap `Start Sending`.

The packet counter should increase in the app, and the Blender receiver should show incoming valid packets.

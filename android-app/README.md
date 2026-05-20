# PocketCam Android Sender

This folder contains the Android sender MVP with Rotation Sensor mode and ARCore 6DoF mode.

## What it does

- Reads the Android Rotation Vector Sensor.
- Converts sensor quaternion data to PocketCam protocol v1 rotation order: `[x, y, z, w]`.
- Starts an ARCore session in 6DoF mode and reads the ARCore camera pose each frame.
- Sends ARCore pose packets with `mode: "arcore"`, `position`, `rotation`, and `tracking`.
- Sends UTF-8 JSON pose packets over UDP.
- Defaults to host `127.0.0.1` and port `8765`.
- Provides editable host and port fields.
- Provides a mode selector for Rotation Sensor mode and ARCore 6DoF mode.
- Shows sensor availability, ARCore availability, tracking state, sending state, packets sent, and the last error.

This MVP does not include binary protocol support, TCP/WebSocket transport, smoothing, recording, or take management.

## Build

1. Open `android-app/` in Android Studio.
2. Let Android Studio sync the Gradle project.
3. Connect an Android device with Developer Options and USB debugging enabled.
4. Run the `app` configuration.

The project is a native Android app written in Kotlin. It uses Android platform APIs for the UI, sensor tracking, JSON, and UDP networking, plus the Google ARCore Android SDK for 6DoF tracking.

The ARCore dependency is declared in `app/build.gradle.kts`:

```kotlin
implementation("com.google.ar:core:1.54.0")
```

`gradle.properties` enables AndroidX because the ARCore SDK depends on `androidx.annotation`.

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
5. Select `Rotation Sensor mode` or `ARCore 6DoF mode`.
6. Tap `Start Sending`.

The packet counter should increase in the app, and the Blender receiver should show incoming valid packets.

## ARCore 6DoF mode

1. Use an ARCore-supported Android device.
2. Install or update Google Play Services for AR if the app prompts for it.
3. Grant camera permission when prompted.
4. Select `ARCore 6DoF mode`.
5. Start the Blender receiver and set the app host/port.
6. Tap `Start Sending`.

In ARCore mode, packets include:

```json
{
  "type": "pose",
  "version": 1,
  "timestamp": 1730000000.123,
  "mode": "arcore",
  "position": [0.12, 1.45, -0.8],
  "rotation": [0.01, 0.72, 0.02, 0.69],
  "tracking": "normal"
}
```

The tracking state maps ARCore `TRACKING` to `normal`, `PAUSED` to `limited`, and `STOPPED` to `lost`.

## Known limitations

- Physical ARCore runtime behavior must be tested on a supported device.
- The current sender still uses UDP, so `adb reverse tcp:8765 tcp:8765` does not forward these packets.
- Rotation coordinate conversion remains minimal until real device calibration is completed.

# Protocol Specification

## Version

Current protocol version: 1

## Transport

Supported transports:

- UDP (recommended for initial MVP)
- TCP
- WebSocket

The transport should deliver UTF-8 JSON objects.

## Pose packet

```json
{
  "type": "pose",
  "version": 1,
  "timestamp": 1730000000.123,
  "mode": "rotation",
  "position": [0.0, 0.0, 0.0],
  "rotation": [0.0, 0.0, 0.0, 1.0],
  "fov": 50.0,
  "tracking": "normal"
}
```

## Required fields

- `type`
- `version`
- `rotation`

## Optional fields

- `timestamp`
- `mode`
- `position`
- `fov`
- `tracking`

## Quaternion format

```text
[x, y, z, w]
```

## Modes

- `rotation`
- `arcore`

## Tracking states

- `normal`
- `limited`
- `lost`

## Coordinate systems

### Android / ARCore

Typical basis:

- X: right
- Y: up
- Z: forward/back depending on API convention

### Blender

- X: right
- Y: forward
- Z: up

The Blender add-on must convert the incoming basis to Blender coordinates.

## Design rules

- Unknown fields must be ignored.
- Missing optional fields must not cause failure.
- Invalid packets should be skipped silently or logged.
- The latest valid pose replaces older poses.

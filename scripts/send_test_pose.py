#!/usr/bin/env python3
import argparse
import json
import math
import socket
import time
from typing import List


def make_rotation_z(angle_radians: float) -> List[float]:
    half_angle = angle_radians * 0.5
    return [0.0, 0.0, math.sin(half_angle), math.cos(half_angle)]


def send_packet(sock: socket.socket, host: str, port: int, rotation: List[float]) -> None:
    packet = {
        "type": "pose",
        "version": 1,
        "timestamp": time.time(),
        "mode": "rotation",
        "rotation": rotation,
        "tracking": "normal",
    }
    data = json.dumps(packet, separators=(",", ":")).encode("utf-8")
    sock.sendto(data, (host, port))


def main() -> None:
    parser = argparse.ArgumentParser(description="Send PocketCam protocol v1 UDP test pose packets.")
    parser.add_argument("--host", default="127.0.0.1", help="Receiver host. Default: 127.0.0.1")
    parser.add_argument("--port", type=int, default=8765, help="Receiver UDP port. Default: 8765")
    parser.add_argument("--hz", type=float, default=30.0, help="Packets per second. Default: 30")
    parser.add_argument("--duration", type=float, default=10.0, help="Seconds to send. Use 0 for forever.")
    parser.add_argument("--once", action="store_true", help="Send one identity rotation packet and exit.")
    args = parser.parse_args()

    if args.hz <= 0:
        raise SystemExit("--hz must be greater than 0")

    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as sock:
        if args.once:
            send_packet(sock, args.host, args.port, [0.0, 0.0, 0.0, 1.0])
            print(f"Sent one packet to {args.host}:{args.port}")
            return

        start = time.monotonic()
        delay = 1.0 / args.hz
        sent = 0

        try:
            while args.duration <= 0 or time.monotonic() - start < args.duration:
                elapsed = time.monotonic() - start
                rotation = make_rotation_z(elapsed)
                send_packet(sock, args.host, args.port, rotation)
                sent += 1
                time.sleep(delay)
        except KeyboardInterrupt:
            pass

    print(f"Sent {sent} packets to {args.host}:{args.port}")


if __name__ == "__main__":
    main()

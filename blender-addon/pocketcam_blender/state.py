import threading
import time
from dataclasses import dataclass
from typing import Optional, Tuple


RotationXYZW = Tuple[float, float, float, float]


@dataclass(frozen=True)
class Pose:
    rotation_xyzw: RotationXYZW
    received_at: float
    timestamp: Optional[float]
    mode: Optional[str]
    tracking: Optional[str]


@dataclass(frozen=True)
class StateSnapshot:
    server_running: bool
    host: str
    port: int
    valid_packets: int
    invalid_packets: int
    last_error: Optional[str]
    latest_pose: Optional[Pose]


class PoseState:
    def __init__(self) -> None:
        self._lock = threading.Lock()
        self._latest_pose: Optional[Pose] = None
        self._valid_packets = 0
        self._invalid_packets = 0
        self._last_error: Optional[str] = None
        self._server_running = False
        self._host = "0.0.0.0"
        self._port = 8765

    def set_server_running(self, running: bool, host: str, port: int) -> None:
        with self._lock:
            self._server_running = running
            self._host = host
            self._port = port
            if running:
                self._last_error = None

    def is_server_running(self) -> bool:
        with self._lock:
            return self._server_running

    def set_latest_pose(
        self,
        rotation_xyzw: RotationXYZW,
        timestamp: Optional[float],
        mode: Optional[str],
        tracking: Optional[str],
    ) -> None:
        with self._lock:
            self._latest_pose = Pose(
                rotation_xyzw=rotation_xyzw,
                received_at=time.time(),
                timestamp=timestamp,
                mode=mode,
                tracking=tracking,
            )
            self._valid_packets += 1
            self._last_error = None

    def get_latest_pose(self) -> Optional[Pose]:
        with self._lock:
            return self._latest_pose

    def mark_invalid(self, reason: str) -> None:
        with self._lock:
            self._invalid_packets += 1
            self._last_error = reason

    def set_last_error(self, reason: str) -> None:
        with self._lock:
            self._last_error = reason

    def snapshot(self) -> StateSnapshot:
        with self._lock:
            return StateSnapshot(
                server_running=self._server_running,
                host=self._host,
                port=self._port,
                valid_packets=self._valid_packets,
                invalid_packets=self._invalid_packets,
                last_error=self._last_error,
                latest_pose=self._latest_pose,
            )


STATE = PoseState()

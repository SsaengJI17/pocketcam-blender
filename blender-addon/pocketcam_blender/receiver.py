import json
import socket
import threading
from typing import Optional

from .coordinate import PosePacketError, parse_pose_packet
from .state import PoseState


class UDPReceiver:
    def __init__(self, state: PoseState, host: str = "0.0.0.0", port: int = 8765) -> None:
        self._state = state
        self._host = host
        self._port = port
        self._socket: Optional[socket.socket] = None
        self._thread: Optional[threading.Thread] = None
        self._stop_event = threading.Event()

    def start(self) -> None:
        if self._thread is not None and self._thread.is_alive():
            return

        udp_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        udp_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        udp_socket.settimeout(0.25)
        udp_socket.bind((self._host, self._port))

        self._socket = udp_socket
        self._stop_event.clear()
        self._state.set_server_running(True, self._host, self._port)

        self._thread = threading.Thread(target=self._run, name="PocketCamUDPReceiver", daemon=True)
        self._thread.start()

    def stop(self) -> None:
        self._stop_event.set()

        if self._socket is not None:
            try:
                self._socket.close()
            except OSError:
                pass
            self._socket = None

        if self._thread is not None:
            self._thread.join(timeout=1.0)
            self._thread = None

        self._state.set_server_running(False, self._host, self._port)

    def _run(self) -> None:
        while not self._stop_event.is_set():
            try:
                assert self._socket is not None
                data, _address = self._socket.recvfrom(65535)
            except socket.timeout:
                continue
            except OSError:
                break

            self._handle_packet(data)

        self._state.set_server_running(False, self._host, self._port)

    def _handle_packet(self, data: bytes) -> None:
        try:
            text = data.decode("utf-8")
            packet = json.loads(text)
            rotation_xyzw, position_xyz, timestamp, mode, tracking = parse_pose_packet(packet)
        except UnicodeDecodeError:
            self._state.mark_invalid("packet is not valid UTF-8")
            return
        except json.JSONDecodeError:
            self._state.mark_invalid("packet is not valid JSON")
            return
        except PosePacketError as exc:
            self._state.mark_invalid(str(exc))
            return

        self._state.set_latest_pose(
            rotation_xyzw,
            position_xyz=position_xyz,
            timestamp=timestamp,
            mode=mode,
            tracking=tracking,
        )

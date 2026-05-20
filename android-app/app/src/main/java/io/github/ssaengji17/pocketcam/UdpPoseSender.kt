package io.github.ssaengji17.pocketcam

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class UdpPoseSender(
    private val onPacketSent: () -> Unit,
    private val onError: (String) -> Unit,
) {
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val sending = AtomicBoolean(false)
    private val sendQueued = AtomicBoolean(false)

    @Volatile
    private var targetHost = "127.0.0.1"

    @Volatile
    private var targetPort = 8765

    val isSending: Boolean
        get() = sending.get()

    fun start(host: String, port: Int) {
        targetHost = host
        targetPort = port
        sending.set(true)
    }

    fun stop() {
        sending.set(false)
    }

    fun close() {
        stop()
        executor.shutdownNow()
    }

    fun send(rotation: RotationQuaternion) {
        send(
            PosePacket(
                mode = "rotation",
                rotation = rotation,
                tracking = "normal",
            ),
        )
    }

    fun send(packet: PosePacket) {
        if (!sending.get()) {
            return
        }
        if (!sendQueued.compareAndSet(false, true)) {
            return
        }

        val host = targetHost
        val port = targetPort
        val payload = packet.toPoseJson(System.currentTimeMillis() / 1000.0).toByteArray(Charsets.UTF_8)

        executor.execute {
            if (!sending.get()) {
                return@execute
            }

            try {
                DatagramSocket().use { socket ->
                    val address = InetAddress.getByName(host)
                    val packet = DatagramPacket(payload, payload.size, address, port)
                    socket.send(packet)
                }
                onPacketSent()
            } catch (exception: Exception) {
                onError(exception.message ?: exception.javaClass.simpleName)
            } finally {
                sendQueued.set(false)
            }
        }
    }
}

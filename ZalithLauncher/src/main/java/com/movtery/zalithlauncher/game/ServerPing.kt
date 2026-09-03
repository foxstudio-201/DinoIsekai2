package com.movtery.zalithlauncher.game

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.net.SocketTimeoutException

data class PingResult(
    val online: Boolean,
    val ping: Long = 0,
    val players: Int = 0,
    val maxPlayers: Int = 0,
    val version: String = "",
    val motd: String = "",
    val error: String? = null
)

object ServerPing {
    suspend fun ping(host: String, port: Int, timeoutMs: Long = 8000): PingResult = withContext(Dispatchers.IO) {
        var socket: Socket? = null
        try {
            socket = Socket()
            socket.soTimeout = timeoutMs.toInt()
            socket.tcpNoDelay = true
            socket.connect(java.net.InetSocketAddress(host, port), timeoutMs.toInt())
            val input = DataInputStream(socket.getInputStream())
            val output = DataOutputStream(socket.getOutputStream())

            // Handshake packet: protocol -1, host, port, next state 1
            val handshake = ByteArrayOutputStream()
            val hs = DataOutputStream(handshake)
            writeVarInt(hs, 0x00)
            writeVarInt(hs, -1)
            writeString(hs, host)
            hs.writeShort(port)
            writeVarInt(hs, 1)
            hs.flush()
            writePacket(output, handshake.toByteArray())

            // Status request packet
            val request = ByteArrayOutputStream()
            val rq = DataOutputStream(request)
            writeVarInt(rq, 0x00)
            rq.flush()
            writePacket(output, request.toByteArray())
            output.flush()

            // Read response: varint len, then packet
            val statusJson = readStatusResponse(input) ?: return@withContext PingResult(online = false, error = "no status response")

            val players = extractJsonInt(statusJson, "online")
            val maxPlayers = extractJsonInt(statusJson, "max")
            val version = extractJsonString(statusJson, "version", "name")

            // Send ping packet to measure latency — if this fails we're still online
            var latency = 0L
            try {
                val pingSent = System.currentTimeMillis()
                val pingPacket = ByteArrayOutputStream()
                val pp = DataOutputStream(pingPacket)
                writeVarInt(pp, 0x01)
                pp.writeLong(pingSent)
                pp.flush()
                writePacket(output, pingPacket.toByteArray())
                output.flush()

                val responseLen = readVarInt(input)
                if (responseLen != null && responseLen in 0..65535) {
                    val payload = ByteArray(responseLen)
                    input.readFully(payload)
                    latency = System.currentTimeMillis() - pingSent
                }
            } catch (_: Exception) { latency = 0L }

            return@withContext PingResult(
                online = true,
                ping = latency,
                players = players,
                maxPlayers = maxPlayers,
                version = version
            )
        } catch (e: SocketTimeoutException) {
            return@withContext PingResult(online = false, error = "timeout")
        } catch (e: Exception) {
            return@withContext PingResult(online = false, error = e.message ?: "error")
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    private fun readStatusResponse(input: DataInputStream): String? {
        val responseLen = readVarInt(input) ?: return null
        if (responseLen < 0 || responseLen > 1048576) return null
        val payload = ByteArray(responseLen)
        input.readFully(payload)
        val in2 = DataInputStream(payload.inputStream())
        val packetId = readVarInt(in2) ?: return null
        if (packetId != 0x00) return null
        val jsonLen = readVarInt(in2) ?: return null
        if (jsonLen < 0 || jsonLen > 1048576) return null
        val jsonBytes = ByteArray(jsonLen)
        in2.readFully(jsonBytes)
        return String(jsonBytes, Charsets.UTF_8)
    }

    private fun writePacket(output: DataOutputStream, payload: ByteArray) {
        writeVarInt(output, payload.size)
        output.write(payload)
    }

    private fun writeVarInt(out: DataOutputStream, value: Int) {
        var v = value
        while (true) {
            if ((v and 0x7FFFFFFF.inv()) == 0) {
                out.writeByte(v)
                return
            }
            out.writeByte((v and 0x7F) or 0x80)
            v = v ushr 7
        }
    }

    private fun readVarInt(input: DataInputStream): Int? {
        var result = 0
        var shift = 0
        while (shift <= 35) {
            val b = input.readByte().toInt() and 0xFF
            result = result or ((b and 0x7F) shl shift)
            shift += 7
            if ((b and 0x80) == 0) return result
        }
        return null
    }

    private fun writeString(out: DataOutputStream, s: String) {
        val bytes = s.toByteArray(Charsets.UTF_8)
        writeVarInt(out, bytes.size)
        out.write(bytes)
    }

    private fun extractJsonInt(json: String, key: String): Int {
        try {
            val idx = json.indexOf("\"$key\"")
            if (idx == -1) return 0
            val colon = json.indexOf(':', idx + key.length + 2)
            if (colon == -1) return 0
            val num = json.substring(colon + 1).trim().takeWhile { it.isDigit() || it == '-' }
            return num.toIntOrNull() ?: 0
        } catch (_: Exception) { return 0 }
    }

    private fun extractJsonString(json: String, obj: String, key: String): String {
        try {
            val objIdx = json.indexOf("\"$obj\"")
            if (objIdx == -1) return ""
            val keyIdx = json.indexOf("\"$key\"", objIdx)
            if (keyIdx == -1) return ""
            val colon = json.indexOf(':', keyIdx + key.length + 2)
            if (colon == -1) return ""
            val start = json.indexOf('"', colon + 1)
            if (start == -1) return ""
            val end = json.indexOf('"', start + 1)
            if (end == -1) return ""
            return json.substring(start + 1, end)
        } catch (_: Exception) { return "" }
    }
}
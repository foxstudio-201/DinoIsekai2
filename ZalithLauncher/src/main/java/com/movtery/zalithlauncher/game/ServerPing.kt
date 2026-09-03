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
    suspend fun ping(host: String, port: Int, timeoutMs: Long = 5000): PingResult = withContext(Dispatchers.IO) {
        var socket: Socket? = null
        try {
            socket = Socket()
            socket.soTimeout = timeoutMs.toInt()
            val start = System.currentTimeMillis()
            socket.connect(java.net.InetSocketAddress(host, port), timeoutMs.toInt())
            val `in` = DataInputStream(socket.getInputStream())
            val out = DataOutputStream(socket.getOutputStream())

            writeVarInt(out, 0x00)
            writeVarInt(out, -1)
            writeString(out, host)
            out.writeShort(port)
            writeVarInt(out, 1)
            flushPacket(out)

            writeVarInt(out, 0x00)
            flushPacket(out)

            val len = readVarInt(`in`)
            if (len == null) {
                return@withContext PingResult(online = false, error = "empty response")
            }
            val packetId = readVarInt(`in`)
            if (packetId != 0x00) {
                return@withContext PingResult(online = false, error = "unexpected packet")
            }
            val jsonLen = readVarInt(`in`) ?: return@withContext PingResult(online = false, error = "no json")
            val jsonBytes = ByteArray(jsonLen)
            `in`.readFully(jsonBytes)
            val json = String(jsonBytes, Charsets.UTF_8)

            val pingTime = System.currentTimeMillis()
            writeVarInt(out, 0x01)
            out.writeLong(pingTime)
            flushPacket(out)

            val pongLen = readVarInt(`in`)
            if (pongLen == null) {
                return@withContext PingResult(online = false, error = "no pong")
            }
            val pongId = readVarInt(`in`)
            val pongTime = `in`.readLong()
            val latency = System.currentTimeMillis() - pingTime

            val players = extractInt(json, "online")
            val maxPlayers = extractInt(json, "max")
            val version = extractString(json, "version", "name")

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

    private fun readVarInt(`in`: DataInputStream): Int? {
        var result = 0
        var shift = 0
        while (shift <= 35) {
            val b = `in`.readByte().toInt() and 0xFF
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

    private val packetBuf = ThreadLocal.withInitial { ByteArrayOutputStream() }
    private fun flushPacket(out: DataOutputStream) {
        out.flush()
    }

    private fun extractInt(json: String, vararg path: String): Int {
        try {
            var pos = 0
            for (p in path) {
                val idx = json.indexOf("\"$p\"", pos)
                if (idx == -1) return 0
                pos = idx + p.length + 2
            }
            val colon = json.indexOf(':', pos)
            if (colon == -1) return 0
            val num = json.substring(colon + 1).trim().takeWhile { it.isDigit() || it == '-' }
            return num.toIntOrNull() ?: 0
        } catch (_: Exception) { return 0 }
    }

    private fun extractString(json: String, obj: String, key: String): String {
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
package com.example.famekodriver.core.network

import android.util.Base64
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.util.zip.CRC32
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object AgoraTokenBuilder {
    const val APP_ID = "38144168c418426faf0ee6a02c9b46e4"
    const val APP_CERTIFICATE = "eac438ace99e45b6971e6d072330013f"

    fun buildToken(channelName: String, uid: Int = 0, expirationTimeInSeconds: Int = 86400): String {
        return try {
            val timestamp = (System.currentTimeMillis() / 1000 + expirationTimeInSeconds).toInt()
            val salt = (10000000..99999999).random()

            val digest = hmacSign(APP_CERTIFICATE, "$APP_ID$channelName$uid$timestamp$salt".toByteArray())
            val tokenBase = "$APP_ID:$channelName:$uid:$timestamp:$salt:${bytesToHex(digest)}"

            Base64.encodeToString(tokenBase.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            ""
        }
    }

    private fun hmacSign(key: String, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(key.toByteArray(), "HmacSHA256")
        mac.init(secretKey)
        return mac.doFinal(data)
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }
}

package com.example.famekodriver.backend.utils

import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object AgoraTokenBuilder {
    val appId: String = System.getenv("AGORA_APP_ID") ?: "38144168c418426faf0ee6a02c9b46e4"
    val appCertificate: String = System.getenv("AGORA_APP_CERTIFICATE") ?: "eac438ace99e45b6971e6d072330013f"

    fun buildToken(channelName: String, uid: Int = 0, expirationTimeInSeconds: Int = 86400): String {
        return try {
            val timestamp = (System.currentTimeMillis() / 1000 + expirationTimeInSeconds).toInt()
            val salt = (10000000..99999999).random()

            val digest = hmacSign(appCertificate, "$appId$channelName$uid$timestamp$salt".toByteArray())
            val tokenBase = "$appId:$channelName:$uid:$timestamp:$salt:${bytesToHex(digest)}"

            Base64.getEncoder().encodeToString(tokenBase.toByteArray())
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

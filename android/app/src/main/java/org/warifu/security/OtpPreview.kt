package org.warifu.security

import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

object OtpPreview {
    private const val STEAM_CHARSET = "23456789BCDFGHJKMNPQRTVWXY"

    /**
     * Generates a live OTP code preview from a Base32 secret string.
     * Returns null if the secret is invalid or empty.
     */
    fun generateLivePreview(
        secretBase32: String,
        isSteam: Boolean = false,
        period: Int = 30,
        digits: Int = 6,
        algorithm: String = "SHA1"
    ): String? {
        val cleanSecret = secretBase32.replace(" ", "").replace("-", "").uppercase()
        val secretBytes = decodeBase32(cleanSecret) ?: return null
        val nowSeconds = System.currentTimeMillis() / 1000L

        return if (isSteam) {
            generateSteamCode(secretBytes, nowSeconds)
        } else {
            generateTotpCode(secretBytes, nowSeconds, period, digits, algorithm)
        }
    }

    fun generateSteamCode(secretBytes: ByteArray, timestampSeconds: Long): String {
        val counter = timestampSeconds / 30L
        val counterBytes = ByteBuffer.allocate(8).putLong(counter).array()

        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(secretBytes, "HmacSHA1"))
        val hash = mac.doFinal(counterBytes)

        val offset = (hash[hash.size - 1].toInt() and 0x0F)
        var binary = ((hash[offset].toInt() and 0x7F) shl 24) or
                ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                (hash[offset + 3].toInt() and 0xFF)

        val code = StringBuilder(5)
        for (i in 0 until 5) {
            val index = binary % 26
            code.append(STEAM_CHARSET[index])
            binary /= 26
        }
        return code.toString()
    }

    private fun generateTotpCode(
        secretBytes: ByteArray,
        timestampSeconds: Long,
        period: Int,
        digits: Int,
        algorithm: String
    ): String {
        val p = if (period <= 0) 30 else period
        val counter = timestampSeconds / p
        val counterBytes = ByteBuffer.allocate(8).putLong(counter).array()

        val hmacAlgo = when (algorithm.uppercase()) {
            "SHA256" -> "HmacSHA256"
            "SHA512" -> "HmacSHA512"
            else -> "HmacSHA1"
        }

        val mac = Mac.getInstance(hmacAlgo)
        mac.init(SecretKeySpec(secretBytes, hmacAlgo))
        val hash = mac.doFinal(counterBytes)

        val offset = (hash[hash.size - 1].toInt() and 0x0F)
        val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
                ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                (hash[offset + 3].toInt() and 0xFF)

        val modulo = 10.0.pow(digits.toDouble()).toInt()
        val token = binary % modulo
        return token.toString().padStart(digits, '0')
    }

    fun decodeBase32(input: String): ByteArray? {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val clean = input.trim().trimEnd('=').uppercase()
        if (clean.isEmpty()) return null

        var buffer = 0
        var bitsLeft = 0
        val out = mutableListOf<Byte>()

        for (c in clean) {
            val valIndex = alphabet.indexOf(c)
            if (valIndex == -1) return null // Invalid base32 char
            buffer = (buffer shl 5) or valIndex
            bitsLeft += 5
            if (bitsLeft >= 8) {
                bitsLeft -= 8
                out.add(((buffer shr bitsLeft) and 0xFF).toByte())
            }
        }
        return out.toByteArray()
    }

    /**
     * Validates Base32 input and returns error message or null if valid.
     */
    fun validateBase32Secret(input: String): String? {
        val clean = input.replace(" ", "").replace("-", "").uppercase()
        if (clean.isEmpty()) return "Secret key cannot be empty"
        for (c in clean) {
            if (c !in 'A'..'Z' && c !in '2'..'7' && c != '=') {
                return "Invalid character '$c'. Base32 only accepts letters A-Z and digits 2-7."
            }
        }
        if (clean.length < 8) {
            return "Secret key is unusually short (min 8 characters recommended)"
        }
        return null
    }
}

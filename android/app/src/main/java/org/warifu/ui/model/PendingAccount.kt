package org.warifu.ui.model

import android.net.Uri
import android.util.Base64
import java.io.ByteArrayInputStream

/**
 * Account preview item parsed from Google Migration transfer QR codes.
 */
data class MigrationPreviewItem(
    val issuer: String,
    val accountName: String,
    val secret: String,
    val isDuplicate: Boolean = false
)

/**
 * Data structure representing a pending 2FA account awaiting user confirmation and naming.
 */
data class PendingAccount(
    val initialIssuer: String = "",
    val initialAccount: String = "",
    val secret: String = "",
    val tag: String = "",
    val isSteam: Boolean = false,
    val isMigration: Boolean = false,
    val rawMigrationUri: String = "",
    val migrationPreviews: List<MigrationPreviewItem> = emptyList(),
    val period: Int = 30,
    val digits: Int = 6,
    val existingTokenId: String? = null // Set if editing an existing token
) {
    companion object {
        /**
         * Parses raw QR payload, URI, or manual key into a PendingAccount model.
         * Never defaults to hardcoded app names, allowing the user to name the service.
         */
        fun parseFromRaw(rawText: String, existingVaultTokens: List<uniffi.warifu_core.TokenView> = emptyList()): PendingAccount? {
            val trimmed = rawText.trim()
            if (trimmed.isEmpty()) return null

            // 1. Google Migration URI
            if (trimmed.startsWith("otpauth-migration://", ignoreCase = true) ||
                trimmed.startsWith("otpauth-migration:", ignoreCase = true)) {
                val previews = parseGoogleMigrationPayload(trimmed, existingVaultTokens)
                return PendingAccount(
                    isMigration = true,
                    rawMigrationUri = trimmed,
                    migrationPreviews = previews
                )
            }

            // 2. Standard otpauth:// or otpauth://steam/ URI
            if (trimmed.startsWith("otpauth://", ignoreCase = true)) {
                return try {
                    val uri = Uri.parse(trimmed)
                    val queryIssuer = uri.getQueryParameter("issuer").orEmpty().trim()
                    val secret = uri.getQueryParameter("secret").orEmpty().replace(" ", "").replace("-", "").uppercase()
                    val period = uri.getQueryParameter("period")?.toIntOrNull() ?: 30
                    val digits = uri.getQueryParameter("digits")?.toIntOrNull() ?: 6

                    // Parse path: "/Issuer:Account" or "/Account"
                    val path = uri.path?.removePrefix("/").orEmpty().trim()
                    val (pathIssuer, pathAccount) = if (path.contains(":")) {
                        val parts = path.split(":", limit = 2)
                        parts[0].trim() to parts[1].trim()
                    } else {
                        "" to path
                    }

                    val effectiveIssuer = queryIssuer.ifBlank { pathIssuer }
                    val effectiveAccount = pathAccount
                    val isSteam = uri.host?.equals("steam", ignoreCase = true) == true ||
                            effectiveIssuer.contains("Steam", ignoreCase = true)

                    PendingAccount(
                        initialIssuer = effectiveIssuer,
                        initialAccount = effectiveAccount,
                        secret = secret,
                        isSteam = isSteam,
                        period = period,
                        digits = digits
                    )
                } catch (_: Exception) {
                    null
                }
            }

            // 3. Raw Secret Key (Base32)
            val cleanKey = trimmed.replace(" ", "").replace("-", "").uppercase()
            if (cleanKey.matches(Regex("^[A-Z2-7=]+$"))) {
                return PendingAccount(
                    initialIssuer = "",
                    initialAccount = "",
                    secret = cleanKey
                )
            }

            return null
        }

        private fun parseGoogleMigrationPayload(
            uriStr: String,
            existingVaultTokens: List<uniffi.warifu_core.TokenView>
        ): List<MigrationPreviewItem> {
            val list = mutableListOf<MigrationPreviewItem>()
            try {
                val uri = Uri.parse(uriStr)
                val rawData = uri.getQueryParameter("data") ?: return emptyList()
                val payloadBytes = Base64.decode(rawData.replace(' ', '+'), Base64.DEFAULT)

                val stream = ByteArrayInputStream(payloadBytes)
                while (stream.available() > 0) {
                    val tag = readVarint(stream)
                    val fieldNumber = tag shr 3
                    val wireType = tag and 0x07

                    if (wireType == 2) {
                        val length = readVarint(stream)
                        val fieldData = ByteArray(length)
                        stream.read(fieldData)

                        if (fieldNumber == 1) { // OtpParameters
                            val item = parseOtpParameter(fieldData)
                            if (item != null) {
                                val isDup = existingVaultTokens.any {
                                    it.issuer.equals(item.issuer, ignoreCase = true) &&
                                    it.accountName.equals(item.accountName, ignoreCase = true)
                                }
                                list.add(item.copy(isDuplicate = isDup))
                            }
                        }
                    } else if (wireType == 0) {
                        readVarint(stream)
                    } else {
                        break
                    }
                }
            } catch (_: Exception) {}
            return list
        }

        private fun parseOtpParameter(bytes: ByteArray): MigrationPreviewItem? {
            val stream = ByteArrayInputStream(bytes)
            var secret: ByteArray? = null
            var name = ""
            var issuer = ""

            while (stream.available() > 0) {
                val tag = readVarint(stream)
                val fieldNumber = tag shr 3
                val wireType = tag and 0x07

                if (wireType == 2) {
                    val length = readVarint(stream)
                    val data = ByteArray(length)
                    stream.read(data)

                    when (fieldNumber) {
                        1 -> secret = data
                        2 -> name = String(data)
                        3 -> issuer = String(data)
                    }
                } else if (wireType == 0) {
                    readVarint(stream)
                } else {
                    break
                }
            }

            if (secret != null) {
                val (pathIssuer, pathAccount) = if (name.contains(":")) {
                    val parts = name.split(":", limit = 2)
                    parts[0].trim() to parts[1].trim()
                } else {
                    "" to name.trim()
                }
                val finalIssuer = issuer.ifBlank { pathIssuer }.ifBlank { "Account" }
                val finalAccount = pathAccount.ifBlank { name }
                return MigrationPreviewItem(
                    issuer = finalIssuer,
                    accountName = finalAccount,
                    secret = base32Encode(secret)
                )
            }
            return null
        }

        private fun readVarint(stream: ByteArrayInputStream): Int {
            var result = 0
            var shift = 0
            while (true) {
                val b = stream.read()
                if (b == -1) break
                result = result or ((b and 0x7F) shl shift)
                if ((b and 0x80) == 0) break
                shift += 7
            }
            return result
        }

        private fun base32Encode(bytes: ByteArray): String {
            val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
            val out = StringBuilder()
            var buffer = 0
            var bitsLeft = 0
            for (b in bytes) {
                buffer = (buffer shl 8) or (b.toInt() and 0xFF)
                bitsLeft += 8
                while (bitsLeft >= 5) {
                    bitsLeft -= 5
                    out.append(alphabet[(buffer shr bitsLeft) and 0x1F])
                }
            }
            if (bitsLeft > 0) {
                buffer = buffer shl (5 - bitsLeft)
                out.append(alphabet[buffer and 0x1F])
            }
            return out.toString()
        }
    }
}

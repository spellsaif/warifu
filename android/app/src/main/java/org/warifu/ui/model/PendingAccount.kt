package org.warifu.ui.model

import android.net.Uri

/**
 * Data structure representing a pending 2FA account awaiting user confirmation and naming.
 */
data class PendingAccount(
    val initialIssuer: String = "",
    val initialAccount: String = "",
    val secret: String = "",
    val isMigration: Boolean = false,
    val rawMigrationUri: String = "",
    val period: Int = 30,
    val digits: Int = 6,
    val existingTokenId: String? = null // Set if editing an existing token
) {
    companion object {
        /**
         * Parses raw QR payload, URI, or manual key into a PendingAccount model.
         * Never defaults to hardcoded app names, allowing the user to name the service.
         */
        fun parseFromRaw(rawText: String): PendingAccount? {
            val trimmed = rawText.trim()
            if (trimmed.isEmpty()) return null

            // 1. Google Migration URI
            if (trimmed.startsWith("otpauth-migration://", ignoreCase = true) ||
                trimmed.startsWith("otpauth-migration:", ignoreCase = true)) {
                return PendingAccount(
                    isMigration = true,
                    rawMigrationUri = trimmed
                )
            }

            // 2. Standard otpauth:// URI
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

                    PendingAccount(
                        initialIssuer = effectiveIssuer,
                        initialAccount = effectiveAccount,
                        secret = secret,
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
    }
}

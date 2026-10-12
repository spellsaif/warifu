package org.warifu

import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import uniffi.warifu_core.*
import java.io.File

class TokenVectorTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUpNativeLibrary() {
            // Point JNA to the built libwarifu_core.so
            val candidatePaths = listOf(
                File("../../core/target/debug").absolutePath,
                File("../core/target/debug").absolutePath,
                File("core/target/debug").absolutePath,
                File("src/main/jniLibs/x86_64").absolutePath,
                File("app/src/main/jniLibs/x86_64").absolutePath,
                File("../app/src/main/jniLibs/x86_64").absolutePath
            )
            for (p in candidatePaths) {
                val candidateFile = File(p, "libwarifu_core.so")
                if (candidateFile.exists()) {
                    System.setProperty("jna.library.path", File(p).absolutePath)
                    System.setProperty("uniffi.component.warifu_core.libraryOverride", "warifu_core")
                    break
                }
            }
        }
    }

    // RFC 6238 Appendix B Official Test Secrets
    // SHA1: b"12345678901234567890" (20 bytes) -> Base32: GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ
    // SHA256: b"12345678901234567890123456789012" (32 bytes) -> Base32: GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZA====
    // SHA512: 64 bytes -> Base32
    private val RFC6238_SECRET_SHA1_B32 = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"
    private val RFC6238_SECRET_SHA256_B32 = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZA"
    private val RFC6238_SECRET_SHA512_B32 = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNA"

    // RFC 6238 Official Test Vectors Table: (Time, SHA1, SHA256, SHA512)
    // 8 digits, 30s period
    private val RFC6238_VECTORS = listOf(
        Triple(59L, "94287082", Pair("46119246", "90693936")),
        Triple(1111111109L, "07081804", Pair("68084774", "25091201")),
        Triple(1111111111L, "14050471", Pair("67062674", "99943326")),
        Triple(1234567890L, "89005924", Pair("91819424", "93441116")),
        Triple(2000000000L, "69279037", Pair("90698825", "38618901")),
        Triple(20000000000L, "65353130", Pair("77737706", "47863826"))
    )

    // RFC 4226 Appendix D Official Test Vectors
    // ASCII Secret: "12345678901234567890" -> Base32: GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ
    private val RFC4226_SECRET_B32 = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"
    private val RFC4226_VECTORS = listOf(
        0L to "755224",
        1L to "287082",
        2L to "359152",
        3L to "969429",
        4L to "338314",
        5L to "254676",
        6L to "287922",
        7L to "162583",
        8L to "399871",
        9L to "520489"
    )

    @Test
    fun testRfc6238TotpVectorsSha1() {
        for ((time, expectedSha1, _) in RFC6238_VECTORS) {
            val isValid = ffiVerifyCode(
                secretBase32 = RFC6238_SECRET_SHA1_B32,
                token = expectedSha1,
                currentTime = time.toULong(),
                toleranceSteps = 0u,
                period = 30u,
                digits = 8u,
                algorithm = "SHA1"
            )
            assertTrue("RFC 6238 SHA1 code $expectedSha1 should verify at time $time", isValid)
        }
    }

    @Test
    fun testRfc6238TotpVectorsSha256() {
        for ((time, _, pair) in RFC6238_VECTORS) {
            val expectedSha256 = pair.first
            val isValid = ffiVerifyCode(
                secretBase32 = RFC6238_SECRET_SHA256_B32,
                token = expectedSha256,
                currentTime = time.toULong(),
                toleranceSteps = 0u,
                period = 30u,
                digits = 8u,
                algorithm = "SHA256"
            )
            assertTrue("RFC 6238 SHA256 code $expectedSha256 should verify at time $time", isValid)
        }
    }

    @Test
    fun testRfc6238TotpVectorsSha512() {
        for ((time, _, pair) in RFC6238_VECTORS) {
            val expectedSha512 = pair.second
            val isValid = ffiVerifyCode(
                secretBase32 = RFC6238_SECRET_SHA512_B32,
                token = expectedSha512,
                currentTime = time.toULong(),
                toleranceSteps = 0u,
                period = 30u,
                digits = 8u,
                algorithm = "SHA512"
            )
            assertTrue("RFC 6238 SHA512 code $expectedSha512 should verify at time $time", isValid)
        }
    }

    @Test
    fun testRfc4226HotpThroughVaultSession() {
        val tempVault = File.createTempFile("warifu_hotp_test", ".bin")
        try {
            val vaultKey = ffiGenerateVaultKey()
            val session = VaultSession.createNew(tempVault.absolutePath, vaultKey)

            for ((counter, expectedCode) in RFC4226_VECTORS) {
                val uri = "otpauth://hotp/TestCorp:user_$counter?secret=$RFC4226_SECRET_B32&counter=$counter&digits=6&algorithm=SHA1"
                val id = session.addTokenFromUri(uri)
                val tokens = session.listTokens(0u)
                val token = tokens.first { it.id == id }
                assertEquals("HOTP counter $counter mismatch", expectedCode, token.currentCode)
            }
        } finally {
            tempVault.delete()
        }
    }

    @Test
    fun testTotp6DigitsThroughVaultSession() {
        val tempVault = File.createTempFile("warifu_totp_test", ".bin")
        try {
            val vaultKey = ffiGenerateVaultKey()
            val session = VaultSession.createNew(tempVault.absolutePath, vaultKey)

            // At time = 59s, period = 30s -> counter = 1
            // RFC 4226 counter 1 is "287082"
            val uri = "otpauth://totp/GitHub:octocat?secret=$RFC4226_SECRET_B32&digits=6&period=30&algorithm=SHA1"
            val id = session.addTokenFromUri(uri)
            val tokens = session.listTokens(59u)
            val token = tokens.first { it.id == id }
            assertEquals("6-digit TOTP at t=59 must match counter 1 code", "287082", token.currentCode)
        } finally {
            tempVault.delete()
        }
    }

    @Test
    fun testVaultLifecycleLockAndUnlock() {
        val tempVault = File.createTempFile("warifu_lifecycle_test", ".bin")
        try {
            val vaultKey = ffiGenerateVaultKey()
            val session = VaultSession.createNew(tempVault.absolutePath, vaultKey)
            val uri = "otpauth://totp/Google:alice?secret=JBSWY3DPEHPK3PXP"
            session.addTokenFromUri(uri)

            val beforeLock = session.listTokens(1000u)
            assertEquals(1, beforeLock.size)

            session.lock()
            try {
                session.listTokens(1000u)
                fail("listTokens on locked session must throw WarifuException")
            } catch (e: WarifuException) {
                // Expected: VaultLocked
            }

            val reopened = VaultSession.open(tempVault.absolutePath, vaultKey)
            val afterReopen = reopened.listTokens(1000u)
            assertEquals(1, afterReopen.size)
            assertEquals("Google", afterReopen[0].issuer)
            assertEquals("alice", afterReopen[0].accountName)
        } finally {
            tempVault.delete()
        }
    }

    @Test
    fun testInvalidUriRejection() {
        val tempVault = File.createTempFile("warifu_invalid_uri", ".bin")
        try {
            val vaultKey = ffiGenerateVaultKey()
            val session = VaultSession.createNew(tempVault.absolutePath, vaultKey)

            // Missing secret
            try {
                session.addTokenFromUri("otpauth://totp/Test:user?issuer=Test")
                fail("Missing secret should fail")
            } catch (e: WarifuException) {
                // Expected
            }

            // Invalid scheme
            try {
                session.addTokenFromUri("https://example.com?secret=JBSWY3DPEHPK3PXP")
                fail("Invalid scheme should fail")
            } catch (e: WarifuException) {
                // Expected
            }

            // Invalid base32
            try {
                session.addTokenFromUri("otpauth://totp/Test:user?secret=INVALID89!@#")
                fail("Invalid base32 secret should fail")
            } catch (e: WarifuException) {
                // Expected
            }
        } finally {
            tempVault.delete()
        }
    }

    @Test
    fun testGoogleMigrationExportAndImportRoundtrip() {
        val vaultFile1 = File.createTempFile("warifu_migration_1", ".bin")
        val vaultFile2 = File.createTempFile("warifu_migration_2", ".bin")
        try {
            val key1 = ffiGenerateVaultKey()
            val session1 = VaultSession.createNew(vaultFile1.absolutePath, key1)

            // Add accounts
            session1.addTokenFromUri("otpauth://totp/GitHub:octocat?secret=JBSWY3DPEHPK3PXP&issuer=GitHub")
            session1.addTokenFromUri("otpauth://totp/Google:alice?secret=JBSWY3DPEHPK3PXP&issuer=Google")

            val exportedUri = session1.exportGoogleMigration()
            assertTrue("Exported migration URI must start with otpauth-migration://", exportedUri.startsWith("otpauth-migration://"))

            // Import into clean vault
            val key2 = ffiGenerateVaultKey()
            val session2 = VaultSession.createNew(vaultFile2.absolutePath, key2)
            val count = session2.importGoogleMigration(exportedUri)
            assertEquals("Must import 2 tokens from Google migration URI", 2u, count)

            val importedTokens = session2.listTokens(1000u)
            assertEquals(2, importedTokens.size)
            assertTrue(importedTokens.any { it.issuer == "GitHub" && it.accountName == "octocat" })
            assertTrue(importedTokens.any { it.issuer == "Google" && it.accountName == "alice" })
        } finally {
            vaultFile1.delete()
            vaultFile2.delete()
        }
    }

    @Test
    fun testSteamGuardGenerationKnownVector() {
        val secretBytes = "12345678901234567890".toByteArray(Charsets.US_ASCII)
        val code = org.warifu.security.OtpPreview.generateSteamCode(secretBytes, 1500000000L)
        assertEquals("Steam Guard token at t=1500000000 must match official vector K23BY", "K23BY", code)
    }
}

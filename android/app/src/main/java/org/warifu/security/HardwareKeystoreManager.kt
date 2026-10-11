package org.warifu.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Manages hardware-backed Key Encryption Keys (KEK) inside Android KeyStore.
 *
 * Implements hardware security binding:
 * - Backed by StrongBox Keymaster (dedicated physical security chip) with graceful fallback to TEE (TrustZone).
 * - Master keys are generated and stored exclusively within the hardware keystore.
 * - Hardware key wraps the 256-bit vault key via AES-256-GCM.
 */
class HardwareKeystoreManager {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val MASTER_KEK_ALIAS = "warifu_master_kek_v1"
        private const val GCM_IV_LENGTH_BYTES = 12
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    /**
     * Checks if the master hardware Key Encryption Key (KEK) exists.
     */
    fun hasMasterKey(): Boolean = keyStore.containsAlias(MASTER_KEK_ALIAS)

    /**
     * Retrieves existing or generates a new AES-256 hardware master key.
     */
    fun getOrCreateMasterKey(): SecretKey {
        if (hasMasterKey()) {
            val key = keyStore.getKey(MASTER_KEK_ALIAS, null) as? SecretKey
            if (key != null) return key
        }

        return generateHardwareKey(attemptStrongBox = true)
    }

    private fun generateHardwareKey(attemptStrongBox: Boolean): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val builder = KeyGenParameterSpec.Builder(
            MASTER_KEK_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)

        // Attempt StrongBox hardware security chip on devices that support it (Android 9+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && attemptStrongBox) {
            try {
                builder.setIsStrongBoxBacked(true)
                keyGenerator.init(builder.build())
                return keyGenerator.generateKey()
            } catch (e: Exception) {
                // Fallback to standard TEE (TrustZone) if StrongBox is unavailable
                return generateHardwareKey(attemptStrongBox = false)
            }
        }

        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }

    /**
     * Wraps (encrypts) the 256-bit Vault Key using the hardware KEK.
     *
     * The resulting payload has the format: [12-byte IV] + [Ciphertext + 16-byte GCM Tag]
     */
    fun wrapVaultKey(vaultKey: ByteArray): ByteArray {
        val kek = getOrCreateMasterKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, kek)

        val iv = cipher.iv
        val ciphertext = cipher.doFinal(vaultKey)

        // Concatenate IV + Ciphertext
        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)

        return combined
    }

    /**
     * Unwraps (decrypts) the 256-bit Vault Key using the hardware KEK.
     */
    fun unwrapVaultKey(wrappedKeyPayload: ByteArray): ByteArray {
        require(wrappedKeyPayload.size > GCM_IV_LENGTH_BYTES) { "Invalid wrapped key payload" }

        val kek = getOrCreateMasterKey()
        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        System.arraycopy(wrappedKeyPayload, 0, iv, 0, GCM_IV_LENGTH_BYTES)

        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, kek, spec)

        val ciphertextOffset = GCM_IV_LENGTH_BYTES
        val ciphertextSize = wrappedKeyPayload.size - GCM_IV_LENGTH_BYTES

        return cipher.doFinal(wrappedKeyPayload, ciphertextOffset, ciphertextSize)
    }

    /**
     * Irrevocably purges the hardware master key from Android KeyStore.
     */
    fun deleteMasterKey() {
        if (hasMasterKey()) {
            keyStore.deleteEntry(MASTER_KEK_ALIAS)
        }
    }
}

import Foundation
import LocalAuthentication
import Security

/// Errors relating to iOS Secure Enclave and Keychain operations.
enum SecureEnclaveError: LocalizedError {
    case accessControlCreationFailed
    case keyGenerationFailed(OSStatus)
    case keyNotFound
    case encryptionFailed
    case decryptionFailed(OSStatus)
    case biometricAuthFailed(String)
    case invalidKeySize

    var errorDescription: String? {
        switch self {
        case .accessControlCreationFailed:
            return "Failed to construct Secure Enclave access control structure."
        case .keyGenerationFailed(let status):
            return "Failed to generate key inside Secure Enclave (OSStatus: \(status))."
        case .keyNotFound:
            return "Hardware Key Encryption Key not found in Keychain."
        case .encryptionFailed:
            return "Secure Enclave envelope encryption failed."
        case .decryptionFailed(let status):
            return "Secure Enclave decryption failed (OSStatus: \(status))."
        case .biometricAuthFailed(let reason):
            return "Biometric authentication failed: \(reason)"
        case .invalidKeySize:
            return "Invalid key size: expected 32 bytes."
        }
    }
}

/// Manages hardware-backed Key Encryption Keys (KEK) protected by the Apple Secure Enclave (SEP).
///
/// Features:
/// - Hardware-enforced biometric gate: Access control uses `.biometryCurrentSet`.
/// - Automatic invalidation: If any new fingerprint or face is enrolled in iOS Settings, the key is permanently revoked.
/// - Device bound: Protected with `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`, preventing inclusion in unencrypted iTunes/iCloud backups.
final class SecureEnclaveManager {

    private static let keyTag = "org.warifu.secure_enclave.kek.v1"

    /// Checks if the hardware Key Encryption Key exists in the iOS Keychain.
    func hasMasterKey() -> Bool {
        let query: [String: Any] = [
            kSecClass as String: kSecClassKey,
            kSecAttrApplicationTag as String: Self.keyTag.data(using: .utf8)!,
            kSecAttrKeyType as String: kSecAttrKeyTypeECSECPrimeRandom,
            kSecReturnRef as String: false
        ]

        let status = SecItemCopyMatching(query as CFDictionary, nil)
        return status == errSecSuccess
    }

    /// Generates a hardware ECC P-256 keypair inside the Secure Enclave bound to current biometrics.
    func getOrCreateMasterKey() throws -> SecKey {
        if hasMasterKey() {
            return try retrieveMasterKey()
        }

        var error: Unmanaged<CFError>?
        guard let accessControl = SecAccessControlCreateWithFlags(
            kCFAllocatorDefault,
            kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
            [.biometryCurrentSet, .privateKeyUsage],
            &error
        ) else {
            throw SecureEnclaveError.accessControlCreationFailed
        }

        let attributes: [String: Any] = [
            kSecAttrKeyType as String: kSecAttrKeyTypeECSECPrimeRandom,
            kSecAttrKeySizeInBits as String: 256,
            kSecAttrTokenID as String: kSecAttrTokenIDSecureEnclave,
            kSecPrivateKeyAttrs as String: [
                kSecAttrIsPermanent as String: true,
                kSecAttrApplicationTag as String: Self.keyTag.data(using: .utf8)!,
                kSecAttrAccessControl as String: accessControl
            ]
        ]

        var keyError: Unmanaged<CFError>?
        guard let privateKey = SecKeyCreateRandomKey(attributes as CFDictionary, &keyError) else {
            let status = (keyError?.takeRetainedValue() as Error?) as? NSError
            throw SecureEnclaveError.keyGenerationFailed(OSStatus(status?.code ?? -1))
        }

        return privateKey
    }

    private func retrieveMasterKey() throws -> SecKey {
        let query: [String: Any] = [
            kSecClass as String: kSecClassKey,
            kSecAttrApplicationTag as String: Self.keyTag.data(using: .utf8)!,
            kSecAttrKeyType as String: kSecAttrKeyTypeECSECPrimeRandom,
            kSecReturnRef as String: true
        ]

        var item: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &item)
        guard status == errSecSuccess, let key = item else {
            throw SecureEnclaveError.keyNotFound
        }

        return (key as! SecKey)
    }

    /// Encrypts (wraps) the 256-bit Vault Key using the Secure Enclave public key via ECIES.
    func wrapVaultKey(vaultKey: Data) throws -> Data {
        guard vaultKey.count == 32 else {
            throw SecureEnclaveError.invalidKeySize
        }

        let privateKey = try getOrCreateMasterKey()
        guard let publicKey = SecKeyCopyPublicKey(privateKey) else {
            throw SecureEnclaveError.encryptionFailed
        }

        var error: Unmanaged<CFError>?
        guard let ciphertext = SecKeyCreateEncryptedData(
            publicKey,
            .eciesEncryptionCofactorVariableIVX963SHA256AESGCM,
            vaultKey as CFData,
            &error
        ) else {
            throw SecureEnclaveError.encryptionFailed
        }

        return ciphertext as Data
    }

    /// Decrypts (unwraps) the 256-bit Vault Key using the Secure Enclave private key.
    ///
    /// The Secure Enclave executes the biometric hardware challenge via `LAContext`.
    func unwrapVaultKey(
        wrappedKey: Data,
        context: LAContext,
        promptReason: String = "Unlock your Warifu Vault"
    ) async throws -> Data {
        context.localizedReason = promptReason

        let query: [String: Any] = [
            kSecClass as String: kSecClassKey,
            kSecAttrApplicationTag as String: Self.keyTag.data(using: .utf8)!,
            kSecAttrKeyType as String: kSecAttrKeyTypeECSECPrimeRandom,
            kSecReturnRef as String: true,
            kSecUseAuthenticationContext as String: context
        ]

        var item: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &item)
        guard status == errSecSuccess, let key = item else {
            throw SecureEnclaveError.decryptionFailed(status)
        }

        let privateKey = (key as! SecKey)

        var error: Unmanaged<CFError>?
        guard let plaintext = SecKeyCreateDecryptedData(
            privateKey,
            .eciesEncryptionCofactorVariableIVX963SHA256AESGCM,
            wrappedKey as CFData,
            &error
        ) else {
            let cfErr = error?.takeRetainedValue()
            throw SecureEnclaveError.biometricAuthFailed(cfErr?.localizedDescription ?? "Decryption denied")
        }

        return plaintext as Data
    }

    /// Irrevocably purges the Key Encryption Key from Keychain.
    func deleteMasterKey() {
        let query: [String: Any] = [
            kSecClass as String: kSecClassKey,
            kSecAttrApplicationTag as String: Self.keyTag.data(using: .utf8)!
        ]
        SecItemDelete(query as CFDictionary)
    }
}

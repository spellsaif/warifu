use crate::memory::LockedBytes;
use crate::vault::EncryptedVaultEnvelope;
use argon2::{Algorithm as ArgonAlgorithm, Argon2, Params, Version};
use chacha20poly1305::aead::{Aead, KeyInit};
use chacha20poly1305::{Key, XChaCha20Poly1305, XNonce};
use thiserror::Error;

pub const SALT_LEN: usize = 32;
pub const NONCE_LEN: usize = 24;
pub const KEY_LEN: usize = 32;

// Production Mobile Argon2id Parameters (RFC 9106)
// Memory: 64 MiB (65,536 KiB), Iterations: 3, Parallelism: 4
pub const ARGON2_MEMORY_KIB: u32 = 65_536;
pub const ARGON2_ITERATIONS: u32 = 3;
pub const ARGON2_LANES: u32 = 4;

#[derive(Error, Debug, PartialEq, Eq)]
pub enum CryptoError {
    #[error("CSPRNG failure: unable to obtain random bytes from OS: {0}")]
    EntropyFailure(String),
    #[error("Key derivation failed: {0}")]
    KdfFailed(String),
    #[error("Authenticated encryption failed")]
    EncryptionFailed,
    #[error("Decryption failed: invalid key or corrupted/tampered ciphertext")]
    DecryptionFailed,
    #[error("Invalid key length: expected 32 bytes, got {0}")]
    InvalidKeyLength(usize),
    #[error("Invalid nonce length: expected 24 bytes, got {0}")]
    InvalidNonceLength(usize),
}

/// Generates a 32-byte cryptographically secure random salt.
pub fn generate_salt() -> Result<[u8; SALT_LEN], CryptoError> {
    let mut salt = [0u8; SALT_LEN];
    getrandom::getrandom(&mut salt)
        .map_err(|e| CryptoError::EntropyFailure(e.to_string()))?;
    Ok(salt)
}

/// Generates a 24-byte cryptographically secure random nonce for XChaCha20.
pub fn generate_nonce() -> Result<[u8; NONCE_LEN], CryptoError> {
    let mut nonce = [0u8; NONCE_LEN];
    getrandom::getrandom(&mut nonce)
        .map_err(|e| CryptoError::EntropyFailure(e.to_string()))?;
    Ok(nonce)
}

/// Generates a fresh 256-bit random Vault Key wrapped in `LockedBytes`.
pub fn generate_vault_key() -> Result<LockedBytes, CryptoError> {
    let mut key = vec![0u8; KEY_LEN];
    getrandom::getrandom(&mut key)
        .map_err(|e| CryptoError::EntropyFailure(e.to_string()))?;
    Ok(LockedBytes::new(key))
}

/// Derives a 256-bit encryption key from a passphrase using Argon2id (RFC 9106).
pub fn derive_key_argon2id(password: &str, salt: &[u8; SALT_LEN]) -> Result<LockedBytes, CryptoError> {
    let params = Params::new(
        ARGON2_MEMORY_KIB,
        ARGON2_ITERATIONS,
        ARGON2_LANES,
        Some(KEY_LEN),
    )
    .map_err(|e| CryptoError::KdfFailed(e.to_string()))?;

    let argon2 = Argon2::new(ArgonAlgorithm::Argon2id, Version::V0x13, params);

    let mut derived_key = vec![0u8; KEY_LEN];
    argon2
        .hash_password_into(password.as_bytes(), salt, &mut derived_key)
        .map_err(|e| CryptoError::KdfFailed(e.to_string()))?;

    Ok(LockedBytes::new(derived_key))
}

/// Encrypts plaintext bytes using XChaCha20-Poly1305 with an optional salt.
pub fn encrypt_envelope(
    plaintext: &[u8],
    key: &LockedBytes,
    salt: Option<&[u8; SALT_LEN]>,
) -> Result<EncryptedVaultEnvelope, CryptoError> {
    if key.len() != KEY_LEN {
        return Err(CryptoError::InvalidKeyLength(key.len()));
    }

    let nonce_bytes = generate_nonce()?;
    let nonce = XNonce::from_slice(&nonce_bytes);
    let cipher_key = Key::from_slice(key.as_slice());
    let cipher = XChaCha20Poly1305::new(cipher_key);

    let ciphertext = cipher
        .encrypt(nonce, plaintext)
        .map_err(|_| CryptoError::EncryptionFailed)?;

    Ok(EncryptedVaultEnvelope {
        format_version: 1,
        salt: salt.map(|s| s.to_vec()).unwrap_or_default(),
        nonce: nonce_bytes.to_vec(),
        ciphertext,
    })
}

/// Decrypts an `EncryptedVaultEnvelope` using XChaCha20-Poly1305.
/// Returns the decrypted bytes inside memory-locked, auto-zeroizing `LockedBytes`.
pub fn decrypt_envelope(
    envelope: &EncryptedVaultEnvelope,
    key: &LockedBytes,
) -> Result<LockedBytes, CryptoError> {
    if key.len() != KEY_LEN {
        return Err(CryptoError::InvalidKeyLength(key.len()));
    }

    if envelope.nonce.len() != NONCE_LEN {
        return Err(CryptoError::InvalidNonceLength(envelope.nonce.len()));
    }

    let nonce = XNonce::from_slice(&envelope.nonce);
    let cipher_key = Key::from_slice(key.as_slice());
    let cipher = XChaCha20Poly1305::new(cipher_key);

    let decrypted = cipher
        .decrypt(nonce, envelope.ciphertext.as_ref())
        .map_err(|_| CryptoError::DecryptionFailed)?;

    Ok(LockedBytes::new(decrypted))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_argon2id_derivation_deterministic() {
        // Test with lighter parameters for fast test runs
        let salt = [0x5au8; SALT_LEN];
        let key_1 = derive_key_argon2id("master_password_test", &salt).unwrap();
        let key_2 = derive_key_argon2id("master_password_test", &salt).unwrap();

        assert_eq!(key_1, key_2);
        assert_eq!(key_1.len(), KEY_LEN);
    }

    #[test]
    fn test_xchacha20_envelope_roundtrip() {
        let key = generate_vault_key().unwrap();
        let salt = generate_salt().unwrap();
        let secret_payload = b"CRITICAL_DATA_THAT_MUST_STAY_SAFE";

        let envelope = encrypt_envelope(secret_payload, &key, Some(&salt)).unwrap();
        assert_eq!(envelope.nonce.len(), NONCE_LEN);
        assert_eq!(envelope.salt.len(), SALT_LEN);
        assert_ne!(envelope.ciphertext, secret_payload);

        let decrypted = decrypt_envelope(&envelope, &key).unwrap();
        assert_eq!(decrypted.as_slice(), secret_payload);
    }

    #[test]
    fn test_tampered_ciphertext_fails_decryption() {
        let key = generate_vault_key().unwrap();
        let secret_payload = b"ORIGINAL_PAYLOAD";

        let mut envelope = encrypt_envelope(secret_payload, &key, None).unwrap();

        // Tamper with one bit of the ciphertext
        if let Some(first_byte) = envelope.ciphertext.first_mut() {
            *first_byte ^= 0x01;
        }

        let result = decrypt_envelope(&envelope, &key);
        assert_eq!(result, Err(CryptoError::DecryptionFailed));
    }

    #[test]
    fn test_wrong_key_fails_decryption() {
        let key_a = generate_vault_key().unwrap();
        let key_b = generate_vault_key().unwrap();
        let secret_payload = b"PAYLOAD";

        let envelope = encrypt_envelope(secret_payload, &key_a, None).unwrap();
        let result = decrypt_envelope(&envelope, &key_b);

        assert_eq!(result, Err(CryptoError::DecryptionFailed));
    }
}

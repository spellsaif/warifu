use crate::algorithm::{Algorithm, Digits};
use crate::crypto::{decrypt_envelope, derive_key_argon2id, encrypt_envelope, generate_salt, generate_vault_key};
use crate::google_migration::{encode_google_migration_uri, parse_google_migration_uri};
use crate::memory::LockedBytes;
use crate::storage::{load_envelope, save_envelope_atomic};
use crate::totp::{generate_next_totp, generate_totp, is_near_expiry, remaining_seconds, verify_totp};
use crate::uri::{parse_otpauth_uri, TokenType};
use crate::vault::{TokenEntry, VaultDatabase};
use std::path::PathBuf;
use std::sync::{Arc, Mutex};
use thiserror::Error;
use uuid::Uuid;

#[derive(Error, Debug, uniffi::Error)]
pub enum WarifuError {
    #[error("Cryptographic error: {message}")]
    CryptoError { message: String },
    #[error("Storage error: {message}")]
    StorageError { message: String },
    #[error("URI parsing error: {message}")]
    ParseError { message: String },
    #[error("Vault error: {message}")]
    VaultError { message: String },
    #[error("Vault session is locked")]
    VaultLocked,
}

#[derive(uniffi::Record, Clone, Debug, PartialEq, Eq)]
pub struct TokenView {
    pub id: String,
    pub issuer: String,
    pub account_name: String,
    pub current_code: String,
    pub remaining_seconds: u32,
    pub period: u32,
    pub is_near_expiry: bool,
    pub next_code: Option<String>,
    pub icon_id: String,
    pub tags: Vec<String>,
    pub is_pinned: bool,
}

#[derive(uniffi::Object)]
pub struct VaultSession {
    vault_path: PathBuf,
    vault_key: Mutex<Option<LockedBytes>>,
    database: Mutex<Option<VaultDatabase>>,
}

#[uniffi::export]
impl VaultSession {
    /// Unlocks an existing encrypted vault using a 32-byte hardware-unwrapped Vault Key.
    #[uniffi::constructor]
    pub fn open(vault_path: String, vault_key_bytes: Vec<u8>) -> Result<Arc<Self>, WarifuError> {
        let path = PathBuf::from(vault_path);
        if !path.exists() {
            return Err(WarifuError::StorageError {
                message: format!("Vault file does not exist at {:?}", path),
            });
        }

        let locked_key = LockedBytes::new(vault_key_bytes);
        let envelope = load_envelope(&path).map_err(|e| WarifuError::StorageError {
            message: e.to_string(),
        })?;

        let decrypted_bytes = decrypt_envelope(&envelope, &locked_key).map_err(|e| {
            WarifuError::CryptoError {
                message: e.to_string(),
            }
        })?;

        let database = VaultDatabase::deserialize(decrypted_bytes.as_slice()).map_err(|e| {
            WarifuError::VaultError {
                message: e.to_string(),
            }
        })?;

        Ok(Arc::new(Self {
            vault_path: path,
            vault_key: Mutex::new(Some(locked_key)),
            database: Mutex::new(Some(database)),
        }))
    }

    /// Creates and initializes a fresh, empty encrypted vault on disk.
    #[uniffi::constructor]
    pub fn create_new(vault_path: String, vault_key_bytes: Vec<u8>) -> Result<Arc<Self>, WarifuError> {
        let path = PathBuf::from(vault_path);
        let locked_key = LockedBytes::new(vault_key_bytes);
        let database = VaultDatabase::new();

        let serialized = database.serialize().map_err(|e| WarifuError::VaultError {
            message: e.to_string(),
        })?;

        let envelope = encrypt_envelope(&serialized, &locked_key, None).map_err(|e| {
            WarifuError::CryptoError {
                message: e.to_string(),
            }
        })?;

        save_envelope_atomic(&path, &envelope).map_err(|e| WarifuError::StorageError {
            message: e.to_string(),
        })?;

        Ok(Arc::new(Self {
            vault_path: path,
            vault_key: Mutex::new(Some(locked_key)),
            database: Mutex::new(Some(database)),
        }))
    }

    /// Evaluates and returns all active OTP tokens with current codes and monotonic timers.
    pub fn list_tokens(&self, current_unix_time: u64) -> Result<Vec<TokenView>, WarifuError> {
        let db_guard = self.database.lock().unwrap();
        let db = db_guard.as_ref().ok_or(WarifuError::VaultLocked)?;

        let mut views = Vec::with_capacity(db.tokens.len());

        for token in &db.tokens {
            let secret = crate::algorithm::SecretKey::new(token.secret.clone());
            let algo = match token.algorithm {
                2 => Algorithm::Sha256,
                3 => Algorithm::Sha512,
                _ => Algorithm::Sha1,
            };

            let digits = Digits::try_from(token.digits).unwrap_or(Digits::Six);
            let period = if token.period == 0 { 30 } else { token.period as u64 };

            let current_code = if token.token_type == 3 {
                // Steam Guard
                crate::steam::generate_steam_code(&secret, current_unix_time)
            } else if token.token_type == 2 {
                // HOTP
                crate::hotp::generate_hotp(&secret, token.counter, digits, algo)
            } else {
                // Standard TOTP
                generate_totp(&secret, current_unix_time, period, digits, algo)
            };

            let rem_sec = remaining_seconds(current_unix_time, period) as u32;
            let near_exp = is_near_expiry(current_unix_time, period, 5);

            let next_code = if near_exp && token.token_type == 1 {
                Some(generate_next_totp(&secret, current_unix_time, period, digits, algo))
            } else {
                None
            };

            views.push(TokenView {
                id: token.id.clone(),
                issuer: token.issuer.clone(),
                account_name: token.account_name.clone(),
                current_code,
                remaining_seconds: rem_sec,
                period: period as u32,
                is_near_expiry: near_exp,
                next_code,
                icon_id: token.icon_id.clone(),
                tags: token.tags.clone(),
                is_pinned: token.is_pinned,
            });
        }

        Ok(views)
    }

    /// Parses and imports a single `otpauth://` URI scanned from a QR code.
    pub fn add_token_from_uri(&self, uri: String) -> Result<String, WarifuError> {
        let parsed = parse_otpauth_uri(&uri).map_err(|e| WarifuError::ParseError {
            message: e.to_string(),
        })?;

        let token_type_id = match parsed.token_type {
            TokenType::Totp => 1,
            TokenType::Hotp => 2,
            TokenType::Steam => 3,
        };

        let algo_id = match parsed.algorithm {
            Algorithm::Sha1 => 1,
            Algorithm::Sha256 => 2,
            Algorithm::Sha512 => 3,
        };

        let id = Uuid::new_v4().to_string();
        let now = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .unwrap_or_default()
            .as_millis() as i64;

        let entry = TokenEntry {
            id: id.clone(),
            issuer: parsed.issuer.unwrap_or_default(),
            account_name: parsed.account_name,
            notes: String::new(),
            secret: parsed.secret.as_bytes().to_vec(),
            token_type: token_type_id,
            algorithm: algo_id,
            digits: parsed.digits.as_u32(),
            period: parsed.period as u32,
            counter: parsed.counter,
            icon_id: String::new(),
            tags: Vec::new(),
            is_pinned: false,
            updated_at_unix_ms: now,
            lamport_clock: 1,
        };

        {
            let mut db_guard = self.database.lock().unwrap();
            let db = db_guard.as_mut().ok_or(WarifuError::VaultLocked)?;
            db.add_token(entry);
        }

        self.save()?;
        Ok(id)
    }

    /// Imports multiple accounts in bulk from a Google Authenticator migration QR code.
    pub fn import_google_migration(&self, migration_uri: String) -> Result<u32, WarifuError> {
        let parsed_tokens = parse_google_migration_uri(&migration_uri).map_err(|e| {
            WarifuError::ParseError {
                message: e.to_string(),
            }
        })?;

        let count = parsed_tokens.len() as u32;
        let now = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .unwrap_or_default()
            .as_millis() as i64;

        {
            let mut db_guard = self.database.lock().unwrap();
            let db = db_guard.as_mut().ok_or(WarifuError::VaultLocked)?;

            for parsed in parsed_tokens {
                let token_type_id = match parsed.token_type {
                    TokenType::Totp => 1,
                    TokenType::Hotp => 2,
                    TokenType::Steam => 3,
                };

                let algo_id = match parsed.algorithm {
                    Algorithm::Sha1 => 1,
                    Algorithm::Sha256 => 2,
                    Algorithm::Sha512 => 3,
                };

                db.add_token(TokenEntry {
                    id: Uuid::new_v4().to_string(),
                    issuer: parsed.issuer.unwrap_or_default(),
                    account_name: parsed.account_name,
                    notes: String::new(),
                    secret: parsed.secret.as_bytes().to_vec(),
                    token_type: token_type_id,
                    algorithm: algo_id,
                    digits: parsed.digits.as_u32(),
                    period: parsed.period as u32,
                    counter: parsed.counter,
                    icon_id: String::new(),
                    tags: Vec::new(),
                    is_pinned: false,
                    updated_at_unix_ms: now,
                    lamport_clock: 1,
                });
            }
        }

        self.save()?;
        Ok(count)
    }

    /// Exports all active tokens back into a Google Authenticator migration URI format.
    pub fn export_google_migration(&self) -> Result<String, WarifuError> {
        let db_guard = self.database.lock().unwrap();
        let db = db_guard.as_ref().ok_or(WarifuError::VaultLocked)?;

        let mut parsed_tokens = Vec::with_capacity(db.tokens.len());
        for t in &db.tokens {
            let algo = match t.algorithm {
                2 => Algorithm::Sha256,
                3 => Algorithm::Sha512,
                _ => Algorithm::Sha1,
            };
            let digits = Digits::try_from(t.digits).unwrap_or(Digits::Six);
            let token_type = match t.token_type {
                2 => TokenType::Hotp,
                3 => TokenType::Steam,
                _ => TokenType::Totp,
            };

            parsed_tokens.push(crate::uri::ParsedToken {
                token_type,
                issuer: if t.issuer.is_empty() { None } else { Some(t.issuer.clone()) },
                account_name: t.account_name.clone(),
                secret: crate::algorithm::SecretKey::new(t.secret.clone()),
                algorithm: algo,
                digits,
                period: t.period as u64,
                counter: t.counter,
            });
        }

        Ok(encode_google_migration_uri(&parsed_tokens))
    }

    /// Deletes a token and records a deletion tombstone for conflict-free multi-device sync.
    pub fn remove_token(&self, id: String) -> Result<(), WarifuError> {
        {
            let mut db_guard = self.database.lock().unwrap();
            let db = db_guard.as_mut().ok_or(WarifuError::VaultLocked)?;
            db.remove_token(&id).map_err(|e| WarifuError::VaultError {
                message: e.to_string(),
            })?;
        }

        self.save()?;
        Ok(())
    }

    /// Persists changes to disk atomically using envelope encryption.
    pub fn save(&self) -> Result<(), WarifuError> {
        let db_guard = self.database.lock().unwrap();
        let db = db_guard.as_ref().ok_or(WarifuError::VaultLocked)?;

        let key_guard = self.vault_key.lock().unwrap();
        let key = key_guard.as_ref().ok_or(WarifuError::VaultLocked)?;

        let serialized = db.serialize().map_err(|e| WarifuError::VaultError {
            message: e.to_string(),
        })?;

        let envelope = encrypt_envelope(&serialized, key, None).map_err(|e| {
            WarifuError::CryptoError {
                message: e.to_string(),
            }
        })?;

        save_envelope_atomic(&self.vault_path, &envelope).map_err(|e| {
            WarifuError::StorageError {
                message: e.to_string(),
            }
        })?;

        Ok(())
    }

    /// Locks the session and purges sensitive decrypted tokens from RAM.
    pub fn lock(&self) {
        let mut key_guard = self.vault_key.lock().unwrap();
        *key_guard = None;

        let mut db_guard = self.database.lock().unwrap();
        *db_guard = None;
    }
}

// Global standalone FFI helpers for mobile apps

/// Generates a fresh 256-bit cryptographically secure Vault Key to be wrapped by Android StrongBox or Apple Secure Enclave.
#[uniffi::export]
pub fn ffi_generate_vault_key() -> Result<Vec<u8>, WarifuError> {
    let key = generate_vault_key().map_err(|e| WarifuError::CryptoError {
        message: e.to_string(),
    })?;
    Ok(key.as_slice().to_vec())
}

/// Generates a 32-byte CSPRNG salt for passphrase key derivation.
#[uniffi::export]
pub fn ffi_generate_salt() -> Result<Vec<u8>, WarifuError> {
    let salt = generate_salt().map_err(|e| WarifuError::CryptoError {
        message: e.to_string(),
    })?;
    Ok(salt.to_vec())
}

/// Derives a 256-bit key from a user passphrase and salt via Argon2id (RFC 9106).
#[uniffi::export]
pub fn ffi_derive_key_argon2id(password: String, salt: Vec<u8>) -> Result<Vec<u8>, WarifuError> {
    if salt.len() != 32 {
        return Err(WarifuError::CryptoError {
            message: "Salt must be precisely 32 bytes".to_string(),
        });
    }

    let mut salt_arr = [0u8; 32];
    salt_arr.copy_from_slice(&salt);

    let key = derive_key_argon2id(&password, &salt_arr).map_err(|e| WarifuError::CryptoError {
        message: e.to_string(),
    })?;

    Ok(key.as_slice().to_vec())
}

/// Verifies an OTP code against current time in constant-time with tolerance window.
#[uniffi::export]
pub fn ffi_verify_code(
    secret_base32: String,
    token: String,
    current_time: u64,
    tolerance_steps: u32,
    period: u32,
    digits: u32,
    algorithm: String,
) -> bool {
    let secret = match crate::base32::decode_secret(&secret_base32) {
        Ok(s) => s,
        Err(_) => return false,
    };

    let algo = match algorithm.to_ascii_uppercase().as_str() {
        "SHA256" => Algorithm::Sha256,
        "SHA512" => Algorithm::Sha512,
        _ => Algorithm::Sha1,
    };

    let dig = match Digits::try_from(digits) {
        Ok(d) => d,
        Err(_) => Digits::Six,
    };

    verify_totp(
        &secret,
        &token,
        current_time,
        tolerance_steps as u64,
        period as u64,
        dig,
        algo,
    )
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_vault_session_lifecycle() {
        let temp_dir = std::env::temp_dir().join(format!("openauth_ffi_{}", Uuid::new_v4()));
        std::fs::create_dir_all(&temp_dir).unwrap();
        let vault_file = temp_dir.join("test_vault.bin");
        let vault_path = vault_file.to_str().unwrap().to_string();

        let vault_key = ffi_generate_vault_key().unwrap();
        assert_eq!(vault_key.len(), 32);

        // 1. Create fresh vault
        let session = VaultSession::create_new(vault_path.clone(), vault_key.clone()).unwrap();

        // 2. Add token from URI
        let uri = "otpauth://totp/AcmeCorp:alice@acme.com?secret=JBSWY3DPEHPK3PXP&issuer=AcmeCorp&digits=6&period=30";
        let token_id = session.add_token_from_uri(uri.to_string()).unwrap();
        assert!(!token_id.is_empty());

        // 3. List tokens
        let tokens = session.list_tokens(1000000).unwrap();
        assert_eq!(tokens.len(), 1);
        assert_eq!(tokens[0].issuer, "AcmeCorp");
        assert_eq!(tokens[0].account_name, "alice@acme.com");
        assert_eq!(tokens[0].current_code.len(), 6);

        // 4. Lock session and verify locked error
        session.lock();
        assert!(matches!(
            session.list_tokens(1000000),
            Err(WarifuError::VaultLocked)
        ));

        // 5. Re-open vault with key
        let reopened = VaultSession::open(vault_path, vault_key).unwrap();
        let tokens_reopened = reopened.list_tokens(1000000).unwrap();
        assert_eq!(tokens_reopened.len(), 1);
        assert_eq!(tokens_reopened[0].id, token_id);

        // 6. Delete token
        reopened.remove_token(token_id).unwrap();
        let tokens_after_delete = reopened.list_tokens(1000000).unwrap();
        assert_eq!(tokens_after_delete.len(), 0);

        // Cleanup
        let _ = std::fs::remove_dir_all(&temp_dir);
    }

    #[test]
    fn test_ffi_helpers() {
        let salt = ffi_generate_salt().unwrap();
        assert_eq!(salt.len(), 32);

        let key = ffi_derive_key_argon2id("passphrase123".to_string(), salt).unwrap();
        assert_eq!(key.len(), 32);

        let is_valid = ffi_verify_code(
            "JBSWY3DPEHPK3PXP".to_string(),
            "000000".to_string(),
            1000,
            1,
            30,
            6,
            "SHA1".to_string(),
        );
        assert!(!is_valid);
    }
}

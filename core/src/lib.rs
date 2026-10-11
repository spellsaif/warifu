//! OpenAuthenticator Core Library
//!
//! Provides mathematically verifiable, memory-safe implementations of:
//! - RFC 4226 (HOTP)
//! - RFC 6238 (TOTP)
//! - Steam Guard Mobile Authenticator
//! - RFC 4648 Base32 Secret Sanitization
//! - Strict `otpauth://` URI Parsing
//! - Bidirectional Google Authenticator Migration Protobuf Ingestion & Export
//! - Memory Pinning (`mlock`) and Guarding (`LockedBytes`)
//! - Argon2id KDF & XChaCha20-Poly1305 Envelope Encryption
//! - Atomic POSIX Storage Persistence

pub mod algorithm;
pub mod base32;
pub mod crypto;
pub mod ffi;
pub mod google_migration;
pub mod hotp;
pub mod memory;
pub mod steam;
pub mod storage;
pub mod totp;
pub mod uri;
pub mod vault;

uniffi::setup_scaffolding!();

pub use algorithm::{Algorithm, Digits, SecretKey};
pub use crypto::{
    decrypt_envelope, derive_key_argon2id, encrypt_envelope, generate_nonce, generate_salt,
    generate_vault_key, CryptoError,
};
pub use google_migration::{encode_google_migration_uri, parse_google_migration_uri};
pub use hotp::generate_hotp;
pub use memory::LockedBytes;
pub use steam::generate_steam_code;
pub use storage::{load_envelope, save_envelope_atomic};
pub use totp::{generate_next_totp, generate_totp, is_near_expiry, remaining_seconds, verify_totp};
pub use uri::{parse_otpauth_uri, ParsedToken, TokenType, MAX_URI_LENGTH};
pub use vault::{EncryptedVaultEnvelope, TokenEntry, Tombstone, VaultDatabase, VaultError};

//! OpenAuthenticator Core Library
//!
//! Provides mathematically verifiable, memory-safe implementations of:
//! - RFC 4226 (HOTP)
//! - RFC 6238 (TOTP)
//! - Steam Guard Mobile Authenticator
//! - RFC 4648 Base32 Secret Sanitization
//! - Strict `otpauth://` URI Parsing
//! - Bidirectional Google Authenticator Migration Protobuf Ingestion & Export

pub mod algorithm;
pub mod base32;
pub mod google_migration;
pub mod hotp;
pub mod steam;
pub mod totp;
pub mod uri;

pub use algorithm::{Algorithm, Digits, SecretKey};
pub use google_migration::{encode_google_migration_uri, parse_google_migration_uri};
pub use hotp::generate_hotp;
pub use steam::generate_steam_code;
pub use totp::{generate_next_totp, generate_totp, is_near_expiry, remaining_seconds, verify_totp};
pub use uri::{parse_otpauth_uri, ParsedToken, TokenType, MAX_URI_LENGTH};

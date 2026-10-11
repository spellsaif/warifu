//! OpenAuthenticator Core Library
//!
//! Provides mathematically verifiable, memory-safe implementations of:
//! - RFC 4226 (HOTP)
//! - RFC 6238 (TOTP)
//! - Steam Guard Mobile Authenticator
//! - RFC 4648 Base32 Secret Sanitization

pub mod algorithm;
pub mod base32;
pub mod hotp;
pub mod steam;
pub mod totp;

pub use algorithm::{Algorithm, Digits, SecretKey};
pub use hotp::generate_hotp;
pub use steam::generate_steam_code;
pub use totp::{generate_next_totp, generate_totp, is_near_expiry, remaining_seconds, verify_totp};

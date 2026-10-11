use crate::algorithm::{Algorithm, Digits, SecretKey};
use crate::hotp::generate_hotp;

/// Generates an RFC 6238 Time-Based One-Time Password (TOTP).
///
/// # Arguments
/// * `secret` - The shared secret key
/// * `unix_timestamp` - Current Unix timestamp in seconds
/// * `period` - Time step interval in seconds (default is 30)
/// * `digits` - Number of output digits (6, 7, or 8)
/// * `algorithm` - HMAC hash algorithm (SHA-1, SHA-256, or SHA-512)
pub fn generate_totp(
    secret: &SecretKey,
    unix_timestamp: u64,
    period: u64,
    digits: Digits,
    algorithm: Algorithm,
) -> String {
    let period = if period == 0 { 30 } else { period };
    let counter = unix_timestamp / period;
    generate_hotp(secret, counter, digits, algorithm)
}

/// Computes the remaining seconds before the current TOTP token expires.
pub fn remaining_seconds(unix_timestamp: u64, period: u64) -> u64 {
    let period = if period == 0 { 30 } else { period };
    period - (unix_timestamp % period)
}

/// Returns true if the token is within the grace period threshold (e.g., <= 5 seconds).
pub fn is_near_expiry(unix_timestamp: u64, period: u64, threshold_seconds: u64) -> bool {
    remaining_seconds(unix_timestamp, period) <= threshold_seconds
}

/// Computes the next consecutive TOTP token (C + 1) for the grace period preview.
pub fn generate_next_totp(
    secret: &SecretKey,
    unix_timestamp: u64,
    period: u64,
    digits: Digits,
    algorithm: Algorithm,
) -> String {
    let period = if period == 0 { 30 } else { period };
    let next_counter = (unix_timestamp / period) + 1;
    generate_hotp(secret, next_counter, digits, algorithm)
}

/// Verifies a user-provided token against the current time with an optional tolerance window.
///
/// Uses constant-time comparison via `subtle::ConstantTimeEq` to prevent side-channel timing attacks.
///
/// # Arguments
/// * `secret` - The shared secret key
/// * `token` - The user-entered token string to verify
/// * `unix_timestamp` - Current Unix timestamp in seconds
/// * `tolerance_steps` - Number of steps before and after current time to tolerate (e.g. 1 for ±1 step)
/// * `period` - Time step interval in seconds
/// * `digits` - Number of digits expected
/// * `algorithm` - HMAC hash algorithm
pub fn verify_totp(
    secret: &SecretKey,
    token: &str,
    unix_timestamp: u64,
    tolerance_steps: u64,
    period: u64,
    digits: Digits,
    algorithm: Algorithm,
) -> bool {
    use subtle::ConstantTimeEq;

    let period = if period == 0 { 30 } else { period };
    let current_counter = unix_timestamp / period;

    let min_counter = current_counter.saturating_sub(tolerance_steps);
    let max_counter = current_counter.saturating_add(tolerance_steps);

    let mut valid: subtle::Choice = 0.into();

    for counter in min_counter..=max_counter {
        let expected = generate_hotp(secret, counter, digits, algorithm);
        if expected.len() == token.len() {
            let is_match = expected.as_bytes().ct_eq(token.as_bytes());
            valid |= is_match;
        }
    }

    valid.into()
}

#[cfg(test)]
mod tests {
    use super::*;

    // RFC 6238 Appendix B Official Test Secrets
    const SEED_SHA1: &[u8] = b"12345678901234567890"; // 20 bytes
    const SEED_SHA256: &[u8] = b"12345678901234567890123456789012"; // 32 bytes
    const SEED_SHA512: &[u8] =
        b"1234567890123456789012345678901234567890123456789012345678901234"; // 64 bytes

    // Table of Official RFC 6238 Appendix B Test Vectors:
    // (Time, Expected SHA1, Expected SHA256, Expected SHA512)
    // All test vectors use Digits::Eight and 30-second steps.
    const TEST_VECTORS: [(u64, &str, &str, &str); 6] = [
        (59, "94287082", "46119246", "90693936"),
        (1111111109, "07081804", "68084774", "25091201"),
        (1111111111, "14050471", "67062674", "99943326"),
        (1234567890, "89005924", "91819424", "93441116"),
        (2000000000, "69279037", "90698825", "38618901"),
        (20000000000, "65353130", "77737706", "47863826"),
    ];

    #[test]
    fn test_rfc6238_sha1_vectors() {
        let secret = SecretKey::new(SEED_SHA1.to_vec());
        for (time, expected_sha1, _, _) in TEST_VECTORS {
            let code = generate_totp(&secret, time, 30, Digits::Eight, Algorithm::Sha1);
            assert_eq!(code, expected_sha1, "SHA1 failed at time {}", time);
        }
    }

    #[test]
    fn test_rfc6238_sha256_vectors() {
        let secret = SecretKey::new(SEED_SHA256.to_vec());
        for (time, _, expected_sha256, _) in TEST_VECTORS {
            let code = generate_totp(&secret, time, 30, Digits::Eight, Algorithm::Sha256);
            assert_eq!(code, expected_sha256, "SHA256 failed at time {}", time);
        }
    }

    #[test]
    fn test_rfc6238_sha512_vectors() {
        let secret = SecretKey::new(SEED_SHA512.to_vec());
        for (time, _, _, expected_sha512) in TEST_VECTORS {
            let code = generate_totp(&secret, time, 30, Digits::Eight, Algorithm::Sha512);
            assert_eq!(code, expected_sha512, "SHA512 failed at time {}", time);
        }
    }

    #[test]
    fn test_remaining_seconds_and_expiry() {
        // At t = 29s with period 30s: 1s remaining, should be near expiry
        assert_eq!(remaining_seconds(29, 30), 1);
        assert!(is_near_expiry(29, 30, 5));

        // At t = 10s with period 30s: 20s remaining, not near expiry
        assert_eq!(remaining_seconds(10, 30), 20);
        assert!(!is_near_expiry(10, 30, 5));
    }

    #[test]
    fn test_verify_totp_with_tolerance() {
        let secret = SecretKey::new(SEED_SHA1.to_vec());
        let current_time = 1234567890;
        let valid_code = generate_totp(&secret, current_time, 30, Digits::Eight, Algorithm::Sha1);

        // Exact match
        assert!(verify_totp(
            &secret,
            &valid_code,
            current_time,
            0,
            30,
            Digits::Eight,
            Algorithm::Sha1
        ));

        // Within 1 step tolerance (30 seconds in past)
        assert!(verify_totp(
            &secret,
            &valid_code,
            current_time + 30,
            1,
            30,
            Digits::Eight,
            Algorithm::Sha1
        ));

        // Outside 1 step tolerance (61 seconds in past)
        assert!(!verify_totp(
            &secret,
            &valid_code,
            current_time + 61,
            1,
            30,
            Digits::Eight,
            Algorithm::Sha1
        ));

        // Invalid code
        assert!(!verify_totp(
            &secret,
            "00000000",
            current_time,
            1,
            30,
            Digits::Eight,
            Algorithm::Sha1
        ));
    }
}

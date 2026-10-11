use crate::algorithm::{Algorithm, Digits, SecretKey};
use ring::hmac;

/// Computes an HMAC digest for the given secret and message using the specified algorithm.
pub(crate) fn compute_hmac(secret: &SecretKey, message: &[u8], algorithm: Algorithm) -> hmac::Tag {
    let signing_key = match algorithm {
        Algorithm::Sha1 => hmac::Key::new(hmac::HMAC_SHA1_FOR_LEGACY_USE_ONLY, secret.as_bytes()),
        Algorithm::Sha256 => hmac::Key::new(hmac::HMAC_SHA256, secret.as_bytes()),
        Algorithm::Sha512 => hmac::Key::new(hmac::HMAC_SHA512, secret.as_bytes()),
    };
    hmac::sign(&signing_key, message)
}

/// Dynamically truncates an HMAC digest according to RFC 4226 Section 5.3.
///
/// Returns a 31-bit unsigned integer.
pub(crate) fn dynamic_truncate(digest: &[u8]) -> u32 {
    // 1. Take the low-order 4 bits of the last byte as the offset
    let offset = (digest[digest.len() - 1] & 0x0f) as usize;

    // 2. Extract 4 consecutive bytes starting at `offset` and mask the MSB to 0
    let b0 = (digest[offset] & 0x7f) as u32;
    let b1 = digest[offset + 1] as u32;
    let b2 = digest[offset + 2] as u32;
    let b3 = digest[offset + 3] as u32;

    (b0 << 24) | (b1 << 16) | (b2 << 8) | b3
}

/// Generates an RFC 4226 HMAC-Based One-Time Password (HOTP).
///
/// # Arguments
/// * `secret` - The shared secret key
/// * `counter` - 64-bit counter value
/// * `digits` - Number of output digits (6, 7, or 8)
/// * `algorithm` - HMAC hash algorithm (Default: SHA-1 per RFC 4226)
pub fn generate_hotp(
    secret: &SecretKey,
    counter: u64,
    digits: Digits,
    algorithm: Algorithm,
) -> String {
    // 1. Convert counter to 8-byte big-endian representation (RFC 4226 Section 5.1)
    let counter_bytes = counter.to_be_bytes();

    // 2. Compute HMAC
    let tag = compute_hmac(secret, &counter_bytes, algorithm);

    // 3. Dynamic Truncation
    let binary_code = dynamic_truncate(tag.as_ref());

    // 4. Modulo 10^Digits and zero-pad
    let otp = binary_code % digits.modulus();
    format!("{:0width$}", otp, width = digits.as_u32() as usize)
}

#[cfg(test)]
mod tests {
    use super::*;

    // Official RFC 4226 Appendix D Test Vectors
    // The secret is the ASCII string "12345678901234567890" (20 bytes)
    const RFC4226_SECRET: &[u8] = b"12345678901234567890";

    #[test]
    fn test_rfc4226_official_vectors() {
        let secret = SecretKey::new(RFC4226_SECRET.to_vec());
        let expected_values = [
            (0, "755224"),
            (1, "287082"),
            (2, "359152"),
            (3, "969429"),
            (4, "338314"),
            (5, "254676"),
            (6, "287922"),
            (7, "162583"),
            (8, "399871"),
            (9, "520489"),
        ];

        for (counter, expected_code) in expected_values {
            let code = generate_hotp(&secret, counter, Digits::Six, Algorithm::Sha1);
            assert_eq!(
                code, expected_code,
                "HOTP mismatch at counter {}",
                counter
            );
        }
    }
}

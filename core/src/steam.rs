use crate::algorithm::{Algorithm, SecretKey};
use crate::hotp::{compute_hmac, dynamic_truncate};

/// Valve Steam Guard custom 26-character alphanumeric alphabet.
///
/// Intentionally excludes vowels (A, E, I, O, U) to prevent offensive words,
/// and confusing glyphs (0, 1, L) to prevent human reading errors.
const STEAM_CHARSET: &[u8; 26] = b"23456789BCDFGHJKMNPQRTVWXY";

/// Generates a Valve Steam Guard 5-character alphanumeric token.
///
/// # Arguments
/// * `secret` - The Base32 decoded secret key
/// * `unix_timestamp` - Current Unix timestamp in seconds
pub fn generate_steam_code(secret: &SecretKey, unix_timestamp: u64) -> String {
    let counter = unix_timestamp / 30;
    let counter_bytes = counter.to_be_bytes();

    // 1. Steam uses HMAC-SHA1
    let tag = compute_hmac(secret, &counter_bytes, Algorithm::Sha1);

    // 2. Dynamic Truncation
    let mut binary_code = dynamic_truncate(tag.as_ref());

    // 3. 5-iteration base-26 character mapping
    let mut code = String::with_capacity(5);
    for _ in 0..5 {
        let index = (binary_code % 26) as usize;
        code.push(STEAM_CHARSET[index] as char);
        binary_code /= 26;
    }

    code
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_steam_code_generation_known_vector() {
        let secret = SecretKey::new(b"12345678901234567890".to_vec());
        let code = generate_steam_code(&secret, 1500000000);
        assert_eq!(code, "K23BY");
    }

    #[test]
    fn test_steam_code_generation_format() {
        // Test with arbitrary 20-byte secret
        let secret = SecretKey::new(vec![0x42; 20]);
        let code = generate_steam_code(&secret, 1500000000);

        assert_eq!(code.len(), 5);
        for c in code.chars() {
            assert!(
                STEAM_CHARSET.contains(&(c as u8)),
                "Invalid character in Steam code: {}",
                c
            );
        }
    }
}

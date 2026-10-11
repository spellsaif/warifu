use crate::algorithm::SecretKey;
use data_encoding::BASE32_NOPAD;
use thiserror::Error;
use zeroize::Zeroize;

#[derive(Error, Debug, PartialEq, Eq)]
pub enum Base32Error {
    #[error("Invalid character in base32 string: '{0}'")]
    InvalidCharacter(char),
    #[error("Failed to decode base32 data")]
    DecodeFailed,
    #[error("Secret string is empty")]
    EmptySecret,
}

/// Decodes an RFC 4648 Base32 encoded secret into a secure, zeroizable `SecretKey`.
///
/// Handles common user inputs:
/// - Strips spaces and hyphens (` ` and `-`)
/// - Strips trailing padding (`=`)
/// - Normalizes lowercase to uppercase
pub fn decode_secret(input: &str) -> Result<SecretKey, Base32Error> {
    let mut cleaned: String = input
        .chars()
        .filter(|c| !c.is_whitespace() && *c != '-' && *c != '=')
        .map(|c| c.to_ascii_uppercase())
        .collect();

    if cleaned.is_empty() {
        return Err(Base32Error::EmptySecret);
    }

    // Decode without padding using BASE32_NOPAD
    let decoded = BASE32_NOPAD
        .decode(cleaned.as_bytes())
        .map_err(|_| Base32Error::DecodeFailed)?;

    // Zeroize the intermediate string buffer
    cleaned.zeroize();

    Ok(SecretKey::new(decoded))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_decode_standard_secret() {
        // "Hello!" in Base32 is "JBSWY3DPEE======"
        let secret = decode_secret("JBSWY3DPEE======").unwrap();
        assert_eq!(secret.as_bytes(), b"Hello!");
    }

    #[test]
    fn test_decode_unpadded_and_lowercase() {
        let secret = decode_secret("jbswy3dpee").unwrap();
        assert_eq!(secret.as_bytes(), b"Hello!");
    }

    #[test]
    fn test_decode_with_spaces_and_hyphens() {
        let secret = decode_secret("JBSW-Y3DP EE").unwrap();
        assert_eq!(secret.as_bytes(), b"Hello!");
    }

    #[test]
    fn test_empty_secret_fails() {
        assert_eq!(decode_secret("   --- ==="), Err(Base32Error::EmptySecret));
    }
}

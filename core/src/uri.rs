use crate::algorithm::{Algorithm, Digits, SecretKey};
use crate::base32::decode_secret;
use thiserror::Error;
use url::Url;

/// Maximum permissible length for an incoming URI to prevent memory exhaustion / DoS attacks.
pub const MAX_URI_LENGTH: usize = 2048;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum TokenType {
    Totp,
    Hotp,
    Steam,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ParsedToken {
    pub token_type: TokenType,
    pub issuer: Option<String>,
    pub account_name: String,
    pub secret: SecretKey,
    pub algorithm: Algorithm,
    pub digits: Digits,
    pub period: u64,
    pub counter: u64,
}

#[derive(Error, Debug, PartialEq, Eq)]
pub enum UriParseError {
    #[error("URI length exceeds maximum allowable threshold ({0} > {MAX_URI_LENGTH})")]
    UriTooLong(usize),
    #[error("Invalid URI syntax: {0}")]
    InvalidUrl(String),
    #[error("Unsupported URI scheme (expected 'otpauth', got '{0}')")]
    UnsupportedScheme(String),
    #[error("Unsupported OTP type (expected 'totp' or 'hotp', got '{0}')")]
    UnsupportedType(String),
    #[error("Missing required parameter: 'secret'")]
    MissingSecret,
    #[error("Failed to decode secret: {0}")]
    InvalidSecret(String),
    #[error("Invalid algorithm parameter: '{0}'")]
    InvalidAlgorithm(String),
    #[error("Invalid digits parameter: '{0}'")]
    InvalidDigits(String),
    #[error("Invalid period parameter: '{0}'")]
    InvalidPeriod(String),
    #[error("Invalid counter parameter: '{0}'")]
    InvalidCounter(String),
}

/// Parses a standard RFC-compliant `otpauth://` URI into a `ParsedToken`.
///
/// Format:
/// `otpauth://(totp|hotp)/[Issuer:]Account?secret=...&issuer=...&algorithm=...&digits=...&period=...&counter=...`
pub fn parse_otpauth_uri(uri_str: &str) -> Result<ParsedToken, UriParseError> {
    if uri_str.len() > MAX_URI_LENGTH {
        return Err(UriParseError::UriTooLong(uri_str.len()));
    }

    let url = Url::parse(uri_str).map_err(|e| UriParseError::InvalidUrl(e.to_string()))?;

    if url.scheme() != "otpauth" {
        return Err(UriParseError::UnsupportedScheme(url.scheme().to_string()));
    }

    let token_type = match url.host_str() {
        Some("totp") => TokenType::Totp,
        Some("hotp") => TokenType::Hotp,
        Some(other) => return Err(UriParseError::UnsupportedType(other.to_string())),
        None => return Err(UriParseError::UnsupportedType("missing".to_string())),
    };

    // Parse path: "/[Issuer:]Account"
    let raw_path = url.path().trim_start_matches('/');
    let decoded_path = percent_encoding::percent_decode_str(raw_path)
        .decode_utf8_lossy()
        .to_string();

    let (path_issuer, account_name) = match decoded_path.split_once(':') {
        Some((issuer_part, account_part)) => {
            let issuer = issuer_part.trim().to_string();
            let account = account_part.trim().to_string();
            (if issuer.is_empty() { None } else { Some(issuer) }, account)
        }
        None => (None, decoded_path.trim().to_string()),
    };

    // Query parameters
    let mut secret_str = None;
    let mut query_issuer = None;
    let mut algorithm = Algorithm::Sha1;
    let mut digits = Digits::Six;
    let mut period = 30u64;
    let mut counter = 0u64;

    for (key, value) in url.query_pairs() {
        match key.as_ref() {
            "secret" => {
                secret_str = Some(value.to_string());
            }
            "issuer" => {
                let trimmed = value.trim();
                if !trimmed.is_empty() {
                    query_issuer = Some(trimmed.to_string());
                }
            }
            "algorithm" => {
                algorithm = match value.to_ascii_uppercase().as_str() {
                    "SHA1" => Algorithm::Sha1,
                    "SHA256" => Algorithm::Sha256,
                    "SHA512" => Algorithm::Sha512,
                    other => return Err(UriParseError::InvalidAlgorithm(other.to_string())),
                };
            }
            "digits" => {
                let d: u32 = value
                    .parse()
                    .map_err(|_| UriParseError::InvalidDigits(value.to_string()))?;
                digits = Digits::try_from(d)
                    .map_err(|_| UriParseError::InvalidDigits(value.to_string()))?;
            }
            "period" => {
                period = value
                    .parse()
                    .map_err(|_| UriParseError::InvalidPeriod(value.to_string()))?;
                if period == 0 {
                    return Err(UriParseError::InvalidPeriod("0".to_string()));
                }
            }
            "counter" => {
                counter = value
                    .parse()
                    .map_err(|_| UriParseError::InvalidCounter(value.to_string()))?;
            }
            _ => {
                // Ignore unrecognized query parameters for forward-compatibility
            }
        }
    }

    let secret_raw = secret_str.ok_or(UriParseError::MissingSecret)?;
    let secret = decode_secret(&secret_raw)
        .map_err(|e| UriParseError::InvalidSecret(e.to_string()))?;

    // Query issuer takes precedence over path issuer if both exist
    let final_issuer = query_issuer.or(path_issuer);

    Ok(ParsedToken {
        token_type,
        issuer: final_issuer,
        account_name,
        secret,
        algorithm,
        digits,
        period,
        counter,
    })
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_parse_standard_totp_uri() {
        let uri = "otpauth://totp/GitHub:octocat?secret=JBSWY3DPEHPK3PXP&issuer=GitHub&algorithm=SHA1&digits=6&period=30";
        let parsed = parse_otpauth_uri(uri).unwrap();

        assert_eq!(parsed.token_type, TokenType::Totp);
        assert_eq!(parsed.issuer, Some("GitHub".to_string()));
        assert_eq!(parsed.account_name, "octocat");
        assert_eq!(parsed.algorithm, Algorithm::Sha1);
        assert_eq!(parsed.digits, Digits::Six);
        assert_eq!(parsed.period, 30);
    }

    #[test]
    fn test_parse_hotp_uri_with_counter() {
        let uri = "otpauth://hotp/Server:admin?secret=JBSWY3DPEHPK3PXP&counter=42&digits=8";
        let parsed = parse_otpauth_uri(uri).unwrap();

        assert_eq!(parsed.token_type, TokenType::Hotp);
        assert_eq!(parsed.issuer, Some("Server".to_string()));
        assert_eq!(parsed.account_name, "admin");
        assert_eq!(parsed.counter, 42);
        assert_eq!(parsed.digits, Digits::Eight);
    }

    #[test]
    fn test_parse_percent_encoded_label() {
        let uri = "otpauth://totp/Example%20Corp%3Auser%40example.com?secret=JBSWY3DPEHPK3PXP";
        let parsed = parse_otpauth_uri(uri).unwrap();

        assert_eq!(parsed.issuer, Some("Example Corp".to_string()));
        assert_eq!(parsed.account_name, "user@example.com");
    }

    #[test]
    fn test_reject_oversized_uri() {
        let long_str = "a".repeat(2049);
        let uri = format!("otpauth://totp/Test?secret={}", long_str);
        assert!(matches!(
            parse_otpauth_uri(&uri),
            Err(UriParseError::UriTooLong(_))
        ));
    }

    #[test]
    fn test_reject_missing_secret() {
        let uri = "otpauth://totp/Test?issuer=Test";
        assert_eq!(parse_otpauth_uri(uri), Err(UriParseError::MissingSecret));
    }

    #[test]
    fn test_reject_invalid_scheme() {
        let uri = "https://example.com/totp?secret=JBSWY3DPEHPK3PXP";
        assert!(matches!(
            parse_otpauth_uri(uri),
            Err(UriParseError::UnsupportedScheme(_))
        ));
    }
}

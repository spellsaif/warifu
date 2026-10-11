use crate::algorithm::{Algorithm, Digits, SecretKey};
use crate::uri::{ParsedToken, TokenType};
use data_encoding::BASE64;
use prost::Message;
use thiserror::Error;
use url::Url;

#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash, PartialOrd, Ord, prost::Enumeration)]
#[repr(i32)]
pub enum AlgorithmProto {
    Unspecified = 0,
    Sha1 = 1,
    Sha256 = 2,
    Sha512 = 3,
    Md5 = 4,
}

#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash, PartialOrd, Ord, prost::Enumeration)]
#[repr(i32)]
pub enum DigitCountProto {
    Unspecified = 0,
    Six = 1,
    Eight = 2,
}

#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash, PartialOrd, Ord, prost::Enumeration)]
#[repr(i32)]
pub enum OtpTypeProto {
    Unspecified = 0,
    Hotp = 1,
    Totp = 2,
}

#[derive(Clone, PartialEq, prost::Message)]
pub struct OtpParameters {
    #[prost(bytes = "vec", tag = "1")]
    pub secret: Vec<u8>,
    #[prost(string, tag = "2")]
    pub name: String,
    #[prost(string, tag = "3")]
    pub issuer: String,
    #[prost(enumeration = "AlgorithmProto", tag = "4")]
    pub algorithm: i32,
    #[prost(enumeration = "DigitCountProto", tag = "5")]
    pub digits: i32,
    #[prost(enumeration = "OtpTypeProto", tag = "6")]
    pub otp_type: i32,
    #[prost(int64, tag = "7")]
    pub counter: i64,
}

#[derive(Clone, PartialEq, prost::Message)]
pub struct MigrationPayload {
    #[prost(message, repeated, tag = "1")]
    pub otp_parameters: Vec<OtpParameters>,
    #[prost(int32, tag = "2")]
    pub version: i32,
    #[prost(int32, tag = "3")]
    pub batch_size: i32,
    #[prost(int32, tag = "4")]
    pub batch_index: i32,
    #[prost(int32, tag = "5")]
    pub batch_id: i32,
}

#[derive(Error, Debug, PartialEq, Eq)]
pub enum GoogleMigrationError {
    #[error("Invalid migration URL format: {0}")]
    InvalidUrl(String),
    #[error("Unsupported migration scheme (expected 'otpauth-migration', got '{0}')")]
    UnsupportedScheme(String),
    #[error("Missing 'data' query parameter")]
    MissingDataParameter,
    #[error("Failed to decode base64 payload")]
    Base64DecodeFailed,
    #[error("Failed to decode protobuf MigrationPayload: {0}")]
    ProtobufDecodeFailed(String),
    #[error("Unsupported algorithm: MD5 is insecure and rejected")]
    InsecureMd5Algorithm,
}

/// Decodes an `otpauth-migration://offline?data=...` URI into a list of `ParsedToken`s.
pub fn parse_google_migration_uri(uri_str: &str) -> Result<Vec<ParsedToken>, GoogleMigrationError> {
    let url = Url::parse(uri_str).map_err(|e| GoogleMigrationError::InvalidUrl(e.to_string()))?;

    if url.scheme() != "otpauth-migration" {
        return Err(GoogleMigrationError::UnsupportedScheme(url.scheme().to_string()));
    }

    let mut data_param = None;
    for (k, v) in url.query_pairs() {
        if k == "data" {
            data_param = Some(v.to_string());
            break;
        }
    }

    let raw_b64 = data_param.ok_or(GoogleMigrationError::MissingDataParameter)?;

    // Base64 decode raw bytes
    let payload_bytes = BASE64
        .decode(raw_b64.as_bytes())
        .map_err(|_| GoogleMigrationError::Base64DecodeFailed)?;

    // Decode Protobuf
    let payload = MigrationPayload::decode(payload_bytes.as_slice())
        .map_err(|e| GoogleMigrationError::ProtobufDecodeFailed(e.to_string()))?;

    let mut tokens = Vec::with_capacity(payload.otp_parameters.len());

    for param in payload.otp_parameters {
        let algorithm = match param.algorithm() {
            AlgorithmProto::Sha1 | AlgorithmProto::Unspecified => Algorithm::Sha1,
            AlgorithmProto::Sha256 => Algorithm::Sha256,
            AlgorithmProto::Sha512 => Algorithm::Sha512,
            AlgorithmProto::Md5 => return Err(GoogleMigrationError::InsecureMd5Algorithm),
        };

        let digits = match param.digits() {
            DigitCountProto::Eight => Digits::Eight,
            DigitCountProto::Six | DigitCountProto::Unspecified => Digits::Six,
        };

        let token_type = match param.otp_type() {
            OtpTypeProto::Hotp => TokenType::Hotp,
            _ => TokenType::Totp,
        };

        // Name parsing: Google Authenticator often formats name as "Issuer:Account" or just "Account"
        let (inferred_issuer, account_name) = match param.name.split_once(':') {
            Some((iss, acc)) => (Some(iss.trim().to_string()), acc.trim().to_string()),
            None => (None, param.name.trim().to_string()),
        };

        let final_issuer = if !param.issuer.trim().is_empty() {
            Some(param.issuer.trim().to_string())
        } else {
            inferred_issuer
        };

        tokens.push(ParsedToken {
            token_type,
            issuer: final_issuer,
            account_name,
            secret: SecretKey::new(param.secret),
            algorithm,
            digits,
            period: 30, // Google Authenticator defaults to 30s
            counter: param.counter as u64,
        });
    }

    Ok(tokens)
}

/// Encodes a slice of `ParsedToken`s into an `otpauth-migration://offline?data=...` URI.
pub fn encode_google_migration_uri(tokens: &[ParsedToken]) -> String {
    let otp_params: Vec<OtpParameters> = tokens
        .iter()
        .map(|t| {
            let algo = match t.algorithm {
                Algorithm::Sha1 => AlgorithmProto::Sha1,
                Algorithm::Sha256 => AlgorithmProto::Sha256,
                Algorithm::Sha512 => AlgorithmProto::Sha512,
            };

            let dig = match t.digits {
                Digits::Eight => DigitCountProto::Eight,
                _ => DigitCountProto::Six,
            };

            let otp_type = match t.token_type {
                TokenType::Hotp => OtpTypeProto::Hotp,
                _ => OtpTypeProto::Totp,
            };

            let name = match &t.issuer {
                Some(iss) => format!("{}:{}", iss, t.account_name),
                None => t.account_name.clone(),
            };

            OtpParameters {
                secret: t.secret.as_bytes().to_vec(),
                name,
                issuer: t.issuer.clone().unwrap_or_default(),
                algorithm: algo as i32,
                digits: dig as i32,
                otp_type: otp_type as i32,
                counter: t.counter as i64,
            }
        })
        .collect();

    let payload = MigrationPayload {
        otp_parameters: otp_params,
        version: 1,
        batch_size: 1,
        batch_index: 0,
        batch_id: 1,
    };

    let mut buf = Vec::with_capacity(payload.encoded_len());
    payload.encode(&mut buf).unwrap();

    let b64 = BASE64.encode(&buf);
    format!("otpauth-migration://offline?data={}", b64)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_roundtrip_google_migration() {
        let original_tokens = vec![
            ParsedToken {
                token_type: TokenType::Totp,
                issuer: Some("Google".to_string()),
                account_name: "test@gmail.com".to_string(),
                secret: SecretKey::new(b"secret1234567890".to_vec()),
                algorithm: Algorithm::Sha1,
                digits: Digits::Six,
                period: 30,
                counter: 0,
            },
            ParsedToken {
                token_type: TokenType::Hotp,
                issuer: Some("GitHub".to_string()),
                account_name: "octocat".to_string(),
                secret: SecretKey::new(b"githubsecret1234".to_vec()),
                algorithm: Algorithm::Sha256,
                digits: Digits::Eight,
                period: 30,
                counter: 15,
            },
        ];

        let migration_uri = encode_google_migration_uri(&original_tokens);
        assert!(migration_uri.starts_with("otpauth-migration://offline?data="));

        let decoded_tokens = parse_google_migration_uri(&migration_uri).unwrap();
        assert_eq!(decoded_tokens.len(), 2);

        assert_eq!(decoded_tokens[0].issuer, Some("Google".to_string()));
        assert_eq!(decoded_tokens[0].account_name, "test@gmail.com");
        assert_eq!(decoded_tokens[0].secret.as_bytes(), b"secret1234567890");
        assert_eq!(decoded_tokens[0].algorithm, Algorithm::Sha1);
        assert_eq!(decoded_tokens[0].digits, Digits::Six);
        assert_eq!(decoded_tokens[0].token_type, TokenType::Totp);

        assert_eq!(decoded_tokens[1].issuer, Some("GitHub".to_string()));
        assert_eq!(decoded_tokens[1].account_name, "octocat");
        assert_eq!(decoded_tokens[1].secret.as_bytes(), b"githubsecret1234");
        assert_eq!(decoded_tokens[1].algorithm, Algorithm::Sha256);
        assert_eq!(decoded_tokens[1].digits, Digits::Eight);
        assert_eq!(decoded_tokens[1].token_type, TokenType::Hotp);
        assert_eq!(decoded_tokens[1].counter, 15);
    }

    #[test]
    fn test_reject_md5_migration() {
        let payload = MigrationPayload {
            otp_parameters: vec![OtpParameters {
                secret: vec![1, 2, 3],
                name: "Insecure".to_string(),
                issuer: "Insecure".to_string(),
                algorithm: AlgorithmProto::Md5 as i32,
                digits: DigitCountProto::Six as i32,
                otp_type: OtpTypeProto::Totp as i32,
                counter: 0,
            }],
            version: 1,
            batch_size: 1,
            batch_index: 0,
            batch_id: 1,
        };

        let mut buf = Vec::new();
        payload.encode(&mut buf).unwrap();
        let b64 = BASE64.encode(&buf);
        let uri = format!("otpauth-migration://offline?data={}", b64);

        assert_eq!(
            parse_google_migration_uri(&uri),
            Err(GoogleMigrationError::InsecureMd5Algorithm)
        );
    }
}

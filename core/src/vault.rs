use prost::Message;
use thiserror::Error;

#[derive(Clone, PartialEq, prost::Message)]
pub struct TokenEntry {
    /// Unique immutable identifier (UUIDv4)
    #[prost(string, tag = "1")]
    pub id: String,

    /// Account details
    #[prost(string, tag = "2")]
    pub issuer: String,
    #[prost(string, tag = "3")]
    pub account_name: String,
    #[prost(string, tag = "4")]
    pub notes: String,

    /// Raw secret bytes
    #[prost(bytes = "vec", tag = "5")]
    pub secret: Vec<u8>,

    /// Token properties
    #[prost(int32, tag = "6")]
    pub token_type: i32,
    #[prost(int32, tag = "7")]
    pub algorithm: i32,
    #[prost(uint32, tag = "8")]
    pub digits: u32,
    #[prost(uint32, tag = "9")]
    pub period: u32,
    #[prost(uint64, tag = "10")]
    pub counter: u64,

    /// UI & Metadata
    #[prost(string, tag = "11")]
    pub icon_id: String,
    #[prost(string, repeated, tag = "12")]
    pub tags: Vec<String>,
    #[prost(bool, tag = "13")]
    pub is_pinned: bool,

    /// CRDT LWW Metadata
    #[prost(int64, tag = "14")]
    pub updated_at_unix_ms: i64,
    #[prost(uint64, tag = "15")]
    pub lamport_clock: u64,
}

#[derive(Clone, PartialEq, prost::Message)]
pub struct Tombstone {
    #[prost(string, tag = "1")]
    pub id: String,
    #[prost(int64, tag = "2")]
    pub deleted_at_unix_ms: i64,
    #[prost(uint64, tag = "3")]
    pub lamport_clock: u64,
}

#[derive(Clone, PartialEq, prost::Message)]
pub struct VaultDatabase {
    #[prost(uint32, tag = "1")]
    pub version: u32,
    #[prost(int64, tag = "2")]
    pub created_at_unix_ms: i64,
    #[prost(int64, tag = "3")]
    pub updated_at_unix_ms: i64,

    #[prost(message, repeated, tag = "4")]
    pub tokens: Vec<TokenEntry>,
    #[prost(message, repeated, tag = "5")]
    pub tombstones: Vec<Tombstone>,
}

#[derive(Clone, PartialEq, prost::Message)]
pub struct EncryptedVaultEnvelope {
    #[prost(uint32, tag = "1")]
    pub format_version: u32,
    #[prost(bytes = "vec", tag = "2")]
    pub salt: Vec<u8>,
    #[prost(bytes = "vec", tag = "3")]
    pub nonce: Vec<u8>,
    #[prost(bytes = "vec", tag = "4")]
    pub ciphertext: Vec<u8>,
}

#[derive(Error, Debug, PartialEq, Eq)]
pub enum VaultError {
    #[error("Serialization failed: {0}")]
    SerializationFailed(String),
    #[error("Deserialization failed: {0}")]
    DeserializationFailed(String),
    #[error("Token with ID {0} not found")]
    TokenNotFound(String),
}

impl VaultDatabase {
    pub fn new() -> Self {
        let now = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .unwrap_or_default()
            .as_millis() as i64;

        Self {
            version: 1,
            created_at_unix_ms: now,
            updated_at_unix_ms: now,
            tokens: Vec::new(),
            tombstones: Vec::new(),
        }
    }

    pub fn serialize(&self) -> Result<Vec<u8>, VaultError> {
        let mut buf = Vec::with_capacity(self.encoded_len());
        self.encode(&mut buf)
            .map_err(|e| VaultError::SerializationFailed(e.to_string()))?;
        Ok(buf)
    }

    pub fn deserialize(bytes: &[u8]) -> Result<Self, VaultError> {
        Self::decode(bytes).map_err(|e| VaultError::DeserializationFailed(e.to_string()))
    }

    pub fn add_token(&mut self, token: TokenEntry) {
        self.tokens.push(token);
        self.mark_updated();
    }

    pub fn remove_token(&mut self, id: &str) -> Result<(), VaultError> {
        let pos = self
            .tokens
            .iter()
            .position(|t| t.id == id)
            .ok_or_else(|| VaultError::TokenNotFound(id.to_string()))?;

        let token = self.tokens.remove(pos);
        let now = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .unwrap_or_default()
            .as_millis() as i64;

        self.tombstones.push(Tombstone {
            id: token.id,
            deleted_at_unix_ms: now,
            lamport_clock: token.lamport_clock + 1,
        });

        self.mark_updated();
        Ok(())
    }

    fn mark_updated(&mut self) {
        self.updated_at_unix_ms = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .unwrap_or_default()
            .as_millis() as i64;
    }
}

impl EncryptedVaultEnvelope {
    pub fn serialize(&self) -> Result<Vec<u8>, VaultError> {
        let mut buf = Vec::with_capacity(self.encoded_len());
        self.encode(&mut buf)
            .map_err(|e| VaultError::SerializationFailed(e.to_string()))?;
        Ok(buf)
    }

    pub fn deserialize(bytes: &[u8]) -> Result<Self, VaultError> {
        Self::decode(bytes).map_err(|e| VaultError::DeserializationFailed(e.to_string()))
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_vault_database_roundtrip() {
        let mut vault = VaultDatabase::new();
        vault.add_token(TokenEntry {
            id: "uuid-123".to_string(),
            issuer: "GitHub".to_string(),
            account_name: "octocat".to_string(),
            notes: "".to_string(),
            secret: vec![1, 2, 3, 4],
            token_type: 1,
            algorithm: 1,
            digits: 6,
            period: 30,
            counter: 0,
            icon_id: "github".to_string(),
            tags: vec!["dev".to_string()],
            is_pinned: true,
            updated_at_unix_ms: 1000,
            lamport_clock: 1,
        });

        let serialized = vault.serialize().unwrap();
        let deserialized = VaultDatabase::deserialize(&serialized).unwrap();

        assert_eq!(deserialized.tokens.len(), 1);
        assert_eq!(deserialized.tokens[0].issuer, "GitHub");
        assert_eq!(deserialized.tokens[0].account_name, "octocat");
        assert!(deserialized.tokens[0].is_pinned);
    }

    #[test]
    fn test_token_removal_creates_tombstone() {
        let mut vault = VaultDatabase::new();
        vault.add_token(TokenEntry {
            id: "token-to-delete".to_string(),
            issuer: "Slack".to_string(),
            account_name: "user".to_string(),
            notes: "".to_string(),
            secret: vec![5, 6, 7],
            token_type: 1,
            algorithm: 1,
            digits: 6,
            period: 30,
            counter: 0,
            icon_id: "".to_string(),
            tags: vec![],
            is_pinned: false,
            updated_at_unix_ms: 1000,
            lamport_clock: 5,
        });

        vault.remove_token("token-to-delete").unwrap();
        assert_eq!(vault.tokens.len(), 0);
        assert_eq!(vault.tombstones.len(), 1);
        assert_eq!(vault.tombstones[0].id, "token-to-delete");
        assert_eq!(vault.tombstones[0].lamport_clock, 6);
    }
}

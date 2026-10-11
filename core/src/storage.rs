use crate::vault::{EncryptedVaultEnvelope, VaultError};
use std::fs::{File, OpenOptions};
use std::io::{Read, Write};
use std::path::{Path, PathBuf};
use uuid::Uuid;

/// Saves an `EncryptedVaultEnvelope` to disk atomically.
///
/// Guaranteed crash-safe:
/// 1. Writes payload to a uniquely named temporary file in the same directory (`<file>.tmp.<uuid>`).
/// 2. Executes `sync_all()` (`fsync`) to flush OS kernel buffers to non-volatile storage.
/// 3. Performs an atomic POSIX rename to the target destination path.
///
/// If the OS crashes or the process is killed at any time during write, the existing
/// target file remains uncorrupted.
pub fn save_envelope_atomic(path: &Path, envelope: &EncryptedVaultEnvelope) -> Result<(), std::io::Error> {
    let parent_dir = path.parent().unwrap_or_else(|| Path::new("."));
    std::fs::create_dir_all(parent_dir)?;

    let temp_file_name = format!(
        "{}.tmp.{}",
        path.file_name()
            .and_then(|f| f.to_str())
            .unwrap_or("vault"),
        Uuid::new_v4()
    );
    let temp_path: PathBuf = parent_dir.join(temp_file_name);

    let bytes = envelope
        .serialize()
        .map_err(|e| std::io::Error::new(std::io::ErrorKind::InvalidData, e.to_string()))?;

    // Scope to ensure File is flushed and closed before rename
    {
        let mut file = OpenOptions::new()
            .write(true)
            .create_new(true)
            .open(&temp_path)?;

        file.write_all(&bytes)?;
        file.sync_all()?;
    }

    // Atomic POSIX rename
    if let Err(e) = std::fs::rename(&temp_path, path) {
        let _ = std::fs::remove_file(&temp_path);
        return Err(e);
    }

    Ok(())
}

/// Loads and deserializes an `EncryptedVaultEnvelope` from disk.
pub fn load_envelope(path: &Path) -> Result<EncryptedVaultEnvelope, VaultError> {
    let mut file = File::open(path)
        .map_err(|e| VaultError::DeserializationFailed(format!("Unable to open file: {}", e)))?;

    let mut bytes = Vec::new();
    file.read_to_end(&mut bytes)
        .map_err(|e| VaultError::DeserializationFailed(format!("Read error: {}", e)))?;

    EncryptedVaultEnvelope::deserialize(&bytes)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_atomic_save_and_load_roundtrip() {
        let temp_dir = std::env::temp_dir().join(format!("openauth_test_{}", Uuid::new_v4()));
        std::fs::create_dir_all(&temp_dir).unwrap();
        let target_path = temp_dir.join("vault.bin");

        let envelope = EncryptedVaultEnvelope {
            format_version: 1,
            salt: vec![1, 2, 3],
            nonce: vec![4, 5, 6],
            ciphertext: vec![7, 8, 9, 10],
        };

        save_envelope_atomic(&target_path, &envelope).unwrap();
        assert!(target_path.exists());

        let loaded = load_envelope(&target_path).unwrap();
        assert_eq!(loaded.format_version, 1);
        assert_eq!(loaded.ciphertext, vec![7, 8, 9, 10]);

        // Cleanup
        let _ = std::fs::remove_dir_all(&temp_dir);
    }
}

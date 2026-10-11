use subtle::ConstantTimeEq;
use zeroize::{Zeroize, ZeroizeOnDrop};

/// Supported HMAC digest algorithms for one-time password generation.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum Algorithm {
    #[default]
    Sha1,
    Sha256,
    Sha512,
}

/// Permissible digits for standard OTP codes.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum Digits {
    #[default]
    Six = 6,
    Seven = 7,
    Eight = 8,
}

impl Digits {
    pub fn as_u32(self) -> u32 {
        self as u32
    }

    pub fn modulus(self) -> u32 {
        match self {
            Digits::Six => 1_000_000,
            Digits::Seven => 10_000_000,
            Digits::Eight => 100_000_000,
        }
    }
}

impl TryFrom<u32> for Digits {
    type Error = &'static str;

    fn try_from(value: u32) -> Result<Self, Self::Error> {
        match value {
            6 => Ok(Digits::Six),
            7 => Ok(Digits::Seven),
            8 => Ok(Digits::Eight),
            _ => Err("Invalid digit count: only 6, 7, and 8 digits are supported"),
        }
    }
}

/// Secure container for raw secret bytes.
///
/// Implements `Zeroize` and `ZeroizeOnDrop` so that memory is deterministically
/// cleared with compiler memory barriers when dropped.
/// Implements constant-time equality via `subtle::ConstantTimeEq`.
#[derive(Clone, Zeroize, ZeroizeOnDrop)]
pub struct SecretKey {
    inner: Vec<u8>,
}

impl PartialEq for SecretKey {
    fn eq(&self, other: &Self) -> bool {
        self.inner.ct_eq(&other.inner).into()
    }
}

impl Eq for SecretKey {}


impl SecretKey {
    pub fn new(bytes: Vec<u8>) -> Self {
        Self { inner: bytes }
    }

    pub fn as_bytes(&self) -> &[u8] {
        &self.inner
    }

    pub fn len(&self) -> usize {
        self.inner.len()
    }

    pub fn is_empty(&self) -> bool {
        self.inner.is_empty()
    }
}

impl std::fmt::Debug for SecretKey {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        // Prevent accidental leaking of secret bytes in debug logs
        f.write_str("SecretKey([REDACTED])")
    }
}

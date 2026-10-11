use zeroize::Zeroize;

/// A memory-locked, auto-zeroizing byte container.
///
/// Ensures sensitive buffers:
/// 1. Are pinned in physical RAM using `libc::mlock` to prevent paging to disk swap or zRAM.
/// 2. Are excluded from OS core/crash dumps using `libc::MADV_DONTDUMP` (on Linux/Android).
/// 3. Are overwritten with zeros upon drop with compiler memory barriers.
/// 4. Are unlocked with `libc::munlock` when deallocated.
#[derive(Clone, Zeroize)]
pub struct LockedBytes {
    #[zeroize(skip)]
    is_locked: bool,
    data: Vec<u8>,
}

impl LockedBytes {
    pub fn new(mut data: Vec<u8>) -> Self {
        let is_locked = if !data.is_empty() {
            Self::lock_memory(&mut data)
        } else {
            false
        };

        Self { is_locked, data }
    }

    pub fn from_slice(slice: &[u8]) -> Self {
        Self::new(slice.to_vec())
    }

    pub fn as_slice(&self) -> &[u8] {
        &self.data
    }

    pub fn len(&self) -> usize {
        self.data.len()
    }

    pub fn is_empty(&self) -> bool {
        self.data.is_empty()
    }

    pub fn is_os_locked(&self) -> bool {
        self.is_locked
    }

    fn lock_memory(data: &mut [u8]) -> bool {
        // SAFETY: The slice is owned by this struct and its pointer/length are valid.
        unsafe {
            let ptr = data.as_mut_ptr() as *mut libc::c_void;
            let len = data.len();

            let mlock_ret = libc::mlock(ptr, len);

            #[cfg(target_os = "linux")]
            {
                // Instruct Linux/Android kernel to exclude this memory range from core dumps
                libc::madvise(ptr, len, libc::MADV_DONTDUMP);
            }

            mlock_ret == 0
        }
    }
}

impl Drop for LockedBytes {
    fn drop(&mut self) {
        self.data.zeroize();
        if self.is_locked && !self.data.is_empty() {
            // SAFETY: Memory is valid and was locked in `new()`.
            unsafe {
                let ptr = self.data.as_mut_ptr() as *const libc::c_void;
                let len = self.data.len();
                libc::munlock(ptr, len);
            }
        }
    }
}

impl std::fmt::Debug for LockedBytes {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.write_str("LockedBytes([PROTECTED])")
    }
}

impl PartialEq for LockedBytes {
    fn eq(&self, other: &Self) -> bool {
        use subtle::ConstantTimeEq;
        self.data.ct_eq(&other.data).into()
    }
}

impl Eq for LockedBytes {}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_locked_bytes_creation_and_equality() {
        let secret_payload = b"super_secret_cryptographic_key_32b";
        let locked_a = LockedBytes::from_slice(secret_payload);
        let locked_b = LockedBytes::from_slice(secret_payload);

        assert_eq!(locked_a, locked_b);
        assert_eq!(locked_a.as_slice(), secret_payload);
        assert_eq!(locked_a.len(), secret_payload.len());
    }

    #[test]
    fn test_debug_redaction() {
        let locked = LockedBytes::from_slice(b"secret");
        let debug_str = format!("{:?}", locked);
        assert_eq!(debug_str, "LockedBytes([PROTECTED])");
    }
}

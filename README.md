# Warifu (割符)

> **The open-source, zero-knowledge authenticator engineered like a fortress, not an afterthought.**  
> *Hardware-bound. Memory-locked. 100% offline.*

[![CI](https://github.com/warifu/warifu/actions/workflows/ci.yml/badge.svg)](https://github.com/warifu/warifu/actions)
[![License](https://img.shields.io/badge/license-MIT%20%2F%20Apache--2.0-blue.svg)](#license)
[![Tests](https://img.shields.io/badge/tests-36%20passed%20(100%25)-success.svg)](#empirical-verification)
[![Rust](https://img.shields.io/badge/rust-2021%20edition-orange.svg)](#core-engine)

---

## The Story Behind the Name

Centuries before digital cryptography, feudal Japan used **Warifu (割符)**—a wooden tally inscribed with an official seal and split cleanly down the middle. One half remained with the issuing authority; the other was carried by the traveler. Access was granted only when the physical wood grains and split seals aligned.

**Warifu brings that ancient philosophy to modern silicon**: the authentication server holds half the secret, your device's hardware security module holds the other, and zero third parties ever see what is inside.

---

## Why Warifu Over Google Authenticator?

| Security & Design Dimension | Google Authenticator | Aegis Authenticator | **Warifu (割符)** |
| :--- | :---: | :---: | :---: |
| **Open Source License** | ❌ Closed source (since v2.x) | ✅ GPL-3.0 | ✅ **MIT / Apache-2.0 Dual** |
| **Cross-Platform Architecture** | iOS & Android | ❌ Android only | ✅ **Unified Rust Core (iOS & Android)** |
| **Memory Zeroization Guarantee** | ❌ Managed GC heap | ⚠️ JVM GC heap | ✅ **Deterministic (`Zeroize` + `mlock` in Rust)** |
| **Hardware Biometric Binding** | ❌ Superficial UI check | ✅ StrongBox / TEE | ✅ **StrongBox/TEE & Secure Enclave (`CryptoObject`)** |
| **Google Migration Import/Export** | ✅ Native | ⚠️ Import only | ✅ **Full Bi-directional Import & Export** |
| **Concealed Tokens by Default** | ❌ Visible | ✅ | ✅ **Configurable (Concealed / One-Tap Reveal)** |
| **Next-Code Preview ($\le 5\text{s}$)** | ❌ None | ❌ None | ✅ **Automatic Grace Period ($C+1$) Indicator** |
| **Steam Guard Authentication** | ❌ None | ✅ | ✅ **Native Alphanumeric Engine** |
| **Network Permission** | ⚠️ Connects to Google Cloud | ✅ 100% Offline | ✅ **Zero Network Permission (100% Air-Gapped)** |
| **Fuzz-Tested Parser Engine** | ❌ Unknown | ⚠️ Unit tests | ✅ **2,500 iterations (libFuzzer / proptest)** |

---

## System Architecture

Warifu adopts a **Headless Security Core** architecture:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                             PRESENTATION LAYER                              │
│                                                                             │
│   Android Client (Kotlin + Jetpack Compose)   iOS Client (Swift + SwiftUI)  │
│   - CameraX + ZXing Offline Scanner           - AVFoundation 60fps Scanner  │
│   - StrongBox BiometricPrompt.CryptoObject    - Secure Enclave LAContext    │
│   - WindowManager.FLAG_SECURE                 - Privacy Snapshot Masking    │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       │ UniFFI C-ABI Cross-Platform Bridge
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                            WARIFU SECURITY CORE                             │
│                                                                             │
│   ┌──────────────────────────────────────────────────────────────────────┐  │
│   │ Memory Sanitization Arena (`mlock`, `ZeroizeOnDrop`, `MADV_DONTDUMP`)│  │
│   └──────────────────────────────────┬───────────────────────────────────┘  │
│                                      │                                      │
│   ┌──────────────────────────┐       ▼        ┌──────────────────────────┐  │
│   │ Cryptographic Engine     │ ◄────────────► │ Vault & Storage Engine   │  │
│   │ - RFC 6238 / 4226 TOTP   │                │ - Protocol Buffers v3    │  │
│   │ - Valve Steam Guard      │                │ - Atomic POSIX fsync     │  │
│   │ - Argon2id (m=64MB, t=3) │                │ - Constant-Time Verify   │  │
│   │ - XChaCha20-Poly1305     │                │ - Google Migration Proto │  │
│   └──────────────────────────┘                └──────────────────────────┘  │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      HARDWARE SECURITY MODULES (HSM)                        │
│                                                                             │
│   Android StrongBox Keymaster (Titan M / SE)   Apple Secure Enclave (SEP)   │
│   - Hardware-backed AES-256 KEK                - 256-bit Secure Enclave KEK │
│   - Biometric sensor cryptographic gate        - `kSecAccessControlBiometry`│
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Inviolable Security Invariants

1. **Zero Plaintext Persistence**: Secret seeds and vault databases are encrypted under authenticated XChaCha20-Poly1305 envelopes before ever touching disk.
2. **Pinned Memory (`mlock`)**: Sensitive memory is locked into volatile physical RAM using `libc::mlock` and excluded from core dumps via `libc::MADV_DONTDUMP`.
3. **Deterministic Zeroization**: Compilers are prevented from eliminating memory-scrubbing instructions via explicit memory barriers (`zeroize`).
4. **Hardware-Bound Biometrics**: Biometric authentication is a physical cryptographic unwrap; bypassing the UI yields cryptographic noise.
5. **Constant-Time Verification**: All token comparisons use `subtle::ConstantTimeEq` to prevent side-channel timing attacks.
6. **Zero Telemetry**: No network permissions, no analytics SDKs, and no tracking pings.

---

## Repository Structure

```
├── README.md                    # Project overview & story
├── PRD.md                       # Product requirements & KPIs
├── architecture.md              # Deep system & cryptographic architecture
├── spec-requirement.md          # Protocol buffer schemas & specifications
├── rulebook.md                  # 5 Inviolable security invariants
├── features.md                  # Comprehensive competitive feature matrix
├── tech.md                      # Dependency manifest & hardware benchmarks
│
├── core/                        # Rust Core Library (31 unit tests passing)
│   ├── src/algorithm.rs         # Zeroized SecretKey & constant-time math
│   ├── src/hotp.rs              # RFC 4226 dynamic truncation
│   ├── src/totp.rs              # RFC 6238 TOTP (SHA-1/256/512) & grace preview
│   ├── src/steam.rs             # Valve Steam Guard 5-char alphanumeric engine
│   ├── src/uri.rs               # Strict DoS-bounded otpauth:// URI parser
│   ├── src/google_migration.rs  # Bidirectional Google Authenticator protobuf parser
│   ├── src/memory.rs            # Pinned RAM (mlock) & auto-zeroizing buffers
│   ├── src/crypto.rs            # Argon2id KDF & XChaCha20-Poly1305 AEAD
│   ├── src/storage.rs           # Crash-safe atomic POSIX file persistence
│   └── src/ffi.rs               # UniFFI VaultSession mobile bridge
│
├── bindings/                    # Generated Cross-Platform Mobile Bindings
│   ├── kotlin/                  # Generated Kotlin package for Android
│   └── swift/                   # Generated Swift package & module maps for iOS
│
├── android/                     # Native Android App (Jetpack Compose)
│   └── app/src/main/            # StrongBox Keystore, CameraX + ZXing scanner
│
├── ios/                         # Native iOS App (SwiftUI)
│   └── Warifu/                  # Secure Enclave Face ID, AVFoundation scanner
│
└── fuzz/                        # libFuzzer and proptest fuzzing harnesses
```

---

## Quickstart & Verification

### 1. Run Unit Tests & RFC Test Vectors
```bash
cd core
cargo test --all-targets
```

### 2. Run Property-Based Fuzz Tests (2,500 iterations)
```bash
cd core
cargo test --test fuzz_properties
```

### 3. Clippy Strict Linter
```bash
cd core
cargo clippy --all-targets --all-features -- -D clippy::all
```

---

## License

Dual-licensed under either:
* **MIT License** ([LICENSE-MIT](LICENSE-MIT) or http://opensource.org/licenses/MIT)
* **Apache License, Version 2.0** ([LICENSE-APACHE](LICENSE-APACHE) or http://www.apache.org/licenses/LICENSE-2.0)

at your option.

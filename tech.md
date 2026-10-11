# Technology Stack, Cryptographic Specifications & Dependency Manifest
## Project: OpenAuthenticator Technology Architecture

---

### 1. Technology Stack Architecture

```
┌────────────────────────────────────────────────────────────────────────┐
│                        PRESENTATION CLIENTS                            │
├───────────────────────────────────┬────────────────────────────────────┤
│           Android Client          │             iOS Client             │
│ Language: Kotlin 1.9+             │ Language: Swift 5.9+               │
│ UI Toolkit: Jetpack Compose       │ UI Toolkit: SwiftUI                │
│ Min SDK: API 26 (Android 8.0)     │ Deployment Target: iOS 16.0+       │
│ Camera: CameraX 1.3+              │ Camera: AVFoundation               │
│ QR Engine: ZXing-cpp (via NDK)    │ QR Engine: Native AVCaptureMetadata│
│ Biometrics: AndroidX Biometric    │ Biometrics: LocalAuthentication    │
└───────────────────────────────────┴────────────────────────────────────┘
                                    │
                                    │ UniFFI (C-ABI / Swift & Kotlin Bindings)
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                      CORE SECURITY ENGINE (RUST)                       │
│ Language: Rust 2021 Edition (MSRV 1.75+)                               │
│ Target Architectures:                                                  │
│ - Android: aarch64-linux-android, armv7-linux-androideabi,             │
│            x86_64-linux-android (emulator)                             │
│ - iOS:     aarch64-apple-ios, aarch64-apple-ios-sim (Apple Silicon),   │
│            x86_64-apple-ios (Intel simulator)                          │
└────────────────────────────────────────────────────────────────────────┘
```

---

### 2. Core Rust Crate Manifest (`Cargo.toml`)

All dependencies are locked, audited for supply-chain vulnerabilities, and selected for constant-time cryptographic performance:

```toml
[package]
name = "openauthenticator-core"
version = "0.1.0"
edition = "2021"
license = "MIT OR Apache-2.0"
authors = ["OpenAuthenticator Contributors"]

[lib]
crate-type = ["staticlib", "cdylib", "lib"]
name = "openauthenticator_core"

[dependencies]
# 1. Cryptographic Primitives & HMAC
ring = "0.17"                   # BoringSSL-derived, assembly-optimized HMAC-SHA1/SHA256/SHA512
chacha20poly1305 = { version = "0.10", features = ["alloc"] } # XChaCha20-Poly1305 AEAD
argon2 = { version = "0.5", features = ["alloc", "rand"] }    # Memory-hard KDF (RFC 9106)
subtle = "2.5"                  # Constant-time comparison primitives (anti-timing attacks)

# 2. Memory Sanitization & Paging Locks
zeroize = { version = "1.7", features = ["derive", "zeroize_derive"] } # Deterministic zeroing
secrecy = { version = "0.8", features = ["zeroize"] }                 # Type-level secret wrappers
libc = "0.2"                    # Direct mlock(2) and MADV_DONTDUMP system calls

# 3. Serialization & Encodings
prost = "0.12"                  # High-performance Protocol Buffers v3 runtime
data-encoding = "2.5"           # Strict RFC 4648 Base32 / Base64 implementation
serde = { version = "1.0", features = ["derive"] }
serde_json = "1.0"              # Used solely for third-party import parsers (Aegis/2FAS)

# 4. FFI Bindings & Utilities
uniffi = { version = "0.26", features = ["cli"] } # Mozilla UniFFI binding generator
thiserror = "1.0"               # Strongly-typed error definitions
uuid = { version = "1.7", features = ["v4", "fast-rng"] } # Monotonic UUIDv4 generation
url = "2.5"                     # RFC-compliant URI parsing for otpauth:// schemes

[dev-dependencies]
criterion = "0.5"               # Nanosecond-level statistical benchmarking
proptest = "1.4"                # Property-based fuzz testing
```

---

### 3. Cryptographic Primitive Specifications

| Primitive Type | Algorithm | Implementation / Standard | Security Parameters |
| :--- | :--- | :--- | :--- |
| **Symmetric Vault Encryption** | `XChaCha20-Poly1305` | RustCrypto / RFC 8439 draft | Key: 256 bits, Nonce: 192 bits (CSPRNG), MAC: 128 bits |
| **Password Key Derivation (KDF)** | `Argon2id` | RFC 9106 (`argon2` crate) | $m = 64\,\text{MiB}$, $t = 3$ passes, $p = 4$ lanes, Salt: 256 bits |
| **Backup Key Derivation** | `Argon2id` | RFC 9106 (`argon2` crate) | $m = 64\,\text{MiB}$, $t = 4$ passes, $p = 4$ lanes, Salt: 256 bits |
| **HMAC Digest Engine** | HMAC-SHA1, HMAC-SHA256, HMAC-SHA512 | `ring` (Assembly optimized) | RFC 2104 / RFC 6238 compliance |
| **Hardware Key Encryption (Android)** | AES-256-GCM | Android KeyStore (`StrongBox` / `TEE`) | Key: 256 bits, Tag: 128 bits, Bound to Biometric auth |
| **Hardware Key Encryption (iOS)** | 256-bit Key | Apple Secure Enclave (`SEP`) | `kSecAccessControlBiometryCurrentSet` |
| **Entropy Source** | OS CSPRNG | `getrandom` (Linux/Android), `SecRandomCopyBytes` (Apple) | Direct kernel entropy pool (`/dev/urandom` / `getrandom(2)`) |

---

### 4. Hardware Security & Acceleration Benchmarks

#### 4.1 ARM Cryptographic Extensions
* Modern mobile CPUs (ARM Cortex-A78, Cortex-X3, Apple A15-A17) implement **ARMv8-A Cryptographic Extensions** (hardware SHA and AES instructions).
* `ring` leverages hardware-accelerated SHA extensions for HMAC calculations:
  * Single HMAC-SHA1 computation: $\approx \mathbf{0.85\,\mu\text{s}}$ ($850\,\text{ns}$).
  * Total time to calculate 100 TOTP tokens on ARM64: $\mathbf{< 0.12\,\text{ms}}$.

#### 4.2 StrongBox vs. TEE Performance Profile (Android)
* **TEE (Trusted Execution Environment)**: Runs on the application processor via ARM TrustZone. Fast (~$1.5\,\text{ms}$ per crypto op), protected against OS kernel compromise.
* **StrongBox Keymaster**: Runs on dedicated physical silicon with its own isolated RAM/CPU (e.g., Titan M2 on Google Pixel, NXP SE). Tamper-resistant, physical-probe resistant. Slower (~$25\,\text{ms} - 45\,\text{ms}$ per crypto op).
* **Architectural Decision**: OpenAuthenticator uses StrongBox to unwrap the **Vault Key (VK)** *once* upon application unlock, holding the decrypted VK in locked RAM (`mlock`). This provides maximum physical tamper resistance with zero impact on UI scrolling or code generation speed.

---

### 5. Build, Cross-Compilation & CI/CD Toolchain

#### 5.1 Android Compilation Toolchain
* **NDK Version**: NDK r26b+
* **Build Tool**: `cargo-ndk`
* Target triples:
  ```bash
  cargo ndk -t arm64-v8a -t armeabi-v7a -t x86_64 -o ../android/app/src/main/jniLibs build --release
  ```
* UniFFI Binding Generation:
  ```bash
  cargo run --bin uniffi-bindgen generate src/openauth.udl --language kotlin --out-dir ../android/app/src/main/java/org/openauth/core
  ```

#### 5.2 iOS Compilation Toolchain
* Target triples:
  * `aarch64-apple-ios` (Physical devices)
  * `aarch64-apple-ios-sim` (Apple Silicon iOS Simulator)
  * `x86_64-apple-ios` (Intel iOS Simulator)
* Bundling: Packaged as an **XCFramework** using `xcodebuild -create-xcframework` combining static libraries and UniFFI Swift module maps.

#### 5.3 Reproducible CI Build Pipeline
* **Environment**: Containerized Ubuntu 22.04 LTS Docker image with pinned Rust toolchain, Android NDK, and Gradle versions.
* **Reproducibility Checks**: Bit-for-bit checksum validation of release artifacts.
* **Security Automation**:
  * `cargo audit` (Vulnerability scanning against RustSec Advisory Database).
  * `cargo deny check` (License compliance and ban enforcement).
  * `cargo fuzz` (LibFuzzer integration with Google OSS-Fuzz compatibility).

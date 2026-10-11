# Engineering, Security & Contribution Rulebook
## Project: OpenAuthenticator Development Standard

---

### 1. Inviolable Security Invariants

Any pull request or code change that violates any of the following invariants will be rejected immediately without exception:

1. **Rule 1: Zero Plaintext Persistence**
   * Raw secret keys, unencrypted seeds, and plaintext vault payloads must NEVER touch non-volatile storage (disk, flash, NVMe, internal cache).
   * All disk persistence must be wrapped in an authenticated encryption envelope (`XChaCha20-Poly1305` or `AES-256-GCM`).
2. **Rule 2: Deterministic Memory Zeroization**
   * Every in-memory representation of a secret (`SecretKey`, `VaultKey`, intermediate HMAC pads, decrypted protobuf bytes) must be enclosed in types that implement `Zeroize` and `ZeroizeOnDrop`.
   * Standard library types (`String`, `Vec<u8>`) must NEVER hold raw secrets unless wrapped in `secrecy::Secret<T>` or pinned memory structures.
3. **Rule 3: Cryptographic Hardware-Bound Biometrics**
   * UI-level boolean authentication checks (`if (isBiometricSuccess) { unlock(); }`) are strictly forbidden.
   * Biometric unlock must always require the cryptographic unwrapping of the hardware-backed Key Encryption Key (`CryptoObject` on Android, `kSecAccessControl` on iOS).
4. **Rule 4: Zero External Network Telemetry**
   * The core application must contain zero tracking SDKs, zero analytics dependencies, and zero crash-reporting pings to external servers.
   * Network requests are strictly limited to user-initiated sync (WebDAV/relays) or optional, user-toggled NTP drift calibration.
5. **Rule 5: Constant-Time Comparisons**
   * All cryptographic token comparisons, MAC validations, and password hash verifications must execute using constant-time comparison algorithms (`subtle::ConstantTimeEq`) to prevent side-channel timing attacks.

---

### 2. Language & Coding Standards

#### 2.1 Rust Security Core
* **Strict Compiler Flags**:
  * All code must compile with zero warnings: `RUSTFLAGS="-D warnings"`.
  * `clippy` checks are enforced on CI: `cargo clippy --all-targets --all-features -- -D clippy::all -D clippy::pedantic -A clippy::must_use_candidate`.
* **No `unsafe` Policy**:
  * `#![forbid(unsafe_code)]` must be placed at the crate root of all business logic crates.
  * The only permissible `unsafe` blocks are strictly inside the low-level memory allocation crate (`mlock` system calls via `libc`), and each instance must include a detailed `// SAFETY:` rationale comment.
* **Error Handling**:
  * Never call `.unwrap()` or `.expect()` in production code paths. Use strongly-typed `thiserror` enums and the `?` operator.

#### 2.2 Android (Kotlin)
* **Architecture**: Jetpack Compose adhering to strict Unidirectional Data Flow (UDF / MVI).
* **Static Analysis**: `detekt` and Android Lint must run in CI with zero baseline suppressions for security or memory issues.
* **Lifecycle Awareness**: ViewModels must never retain references to plaintext token lists across application backgrounding events; all cached memory must be cleared when the app moves to `Lifecycle.State.STOPPED`.

#### 2.3 iOS (Swift)
* **Architecture**: SwiftUI with strict Observation framework (`@Observable`).
* **Static Analysis**: `SwiftLint` with strict rules enabled; zero compiler warnings allowed.
* **Memory Management**: Avoid capturing `self` strongly in background timers or closures; use `[weak self]` to guarantee immediate deallocation.

---

### 3. Cryptographic Implementation Guidelines

* **Never Roll Your Own Crypto**: Always use audited, peer-reviewed cryptographic libraries (`ring`, `aws-lc-rs`, `chacha20poly1305`, `argon2`).
* **Entropy Sources**: All random numbers (salts, nonces, key generation) must originate from the operating system's Cryptographically Secure Pseudorandom Number Generator (CSPRNG):
  * Rust: `rand::rngs::OsRng` / `getrandom` crate.
  * Android: `java.security.SecureRandom`.
  * iOS: `SecRandomCopyBytes`.
* **Argon2id Minimum Parameters**:
  * Memory: $m \ge 64\,\text{MiB}$ ($65,536\,\text{KiB}$)
  * Iterations: $t \ge 3$
  * Parallelism: $p \ge 4$
  * Salt length: $\ge 32$ bytes

---

### 4. Testing, Verification & Fuzzing Mandates

No feature will be merged into `main` without satisfying the three testing tiers:

#### Tier 1: Deterministic Test Vectors (Unit Testing)
* 100% of the official test vectors from **RFC 4226 (Appendix D)** and **RFC 6238 (Appendix B)** must pass.
* Google’s **Project Wycheproof** test suites for HMAC-SHA1, HMAC-SHA256, and AES-GCM must be integrated and verified.

#### Tier 2: Fuzz Testing (Differential & Robustness Testing)
* All external parsers (`otpauth://` URIs, Google Migration protobufs, Aegis JSON schemas, QR byte streams) must be instrumented with `cargo-fuzz` (libFuzzer).
* CI runs continuous fuzzing regression checks. Parsers must not crash or panic on arbitrarily malformed, truncated, or cyclic inputs.

#### Tier 3: Memory Dump & Leak Verification
* Automated memory analysis test: The test decrypts a test vault, generates 100 codes, drops all handles, triggers `drop()`, and performs a core dump analysis to assert that the test secret strings do NOT appear in the process memory footprint.

---

### 5. Git Governance, Supply Chain & Reproducible Builds

1. **Commit Signing**: All git commits must be cryptographically signed using GPG or SSH keys verified against GitHub/GitLab identity.
2. **Dependency Auditing**:
   * Rust: `cargo-audit` and `cargo-deny` run on every PR to detect known CVEs and restrict unapproved licenses (only MIT, Apache-2.0, BSD-3-Clause, or MPL-2.0 permitted).
   * Dependency versions must be strictly pinned in `Cargo.lock`.
3. **Reproducible Builds**:
   * Android APKs and iOS binaries must be deterministically reproducible via Dockerized CI builds.
   * Every release publishes an automated **Software Bill of Materials (SBOM)** in CycloneDX and SPDX formats alongside SHA-256 checksums.

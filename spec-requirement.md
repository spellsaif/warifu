# Specification & Requirements Document
## Project: Warifu Technical Specifications

---

### 1. Functional Requirements (FR)

#### 1.1 Algorithmic & Cryptographic Engine
* **FR-01: RFC 6238 TOTP Engine:**
  * Must support Hash algorithms: HMAC-SHA-1, HMAC-SHA-256, and HMAC-SHA-512.
  * Must support Output Digits: 6, 7, and 8 digits (zero-padded).
  * Must support Time Steps ($X$): 15s, 30s, 60s, and 120s (default: 30s).
  * Must compute codes based on the current Unix epoch time $T$.
* **FR-02: RFC 4226 HOTP Engine:**
  * Must support Counter-based generation starting at arbitrary 64-bit integer counters ($C \ge 0$).
  * Must increment counter only upon explicit user invocation (e.g., "Generate Next Code" button).
* **FR-03: Steam Guard Compatibility:**
  * Must decode standard Base32 secrets.
  * Must derive 5-character alphanumeric codes using the Steam character set: `23456789BCDFGHJKMNPQRTVWXY`.
* **FR-04: Clock Drift Compensation ($\Delta t$):**
  * Must maintain a persistent, signed offset $\Delta t$ in seconds: $T_{\text{effective}} = T_{\text{system}} + \Delta t$.
  * Must allow manual drift calibration and optional automated drift calibration via HTTP `Date` header parsing against NIST/Cloudflare time endpoints over TLS.

#### 1.2 Import, Export & Parsing
* **FR-05: Standard URI Parsing (`otpauth://`):**
  * Must parse standards-compliant URIs: `otpauth://(totp|hotp)/[Issuer:]Account?secret=...&issuer=...&algorithm=...&digits=...&period=...&counter=...`.
  * Must reject URIs with missing or invalid `secret` parameters.
  * Must tolerate unpadded Base32 and lowercase Base32 strings.
  * Must cap maximum URI length at 2048 bytes to mitigate memory exhaustion.
* **FR-06: Google Authenticator Migration Ingestion:**
  * Must parse URIs matching schema `otpauth-migration://offline?data=<Base64EncodedProtobuf>`.
  * Must decode the Google Migration Protocol Buffer payload, unpack multiple accounts in a single batch, and add them to the vault atomically.
* **FR-07: Aegis & 2FAS Import Support:**
  * Must decrypt and import Aegis JSON vaults (Scrypt / Argon2id + AES-256-GCM).
  * Must import unencrypted 2FAS / Bitwarden JSON exports.
* **FR-08: Encrypted Vault Export:**
  * Must allow export of the entire vault encrypted with user-defined passphrase using Argon2id ($m=64\,\text{MB}, t=4, p=4$) and XChaCha20-Poly1305.

#### 1.3 Vault Management & UX
* **FR-09: Token Masking (Concealment):**
  * All tokens must be obfuscated by default (`••••••`).
  * Tap on item must reveal code or directly copy to clipboard according to user preference.
* **FR-10: Next-Code Preview:**
  * When $T_{\text{remaining}} \le 5\,\text{s}$, the UI must display a warning indicator and compute $C+1$ code in memory, offering a "Copy Next Code" action.
* **FR-11: Categorization & Organization:**
  * Must support custom groups/tags, custom icons (from a predefined offline SVG icon pack), and favorited/pinned tokens.
  * Must provide fuzzy search matching against `issuer`, `account_name`, and `tags`.

---

### 2. Non-Functional Requirements (NFR)

#### 2.1 Performance
* **NFR-01 (Launch Time):** Cold launch to interactive token list must be $\le 150\,\text{ms}$ on flagship devices and $\le 300\,\text{ms}$ on mid-range devices.
* **NFR-02 (Evaluation Latency):** TOTP code calculation for a vault containing 200 tokens must take $\le 1\,\text{ms}$ total.
* **NFR-03 (Frame Rate):** Animation frame rate (progress timers and list scrolling) must maintain consistent 60fps / 120fps (ProMotion) without UI thread blocking.

#### 2.2 Security & Hardening
* **NFR-04 (Zeroization):** 100% of heap and stack buffers containing raw secret keys, decrypted vault buffers, and intermediate HMAC keys must be zeroized using `ZeroizeOnDrop` before memory reclamation.
* **NFR-05 (Screen Leak Prevention):** 
  * Android: Must enable `FLAG_SECURE` across all application activities.
  * iOS: Must mask views with a blur effect or blank canvas upon `sceneWillDeactivateNotification`.
* **NFR-06 (Clipboard Safety):**
  * Android: Must set `ClipDescription.EXTRA_IS_SENSITIVE` on copied tokens.
  * Both platforms: Background task must clear clipboard content after 30 seconds if the clipboard still contains the copied OTP.
* **NFR-07 (Zero Telemetry):** Zero third-party analytics SDKs, zero crash-reporting servers (unless user opts into local file export), and zero external network calls in core mode.

#### 2.3 Portability & Reliability
* **NFR-08 (Zero Cloud Dependency):** App must be 100% functional in airplane mode / air-gapped environments.
* **NFR-09 (Crash Safety):** All disk write operations must be atomic (`renameat2` / atomic POSIX rename). App termination at any point during write must leave the existing vault uncorrupted.

---

### 3. Complete Protocol Buffer Schema Specification (`vault.proto`)

The application vault format is serialized using Protocol Buffers version 3.

```protobuf
syntax = "proto3";

package warifu.vault;

enum Algorithm {
  ALGORITHM_UNSPECIFIED = 0;
  ALGORITHM_SHA1 = 1;
  ALGORITHM_SHA256 = 2;
  ALGORITHM_SHA512 = 3;
}

enum TokenType {
  TOKEN_TYPE_UNSPECIFIED = 0;
  TOKEN_TYPE_TOTP = 1;
  TOKEN_TYPE_HOTP = 2;
  TOKEN_TYPE_STEAM = 3;
}

message TokenEntry {
  // Unique immutable identifier (UUIDv4)
  string id = 1;
  
  // Account details
  string issuer = 2;
  string account_name = 3;
  string notes = 4;
  
  // Raw secret bytes (decoded from Base32)
  bytes secret = 5;
  
  // Token properties
  TokenType token_type = 6;
  Algorithm algorithm = 7;
  uint32 digits = 8;        // e.g. 6, 7, 8
  uint32 period = 9;        // e.g. 30, 60 seconds
  uint64 counter = 10;      // HOTP counter
  
  // UI & Metadata
  string icon_id = 11;
  repeated string tags = 12;
  bool is_pinned = 13;
  
  // CRDT LWW Metadata
  int64 updated_at_unix_ms = 14;
  uint64 lamport_clock = 15;
}

message Tombstone {
  string id = 1;
  int64 deleted_at_unix_ms = 2;
  uint64 lamport_clock = 3;
}

message VaultDatabase {
  uint32 version = 1;
  int64 created_at_unix_ms = 2;
  int64 updated_at_unix_ms = 3;
  
  repeated TokenEntry tokens = 4;
  repeated Tombstone tombstones = 5;
}

message EncryptedVaultEnvelope {
  uint32 format_version = 1;
  bytes salt = 2;           // 32-byte KDF salt (if password-protected)
  bytes nonce = 3;          // 24-byte XChaCha20 nonce
  bytes ciphertext = 4;     // Encrypted VaultDatabase payload
  bytes auth_tag = 5;       // 16-byte Poly1305 MAC tag
}
```

---

### 4. Google Migration Protocol Buffer Schema (`google_migration.proto`)

For 100% interoperability with Google Authenticator QR migration payloads:

```protobuf
syntax = "proto3";

package google.auth.migration;

message MigrationPayload {
  enum Algorithm {
    ALGO_UNSPECIFIED = 0;
    ALGO_SHA1 = 1;
    ALGO_SHA256 = 2;
    ALGO_SHA512 = 3;
    ALGO_MD5 = 4;
  }

  enum DigitCount {
    DIGIT_COUNT_UNSPECIFIED = 0;
    DIGIT_COUNT_SIX = 1;
    DIGIT_COUNT_EIGHT = 2;
  }

  enum OtpType {
    OTP_TYPE_UNSPECIFIED = 0;
    OTP_TYPE_HOTP = 1;
    OTP_TYPE_TOTP = 2;
  }

  message OtpParameters {
    bytes secret = 1;
    string name = 2;
    string issuer = 3;
    Algorithm algorithm = 4;
    DigitCount digits = 5;
    OtpType type = 6;
    int64 counter = 7;
  }

  repeated OtpParameters otp_parameters = 1;
  int32 version = 2;
  int32 batch_size = 3;
  int32 batch_index = 4;
  int32 batch_id = 5;
}
```

---

### 5. Platform Compatibility Matrix

| Platform | Minimum OS Version | Target OS Version | Hardware Security Integration |
| :--- | :--- | :--- | :--- |
| **Android** | Android 8.0 (API 26) | Android 14+ (API 34+) | StrongBox Keymaster (fallback to ARM TrustZone TEE); `BiometricPrompt.CryptoObject` |
| **iOS** | iOS 16.0 | iOS 17+ | Apple Secure Enclave (SEP); Keychain Services with `kSecAccessControlBiometryCurrentSet` |
| **Wear OS** | Wear OS 3.0 (API 30) | Wear OS 4.0+ | Encrypted local token mirror synced via Bluetooth RFCOMM channel with mutual authentication |
| **watchOS** | watchOS 9.0 | watchOS 10.0+ | Encrypted token mirror via WatchConnectivity framework with AES-GCM transport |

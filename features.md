# Feature Matrix & Functional Breakdown
## Project: Warifu Capabilities

---

### 1. Competitive Architecture & Feature Comparison

| Feature / Dimension | Google Authenticator | Aegis Authenticator | 2FAS | Ente Auth | **Warifu (This Project)** |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **Open Source License** | ❌ Proprietary (since v2.x) | ✅ GPL-3.0 | ✅ GPL-3.0 | ✅ GPL-3.0 | ✅ **MIT / Apache-2.0 Dual** |
| **Cross-Platform Support** | iOS & Android | ❌ Android only | iOS & Android | iOS, Android, Desktop | ✅ **iOS & Android (Unified Rust Core)** |
| **Memory Zeroization Guarantee** | ❌ Java/ObjC GC | ⚠️ JVM GC Heap | ⚠️ Swift/JVM GC Heap | ⚠️ Flutter Dart GC | ✅ **Guaranteed (`Zeroize` + `mlock` in Rust)** |
| **Hardware Biometric Crypto Binding** | ❌ (Basic UI check) | ✅ Android StrongBox/TEE | ⚠️ OS Keychain | ⚠️ OS Keychain | ✅ **StrongBox/TEE & Secure Enclave (`CryptoObject`)** |
| **E2EE Cloud Sync** | ⚠️ Proprietary Google Sync | ❌ (Local/SAF only) | ✅ iCloud / GDrive | ✅ Custom E2EE Relay | ✅ **CRDT E2EE (WebDAV / Nextcloud / Blind Relay)** |
| **Google Migration Import/Export** | ✅ Native | ✅ Import only | ✅ Import only | ✅ Import only | ✅ **Full Bi-directional Import & Export** |
| **Concealed Codes by Default** | ❌ | ✅ | ✅ | ✅ | ✅ **Configurable (Concealed / One-Tap Reveal)** |
| **Next-Code Preview ($\le 5\text{s}$)** | ❌ | ❌ | ❌ | ❌ | ✅ **Automatic Next-Code ($C+1$) Indicator** |
| **Steam Guard Authentication** | ❌ | ✅ | ✅ | ❌ | ✅ **Native Alphanumeric Support** |
| **SHA-256 / SHA-512 Support** | ⚠️ Buggy / Unreliable | ✅ | ✅ | ✅ | ✅ **100% RFC 6238 Compliant** |
| **Air-Gapped / Zero Network Mode** | ⚠️ Requests Account | ✅ 100% Offline | ⚠️ | ⚠️ Cloud-first | ✅ **100% Offline by Default** |
| **Fuzz-Tested Parser Engine** | ❌ Unknown | ⚠️ Unit tests | ⚠️ Unit tests | ⚠️ Unit tests | ✅ **Continuous `libFuzzer` in CI** |

---

### 2. Detailed Functional Breakdown

#### 2.1 Core Authentication Engine
* **RFC 6238 TOTP Engine**:
  * Digest algorithms: HMAC-SHA1, HMAC-SHA256, HMAC-SHA512.
  * Code lengths: 6, 7, 8 digits.
  * Time steps: 15 seconds, 30 seconds (standard), 60 seconds, 120 seconds.
* **RFC 4226 HOTP Engine**:
  * Event/counter-based token generation.
  * Visual counter increment button with confirmation to prevent desynchronization.
* **Steam Guard Mobile Authenticator**:
  * Decodes Base32 secret seeds into Steam's proprietary 5-character alphabet (`23456789BCDFGHJKMNPQRTVWXY`).
* **Clock Skew Correction**:
  * Automatically calculates time-drift offsets against TLS server `Date` headers without transmitting identifying account information.
  * Manual drift adjustment slider (-120s to +120s).

#### 2.2 User Interface & Interaction Design
* **Sub-150ms Launch**: Clean, declarative UI with zero bloatware or splash-screen delays.
* **Concealed Tokens**:
  * Masked tokens displayed as `••••••` to defeat shoulder-surfing.
  * Single tap reveals the code for 10 seconds; double tap copies directly to the clipboard.
* **High-Contrast Monotonic Timers**:
  * Circular progress ring synchronized with the hardware display refresh rate (60Hz / 120Hz ProMotion).
  * Color transitions: Green ($>10\text{s}$), Amber ($5-10\text{s}$), Red ($<5\text{s}$).
* **Next-Code Grace Period Preview**:
  * When $<5$ seconds remain on the current token, a subtle badge reveals the upcoming code so users don't copy an expired token right before form submission.
* **Organization & Organization Power Tools**:
  * Nested folders, customizable color tags, and favorite pinning.
  * High-speed fuzzy search filtering across issuer, account name, and tags.
  * Built-in vector icon repository containing $>500$ popular service brand icons bundled offline.

#### 2.3 Camera & Barcode Scanner Pipeline
* **Zero-Cloud QR Scanner**:
  * Uses **CameraX** (Android) and **AVFoundation** (iOS) integrated with an embedded native C++/Rust port of **ZXing-cpp**.
  * Operates 100% offline without requiring Google Play Services or Apple Vision cloud connections.
  * Instant auto-detection, auto-focus, and pinch-to-zoom torch support.
* **Batch QR Scanning**:
  * Ability to scan multiple QR codes consecutively when setting up multiple 2FA services in one session.

#### 2.4 Import & Export Capabilities
* **Google Authenticator Ingestion & Emulation**:
  * Direct scanning and decoding of `otpauth-migration://offline?data=...` QR codes.
  * Ability to export tokens back into Google Authenticator QR format for users needing reverse compatibility.
* **Encrypted Vault Export**:
  * Complete export to a single `.warifu` encrypted file (Argon2id + XChaCha20-Poly1305).
* **Interoperability Importers**:
  * Aegis Encrypted JSON.
  * 2FAS Backup JSON.
  * Bitwarden Authenticator JSON / CSV.
  * FreeOTP / FreeOTP+ JSON.

#### 2.5 Synchronization & Backup Options
* **Air-Gapped Local Backups**:
  * Automatic scheduled local backups saved to external storage via Android SAF or iOS Documents.
* **Self-Hosted Cloud Sync**:
  * Zero-knowledge sync directly to user-controlled **Nextcloud** or **WebDAV** instances.
* **CRDT Multi-Device State Engine**:
  * State-based Last-Write-Wins Element-Set handles token additions, renames, and deletions seamlessly across multiple devices without data loss.

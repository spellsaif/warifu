# Product Requirements Document (PRD)
## Project: AegisCore / OpenAuthenticator (Next-Generation Open Source 2FA)

---

### 1. Executive Summary & Vision
**Product Name (Working Title):** `OpenAuthenticator`  
**Mission:** Provide an open-source, mathematically verifiable, zero-knowledge, and offline-first multi-factor authenticator for Android and iOS that matches the instantaneous UX of Google Authenticator while eliminating its critical privacy and security shortcomings.

#### Core Value Propositions:
1. **Zero-Knowledge Data Sovereignty:** Cryptographic keys and OTP secrets never leave device-controlled hardware enclaves in plaintext. Cloud sync, if enabled, is completely end-to-end encrypted with zero server visibility into account metadata or secrets.
2. **Deterministic Memory Safety:** Powered by a shared, audited Rust core that guarantees deterministic secret zeroization and memory-locking (`mlock`), preventing cold-boot and memory-dump extraction attacks.
3. **True Hardware-Bound Biometrics:** Biometric authentication is not a superficial UI boolean check; it physically gates the cryptographic unwrapping of the vault master key inside the device's hardware security module (Android StrongBox / Apple Secure Enclave).
4. **Instantaneous Native Performance:** Sub-150ms cold-start time, sub-1ms code generation, zero stutter, and instant offline camera-based QR scanning without third-party proprietary dependencies (e.g., Google Play Services).
5. **Universal Portability:** Seamless import from and export to existing standards, including Google Authenticator migration payloads, Aegis encrypted vaults, Bitwarden, and 2FAS.

---

### 2. User Personas & Target Audience

| Persona | Description | Primary Needs |
| :--- | :--- | :--- |
| **P1: Everyday Privacy Seeker** | Transitioning away from Google/Microsoft Authenticator due to privacy or vendor lock-in concerns. | Zero learning curve, fast launch, quick search, reliable automatic backup, simple migration import. |
| **P2: Security & DevOps Engineer** | Manages hundreds of enterprise accounts, cloud consoles, and infrastructure tokens. | Support for non-standard parameters (SHA-256/512, 8-digit, non-30s intervals), Steam Guard, hardware tokens (YubiKey), strict screen/clipboard isolation. |
| **P3: Air-Gapped / Sovereign User** | Operates on de-Googled ROMs (GrapheneOS, CalyxOS) or offline devices. | 100% offline functionality, zero network permissions required for core operations, self-hosted WebDAV/Nextcloud backup support. |

---

### 3. Key Performance Indicators (KPIs) & Empirical Targets

To prevent subjective assessments, all milestones must satisfy measurable, empirical thresholds:

| Metric | Target Threshold | Validation Method |
| :--- | :--- | :--- |
| **Cold Start Latency** | $< 150\,\text{ms}$ to interactive token list | Trace profiling on Snapdragon 8 Gen 2 / Apple A16 |
| **TOTP Evaluation Latency** | $< 0.5\,\text{ms}$ per 100 tokens | Rust microbenchmark (`criterion`) |
| **Argon2id Vault Unlock** | $200\,\text{ms} - 350\,\text{ms}$ | Measured on mobile ARM64 ($m=64\,\text{MB}, t=3, p=4$) |
| **Memory Retention** | $0$ plaintext secret bytes surviving post-drop | Core dump analysis & memory scan via Valgrind/GDB |
| **RFC Compliance** | $100\%$ pass rate on RFC 4226 & RFC 6238 vectors | Automated unit test suite with Wycheproof vectors |
| **Network Leakage** | $0$ outbound packets in offline mode | Wireshark / eBPF packet capture audit |
| **App Bundle Size** | $< 18\,\text{MB}$ download size | Universal APK / IPA size profiling |

---

### 4. Scope & Feature Boundaries

#### 4.1 In-Scope (Phase 1 / MVP)
* RFC 6238 TOTP (SHA-1, SHA-256, SHA-512; 6, 7, 8 digits; 15s to 120s steps).
* RFC 4226 HOTP (Counter-based code generation).
* Steam Guard authentication (custom 5-character base32 alphanumeric format).
* Hardware-bound biometric unlock via Android StrongBox/TEE and iOS Secure Enclave.
* Strict memory zeroization (`Zeroize` + `mlock`).
* Offline QR code scanning via native camera pipelines (CameraX + native ZXing-cpp / AVFoundation).
* Bi-directional migration with Google Authenticator (`otpauth-migration://offline?data=...`).
* Encrypted local vault backup/export (`.enc` format using Argon2id + XChaCha20-Poly1305).
* OS security hardening (`FLAG_SECURE`, clipboard auto-purge, iOS privacy snapshot masking).
* Category, tag, and search filtering with concealed codes (tap to reveal).

#### 4.2 In-Scope (Phase 2 / Advanced)
* Multi-device E2EE synchronization via CRDTs (LWW-Element-Set) using self-hosted WebDAV or blind zero-knowledge relay.
* YubiKey OATH-TOTP hardware token integration over NFC and USB-C.
* Companion watch app (Wear OS and watchOS).

#### 4.3 Explicitly Out-of-Scope
* Storing or handling master passwords on remote servers.
* Proprietary, non-exportable vault formats.
* In-app telemetry, ad tracking, third-party analytics (e.g., Firebase, Sentry).
* SMS-based 2FA aggregation (insecure and out of architectural domain).

---

### 5. Detailed User Journeys

#### 5.1 Onboarding & Google Authenticator Migration
1. User installs application. Zero onboarding prompts for cloud registration or email signup.
2. Initial prompt: "Protect Vault with Biometrics or Master Password".
3. User selects "Import from Google Authenticator".
4. User displays Google Authenticator's "Transfer accounts" QR code on an old device or computer.
5. In-app scanner decodes protobuf stream; all accounts, secrets, and algorithms are parsed into the local encrypted vault within $<100\,\text{ms}$.
6. Confirmation screen verifies account count and prompts for immediate encrypted local backup.

#### 5.2 Daily Authentication & Copy
1. User opens app -> Hardware prompts for Biometric authentication.
2. Sensor validates biometric -> Secure Enclave / StrongBox unwraps vault key -> Vault decrypted in RAM.
3. User sees tokens (masked with `••••••` if concealment enabled).
4. User taps token row:
   * Code is copied to clipboard.
   * Android 13+ marks payload as `EXTRA_IS_SENSITIVE` (suppressing system clipboard preview).
   * Background OS timer triggers clipboard sanitization after 30 seconds.
   * If token is within 5 seconds of expiration, UI displays next code preview indicator.

#### 5.3 Offline Encrypted Backup
1. User navigates to Settings -> Backup & Export.
2. User provides a custom backup passphrase.
3. App derives encryption key via Argon2id ($m=64\,\text{MB}, t=4, p=4$).
4. Serializes vault to Protocol Buffer, encrypts with XChaCha20-Poly1305, and saves via Android SAF (Storage Access Framework) or iOS Share Sheet.

---

### 6. Compliance, Governance & Audit Readiness
* **RFC Standards Compliance:** Strict validation against RFC 4226, RFC 6238, RFC 4648, and RFC 9106.
* **OWASP MASVS (Mobile Application Security Verification Standard):** Must meet MASVS-L2 (Defense-in-Depth) and MASVS-CRYPTO requirements.
* **Third-Party Security Audit Target:** Codebase structured to undergo formal third-party audit (e.g., Cure53, NCC Group) with documented cryptographic threat models and reproducible builds.

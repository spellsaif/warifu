import Foundation
import LocalAuthentication
import SwiftUI

@Observable
final class AuthViewModel {

    var isUnlocked: Boolean = false
    var tokens: [TokenView] = []
    var filteredTokens: [TokenView] = []
    var searchQuery: String = "" {
        didSet {
            filterTokens()
        }
    }
    var areCodesConcealed: Bool = true
    var statusToast: String? = nil

    private var session: VaultSession?
    private var tickerTimer: Timer?
    private let enclaveManager = SecureEnclaveManager()

    private var vaultURL: URL {
        FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("vault.bin")
    }

    private var wrappedKeyURL: URL {
        FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("wrapped_kek.bin")
    }

    var hasExistingVault: Bool {
        FileManager.default.fileExists(atPath: vaultURL.path) &&
        FileManager.default.fileExists(atPath: wrappedKeyURL.path)
    }

    // MARK: - Vault Lifecycle & Biometric Unlock

    func unlockWithBiometrics() async {
        guard hasExistingVault else {
            statusToast = "No vault found on device."
            return
        }

        do {
            let wrappedKeyData = try Data(contentsOf: wrappedKeyURL)
            let context = LAContext()

            // 1. Hardware unwrapping inside Apple Secure Enclave
            let unwrappedKey = try await enclaveManager.unwrapVaultKey(
                wrappedKey: wrappedKeyData,
                context: context
            )

            // 2. Open vault session in Rust core
            let keyBytes = [UInt8](unwrappedKey)
            let openedSession = try VaultSession.open(
                vaultPath: vaultURL.path,
                vaultKeyBytes: keyBytes
            )

            await MainActor.run {
                self.session = openedSession
                self.isUnlocked = true
                self.startTicker()
                self.refreshTokens()
            }
        } catch {
            await MainActor.run {
                self.statusToast = "Authentication failed: \(error.localizedDescription)"
            }
        }
    }

    func createNewVault() async {
        do {
            // 1. Generate fresh 256-bit Vault Key in Rust
            let rawVaultKey = try ffiGenerateVaultKey()
            let rawData = Data(rawVaultKey)

            // 2. Wrap Vault Key using Apple Secure Enclave
            let wrappedKeyData = try enclaveManager.wrapVaultKey(vaultKey: rawData)
            try wrappedKeyData.write(to: wrappedKeyURL, options: .atomic)

            // 3. Create fresh vault file
            let newSession = try VaultSession.createNew(
                vaultPath: vaultURL.path,
                vaultKeyBytes: rawVaultKey
            )

            await MainActor.run {
                self.session = newSession
                self.isUnlocked = true
                self.startTicker()
                self.refreshTokens()
                self.statusToast = "Vault initialized and secured with Secure Enclave."
            }
        } catch {
            await MainActor.run {
                self.statusToast = "Vault creation failed: \(error.localizedDescription)"
            }
        }
    }

    func lockVault() {
        tickerTimer?.invalidate()
        tickerTimer = nil
        session?.lock()
        session = nil
        isUnlocked = false
        tokens = []
        filteredTokens = []
    }

    // MARK: - Token Management

    private func startTicker() {
        tickerTimer?.invalidate()
        tickerTimer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] _ in
            self?.refreshTokens()
        }
    }

    func refreshTokens() {
        guard let currentSession = session else { return }
        let now = UInt64(Date().timeIntervalSince1970)

        do {
            let list = try currentSession.listTokens(currentUnixTime: now)
            DispatchQueue.main.async {
                self.tokens = list
                self.filterTokens()
            }
        } catch {
            // Session may be locked
        }
    }

    func addToken(from uri: String) -> Bool {
        guard let currentSession = session else { return false }
        do {
            _ = try currentSession.addTokenFromUri(uri: uri)
            refreshTokens()
            statusToast = "Account added successfully."
            return true
        } catch {
            statusToast = "Invalid QR code: \(error.localizedDescription)"
            return false
        }
    }

    func importGoogleMigration(uri: String) -> Int {
        guard let currentSession = session else { return 0 }
        do {
            let count = try currentSession.importGoogleMigration(migrationUri: uri)
            refreshTokens()
            statusToast = "Imported \(count) accounts from Google."
            return Int(count)
        } catch {
            statusToast = "Google import failed: \(error.localizedDescription)"
            return 0
        }
    }

    func exportGoogleMigration() -> String? {
        guard let currentSession = session else { return nil }
        do {
            return try currentSession.exportGoogleMigration()
        } catch {
            statusToast = "Export failed: \(error.localizedDescription)"
            return nil
        }
    }

    func deleteToken(id: String) {
        guard let currentSession = session else { return }
        do {
            try currentSession.removeToken(id: id)
            refreshTokens()
        } catch {
            statusToast = "Delete failed: \(error.localizedDescription)"
        }
    }

    func copyToken(_ code: String) {
        SecurityHardening.copyTokenToClipboard(code)
        statusToast = "Code copied (clears in 30s)."
    }

    private func filterTokens() {
        if searchQuery.trimmingCharacters(in: .whitespaces).isEmpty {
            filteredTokens = tokens
            return
        }
        let q = searchQuery.lowercased()
        filteredTokens = tokens.filter {
            $0.issuer.lowercased().contains(q) ||
            $0.accountName.lowercased().contains(q) ||
            $0.tags.contains(where: { $0.lowercased().contains(q) })
        }
    }
}

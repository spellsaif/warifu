import SwiftUI
import UIKit

/// iOS operating system security hardening utilities.
enum SecurityHardening {

    /// Copies an OTP code securely to the iOS system clipboard:
    /// 1. `localOnly: true`: Disables Apple Universal Clipboard to prevent leaking tokens over Bluetooth/iCloud to nearby devices.
    /// 2. `expirationDate`: Configures the iOS pasteboard to automatically purge the token after 30 seconds.
    static func copyTokenToClipboard(_ token: String) {
        let pasteboard = UIPasteboard.general
        let pasteboardItem: [String: Any] = [
            "public.utf8-plain-text": token
        ]

        let options: [UIPasteboard.OptionsKey: Any] = [
            .expirationDate: Date().addingTimeInterval(30),
            .localOnly: true
        ]

        pasteboard.setItems([pasteboardItem], options: options)
    }
}

/// SwiftUI ViewModifier that overlays a blur shield whenever the app is backgrounded or in the App Switcher.
struct PrivacyShieldModifier: ViewModifier {
    @Environment(\.scenePhase) private var scenePhase

    func body(content: Content) -> some View {
        content
            .overlay {
                if scenePhase != .active {
                    ZStack {
                        Rectangle()
                            .fill(.ultraThinMaterial)
                            .ignoresSafeArea()

                        VStack(spacing: 12) {
                            Image(systemName: "lock.shield.fill")
                                .font(.system(size: 48))
                                .foregroundStyle(.secondary)

                            Text("Warifu")
                                .font(.headline)
                                .foregroundStyle(.secondary)
                        }
                    }
                    .transition(.opacity)
                }
            }
    }
}

extension View {
    /// Applies an automatic privacy shield to mask UI contents in the iOS App Switcher snapshot.
    func privacyShield() -> some View {
        modifier(PrivacyShieldModifier())
    }
}

import SwiftUI

@main
struct WarifuApp: App {
    @State private var viewModel = AuthViewModel()
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            Group {
                if viewModel.isUnlocked {
                    TokenListView(viewModel: viewModel)
                } else {
                    LockView(
                        hasExistingVault: viewModel.hasExistingVault,
                        onUnlockClicked: {
                            Task {
                                await viewModel.unlockWithBiometrics()
                            }
                        },
                        onCreateVaultClicked: {
                            Task {
                                await viewModel.createNewVault()
                            }
                        }
                    )
                }
            }
            .privacyShield()
            .onChange(of: scenePhase) { oldPhase, newPhase in
                if newPhase == .background {
                    // Lock and purge RAM when app leaves foreground
                    viewModel.lockVault()
                }
            }
        }
    }
}

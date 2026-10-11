import SwiftUI

struct LockView: View {
    let hasExistingVault: Bool
    let onUnlockClicked: () -> Void
    let onCreateVaultClicked: () -> Void

    var body: some View {
        ZStack {
            Color(UIColor.systemBackground)
                .ignoresSafeArea()

            VStack(spacing: 24) {
                Spacer()

                Image(systemName: "lock.shield.fill")
                    .font(.system(size: 80))
                    .foregroundStyle(.tint)

                VStack(spacing: 8) {
                    Text("OpenAuthenticator")
                        .font(.title)
                        .fontWeight(.bold)

                    Text("Hardware-Secured Multi-Factor Authentication")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }

                Spacer()

                VStack(spacing: 16) {
                    if hasExistingVault {
                        Button(action: onUnlockClicked) {
                            HStack(spacing: 10) {
                                Image(systemName: "faceid")
                                    .font(.title3)
                                Text("Unlock with Face ID")
                                    .fontWeight(.semibold)
                            }
                            .frame(maxWidth: .infinity)
                            .frame(height: 54)
                            .background(Color.openAuthBlue)
                            .foregroundStyle(.white)
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                        }
                    } else {
                        Button(action: onCreateVaultClicked) {
                            Text("Create Secure Vault")
                                .fontWeight(.semibold)
                                .frame(maxWidth: .infinity)
                                .frame(height: 54)
                                .background(Color.openAuthBlue)
                                .foregroundStyle(.white)
                                .clipShape(RoundedRectangle(cornerRadius: 14))
                        }
                    }
                }
                .padding(.horizontal, 24)
                .padding(.bottom, 32)
            }
        }
    }
}

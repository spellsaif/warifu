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

                WarifuLogoView(size: 96)

                VStack(spacing: 6) {
                    Text("Warifu")
                        .font(.system(size: 32, weight: .bold))

                    Text("割符")
                        .font(.system(size: 18, weight: .medium))
                        .foregroundStyle(.tint.opacity(0.85))

                    Text("Hardware-Secured Multi-Factor Authentication")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .padding(.top, 4)
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
                            .background(Color.warifuBlue)
                            .foregroundStyle(.white)
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                        }
                    } else {
                        Button(action: onCreateVaultClicked) {
                            Text("Create Secure Vault")
                                .fontWeight(.semibold)
                                .frame(maxWidth: .infinity)
                                .frame(height: 54)
                                .background(Color.warifuBlue)
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

import SwiftUI

struct TokenListView: View {
    @Bindable var viewModel: AuthViewModel
    @State private var isShowingScanner = false

    var body: some View {
        NavigationStack {
            Group {
                if viewModel.filteredTokens.isEmpty {
                    VStack(spacing: 16) {
                        Image(systemName: "shield.slash")
                            .font(.system(size: 64))
                            .foregroundStyle(.secondary.opacity(0.5))

                        Text(viewModel.searchQuery.isEmpty ? "No 2FA Accounts Yet" : "No Matching Accounts")
                            .font(.headline)
                            .foregroundStyle(.secondary)

                        Text("Tap the scan button to add your first account.")
                            .font(.subheadline)
                            .foregroundStyle(.tertiary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 32)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    ScrollView {
                        LazyVStack(spacing: 12) {
                            ForEach(viewModel.filteredTokens, id: \.id) { token in
                                TokenCardView(
                                    token: token,
                                    isConcealed: viewModel.areCodesConcealed,
                                    onToggleConceal: {
                                        viewModel.areCodesConcealed.toggle()
                                    },
                                    onCopyCode: { code in
                                        viewModel.copyToken(code)
                                    }
                                )
                                .contextMenu {
                                    Button(role: .destructive) {
                                        viewModel.deleteToken(id: token.id)
                                    } label: {
                                        Label("Delete Account", systemImage: "trash")
                                    }
                                }
                            }
                        }
                        .padding(16)
                    }
                }
            }
            .navigationTitle("Warifu")
            .searchable(text: $viewModel.searchQuery, prompt: "Search accounts or tags…")
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button {
                        viewModel.areCodesConcealed.toggle()
                    } label: {
                        Image(systemName: viewModel.areCodesConcealed ? "eye" : "eye.slash")
                    }
                }

                ToolbarItem(placement: .topBarTrailing) {
                    HStack(spacing: 16) {
                        Button {
                            isShowingScanner = true
                        } label: {
                            Image(systemName: "qrcode.viewfinder")
                                .font(.system(size: 18, weight: .bold))
                        }

                        Menu {
                            Button {
                                isShowingScanner = true
                            } label: {
                                Label("Import from Google", systemImage: "arrow.down.doc")
                            }

                            Divider()

                            Button(role: .destructive) {
                                viewModel.lockVault()
                            } label: {
                                Label("Lock Vault", systemImage: "lock")
                            }
                        } label: {
                            Image(systemName: "ellipsis.circle")
                        }
                    }
                }
            }
            .sheet(isPresented: $isShowingScanner) {
                QrScannerView(
                    onQrCodeScanned: { scannedUri in
                        isShowingScanner = false
                        if scannedUri.hasPrefix("otpauth-migration://") {
                            _ = viewModel.importGoogleMigration(uri: scannedUri)
                        } else {
                            _ = viewModel.addToken(from: scannedUri)
                        }
                    },
                    onDismiss: {
                        isShowingScanner = false
                    }
                )
            }
            .overlay(alignment: .bottom) {
                if let message = viewModel.statusToast {
                    Text(message)
                        .font(.subheadline)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                        .background(.ultraThinMaterial)
                        .clipShape(Capsule())
                        .shadow(radius: 4)
                        .padding(.bottom, 24)
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                        .task {
                            try? await Task.sleep(nanoseconds: 2_500_000_000)
                            withAnimation {
                                viewModel.statusToast = nil
                            }
                        }
                }
            }
        }
    }
}

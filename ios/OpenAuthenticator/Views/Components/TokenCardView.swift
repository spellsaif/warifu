import SwiftUI

struct TokenCardView: View {
    let token: TokenView
    let isConcealed: Bool
    let onToggleConceal: () -> Void
    let onCopyCode: (String) -> Void

    private var formattedCode: String {
        let raw = token.currentCode
        if raw.count == 6 {
            let start = raw.prefix(3)
            let end = raw.suffix(3)
            return "\(start) \(end)"
        } else if raw.count == 8 {
            let start = raw.prefix(4)
            let end = raw.suffix(4)
            return "\(start) \(end)"
        }
        return raw
    }

    private var formattedNextCode: String {
        guard let next = token.nextCode else { return "" }
        if next.count == 6 {
            return "\(next.prefix(3)) \(next.suffix(3))"
        }
        return next
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Header: Issuer, Account Name & Pinned icon
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(token.issuer.isEmpty ? "Authentication Code" : token.issuer)
                        .font(.headline)
                        .fontWeight(.bold)
                        .foregroundStyle(.primary)

                    Text(token.accountName)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }

                Spacer()

                if token.isPinned {
                    Image(systemName: "pin.fill")
                        .font(.caption)
                        .foregroundStyle(.tint)
                }
            }

            // Body: Code & Timer
            HStack(alignment: .center) {
                Text(isConcealed ? "••••••" : formattedCode)
                    .font(.system(size: 28, weight: .heavy, design: .monospaced))
                    .foregroundStyle(.tint)
                    .contentTransition(.numericText())

                Spacer()

                HStack(spacing: 12) {
                    Button(action: onToggleConceal) {
                        Image(systemName: isConcealed ? "eye" : "eye.slash")
                            .font(.system(size: 16))
                            .foregroundStyle(.secondary)
                    }
                    .buttonStyle(.plain)

                    CountdownRing(
                        remainingSeconds: token.remainingSeconds,
                        period: token.period
                    )
                }
            }

            // Next-Code Preview Banner (Grace Period Warning)
            if token.isNearExpiry, let _ = token.nextCode {
                HStack {
                    Text("Next: \(formattedNextCode)")
                        .font(.system(size: 12, weight: .semibold, design: .monospaced))
                        .foregroundStyle(.tint)

                    Spacer()

                    Text("Expires soon")
                        .font(.caption2)
                        .foregroundStyle(.red)
                }
                .padding(.horizontal, 10)
                .padding(.vertical, 6)
                .background(Color.openAuthBlue.opacity(0.08))
                .clipShape(RoundedRectangle(cornerRadius: 6))
                .transition(.opacity)
            }
        }
        .padding(16)
        .background(Color.cardBackground)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .contentShape(Rectangle())
        .onTapGesture {
            onCopyCode(token.currentCode)
            UIImpactFeedbackGenerator(style: .medium).impactOccurred()
        }
    }
}

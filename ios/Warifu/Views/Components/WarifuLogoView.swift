import SwiftUI

/// Minimalist Vector Logo for Warifu (割符).
/// Represents the two interlocking halves of the ancient authentication tally.
struct WarifuLogoView: View {
    var size: CGFloat = 96

    var body: some View {
        GeometryReader { geometry in
            let w = geometry.size.width
            let h = geometry.size.height

            // Scale factor relative to 512x512 base canvas
            let scaleX = w / 512.0
            let scaleY = h / 512.0

            ZStack {
                // Left Tally Half (Hardware Key)
                Path { path in
                    path.move(to: CGPoint(x: 238 * scaleX, y: 72 * scaleY))
                    path.addCurve(
                        to: CGPoint(x: 84 * scaleX, y: 246 * scaleY),
                        control1: CGPoint(x: 140 * scaleX, y: 72 * scaleY),
                        control2: CGPoint(x: 84 * scaleX, y: 126 * scaleY)
                    )
                    path.addCurve(
                        to: CGPoint(x: 238 * scaleX, y: 440 * scaleY),
                        control1: CGPoint(x: 84 * scaleX, y: 344 * scaleY),
                        control2: CGPoint(x: 160 * scaleX, y: 412 * scaleY)
                    )
                    path.addLine(to: CGPoint(x: 238 * scaleX, y: 316 * scaleY))
                    path.addCurve(
                        to: CGPoint(x: 196 * scaleX, y: 274 * scaleY),
                        control1: CGPoint(x: 214 * scaleX, y: 316 * scaleY),
                        control2: CGPoint(x: 196 * scaleX, y: 298 * scaleY)
                    )
                    path.addCurve(
                        to: CGPoint(x: 238 * scaleX, y: 232 * scaleY),
                        control1: CGPoint(x: 196 * scaleX, y: 250 * scaleY),
                        control2: CGPoint(x: 214 * scaleX, y: 232 * scaleY)
                    )
                    path.closeSubpath()
                }
                .fill(
                    LinearGradient(
                        colors: [Color(hex: 0x3B82F6), Color(hex: 0x1D4ED8)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )

                // Right Tally Half (Verifier Token)
                Path { path in
                    path.move(to: CGPoint(x: 274 * scaleX, y: 72 * scaleY))
                    path.addCurve(
                        to: CGPoint(x: 428 * scaleX, y: 246 * scaleY),
                        control1: CGPoint(x: 372 * scaleX, y: 72 * scaleY),
                        control2: CGPoint(x: 428 * scaleX, y: 126 * scaleY)
                    )
                    path.addCurve(
                        to: CGPoint(x: 274 * scaleX, y: 440 * scaleY),
                        control1: CGPoint(x: 428 * scaleX, y: 344 * scaleY),
                        control2: CGPoint(x: 352 * scaleX, y: 412 * scaleY)
                    )
                    path.addLine(to: CGPoint(x: 274 * scaleX, y: 292 * scaleY))
                    path.addCurve(
                        to: CGPoint(x: 316 * scaleX, y: 250 * scaleY),
                        control1: CGPoint(x: 298 * scaleX, y: 292 * scaleY),
                        control2: CGPoint(x: 316 * scaleX, y: 274 * scaleY)
                    )
                    path.addCurve(
                        to: CGPoint(x: 274 * scaleX, y: 208 * scaleY),
                        control1: CGPoint(x: 316 * scaleX, y: 226 * scaleY),
                        control2: CGPoint(x: 298 * scaleX, y: 208 * scaleY)
                    )
                    path.closeSubpath()
                }
                .fill(
                    LinearGradient(
                        colors: [Color(hex: 0x06B6D4), Color(hex: 0x0284C7)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )

                // Central Spark / Core Proof
                Circle()
                    .fill(Color.white.opacity(0.95))
                    .frame(width: 24 * scaleX, height: 24 * scaleY)
                    .position(x: 256 * scaleX, y: 262 * scaleY)
            }
        }
        .frame(width: size, height: size)
    }
}

private extension Color {
    init(hex: UInt32) {
        let r = Double((hex >> 16) & 0xFF) / 255.0
        let g = Double((hex >> 8) & 0xFF) / 255.0
        let b = Double(hex & 0xFF) / 255.0
        self.init(red: r, green: g, blue: b)
    }
}

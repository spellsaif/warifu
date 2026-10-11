import SwiftUI

struct CountdownRing: View {
    let remainingSeconds: UInt32
    let period: UInt32

    var size: CGFloat = 40
    var strokeWidth: CGFloat = 3.5

    private var progress: Double {
        let total = period == 0 ? 30.0 : Double(period)
        return min(max(Double(remainingSeconds) / total, 0.0), 1.0)
    }

    private var ringColor: Color {
        if remainingSeconds <= 5 {
            return .timerRed
        } else if remainingSeconds <= 10 {
            return .timerAmber
        } else {
            return .timerGreen
        }
    }

    var body: some View {
        ZStack {
            // Track background
            Circle()
                .stroke(ringColor.opacity(0.15), lineWidth: strokeWidth)

            // Animated progress arc
            Circle()
                .trim(from: 0.0, to: progress)
                .stroke(ringColor, style: StrokeStyle(lineWidth: strokeWidth, lineCap: .round))
                .rotationEffect(.degrees(-90))
                .animation(.easeInOut(duration: 0.3), value: progress)

            // Remaining seconds digit
            Text("\(remainingSeconds)")
                .font(.system(size: 12, weight: .bold, design: .rounded))
                .foregroundStyle(ringColor)
        }
        .frame(width: size, height: size)
    }
}

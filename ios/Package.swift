// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "Warifu",
    platforms: [
        .iOS(.v17)
    ],
    products: [
        .library(
            name: "Warifu",
            targets: ["Warifu"]
        )
    ],
    targets: [
        .target(
            name: "Warifu",
            path: "Warifu",
            exclude: [
                "App/Info.plist"
            ],
            cSettings: [
                .headerSearchPath("Core")
            ]
        )
    ]
)

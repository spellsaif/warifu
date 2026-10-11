// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "OpenAuthenticator",
    platforms: [
        .iOS(.v17)
    ],
    products: [
        .library(
            name: "OpenAuthenticator",
            targets: ["OpenAuthenticator"]
        )
    ],
    targets: [
        .target(
            name: "OpenAuthenticator",
            path: "OpenAuthenticator",
            exclude: [
                "App/Info.plist"
            ],
            cSettings: [
                .headerSearchPath("Core")
            ]
        )
    ]
)

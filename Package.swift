// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapacitorAutoPay",
    platforms: [.iOS(.v14)],
    products: [
        .library(
            name: "CapacitorAutoPay",
            targets: ["AutoPayPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", branch: "main")
    ],
    targets: [
        .target(
            name: "AutoPayPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/AutoPayPlugin"),
        .testTarget(
            name: "AutoPayPluginTests",
            dependencies: ["AutoPayPlugin"],
            path: "ios/Tests/AutoPayPluginTests")
    ]
)
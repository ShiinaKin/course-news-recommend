// swift-tools-version:6.0
import PackageDescription

let package = Package(
    name: "macocr",
    platforms: [
        .macOS(.v13)
    ],
    products: [
        .executable(name: "macocr", targets: ["macocr"]),
    ],
    targets: [
        .executableTarget(
            name: "macocr",
            linkerSettings: [
                .linkedFramework("Vision"),
                .linkedFramework("CoreImage"),
                .linkedFramework("ImageIO")
            ]
        ),
    ]
)

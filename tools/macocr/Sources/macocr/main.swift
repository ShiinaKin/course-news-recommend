import Foundation
import Vision
import ImageIO
import CoreGraphics

struct Options {
    let imagePath: String
    let languages: [String]
    let fast: Bool
}

enum CLIError: Error {
    case missingImage
    case cannotLoadImage
}

func parseArguments() throws -> Options {
    var args = CommandLine.arguments.dropFirst()
    guard let imagePath = args.first else {
        throw CLIError.missingImage
    }
    args = args.dropFirst()
    var languages: [String] = ["zh-Hans", "en"]
    var fast = false
    while let flag = args.first {
        args = args.dropFirst()
        switch flag {
        case "--langs":
            if let value = args.first {
                languages = value.split(separator: ",").map { String($0) }
                args = args.dropFirst()
            }
        case "--fast":
            fast = true
        case "--accurate":
            fast = false
        default:
            break
        }
    }
    return Options(imagePath: imagePath, languages: languages, fast: fast)
}

func loadCGImage(path: String) throws -> CGImage {
    let url = URL(fileURLWithPath: path)
    guard let source = CGImageSourceCreateWithURL(url as CFURL, nil),
          let image = CGImageSourceCreateImageAtIndex(source, 0, nil) else {
        throw CLIError.cannotLoadImage
    }
    return image
}

func recognizeText(options: Options) throws -> [String: Any] {
    let cgImage = try loadCGImage(path: options.imagePath)
    let request = VNRecognizeTextRequest()
    let normalizedLanguages = options.languages.map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
    request.recognitionLanguages = normalizedLanguages
    let forceAccurate = normalizedLanguages.contains { language in
        let lower = language.lowercased()
        return lower.hasPrefix("zh") || lower.hasPrefix("ja") || lower.hasPrefix("ko")
    }
    let useFast = options.fast && !forceAccurate
    if options.fast && !useFast {
        fputs("macocr: --fast 模式不支持当前语言，自动切换到精确识别。\n", stderr)
    }
    request.recognitionLevel = useFast ? .fast : .accurate
    request.usesCPUOnly = false
    request.usesLanguageCorrection = true
    let handler = VNImageRequestHandler(cgImage: cgImage, options: [:])
    try handler.perform([request])

    var lines: [[String: Any]] = []
    if let observations = request.results {
        for observation in observations {
            let candidate = observation.topCandidates(1).first
            var boundingBox: [CGFloat] = []
            let rect = observation.boundingBox
            boundingBox.append(rect.origin.x)
            boundingBox.append(rect.origin.y)
            boundingBox.append(rect.size.width)
            boundingBox.append(rect.size.height)
            lines.append([
                "text": candidate?.string ?? "",
                "confidence": observation.confidence,
                "boundingBox": boundingBox
            ])
        }
    }
    return [
        "lines": lines,
        "width": cgImage.width,
        "height": cgImage.height
    ]
}

func outputJSON(_ payload: [String: Any]) throws {
    let data = try JSONSerialization.data(withJSONObject: payload, options: [.prettyPrinted])
    if let string = String(data: data, encoding: .utf8) {
        print(string)
    }
}

do {
    let options = try parseArguments()
    let result = try recognizeText(options: options)
    try outputJSON(result)
} catch CLIError.missingImage {
    fputs("Usage: macocr <image_path> [--langs zh-Hans,en] [--fast]\n", stderr)
    exit(1)
} catch {
    fputs("Error: \(error.localizedDescription)\n", stderr)
    exit(2)
}

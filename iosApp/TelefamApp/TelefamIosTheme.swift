import SwiftUI

enum TelefamThemeMode: String, CaseIterable, Identifiable {
    case system
    case light
    case dark

    var id: String { rawValue }

    var label: String {
        switch self {
        case .system: return "System"
        case .light: return "Light"
        case .dark: return "Dark"
        }
    }

    var next: TelefamThemeMode {
        switch self {
        case .system: return .light
        case .light: return .dark
        case .dark: return .system
        }
    }

    var preferredColorScheme: ColorScheme? {
        switch self {
        case .system: return nil
        case .light: return .light
        case .dark: return .dark
        }
    }
}

enum TelefamPalette {
    static let crimson = Color(red: 200.0 / 255.0, green: 10.0 / 255.0, blue: 15.0 / 255.0)
    static let crimsonBright = Color(red: 225.0 / 255.0, green: 19.0 / 255.0, blue: 25.0 / 255.0)
    static let crimsonDarkMode = Color(red: 224.0 / 255.0, green: 36.0 / 255.0, blue: 42.0 / 255.0)
    static let blushLight = Color(red: 252.0 / 255.0, green: 236.0 / 255.0, blue: 236.0 / 255.0)
    static let blushDark = Color(red: 43.0 / 255.0, green: 21.0 / 255.0, blue: 23.0 / 255.0)

    static let darkBackground = Color(red: 14.0 / 255.0, green: 16.0 / 255.0, blue: 20.0 / 255.0)
    static let darkSurface = Color(red: 23.0 / 255.0, green: 26.0 / 255.0, blue: 33.0 / 255.0)
    static let darkSearchField = Color(red: 17.0 / 255.0, green: 24.0 / 255.0, blue: 36.0 / 255.0)
    static let darkSurfaceVariant = Color(red: 32.0 / 255.0, green: 36.0 / 255.0, blue: 46.0 / 255.0)
    static let darkBorder = Color(red: 45.0 / 255.0, green: 50.0 / 255.0, blue: 63.0 / 255.0)
    static let darkTextPrimary = Color(red: 243.0 / 255.0, green: 244.0 / 255.0, blue: 246.0 / 255.0)
    static let darkTextMuted = Color(red: 156.0 / 255.0, green: 163.0 / 255.0, blue: 175.0 / 255.0)

    static let lightBackground = Color.white
    static let lightSurfaceVariant = Color(red: 249.0 / 255.0, green: 250.0 / 255.0, blue: 251.0 / 255.0)
    static let lightBorder = Color(red: 226.0 / 255.0, green: 228.0 / 255.0, blue: 233.0 / 255.0)
    static let lightTextPrimary = Color(red: 26.0 / 255.0, green: 28.0 / 255.0, blue: 30.0 / 255.0)
    static let lightTextMuted = Color(red: 107.0 / 255.0, green: 114.0 / 255.0, blue: 128.0 / 255.0)

    static func background(_ scheme: ColorScheme) -> Color {
        scheme == .dark ? darkBackground : lightBackground
    }

    static func surface(_ scheme: ColorScheme) -> Color {
        scheme == .dark ? darkSurface : .white
    }

    static func searchFieldBackground(_ scheme: ColorScheme) -> Color {
        scheme == .dark ? darkSearchField : .white
    }

    static func surfaceVariant(_ scheme: ColorScheme) -> Color {
        scheme == .dark ? darkSurfaceVariant : lightSurfaceVariant
    }

    static func border(_ scheme: ColorScheme) -> Color {
        scheme == .dark ? darkBorder : lightBorder
    }

    static func textPrimary(_ scheme: ColorScheme) -> Color {
        scheme == .dark ? darkTextPrimary : lightTextPrimary
    }

    static func textMuted(_ scheme: ColorScheme) -> Color {
        scheme == .dark ? darkTextMuted : lightTextMuted
    }

    static func blush(_ scheme: ColorScheme) -> Color {
        scheme == .dark ? blushDark : blushLight
    }
}

extension Color {
    init(hex: UInt, opacity: Double = 1.0) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255.0,
            green: Double((hex >> 8) & 0xFF) / 255.0,
            blue: Double(hex & 0xFF) / 255.0,
            opacity: opacity
        )
    }
}

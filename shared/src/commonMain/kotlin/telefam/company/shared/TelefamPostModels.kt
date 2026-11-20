package telefam.company.shared

enum class SharedPostMode(val apiValue: String, val label: String) {
  TEXT("text", "Text"),
  PHOTOS("photos", "Photos"),
  VIDEO("video", "Video"),
}

enum class SharedPostThemeKind {
  GRADIENT,
  IMAGE_SCENE,
}

data class SharedPostTheme(
  val id: String,
  val label: String,
  val kind: SharedPostThemeKind,
  val startColorArgb: Long,
  val endColorArgb: Long,
  val accentColorArgb: Long,
  val isLightInk: Boolean,
)

enum class SharedPostFont(val id: String, val label: String) {
  CLASSIC("classic", "Classic"),
  SERIF("serif", "Serif"),
  HAND("hand", "Hand"),
  TYPEWRITER("typewriter", "Typewriter"),
  BOLD("bold", "Bold"),
  MONO("mono", "Mono"),
  DISPLAY("display", "Display"),
  ROUNDED("rounded", "Rounded"),
  ITALIC("italic", "Italic"),
}

data class SharedSongTrack(
  val trackId: String,
  val title: String,
  val artist: String,
  val album: String = "",
  val artworkUrl: String = "",
  val previewUrl: String = "",
  val durationMs: Long = 0L,
)

data class SharedHashtagItem(
  val tag: String,
  val postCount: Long,
)

object SharedPostRules {
  const val MAX_TEXT_CHARS = 280
  const val MAX_CAPTION_CHARS = 2200
  const val MAX_PHOTOS = 10
  const val MAX_VIDEO_BYTES = 200L * 1024L * 1024L

  val themes: List<SharedPostTheme> = listOf(
    SharedPostTheme("crimson", "Crimson", SharedPostThemeKind.GRADIENT, 0xFFB8080D, 0xFFE31B23, 0xFFFFFFFF, true),
    SharedPostTheme("coral", "Coral", SharedPostThemeKind.GRADIENT, 0xFFFF6A45, 0xFFFFB057, 0xFFFFFFFF, true),
    SharedPostTheme("ocean", "Ocean", SharedPostThemeKind.GRADIENT, 0xFF0077CC, 0xFF1BB8F5, 0xFFFFFFFF, true),
    SharedPostTheme("emerald", "Emerald", SharedPostThemeKind.GRADIENT, 0xFF0D7A4E, 0xFF28BA77, 0xFFFFFFFF, true),
    SharedPostTheme("violet", "Violet", SharedPostThemeKind.GRADIENT, 0xFF6B2CD4, 0xFF9E5DF2, 0xFFFFFFFF, true),
    SharedPostTheme("obsidian", "Obsidian", SharedPostThemeKind.GRADIENT, 0xFF141821, 0xFF262D3B, 0xFFFFFFFF, true),
    SharedPostTheme("cream", "Cream", SharedPostThemeKind.GRADIENT, 0xFFFAF3E0, 0xFFF2E6CA, 0xFF1E222A, false),
    SharedPostTheme("blush", "Blush", SharedPostThemeKind.GRADIENT, 0xFFFFAEC0, 0xFFFFCFD9, 0xFF2A1419, false),
    SharedPostTheme("scene_aurora", "Aurora", SharedPostThemeKind.IMAGE_SCENE, 0xFF0A1628, 0xFF163D4E, 0xFF42E6B5, true),
    SharedPostTheme("scene_crimson_silk", "Silk", SharedPostThemeKind.IMAGE_SCENE, 0xFF7B0509, 0xFFD91820, 0xFFFF6B6B, true),
    SharedPostTheme("scene_dunes", "Dunes", SharedPostThemeKind.IMAGE_SCENE, 0xFF7F2A22, 0xFFF28E4B, 0xFFFFD180, true),
    SharedPostTheme("scene_waves", "Waves", SharedPostThemeKind.IMAGE_SCENE, 0xFF0B3C5D, 0xFF1D7898, 0xFF7FD8EA, true),
    SharedPostTheme("scene_stars", "Stars", SharedPostThemeKind.IMAGE_SCENE, 0xFF0D1021, 0xFF232946, 0xFFEEC643, true),
    SharedPostTheme("scene_botanical", "Botanical", SharedPostThemeKind.IMAGE_SCENE, 0xFF3E1826, 0xFF7C273F, 0xFFF7A1B5, true),
    SharedPostTheme("scene_paper", "Paper", SharedPostThemeKind.IMAGE_SCENE, 0xFFF5EFE2, 0xFFEAE0CE, 0xFFC8B89E, false),
  )

  fun sanitizeHashtag(raw: String): String {
    val clean = raw.trim().removePrefix("#").lowercase()
    val sb = StringBuilder(50)
    for (ch in clean) {
      if ((ch in 'a'..'z') || (ch in '0'..'9') || ch == '_') {
        sb.append(ch)
        if (sb.length >= 50) break
      }
    }
    return sb.toString()
  }
}

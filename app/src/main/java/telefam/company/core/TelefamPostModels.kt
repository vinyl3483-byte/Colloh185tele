package telefam.company.core

import java.security.MessageDigest
import java.util.Locale

enum class PostMode(val apiValue: String, val label: String) {
  TEXT("text", "Text"),
  PHOTOS("photos", "Photos"),
  VIDEO("video", "Video"),
}

enum class PostThemeKind {
  GRADIENT,
  IMAGE_SCENE,
}

data class PostBackgroundTheme(
  val id: String,
  val label: String,
  val kind: PostThemeKind,
  val startColorArgb: Long,
  val endColorArgb: Long,
  val accentColorArgb: Long,
  val isLightInk: Boolean,
)

enum class PostFontOption(val id: String, val label: String) {
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

data class PostSongTrack(
  val trackId: String,
  val title: String,
  val artist: String,
  val album: String = "",
  val artworkUrl: String = "",
  val previewUrl: String = "",
  val durationMs: Long = 0L,
  val source: String = "",
)

data class HashtagItem(
  val tag: String,
  val postCount: Long,
)

data class VideoRenditionSummary(
  val qualityLabel: String,
  val width: Int,
  val height: Int,
  val bitrateKbps: Int,
  val storageObjectKey: String,
  val byteSize: Int,
)

enum class PostUploadStatus {
  IDLE,
  PREPARING,
  UPLOADING,
  UPLOADED,
  QUEUED,
  FAILED,
}

object TelefamPostRules {
  const val MAX_TEXT_POST_CHARS = 280
  const val MAX_CAPTION_CHARS = 2200
  const val MAX_PHOTOS_COUNT = 10
  const val MAX_VIDEO_BYTES = 200L * 1024L * 1024L // 200 MB
  const val MAX_TAGGED_PEOPLE = 25
  const val MAX_HASHTAGS = 20

  val backgroundThemes: List<PostBackgroundTheme> = listOf(
    // Core solid / gradient circles matching Ref Image 1
    PostBackgroundTheme("crimson", "Crimson", PostThemeKind.GRADIENT, 0xFFB8080D, 0xFFE31B23, 0xFFFFFFFF, true),
    PostBackgroundTheme("coral", "Coral", PostThemeKind.GRADIENT, 0xFFFF6A45, 0xFFFFB057, 0xFFFFFFFF, true),
    PostBackgroundTheme("ocean", "Ocean", PostThemeKind.GRADIENT, 0xFF0077CC, 0xFF1BB8F5, 0xFFFFFFFF, true),
    PostBackgroundTheme("emerald", "Emerald", PostThemeKind.GRADIENT, 0xFF0D7A4E, 0xFF28BA77, 0xFFFFFFFF, true),
    PostBackgroundTheme("violet", "Violet", PostThemeKind.GRADIENT, 0xFF6B2CD4, 0xFF9E5DF2, 0xFFFFFFFF, true),
    PostBackgroundTheme("obsidian", "Obsidian", PostThemeKind.GRADIENT, 0xFF141821, 0xFF262D3B, 0xFFFFFFFF, true),
    PostBackgroundTheme("cream", "Cream", PostThemeKind.GRADIENT, 0xFFFAF3E0, 0xFFF2E6CA, 0xFF1E222A, false),
    PostBackgroundTheme("blush", "Blush", PostThemeKind.GRADIENT, 0xFFFFAEC0, 0xFFFFCFD9, 0xFF2A1419, false),
    // Image-based scenic & textured themes
    PostBackgroundTheme("scene_aurora", "Aurora", PostThemeKind.IMAGE_SCENE, 0xFF0A1628, 0xFF163D4E, 0xFF42E6B5, true),
    PostBackgroundTheme("scene_crimson_silk", "Silk", PostThemeKind.IMAGE_SCENE, 0xFF7B0509, 0xFFD91820, 0xFFFF6B6B, true),
    PostBackgroundTheme("scene_dunes", "Dunes", PostThemeKind.IMAGE_SCENE, 0xFF7F2A22, 0xFFF28E4B, 0xFFFFD180, true),
    PostBackgroundTheme("scene_waves", "Waves", PostThemeKind.IMAGE_SCENE, 0xFF0B3C5D, 0xFF1D7898, 0xFF7FD8EA, true),
    PostBackgroundTheme("scene_stars", "Stars", PostThemeKind.IMAGE_SCENE, 0xFF0D1021, 0xFF232946, 0xFFEEC643, true),
    PostBackgroundTheme("scene_botanical", "Botanical", PostThemeKind.IMAGE_SCENE, 0xFF3E1826, 0xFF7C273F, 0xFFF7A1B5, true),
    PostBackgroundTheme("scene_paper", "Paper", PostThemeKind.IMAGE_SCENE, 0xFFF5EFE2, 0xFFEAE0CE, 0xFFC8B89E, false),
  )

  fun findTheme(themeId: String): PostBackgroundTheme =
    backgroundThemes.firstOrNull { it.id == themeId } ?: backgroundThemes.first()

  fun findFont(fontId: String): PostFontOption =
    PostFontOption.entries.firstOrNull { it.id == fontId } ?: PostFontOption.CLASSIC

  fun sanitizeHashtag(raw: String): String {
    val clean = raw.trim().removePrefix("#").lowercase(Locale.US)
    val sb = StringBuilder(50)
    for (ch in clean) {
      if ((ch in 'a'..'z') || (ch in '0'..'9') || ch == '_') {
        sb.append(ch)
        if (sb.length >= 50) break
      }
    }
    return sb.toString()
  }

  /**
   * Computes deterministic content_id from ordered per-frame 64-bit perceptual dHashes
   * and trimmed clip span. Matches Go server Media Processor & C++ FingerprintEngine byte-for-byte.
   */
  fun computeVideoContentId(
    frameHashes: List<String>,
    durationMs: Long,
    trimStartMs: Long,
    trimEndMs: Long,
  ): String {
    val cleanFrames = frameHashes.mapNotNull { h ->
      val trimmed = h.trim().lowercase(Locale.US)
      if (trimmed.length == 16 && trimmed.all { it in '0'..'9' || it in 'a'..'f' }) trimmed else null
    }
    val joined = cleanFrames.joinToString(",")
    if (NativeTelefamBridge.isNativeLoaded) {
      return NativeTelefamBridge.nativeComputeVideoContentId(joined, durationMs, trimStartMs, trimEndMs)
    }
    val payload = "tf_vid_fp_v1|$joined|dur=$durationMs|trim=$trimStartMs:$trimEndMs"
    val digest = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8))
    val hex = digest.take(16).joinToString("") { "%02x".format(it) }
    return "vid_$hex"
  }

  fun computePhotoContentId(dHashHex: String, sha256Hex: String): String {
    val payload = "tf_img_fp_v1|${dHashHex.lowercase(Locale.US)}|${sha256Hex.lowercase(Locale.US)}"
    val digest = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8))
    return "img_" + digest.take(16).joinToString("") { "%02x".format(it) }
  }

  fun formatMediaDuration(ms: Long): String {
    val totalSec = (ms.coerceAtLeast(0L) / 1000L)
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    return String.format(Locale.US, "%d:%02d", mins, secs)
  }
}

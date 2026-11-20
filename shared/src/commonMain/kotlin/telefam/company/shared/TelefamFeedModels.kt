package telefam.company.shared

import kotlin.math.abs

enum class SharedFeedTab(val apiParam: String, val label: String) {
  FOLLOWING("following", "Following"),
  FOR_YOU("for_you", "For You"),
}

enum class SharedFeedAction(val apiValue: String) {
  LIKE("like"),
  UNLIKE("unlike"),
  FAVOURITE("favourite"),
  UNFAVOURITE("unfavourite"),
  REPOST("repost"),
  UNREPOST("unrepost"),
  SHARE("share"),
  NOT_INTERESTED("not_interested"),
  REPORT("report"),
  BLOCK("block"),
  WATCH("watch"),
  SKIP("skip"),
  VIEW("view"),
}

enum class SharedPostPrivacy(val apiValue: String, val label: String) {
  PUBLIC("public", "Public"),
  FRIENDS("friends", "Friends"),
  PRIVATE("private", "Private");

  companion object {
    fun fromApi(raw: String): SharedPostPrivacy =
      entries.firstOrNull { it.apiValue.equals(raw.trim(), ignoreCase = true) } ?: PUBLIC
  }
}

enum class SharedOwnerProfileTab(val label: String) {
  POSTS("Posts"),
  SAVED("Saved"),
  REPOSTS("Reposts"),
  LOCKED("Locked"),
}

data class SharedFeedVideoRendition(
  val qualityLabel: String,
  val width: Int,
  val height: Int,
  val bitrateKbps: Int,
  val url: String,
  val byteSize: Int,
)

data class FittedMediaDimensions(
  val width: Float,
  val height: Float,
)

object SharedFeedMath {
  /**
   * Preserves exact source aspect ratio within available safe container bounds
   * so ultra-wide (21:9), square (1:1), 4:5, 16:9, or 9:16 videos never stretch,
   * shrink unexpectedly, or clip underneath top/bottom navigation bars.
   */
  fun computeFittedMediaSize(
    containerWidth: Float,
    containerHeight: Float,
    mediaWidth: Int,
    mediaHeight: Int,
  ): FittedMediaDimensions {
    if (containerWidth <= 0f || containerHeight <= 0f) {
      return FittedMediaDimensions(0f, 0f)
    }
    val safeMediaW = if (mediaWidth > 0) mediaWidth.toFloat() else 9f
    val safeMediaH = if (mediaHeight > 0) mediaHeight.toFloat() else 16f
    val mediaRatio = safeMediaW / safeMediaH
    val containerRatio = containerWidth / containerHeight

    // If nearly identical aspect ratio (e.g. phone 9:16 portrait), fill container cleanly.
    if (abs(mediaRatio - containerRatio) <= 0.04f) {
      return FittedMediaDimensions(containerWidth, containerHeight)
    }

    return if (mediaRatio > containerRatio) {
      val fittedW = containerWidth
      val fittedH = (containerWidth / mediaRatio).coerceAtMost(containerHeight)
      FittedMediaDimensions(fittedW, fittedH)
    } else {
      val fittedH = containerHeight
      val fittedW = (containerHeight * mediaRatio).coerceAtMost(containerWidth)
      FittedMediaDimensions(fittedW, fittedH)
    }
  }

  fun formatCompactCount(count: Long): String {
    if (count <= 0L) return "0"
    if (count >= 1_000_000L) {
      val tenths = (count / 100_000L)
      val whole = tenths / 10L
      val rem = tenths % 10L
      return if (rem == 0L) "${whole}M" else "${whole}.${rem}M"
    }
    if (count >= 10_000L) {
      val tenths = (count / 100L)
      val whole = tenths / 10L
      val rem = tenths % 10L
      return if (rem == 0L) "${whole}K" else "${whole}.${rem}K"
    }
    val raw = count.toString()
    if (raw.length <= 3) return raw
    val sb = StringBuilder()
    var digitCount = 0
    for (i in raw.length - 1 downTo 0) {
      sb.append(raw[i])
      digitCount++
      if (digitCount % 3 == 0 && i > 0) sb.append(',')
    }
    return sb.reverse().toString()
  }
}

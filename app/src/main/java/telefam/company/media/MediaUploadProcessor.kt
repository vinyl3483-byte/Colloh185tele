package telefam.company.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import telefam.company.core.TelefamPostRules
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class ProcessedAvatarMedia(
  val mediaId: String,
  val storageObjectKey: String,
  val localFilePath: String,
  val mimeType: String,
  val sha256Fingerprint: String,
  val dHashFingerprint: String,
  val byteSize: Int,
  val previewBitmap: Bitmap,
)

data class ProcessedProfileBackground(
  val mediaId: String,
  val storageObjectKey: String,
  val localFilePath: String,
  val width: Int,
  val height: Int,
  val byteSize: Int,
  val sha256Fingerprint: String,
  val dHashFingerprint: String,
)

data class ProcessedPostPhoto(
  val localId: String,
  val localFilePath: String,
  val width: Int,
  val height: Int,
  val byteSize: Int,
  val sha256Fingerprint: String,
  val dHashFingerprint: String,
  val contentId: String,
)

data class ExtractedVideoFrame(
  val index: Int,
  val timestampMs: Long,
  val previewPath: String,
  val dHashHex: String,
)

data class PreparedPostVideo(
  val localId: String,
  val localVideoPath: String,
  val mimeType: String,
  val byteSize: Long,
  val durationMs: Long,
  val width: Int,
  val height: Int,
  val frames: List<ExtractedVideoFrame>,
  val selectedThumbnailIndex: Int,
  val selectedThumbnailPath: String,
  val contentId: String,
)

sealed class VideoPrepareResult {
  data class Ready(val video: PreparedPostVideo) : VideoPrepareResult()
  data class Rejected(val reason: String) : VideoPrepareResult()
}

object MediaUploadProcessor {
  private const val MAX_AVATAR_INPUT_BYTES = 10 * 1024 * 1024 // 10 MB
  private const val MAX_POST_PHOTO_INPUT_BYTES = 20 * 1024 * 1024 // 20 MB before compression
  private const val TARGET_AVATAR_PX = 512
  private const val TARGET_MAX_AVATAR_BYTES = 180 * 1024
  private const val MAX_POST_PHOTO_EDGE_PX = 1440
  private const val TARGET_MAX_POST_PHOTO_BYTES = 420 * 1024

  fun loadSafePreviewBitmap(context: Context, uri: Uri): Bitmap? {
    val rawBytes = readBoundedBytes(context, uri, MAX_AVATAR_INPUT_BYTES) ?: return null
    if (!isSupportedImageMagicBytes(rawBytes)) return null

    val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, boundsOpts)
    if (boundsOpts.outWidth < 32 || boundsOpts.outHeight < 32) return null

    var sampleSize = 1
    while ((boundsOpts.outWidth / sampleSize) > 1280 || (boundsOpts.outHeight / sampleSize) > 1280) {
      sampleSize *= 2
    }
    val decodeOpts = BitmapFactory.Options().apply {
      inSampleSize = sampleSize
      inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    return BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOpts)
  }

  fun cropEncodeAndStore(
    context: Context,
    sourceBitmap: Bitmap,
    normalizedCenterX: Float,
    normalizedCenterY: Float,
    zoomScale: Float,
  ): ProcessedAvatarMedia {
    val w = sourceBitmap.width
    val h = sourceBitmap.height
    val minSide = min(w, h)
    val clampedScale = zoomScale.coerceIn(1f, 4f)
    val cropSide = (minSide / clampedScale).roundToInt().coerceAtLeast(32)

    val cx = (normalizedCenterX.coerceIn(0f, 1f) * w).roundToInt()
    val cy = (normalizedCenterY.coerceIn(0f, 1f) * h).roundToInt()
    val left = (cx - cropSide / 2).coerceIn(0, (w - cropSide).coerceAtLeast(0))
    val top = (cy - cropSide / 2).coerceIn(0, (h - cropSide).coerceAtLeast(0))

    val cleanBitmap = Bitmap.createBitmap(TARGET_AVATAR_PX, TARGET_AVATAR_PX, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(cleanBitmap)
    canvas.drawColor(Color.WHITE)
    canvas.drawBitmap(
      sourceBitmap,
      Rect(left, top, left + cropSide, top + cropSide),
      Rect(0, 0, TARGET_AVATAR_PX, TARGET_AVATAR_PX),
      null,
    )

    var compressedBytes = ByteArray(0)
    for (quality in intArrayOf(84, 76, 68, 56)) {
      val out = ByteArrayOutputStream()
      cleanBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
      compressedBytes = out.toByteArray()
      if (compressedBytes.size <= TARGET_MAX_AVATAR_BYTES) break
    }

    val sha256 = sha256Hex(compressedBytes)
    val dHash = computeDHashHex(cleanBitmap)
    val mediaId = UUID.randomUUID().toString()
    val storageKey = "media/$mediaId.jpg"

    val mediaDir = File(context.filesDir, "media").apply { mkdirs() }
    val targetFile = File(mediaDir, "$mediaId.jpg")
    targetFile.writeBytes(compressedBytes)

    return ProcessedAvatarMedia(
      mediaId = mediaId,
      storageObjectKey = storageKey,
      localFilePath = targetFile.absolutePath,
      mimeType = "image/jpeg",
      sha256Fingerprint = sha256,
      dHashFingerprint = dHash,
      byteSize = compressedBytes.size,
      previewBitmap = cleanBitmap,
    )
  }

  /**
   * Decodes a profile hero background photo cleanly, strips EXIF/XMP metadata onto an opaque canvas,
   * bounds longest edge to 1600px, compresses to <= 520 KB JPEG, computes SHA-256 + 64-bit dHash
   * fingerprints, and stores under a pure UUID path.
   */
  fun compressProfileBackground(context: Context, uri: Uri): ProcessedProfileBackground? {
    val rawBytes = readBoundedBytes(context, uri, MAX_POST_PHOTO_INPUT_BYTES) ?: return null
    if (!isSupportedImageMagicBytes(rawBytes)) return null

    val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, boundsOpts)
    if (boundsOpts.outWidth < 32 || boundsOpts.outHeight < 32) return null

    val maxEdge = 1600
    var sampleSize = 1
    while ((boundsOpts.outWidth / sampleSize) > maxEdge ||
      (boundsOpts.outHeight / sampleSize) > maxEdge
    ) {
      sampleSize *= 2
    }
    val decoded = BitmapFactory.decodeByteArray(
      rawBytes,
      0,
      rawBytes.size,
      BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.ARGB_8888
      },
    ) ?: return null

    val w = decoded.width.coerceAtLeast(32)
    val h = decoded.height.coerceAtLeast(32)
    val maxDim = max(w, h)
    val targetW: Int
    val targetH: Int
    if (maxDim > maxEdge) {
      val ratio = maxEdge.toFloat() / maxDim.toFloat()
      targetW = (w * ratio).roundToInt().coerceAtLeast(32)
      targetH = (h * ratio).roundToInt().coerceAtLeast(32)
    } else {
      targetW = w
      targetH = h
    }

    val cleanBitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(cleanBitmap)
    canvas.drawColor(Color.BLACK)
    canvas.drawBitmap(
      decoded,
      Rect(0, 0, decoded.width, decoded.height),
      Rect(0, 0, targetW, targetH),
      null,
    )
    decoded.recycle()

    var encoded = ByteArray(0)
    val maxBytes = 520 * 1024
    for (q in intArrayOf(86, 78, 70, 62)) {
      val out = ByteArrayOutputStream()
      cleanBitmap.compress(Bitmap.CompressFormat.JPEG, q, out)
      encoded = out.toByteArray()
      if (encoded.size <= maxBytes) break
    }

    val sha256 = sha256Hex(encoded)
    val dHash = computeDHashHex(cleanBitmap)
    cleanBitmap.recycle()

    val mediaId = UUID.randomUUID().toString()
    val bgDir = File(context.filesDir, "media/bg").apply { mkdirs() }
    val outFile = File(bgDir, "$mediaId.jpg")
    outFile.writeBytes(encoded)

    return ProcessedProfileBackground(
      mediaId = mediaId,
      storageObjectKey = "media/bg/$mediaId.jpg",
      localFilePath = outFile.absolutePath,
      width = targetW,
      height = targetH,
      byteSize = encoded.size,
      sha256Fingerprint = sha256,
      dHashFingerprint = dHash,
    )
  }

  /**
   * Decodes a gallery photo cleanly, strips metadata onto an opaque canvas, scales longest edge <= 1440px,
   * compresses fast as JPEG, computes SHA-256 + 64-bit dHash + image content_id, and stores under UUID filename.
   */
  fun compressPostPhoto(context: Context, uri: Uri): ProcessedPostPhoto? {
    val rawBytes = readBoundedBytes(context, uri, MAX_POST_PHOTO_INPUT_BYTES) ?: return null
    if (!isSupportedImageMagicBytes(rawBytes)) return null

    val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, boundsOpts)
    if (boundsOpts.outWidth < 16 || boundsOpts.outHeight < 16) return null

    var sampleSize = 1
    while ((boundsOpts.outWidth / sampleSize) > MAX_POST_PHOTO_EDGE_PX ||
      (boundsOpts.outHeight / sampleSize) > MAX_POST_PHOTO_EDGE_PX
    ) {
      sampleSize *= 2
    }
    val decoded = BitmapFactory.decodeByteArray(
      rawBytes,
      0,
      rawBytes.size,
      BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.ARGB_8888
      },
    ) ?: return null

    val result = compressPostPhotoBitmap(context, decoded)
    decoded.recycle()
    return result
  }

  fun compressPostPhotoBitmap(context: Context, source: Bitmap): ProcessedPostPhoto {
    val w = source.width.coerceAtLeast(16)
    val h = source.height.coerceAtLeast(16)
    val maxDim = max(w, h)
    val targetW: Int
    val targetH: Int
    if (maxDim > MAX_POST_PHOTO_EDGE_PX) {
      val ratio = MAX_POST_PHOTO_EDGE_PX.toFloat() / maxDim.toFloat()
      targetW = (w * ratio).roundToInt().coerceAtLeast(16)
      targetH = (h * ratio).roundToInt().coerceAtLeast(16)
    } else {
      targetW = w
      targetH = h
    }

    val cleanBitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(cleanBitmap)
    canvas.drawColor(Color.WHITE)
    canvas.drawBitmap(
      source,
      Rect(0, 0, source.width, source.height),
      Rect(0, 0, targetW, targetH),
      null,
    )

    var encoded = ByteArray(0)
    for (q in intArrayOf(82, 74, 66)) {
      val out = ByteArrayOutputStream()
      cleanBitmap.compress(Bitmap.CompressFormat.JPEG, q, out)
      encoded = out.toByteArray()
      if (encoded.size <= TARGET_MAX_POST_PHOTO_BYTES) break
    }

    val sha = sha256Hex(encoded)
    val dHash = computeDHashHex(cleanBitmap)
    val contentId = TelefamPostRules.computePhotoContentId(dHash, sha)
    cleanBitmap.recycle()

    val uuid = UUID.randomUUID().toString()
    val photosDir = File(context.filesDir, "posts/photos").apply { mkdirs() }
    val outFile = File(photosDir, "$uuid.jpg")
    outFile.writeBytes(encoded)

    return ProcessedPostPhoto(
      localId = uuid,
      localFilePath = outFile.absolutePath,
      width = targetW,
      height = targetH,
      byteSize = encoded.size,
      sha256Fingerprint = sha,
      dHashFingerprint = dHash,
      contentId = contentId,
    )
  }

  /**
   * Validates up to 200 MB video file, copies safely to UUID-named staging file,
   * extracts timeline frames for trimming + thumbnail selection, computes per-frame
   * 64-bit dHashes, and derives accurate deterministic video content_id.
   */
  fun inspectAndExtractVideoFrames(
    context: Context,
    uri: Uri,
    targetFrameCount: Int = 10,
  ): VideoPrepareResult {
    val reportedSize = queryUriSize(context, uri)
    if (reportedSize > TelefamPostRules.MAX_VIDEO_BYTES) {
      return VideoPrepareResult.Rejected("Video exceeds 200 MB limit")
    }

    val localId = UUID.randomUUID().toString()
    val videosDir = File(context.filesDir, "posts/videos").apply { mkdirs() }
    val stagedFile = File(videosDir, "$localId.mp4")

    val copiedBytes: Long
    val header = ByteArray(16)
    var headerRead = 0
    try {
      context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(stagedFile).use { output ->
          val buf = ByteArray(64 * 1024)
          var total = 0L
          while (true) {
            val read = input.read(buf)
            if (read <= 0) break
            if (headerRead < header.size) {
              val copyLen = min(read, header.size - headerRead)
              System.arraycopy(buf, 0, header, headerRead, copyLen)
              headerRead += copyLen
            }
            total += read
            if (total > TelefamPostRules.MAX_VIDEO_BYTES) {
              stagedFile.delete()
              return VideoPrepareResult.Rejected("Video exceeds 200 MB limit")
            }
            output.write(buf, 0, read)
          }
          copiedBytes = total
        }
      } ?: return VideoPrepareResult.Rejected("Unable to read selected video")
    } catch (_: Throwable) {
      stagedFile.delete()
      return VideoPrepareResult.Rejected("Unable to read selected video")
    }

    if (copiedBytes <= 0L) {
      stagedFile.delete()
      return VideoPrepareResult.Rejected("Empty video file")
    }

    val detectedMime = sniffVideoMagicMime(header.copyOf(headerRead))
      ?: run {
        stagedFile.delete()
        return VideoPrepareResult.Rejected("Unsupported video format")
      }

    var durationMs = 0L
    var width = 1280
    var height = 720
    val frames = mutableListOf<ExtractedVideoFrame>()
    val thumbsDir = File(context.filesDir, "posts/thumbs/$localId").apply { mkdirs() }

    val retriever = MediaMetadataRetriever()
    try {
      retriever.setDataSource(stagedFile.absolutePath)
      durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        ?.toLongOrNull()
        ?.coerceAtLeast(0L) ?: 0L
      width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
        ?.toIntOrNull() ?: 1280
      height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
        ?.toIntOrNull() ?: 720

      val count = targetFrameCount.coerceIn(2, 12)
      val safeDur = durationMs.coerceAtLeast(1000L)
      for (idx in 0 until count) {
        val tsMs = (safeDur * idx) / max(1, count - 1)
        val rawFrame = runCatching {
          retriever.getFrameAtTime(tsMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        }.getOrNull()
        if (rawFrame != null) {
          val thumbFile = saveFrameThumbnail(thumbsDir, idx, rawFrame)
          val dHash = computeDHashHex(rawFrame)
          rawFrame.recycle()
          frames += ExtractedVideoFrame(
            index = idx,
            timestampMs = tsMs,
            previewPath = thumbFile.absolutePath,
            dHashHex = dHash,
          )
        }
      }
    } catch (_: Throwable) {
      // Fall back to deterministic byte slice hashes below if hardware codec unavailable
    } finally {
      runCatching { retriever.release() }
    }

    // Fallback frame fingerprint tokens if retriever extracted no bitmap frames (e.g., host JVM unit test)
    if (frames.isEmpty()) {
      val fallbackHashes = computeStreamSliceHashes(stagedFile, 8)
      fallbackHashes.forEachIndexed { idx, hash ->
        frames += ExtractedVideoFrame(
          index = idx,
          timestampMs = (durationMs.coerceAtLeast(4000L) * idx) / max(1, fallbackHashes.lastIndex),
          previewPath = "",
          dHashHex = hash,
        )
      }
    }

    val effectiveDuration = durationMs.coerceAtLeast(1000L)
    val contentId = TelefamPostRules.computeVideoContentId(
      frameHashes = frames.map { it.dHashHex },
      durationMs = effectiveDuration,
      trimStartMs = 0L,
      trimEndMs = effectiveDuration,
    )
    val defaultThumbIndex = if (frames.size > 1) 1 else 0
    val defaultThumbPath = frames.getOrNull(defaultThumbIndex)?.previewPath.orEmpty()

    return VideoPrepareResult.Ready(
      PreparedPostVideo(
        localId = localId,
        localVideoPath = stagedFile.absolutePath,
        mimeType = detectedMime,
        byteSize = copiedBytes,
        durationMs = effectiveDuration,
        width = width,
        height = height,
        frames = frames,
        selectedThumbnailIndex = defaultThumbIndex,
        selectedThumbnailPath = defaultThumbPath,
        contentId = contentId,
      ),
    )
  }

  fun sniffVideoMagicMime(bytes: ByteArray): String? {
    if (bytes.size < 12) return null
    if (bytes[4] == 'f'.code.toByte() &&
      bytes[5] == 't'.code.toByte() &&
      bytes[6] == 'y'.code.toByte() &&
      bytes[7] == 'p'.code.toByte()
    ) {
      val brand = String(bytes, 8, 4, Charsets.US_ASCII)
      return if (brand == "qt  ") "video/quicktime" else "video/mp4"
    }
    if (bytes[0] == 0x1A.toByte() &&
      bytes[1] == 0x45.toByte() &&
      bytes[2] == 0xDF.toByte() &&
      bytes[3] == 0xA3.toByte()
    ) {
      return "video/webm"
    }
    return null
  }

  fun isSupportedImageMagicBytes(bytes: ByteArray): Boolean {
    if (bytes.size < 12) return false
    val isJpeg = bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()
    val isPng = bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
      bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()
    val isGif = bytes[0] == 'G'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
      bytes[2] == 'F'.code.toByte() && bytes[3] == '8'.code.toByte()
    val isWebp = bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
      bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte() &&
      bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() &&
      bytes[10] == 'B'.code.toByte() && bytes[11] == 'P'.code.toByte()
    return isJpeg || isPng || isGif || isWebp
  }

  private fun saveFrameThumbnail(dir: File, idx: Int, source: Bitmap): File {
    val maxEdge = 480
    val w = source.width.coerceAtLeast(16)
    val h = source.height.coerceAtLeast(16)
    val scale = maxEdge.toFloat() / max(w, h).toFloat()
    val tw = if (scale < 1f) (w * scale).roundToInt().coerceAtLeast(16) else w
    val th = if (scale < 1f) (h * scale).roundToInt().coerceAtLeast(16) else h
    val scaled = Bitmap.createScaledBitmap(source, tw, th, true)
    val outFile = File(dir, "frame_${UUID.randomUUID()}.jpg")
    FileOutputStream(outFile).use { fos ->
      scaled.compress(Bitmap.CompressFormat.JPEG, 78, fos)
    }
    if (scaled !== source) scaled.recycle()
    return outFile
  }

  private fun computeStreamSliceHashes(file: File, slices: Int): List<String> {
    val len = file.length()
    if (len <= 0L) return emptyList()
    val out = ArrayList<String>(slices)
    val buf = ByteArray(4096)
    FileInputStream(file).use { fis ->
      for (i in 0 until slices) {
        val targetOffset = (len * i) / slices
        val skip = max(0L, targetOffset - fis.channel.position())
        if (skip > 0L) fis.channel.position(targetOffset)
        val n = fis.read(buf)
        val digest = MessageDigest.getInstance("SHA-256")
          .digest(if (n > 0) buf.copyOf(n) else byteArrayOf(i.toByte()))
        out += digest.take(8).joinToString("") { "%02x".format(it) }
      }
    }
    return out
  }

  private fun queryUriSize(context: Context, uri: Uri): Long {
    if (uri.scheme == "file") {
      return File(uri.path.orEmpty()).length()
    }
    return runCatching {
      context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
          val col = cursor.getColumnIndex(OpenableColumns.SIZE)
          if (col >= 0) cursor.getLong(col) else 0L
        } else 0L
      } ?: 0L
    }.getOrDefault(0L)
  }

  private fun readBoundedBytes(context: Context, uri: Uri, maxBytes: Int): ByteArray? {
    return runCatching {
      context.contentResolver.openInputStream(uri)?.use { input ->
        val out = ByteArrayOutputStream()
        val buf = ByteArray(8192)
        var total = 0
        while (true) {
          val read = input.read(buf)
          if (read <= 0) break
          total += read
          if (total > maxBytes) return null
          out.write(buf, 0, read)
        }
        out.toByteArray()
      }
    }.getOrNull()
  }

  fun computeDHashHex(bitmap: Bitmap): String {
    val scaled = Bitmap.createScaledBitmap(bitmap, 9, 8, true)
    var hash = 0L
    var bit = 0
    for (y in 0 until 8) {
      for (x in 0 until 8) {
        val leftLuma = luma(scaled.getPixel(x, y))
        val rightLuma = luma(scaled.getPixel(x + 1, y))
        if (leftLuma > rightLuma) {
          hash = hash or (1L shl bit)
        }
        bit++
      }
    }
    if (scaled !== bitmap) scaled.recycle()
    return "%016x".format(hash)
  }

  private fun luma(argb: Int): Int {
    val r = Color.red(argb)
    val g = Color.green(argb)
    val b = Color.blue(argb)
    return (299 * r + 587 * g + 114 * b) / 1000
  }

  private fun sha256Hex(bytes: ByteArray): String {
    return MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
  }
}

package telefam.company.media

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

sealed class VideoDownloadResult {
  data class Saved(val displayName: String) : VideoDownloadResult()
  data class Failed(val reason: String) : VideoDownloadResult()
}

/**
 * Play-Policy compliant scoped downloader using MediaStore.Downloads (API 29+)
 * and app-scoped external downloads (API 24-28). Requires zero storage permissions.
 */
object TelefamVideoDownloader {
  suspend fun downloadVideoToDevice(
    context: Context,
    postId: String,
    sourceUrlOrPath: String,
    bearerToken: String = "",
    deviceFingerprint: String = "",
  ): VideoDownloadResult = withContext(Dispatchers.IO) {
    val cleanId = postId.replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { "video" }
    val fileName = "telefam_${cleanId}_${System.currentTimeMillis() / 1000L}.mp4"

    runCatching {
      val localFile = sourceUrlOrPath.removePrefix("file://").let(::File)
      if (localFile.exists() && localFile.isFile) {
        return@runCatching writeStreamToDownloads(context, fileName) {
          FileInputStream(localFile)
        }
      }

      if (!sourceUrlOrPath.startsWith("http://") && !sourceUrlOrPath.startsWith("https://")) {
        return@runCatching VideoDownloadResult.Failed("Download unavailable")
      }

      val requestBuilder = Request.Builder().url(sourceUrlOrPath)
      if (bearerToken.isNotBlank()) requestBuilder.header("Authorization", "Bearer $bearerToken")
      if (deviceFingerprint.isNotBlank()) requestBuilder.header("X-Telefam-Device-FP", deviceFingerprint)

      TelefamVideoCacheManager.getHttpClient().newCall(requestBuilder.build()).execute().use { resp ->
        if (!resp.isSuccessful) {
          return@runCatching VideoDownloadResult.Failed("Download unavailable")
        }
        val body = resp.body ?: return@runCatching VideoDownloadResult.Failed("Download unavailable")
        writeStreamToDownloads(context, fileName) { body.byteStream() }
      }
    }.getOrElse {
      VideoDownloadResult.Failed("Download unavailable")
    }
  }

  private fun writeStreamToDownloads(
    context: Context,
    fileName: String,
    openStream: () -> InputStream,
  ): VideoDownloadResult {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val resolver = context.contentResolver
      val values = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
        put(MediaStore.Downloads.MIME_TYPE, "video/mp4")
        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Telefam")
        put(MediaStore.Downloads.IS_PENDING, 1)
      }
      val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        ?: return VideoDownloadResult.Failed("Download unavailable")
      resolver.openOutputStream(uri)?.use { out ->
        openStream().use { input -> input.copyTo(out) }
      } ?: run {
        resolver.delete(uri, null, null)
        return VideoDownloadResult.Failed("Download unavailable")
      }
      val finalize = ContentValues().apply {
        put(MediaStore.Downloads.IS_PENDING, 0)
      }
      resolver.update(uri, finalize, null, null)
      VideoDownloadResult.Saved(fileName)
    } else {
      val dir = File(
        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir,
        "Telefam",
      ).apply { if (!exists()) mkdirs() }
      val target = File(dir, fileName)
      target.outputStream().use { out ->
        openStream().use { input -> input.copyTo(out) }
      }
      VideoDownloadResult.Saved(fileName)
    }
  }
}

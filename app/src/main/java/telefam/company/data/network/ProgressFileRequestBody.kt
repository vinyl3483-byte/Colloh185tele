package telefam.company.data.network

import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.File
import java.io.FileInputStream

/**
 * Streams large files (up to 200 MB video) in 64 KB chunks without loading the file into heap,
 * reporting accurate upload progress in [0.0f .. 1.0f].
 */
class ProgressFileRequestBody(
  private val file: File,
  private val contentType: MediaType = "application/octet-stream".toMediaType(),
  private val onProgress: (Float) -> Unit,
) : RequestBody() {

  override fun contentType(): MediaType = contentType

  override fun contentLength(): Long = file.length()

  override fun writeTo(sink: BufferedSink) {
    val total = file.length().coerceAtLeast(1L)
    val buffer = ByteArray(64 * 1024)
    var uploaded = 0L
    var lastReportedPct = -1
    FileInputStream(file).use { input ->
      while (true) {
        val read = input.read(buffer)
        if (read <= 0) break
        sink.write(buffer, 0, read)
        uploaded += read
        val pct = ((uploaded * 100L) / total).toInt().coerceIn(0, 100)
        if (pct != lastReportedPct) {
          lastReportedPct = pct
          onProgress((uploaded.toFloat() / total.toFloat()).coerceIn(0f, 1f))
        }
      }
    }
  }
}

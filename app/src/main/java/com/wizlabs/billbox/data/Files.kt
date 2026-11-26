package com.wizlabs.billbox.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.File
import java.util.UUID

/** PDF bills on the phone (N4): copied into app storage, first page rendered as a thumbnail. */
class Files(private val context: Context) {

    val dir: File = File(context.filesDir, "docs").apply { mkdirs() }
    private val thumbs: File = File(context.cacheDir, "pdf-thumbs").apply { mkdirs() }

    fun file(name: String) = File(dir, name)

    data class Imported(val localName: String, val displayName: String)

    /** Copies a picked PDF. Throws [IllegalArgumentException] with a short reason when it can't be used. */
    fun import(uri: Uri): Imported {
        val display = displayName(uri)
        val name = "${UUID.randomUUID()}.pdf"
        val out = file(name)
        var total = 0L
        try {
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "unreadable" }
                out.outputStream().use { o ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf); if (n < 0) break
                        total += n
                        require(total <= MAX_BYTES) { "too-big" }
                        o.write(buf, 0, n)
                    }
                }
            }
            require(total > 0) { "unreadable" }
            // Must be a real PDF with at least one page.
            val ok = runCatching { ParcelFileDescriptor.open(out, ParcelFileDescriptor.MODE_READ_ONLY).use { withRenderer(it) { r -> r.pageCount > 0 } } }
                .getOrDefault(false)
            require(ok) { "not-pdf" }
        } catch (e: Exception) {
            out.delete(); throw if (e is IllegalArgumentException) e else IllegalArgumentException("unreadable")
        }
        return Imported(name, display)
    }

    private fun displayName(uri: Uri): String {
        val n = runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull()
        return (n ?: "bill.pdf").let { if (it.endsWith(".pdf", true)) it else "$it.pdf" }.take(80)
    }

    fun delete(name: String?) {
        if (name == null) return
        file(name).delete(); File(thumbs, "$name.png").delete()
    }

    fun deleteAll() {
        dir.listFiles()?.forEach { it.delete() }
        thumbs.listFiles()?.forEach { it.delete() }
    }

    /** First page as a bitmap about [widthPx] wide, cached on disk. Call off the main thread. The renderer is closed before returning. */
    fun thumbnail(name: String, widthPx: Int): Bitmap? {
        val f = file(name)
        if (!f.exists()) return null
        val cache = File(thumbs, "$name.png")
        if (cache.exists()) android.graphics.BitmapFactory.decodeFile(cache.path)?.let { return it }
        return runCatching {
            ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                withRenderer(pfd) { r ->
                    val page = r.openPage(0)
                    try {
                        val w = widthPx.coerceIn(120, 900)
                        val h = (w.toFloat() * page.height / page.width).toInt().coerceAtLeast(1)
                        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        cache.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 90, it) }
                        bmp
                    } finally { page.close() }
                }
            }
        }.getOrNull()
    }

    /** PdfRenderer is only AutoCloseable on new Android versions, so close it by hand. */
    private fun <T> withRenderer(pfd: ParcelFileDescriptor, block: (PdfRenderer) -> T): T {
        val r = PdfRenderer(pfd)
        try { return block(r) } finally { r.close() }
    }

    companion object {
        const val MAX_BYTES = 15L * 1024 * 1024
    }
}

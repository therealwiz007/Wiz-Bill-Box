package com.wizlabs.billbox.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.LruCache
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.util.UUID

/** Bill photos on the phone: imported, shrunk to a sensible size, and decoded for display. */
class Photos(private val context: Context) {

    val dir: File = File(context.filesDir, "photos").apply { mkdirs() }

    fun file(name: String) = File(dir, name)

    /** A fresh file the camera app can write a full-size picture into. */
    fun newCameraFile(): File = File(File(context.cacheDir, "camera").apply { mkdirs() }, "shot-${System.currentTimeMillis()}.jpg")

    /**
     * Copies a picture (camera or gallery) into app storage as an upright JPEG no larger than
     * [MAX_SIDE] px, which keeps receipts readable while making Drive uploads quick.
     * Returns the stored file name.
     */
    fun import(uri: Uri): String {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0) { "Not an image" }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
        val decoded = context.contentResolver.openInputStream(uri).use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("Could not read the image")
        val rotation = runCatching {
            context.contentResolver.openInputStream(uri).use { ExifInterface(it!!).rotationDegrees }
        }.getOrDefault(0)
        val upright = scaleAndRotate(decoded, rotation)
        val name = "${UUID.randomUUID()}.jpg"
        file(name).outputStream().use { upright.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        if (upright !== decoded) decoded.recycle()
        upright.recycle()
        return name
    }

    private fun scaleAndRotate(src: Bitmap, rotation: Int): Bitmap {
        val longest = maxOf(src.width, src.height)
        val scale = if (longest > MAX_SIDE) MAX_SIDE.toFloat() / longest else 1f
        if (scale == 1f && rotation == 0) return src
        val m = Matrix().apply { postScale(scale, scale); postRotate(rotation.toFloat()) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    }

    fun delete(name: String?) {
        if (name != null) { file(name).delete(); cache.remove(name) }
    }

    fun deleteAll() {
        dir.listFiles()?.forEach { it.delete() }
        cache.evictAll()
    }

    private val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    /** Decodes a stored photo at roughly [targetPx] on its longest side. Call off the main thread. */
    fun load(name: String, targetPx: Int): Bitmap? {
        val key = "$name@$targetPx"
        cache.get(key)?.let { return it }
        val f = file(name)
        if (!f.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= targetPx) sample *= 2
        val bmp = BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        cache.put(key, bmp)
        return bmp
    }

    companion object {
        const val MAX_SIDE = 2400
    }
}

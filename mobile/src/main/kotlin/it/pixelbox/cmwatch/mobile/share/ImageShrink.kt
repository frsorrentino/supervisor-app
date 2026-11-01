package it.pixelbox.cmwatch.mobile.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import it.pixelbox.cmwatch.rules.ShareImage
import java.io.ByteArrayOutputStream

/** L'immagine condivisa ridotta a 1600 px sul lato lungo, JPEG q85 (contratto 1.19); null se non si legge. */
object ImageShrink {
    fun jpeg(ctx: Context, uri: Uri): ByteArray? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0) return null
        val opts = BitmapFactory.Options().apply { inSampleSize = ShareImage.sampleSize(bounds.outWidth, bounds.outHeight) }
        val decoded = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
        val raw = turned(decoded, orientation(ctx, uri))
        val (w, h) = ShareImage.scaled(raw.width, raw.height)
        val bmp = if (w == raw.width && h == raw.height) raw else Bitmap.createScaledBitmap(raw, w, h, true)
        ByteArrayOutputStream().use { out -> bmp.compress(Bitmap.CompressFormat.JPEG, 85, out); out.toByteArray() }
    }.getOrNull()

    /** L'orientamento EXIF di un'immagine; 1, cioè dritta, se non c'è o non si legge. */
    fun orientation(ctx: Context, uri: Uri): Int = runCatching { ctx.contentResolver.openInputStream(uri)?.use { orientation(it) } }.getOrNull() ?: 1

    fun orientation(path: String): Int = runCatching { java.io.File(path).inputStream().use { orientation(it) } }.getOrElse { 1 }

    private fun orientation(input: java.io.InputStream): Int {
        val exif = ExifInterface(input)
        return exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    }

    /** La bitmap raddrizzata: il JPEG ricompresso non porta più l'EXIF, quindi i pixel devono essere già giusti. */
    fun turned(src: Bitmap, exifOrientation: Int): Bitmap {
        val t = ShareImage.turn(exifOrientation)
        if (t.degrees == 0 && !t.mirror) return src
        val m = Matrix().apply { if (t.mirror) postScale(-1f, 1f); postRotate(t.degrees.toFloat()) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    }
}

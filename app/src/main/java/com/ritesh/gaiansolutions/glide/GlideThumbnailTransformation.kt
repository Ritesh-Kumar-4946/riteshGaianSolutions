package com.ritesh.gaiansolutions.glide

import android.graphics.Bitmap
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.nio.ByteBuffer
import java.security.MessageDigest

/**
 * Created by Ritesh Kumar on 09/12/22 , 11:42 AM
 * Contact: riteshkumar.4946@gmail.com , +91-7415984946
 */

private const val MAX_LINES = 7
private const val MAX_COLUMNS = 7
private const val THUMBNAILS_EACH = 5000 // milliseconds

class GlideThumbnailTransformation(position: Long) : BitmapTransformation() {

    private val x: Int
    private val y: Int

    init {
        val square = position.toInt().div(THUMBNAILS_EACH)
        x = square % MAX_COLUMNS
        y = square / MAX_COLUMNS
    }

    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        val width = toTransform.width / MAX_COLUMNS
        val height = toTransform.height / MAX_LINES

        return Bitmap.createBitmap(toTransform, x * width, y * height, width, height)
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        val data: ByteArray = ByteBuffer.allocate(8).putInt(x).putInt(y).array()
        messageDigest.update(data)
    }

    override fun hashCode(): Int {
        return (x.toString() + y.toString()).hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if(other !is GlideThumbnailTransformation) return false
        return other.x == x && other.y == y
    }
}
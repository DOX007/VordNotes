package com.mhss.app.core.util

import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream

fun Bitmap.toBase64(
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
    quality: Int = 90
): String {
    val stream = ByteArrayOutputStream()
    compress(format, quality, stream)
    return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
}


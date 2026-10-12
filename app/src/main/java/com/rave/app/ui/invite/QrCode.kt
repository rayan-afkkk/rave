package com.rave.app.ui.invite

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

/** Renders [text] as a QR bitmap ([dark] modules on a transparent background). */
fun qrBitmap(text: String, size: Int = 640, dark: Int = Color.BLACK): Bitmap {
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, mapOf(EncodeHintType.MARGIN to 1))
    val pixels = IntArray(size * size) { i -> if (matrix[i % size, i / size]) dark else Color.TRANSPARENT }
    return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
}

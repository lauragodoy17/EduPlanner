package com.softcg.myapplication.core.qr

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** Dibuja un texto como QR. Módulos oscuros sobre blanco: el contraste alto es lo que lo hace legible. */
object GeneradorQr {

    fun generar(texto: String, lado: Int, colorModulos: Int = Color.BLACK): Bitmap {
        val hints = mapOf(
            // M: tolera ~15 % del código dañado o con reflejos de la pantalla, sin hacerlo demasiado denso
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 1
        )
        val matriz = QRCodeWriter().encode(texto, BarcodeFormat.QR_CODE, lado, lado, hints)
        val pixeles = IntArray(lado * lado) { i -> if (matriz[i % lado, i / lado]) colorModulos else Color.WHITE }
        return Bitmap.createBitmap(pixeles, lado, lado, Bitmap.Config.ARGB_8888)
    }
}

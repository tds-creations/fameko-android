package com.example.famekodriver.customer.ui.components

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.animation.LinearInterpolator
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.maplibre.android.annotations.Icon
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.Marker
import org.maplibre.android.geometry.LatLng
import kotlin.math.min

@Suppress("DEPRECATION")
fun createMarkerIcon(context: android.content.Context, color: Color): Icon {
    val size = 64
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    
    // Draw pin body
    paint.color = color.toArgb()
    val pinPath = android.graphics.Path()
    pinPath.moveTo(size / 2f, size.toFloat())
    pinPath.lineTo(size * 0.15f, size * 0.4f)
    val rectF = android.graphics.RectF(size * 0.15f, 0f, size * 0.85f, size * 0.7f)
    pinPath.arcTo(rectF, 150f, 240f, false)
    pinPath.close()
    
    canvas.drawPath(pinPath, paint)

    // Draw inner circle
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(size / 2f, size * 0.35f, size / 7f, paint)

    return IconFactory.getInstance(context).fromBitmap(bitmap)
}

@Suppress("DEPRECATION")
fun animateMarker(marker: Marker, startPos: LatLng, endPos: LatLng, duration: Long = 1500, onEnd: () -> Unit = {}) {
    val handler = Handler(Looper.getMainLooper())
    val start = SystemClock.uptimeMillis()
    val interpolator = LinearInterpolator()

    handler.post(object : Runnable {
        override fun run() {
            val elapsed = SystemClock.uptimeMillis() - start
            val t = min(1f, interpolator.getInterpolation(elapsed.toFloat() / duration))

            val lat = t * endPos.latitude + (1 - t) * startPos.latitude
            val lng = t * endPos.longitude + (1 - t) * startPos.longitude

            try {
                marker.position = LatLng(lat, lng)
            } catch (_: Exception) {}

            if (t < 1f) {
                handler.postDelayed(this, 16)
            } else {
                onEnd()
            }
        }
    })
}

package com.canopas.catchme.ui.flow.home.map.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.CornerPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.canopas.catchme.data.models.auth.ApiUser
import com.canopas.catchme.data.models.location.ApiLocation
import com.canopas.catchme.ui.theme.AppTheme.colorScheme
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun MapMarker(
    user: ApiUser,
    location: ApiLocation,
    onClick: () -> Unit
) {
    val state = rememberMarkerState(
        position = LatLng(
            location.latitude,
            location.longitude
        )
    )

    var iconState by remember { mutableStateOf<BitmapDescriptor?>(null) }
    val context = LocalContext.current
    val markerColor = colorScheme.primary.toArgb()

    LaunchedEffect(key1 = Unit, block = {
        iconState = loadBitmapDescriptorFromUrl(
            context,
            user.profile_image,
            user.first_name ?: "",
            markerColor
        )
    })

    Marker(
        state = state,
        icon = iconState,
        onClick = {
            onClick()
            false
        }
    )
}

private suspend fun loadBitmapDescriptorFromUrl(
    context: Context,
    imageUrl: String?,
    userName: String,
    markerColor: Int
): BitmapDescriptor {
    return withContext(Dispatchers.IO) {
        if (imageUrl.isNullOrEmpty()) {
            return@withContext BitmapDescriptorFactory.fromBitmap(
                createPlaceHolderBitmap(
                    userName.first().toString(),
                    markerColor
                )
            )
        }
        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .allowHardware(false)
            .size(250)
            .build()

        val result = loader.execute(request)
        val drawable = (result as? SuccessResult)?.drawable
        val bitmap = (drawable as? BitmapDrawable)?.bitmap ?: createPlaceHolderBitmap(
            userName.first().toString(),
            markerColor
        )

        return@withContext BitmapDescriptorFactory.fromBitmap(transformBitmap(bitmap, markerColor))
    }
}

private fun createPlaceHolderBitmap(markerLabel: String, markerColor: Int): Bitmap {
    val size = 250
    val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val r = size / 2f

    val whiteBorder = Paint()
    whiteBorder.isAntiAlias = true
    whiteBorder.color = markerColor
    whiteBorder.alpha = 180
    whiteBorder.strokeWidth = 15f
    canvas.drawCircle(r, r, r, whiteBorder)

    val paint = Paint()
    paint.color = Color.WHITE
    paint.textSize = 80f
    paint.isAntiAlias = true
    paint.textAlign = Paint.Align.CENTER
    paint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD))

    val xPos = canvas.width / 2f
    val yPos = (canvas.height / 2 - (paint.descent() + paint.ascent()) / 2)

    canvas.drawText(markerLabel.uppercase(), xPos, yPos, paint)

    return output
}

private fun transformBitmap(source: Bitmap, markerColor: Int): Bitmap {
    val photoMargin = 30f
    val strokeWidth = 15f
    val triangleMargin = 10f
    val size = 250

    val r = size / 2f
    val output =
        Bitmap.createBitmap(
            (size + triangleMargin).roundToInt(),
            (size + triangleMargin).roundToInt(),
            Bitmap.Config.ARGB_8888
        )
    val canvas = Canvas(output)

    val trianglePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    trianglePaint.strokeWidth = 5f
    trianglePaint.color = markerColor
    trianglePaint.style = Paint.Style.FILL_AND_STROKE
    trianglePaint.isAntiAlias = true
    trianglePaint.pathEffect = CornerPathEffect(15f)

    val triangle = Path()
    triangle.fillType = Path.FillType.EVEN_ODD
    triangle.moveTo(size - strokeWidth, size / 2f)
    triangle.lineTo(size / 2f, size + triangleMargin)
    triangle.lineTo(strokeWidth, size / 2f)
    triangle.close()
    canvas.drawPath(triangle, trianglePaint)

    val paintBorder = Paint()
    paintBorder.isAntiAlias = true
    paintBorder.color = markerColor
    paintBorder.strokeWidth = strokeWidth
    canvas.drawCircle(r, r, r - strokeWidth, paintBorder)

    val whiteBorder = Paint()
    whiteBorder.isAntiAlias = true
    whiteBorder.color = Color.WHITE
    whiteBorder.strokeWidth = strokeWidth
    canvas.drawCircle(r, r, r - 22f, whiteBorder)

    val paint = Paint()
    paint.isAntiAlias = true
    paint.setShader(BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP))
    canvas.drawCircle(r, r, r - photoMargin, paint)

    if (source != output) {
        source.recycle()
    }
    return output
}

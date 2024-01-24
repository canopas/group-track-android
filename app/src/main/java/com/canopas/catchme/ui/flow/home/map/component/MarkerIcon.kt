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
import android.util.DisplayMetrics
import android.util.TypedValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.canopas.catchme.data.models.user.ApiUser
import com.canopas.catchme.ui.theme.AppTheme
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun rememberMarkerIconState(user: ApiUser): BitmapDescriptor? {
    val context = LocalContext.current
    val markerColor = AppTheme.colorScheme.primary.toArgb()
    val markerIconSize = with(LocalDensity.current) { 60.dp.toPx() }
    val markerTextSize = with(LocalDensity.current) { 28.sp.toPx() }

    var iconState by remember { mutableStateOf<BitmapDescriptor?>(null) }

    LaunchedEffect(user) {
        iconState = loadBitmapDescriptorFromUrl(
            context,
            user,
            markerColor, markerIconSize, markerTextSize
        )
    }
    return iconState
}

private suspend fun loadBitmapDescriptorFromUrl(
    context: Context,
    apiUser: ApiUser,
    markerColor: Int,
    markerIconSize: Float,
    markerTextSize: Float
): BitmapDescriptor {
    val imageUrl = apiUser.profile_image
    val userName = apiUser.first_name?.first()?.toString() ?: ""
    return withContext(Dispatchers.IO) {
        if (imageUrl.isNullOrEmpty()) {
            val placeHolder = createPlaceHolderBitmap(
                userName,
                markerColor, markerIconSize.roundToInt(), markerTextSize
            )

            transformBitmap(context, placeHolder, markerColor, markerIconSize)

        } else {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .allowHardware(false)
                .size(markerIconSize.roundToInt())
                .build()

            val result = loader.execute(request)
            val drawable = (result as? SuccessResult)?.drawable
            val bitmap = (drawable as? BitmapDrawable)?.bitmap ?: createPlaceHolderBitmap(
                userName,
                markerColor, markerIconSize.roundToInt(), markerTextSize
            )

            transformBitmap(
                context,
                bitmap,
                markerColor, markerIconSize
            )

        }
    }
}

private fun createPlaceHolderBitmap(
    markerLabel: String,
    markerColor: Int,
    markerIconSize: Int,
    markerTextSize: Float
): Bitmap {
    val output = Bitmap.createBitmap(markerIconSize, markerIconSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val r = markerIconSize / 2f

    val whiteBorder = Paint()
    whiteBorder.isAntiAlias = true
    whiteBorder.color = markerColor
    whiteBorder.alpha = 180
    canvas.drawCircle(r, r, r, whiteBorder)

    val paint = Paint()
    paint.color = Color.WHITE
    paint.textSize = markerTextSize
    paint.isAntiAlias = true
    paint.textAlign = Paint.Align.CENTER
    paint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.NORMAL))

    val xPos = canvas.width / 2f
    val yPos = (canvas.height / 2 - (paint.descent() + paint.ascent()) / 2)

    canvas.drawText(markerLabel.uppercase(), xPos, yPos, paint)

    return output
}

private fun transformBitmap(
    context: Context,
    source: Bitmap,
    markerColor: Int,
    markerIconSize: Float
): BitmapDescriptor {
    val dm = context.resources.displayMetrics

    val photoMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6f, dm)
    val strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3f, dm)
    val whiteStrokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4.5f, dm)

    val triangleMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5f, dm)
    val size = markerIconSize.roundToInt()

    val r = size / 2f
    val output =
        Bitmap.createBitmap(
            (size + triangleMargin).roundToInt(),
            (size + triangleMargin).roundToInt(),
            Bitmap.Config.ARGB_8888
        )
    val canvas = Canvas(output)

    val trianglePaint = Paint(Paint.ANTI_ALIAS_FLAG)
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
    canvas.drawCircle(r, r, r - strokeWidth, paintBorder)

    val whiteBorder = Paint()
    whiteBorder.isAntiAlias = true
    whiteBorder.color = Color.WHITE
    canvas.drawCircle(r, r, r - whiteStrokeWidth, whiteBorder)

    val paint = Paint()
    paint.isAntiAlias = true
    paint.setShader(BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP))
    canvas.drawCircle(r, r, r - photoMargin, paint)

    if (source != output) {
        source.recycle()
    }
    return BitmapDescriptorFactory.fromBitmap(output)
}

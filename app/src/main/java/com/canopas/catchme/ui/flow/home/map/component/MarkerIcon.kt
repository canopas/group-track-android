package com.canopas.catchme.ui.flow.home.map.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.util.TypedValue
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
import com.canopas.catchme.data.models.user.ApiUser
import com.canopas.catchme.ui.theme.AppTheme
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private const val MARKER_ICON_SIZE = 60f

@Composable
fun rememberMarkerIconState(user: ApiUser): BitmapDescriptor? {
    val context = LocalContext.current
    val markerColor = AppTheme.colorScheme.primary.toArgb()

    var iconState by remember { mutableStateOf<BitmapDescriptor?>(null) }

    LaunchedEffect(user) {
        iconState = loadBitmapDescriptorFromUrl(
            context,
            user,
            markerColor
        )
    }
    return iconState
}

private suspend fun loadBitmapDescriptorFromUrl(
    context: Context,
    apiUser: ApiUser,
    markerColor: Int
): BitmapDescriptor {
    val imageUrl = apiUser.profile_image
    val userName = apiUser.first_name?.first()?.toString() ?: ""
    val dm = context.resources.displayMetrics
    val markerIconSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MARKER_ICON_SIZE, dm).roundToInt()

    return withContext(Dispatchers.IO) {
        if (imageUrl.isNullOrEmpty()) {
            val placeHolder = createPlaceHolderBitmap(
                context,
                userName,
                markerColor
            )

            transformBitmap(context, placeHolder)
        } else {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .allowHardware(false)
                .size(markerIconSize)
                .build()

            val result = loader.execute(request)
            val drawable = (result as? SuccessResult)?.drawable
            val bitmap = (drawable as? BitmapDrawable)?.bitmap ?: createPlaceHolderBitmap(
                context,
                userName,
                markerColor
            )

            transformBitmap(
                context,
                bitmap
            )
        }
    }
}

private fun createPlaceHolderBitmap(
    context: Context,
    markerLabel: String,
    markerColor: Int
): Bitmap {
    val dm = context.resources.displayMetrics
    val markerIconSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MARKER_ICON_SIZE, dm).roundToInt()
    val markerTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 28f, dm)
    val corner = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, dm)
    val output = Bitmap.createBitmap(markerIconSize, markerIconSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)

    val bgPaint = Paint()
    bgPaint.isAntiAlias = true
    bgPaint.color = markerColor
    bgPaint.alpha = 180
    canvas.drawRoundRect(
        RectF(0f, 0f, markerIconSize.toFloat(), markerIconSize.toFloat()),
        corner,
        corner,
        bgPaint
    )

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
    source: Bitmap
): BitmapDescriptor {
    val dm = context.resources.displayMetrics

    val size = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MARKER_ICON_SIZE, dm).roundToInt()
    val photoMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, dm)
    val corner = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, dm)
    val pointerCorner = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3f, dm)

    val shadowMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, dm)

    val output =
        Bitmap.createBitmap(
            (size + shadowMargin + shadowMargin).roundToInt(),
            (size + shadowMargin + shadowMargin).roundToInt(),
            Bitmap.Config.ARGB_8888
        )
    val canvas = Canvas(output)

    val corners = floatArrayOf(
        corner,
        corner,
        corner,
        corner,
        corner,
        corner,
        pointerCorner,
        pointerCorner
    )

    val bgPaint = Paint()

    bgPaint.isAntiAlias = true
    bgPaint.color = Color.WHITE

    bgPaint.setShadowLayer(shadowMargin, 0f, 0f, Color.GRAY)

    val rect = RectF(
        shadowMargin,
        shadowMargin,
        size.toFloat() - shadowMargin,
        size.toFloat() - shadowMargin
    )
    val path = Path()
    path.addRoundRect(rect, corners, Path.Direction.CW)
    canvas.drawPath(path, bgPaint)

    val paint = Paint()
    paint.isAntiAlias = true
    paint.setShader(BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP))
    canvas.drawRoundRect(
        RectF(photoMargin, photoMargin, size - photoMargin, size - photoMargin),
        corner,
        corner,
        paint
    )

    if (source != output) {
        source.recycle()
    }
    return BitmapDescriptorFactory.fromBitmap(output)
}

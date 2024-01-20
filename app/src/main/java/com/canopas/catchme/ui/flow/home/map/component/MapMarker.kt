package com.canopas.catchme.ui.flow.home.map.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.canopas.catchme.R
import com.canopas.catchme.data.models.auth.ApiUser
import com.canopas.catchme.data.models.location.ApiLocation
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PinConfig
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

@Composable
fun MapMarker(
//    user: ApiUser,
//    location: ApiLocation,
    onClick: () -> Unit
) {

    var iconState by remember { mutableStateOf<BitmapDescriptor?>(null) }
    val context = LocalContext.current

    LaunchedEffect(key1 = Unit, block = {
        iconState = loadBitmapDescriptorFromUrl(
            context,
            "https://images.dog.ceo/breeds/saluki/n02091831_3400.jpg"
        )

    })

    Marker(
        state = rememberMarkerState(
            position = LatLng(
                21.231809060338193,
                72.83629238605499
            )
        ),
        title = "Marker1",
        snippet = "Marker in Singapore",
        icon = iconState,
        onClick = {
            onClick()
            false
        }
    )
}

suspend fun loadBitmapDescriptorFromUrl(context: Context, imageUrl: String): BitmapDescriptor? {
    return withContext(Dispatchers.IO) {
        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            /*.error(R.drawable.ic_launcher_background)*/
            .allowHardware(false) // Disable hardware bitmaps.
            .build()

        val result = loader.execute(request)
        Timber.e("XXX result $result")
        val drawable = (result as? SuccessResult)?.drawable
        val bitmap = (drawable as? BitmapDrawable)?.bitmap ?: return@withContext null
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 180, 180, false)
        return@withContext BitmapDescriptorFactory.fromBitmap(getRoundedCornerBitmap(resizedBitmap))
    }
}

fun getRoundedCornerBitmap(bitmap: Bitmap): Bitmap {
    val w = bitmap.width
    val h = bitmap.height
    val radius = (h / 2).coerceAtMost(w / 2)
    val output = Bitmap.createBitmap(w + 16, h + 16, Bitmap.Config.ARGB_8888)
    val paint = Paint()
    paint.isAntiAlias = true
    val canvas = Canvas(output)
    canvas.drawARGB(0, 0, 0, 0)
    paint.style = Paint.Style.FILL
    canvas.drawCircle((w / 2 + 8).toFloat(), (h / 2 + 8).toFloat(), radius.toFloat(), paint)

    paint.xfermode =
        PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(bitmap, 4f, 4f, paint)
    paint.xfermode = null
    paint.style = Paint.Style.STROKE
    paint.color = Color.WHITE
    paint.strokeWidth = 10f
    canvas.drawCircle((w / 2 + 8).toFloat(), (h / 2 + 8).toFloat(), radius.toFloat(), paint)
    return output

}
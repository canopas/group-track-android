package com.canopas.yourspace.ui.flow.settings.profile.component

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canopas.yourspace.R
import com.canopas.yourspace.data.models.user.ApiUser
import com.canopas.yourspace.ui.component.motionClickEvent
import com.canopas.yourspace.ui.flow.settings.ProfileImageView
import com.canopas.yourspace.ui.theme.AppTheme
import java.io.File
import java.io.FileOutputStream

@Composable
fun UserProfileView(
    modifier: Modifier,
    profileUrl: String?,
    onProfileChanged: (File?) -> Unit,
    onProfileImageClicked: () -> Unit,
    dismissProfileChooser: () -> Unit,
    showProfileChooser: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val imageCropLauncher = rememberLauncherForActivityResult(contract = CropImageContract()) { result ->
        result.uriContent?.let {
            imageUri = it
            val fileName = "IMG_${System.currentTimeMillis()}.jpg"
            val file = File(context.cacheDir, "images/$fileName")
            val inputStream = context.contentResolver.openInputStream(imageUri!!)

            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            onProfileChanged(file)
        }
    }

    val imagePickerLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                imageCropLauncher.launch(
                    CropImageContractOptions(uri, CropImageOptions())
                )
            }
        }

    val cameraLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicturePreview()) { bitmap ->

            bitmap?.let {
                val fileName = "IMG_${System.currentTimeMillis()}.jpg"
                val file = File(context.cacheDir, "images/$fileName")
                try {
                    val out = FileOutputStream(file)
                    it.compress(Bitmap.CompressFormat.JPEG, 100, out)
                    out.flush()
                    out.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                onProfileChanged(file)
            }
        }

    if (showProfileChooser) {
        ProfileImageChooser(
            onCameraClick = {
                cameraLauncher.launch()
                dismissProfileChooser()
            },
            onGalleryClick = {
                imagePickerLauncher.launch("image/*")
                dismissProfileChooser()
            },
            onRemovePhotoClick = {
                onProfileChanged(null)
                dismissProfileChooser()
            },
            onDismissClick = dismissProfileChooser

        )
    }

    Box(modifier = modifier.size(110.dp), contentAlignment = Alignment.Center) {
        val setProfile =
            profileUrl ?: R.drawable.ic_user_profile_placeholder

        Image(
            painter = rememberAsyncImagePainter(
                ImageRequest.Builder(LocalContext.current).data(data = setProfile).build()
            ),
            modifier = Modifier.fillMaxSize()
                .border(1.dp, AppTheme.colorScheme.textPrimary, CircleShape)
                .background(AppTheme.colorScheme.containerHigh, CircleShape)
                .padding(if (profileUrl == null) 32.dp else 0.dp),
            contentScale = ContentScale.Crop,
            contentDescription = "ProfileImage"
        )

//        Image(
//            painter = painterResource(id = R.drawable.ic_edit_user_profile),
//            contentDescription = null,
//            modifier = Modifier
//                .align(Alignment.BottomEnd)
//                .size(32.dp)
//                .motionClickEvent { onProfileImageClicked() }
//        )
    }
}

@Composable
fun ProfileView(user: ApiUser, onClick: () -> Unit) {
    val userName = user.fullName
    val profileImageUrl = user.profile_image ?: ""

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .motionClickEvent {
                onClick()
            }
            .padding(vertical = 12.dp, horizontal = 16.dp)
    ) {
        ProfileImageView(
            data = profileImageUrl,
            modifier = Modifier
                .size(100.dp)
                .border(1.dp, AppTheme.colorScheme.textDisabled, CircleShape),
            char = user.fullName.firstOrNull().toString()
        )

        Column(
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f)
        ) {
            Text(
                text = userName ?: "",
                style = AppTheme.appTypography.subTitle2,
                color = AppTheme.colorScheme.textPrimary
            )
        }
        Icon(
            Icons.Default.KeyboardArrowRight,
            contentDescription = "Consulting Image",
            modifier = Modifier.padding(horizontal = 8.dp),
            tint = AppTheme.colorScheme.textSecondary
        )
    }
}

@Composable
private fun ProfileImageChooser(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onRemovePhotoClick: () -> Unit,
    onDismissClick: () -> Unit
) {
    Dialog(
        onDismissRequest = { onDismissClick() },
        DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(AppTheme.colorScheme.containerNormalOnSurface),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = stringResource(R.string.edit_profile_image_chooser_camera_text),
                    style = AppTheme.appTypography.subTitle2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                        .motionClickEvent { onCameraClick() }
                )
                Text(
                    text = stringResource(R.string.edit_profile_image_chooser_gallery_text),
                    style = AppTheme.appTypography.subTitle2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                        .motionClickEvent {
                            onGalleryClick()
                        }
                )
                Text(
                    text = stringResource(R.string.edit_profile_image_chooser_remove_photo_text),
                    style = AppTheme.appTypography.subTitle2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                        .motionClickEvent { onRemovePhotoClick() }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

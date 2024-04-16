package com.canopas.yourspace.ui.flow.settings.support

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.canopas.yourspace.R
import com.canopas.yourspace.ui.component.AppAlertDialog
import com.canopas.yourspace.ui.component.AppBanner
import com.canopas.yourspace.ui.component.PrimaryButton
import com.canopas.yourspace.ui.component.motionClickEvent
import com.canopas.yourspace.ui.theme.AppTheme
import java.io.File
import java.io.FileInputStream
import kotlin.reflect.KFunction1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen() {
    val viewModel = hiltViewModel<SupportViewModel>()
    val state by viewModel.state.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTheme.colorScheme.surface),
                title = {
                    Text(
                        text = stringResource(id = R.string.support_title),
                        style = AppTheme.appTypography.header3
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = ""
                        )
                    }
                }
            )
        }
    ) {
        SupportContent(modifier = Modifier.padding(it))
    }

    if (state.error != null) {
        AppBanner(msg = state.error!!) {
            viewModel.resetErrorState()
        }
    }

    if (state.attachmentSizeLimitExceed) {
        AppBanner(msg = stringResource(id = R.string.support_attachment_size_limit_exceed)) {
            viewModel.resetErrorState()
        }
    }

    if (state.requestSent) {
        AppAlertDialog(
            subTitle = stringResource(id = R.string.support_request_sent),
            confirmBtnText = stringResource(id = R.string.common_btn_ok),
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            onConfirmClick = {
                viewModel.dismissSuccessPopup()
            })
    }
}

@Composable
fun SupportContent(modifier: Modifier) {
    val viewModel = hiltViewModel<SupportViewModel>()
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
            .padding(bottom = 20.dp),
    ) {

        val titleInteractionSource = remember { MutableInteractionSource() }
        val descriptionInteractionSource = remember { MutableInteractionSource() }
        val isTitleFocused by titleInteractionSource.collectIsFocusedAsState()
        val isDescriptionFocused by descriptionInteractionSource.collectIsFocusedAsState()
        val focusRequester = remember { FocusRequester() }

        Text(
            text = stringResource(id = R.string.support_text_field_title),
            color = if (isTitleFocused) AppTheme.colorScheme.primary else AppTheme.colorScheme.textDisabled,
            style = AppTheme.appTypography.body2,
            modifier = Modifier.padding(top = 24.dp)
        )

        BasicTextField(
            value = state.title,
            onValueChange = { viewModel.onTitleChanged(it) },
            maxLines = 1,
            interactionSource = titleInteractionSource,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .fillMaxWidth(),
            singleLine = true,
            textStyle = AppTheme.appTypography.subTitle1.copy(color = AppTheme.colorScheme.textPrimary),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
            }),
            cursorBrush = SolidColor(AppTheme.colorScheme.primary)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Divider(
            Modifier
                .fillMaxWidth(),
            color = if (isTitleFocused) AppTheme.colorScheme.primary else AppTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(id = R.string.support_text_field_desc),
            color = if (isDescriptionFocused) AppTheme.colorScheme.primary else AppTheme.colorScheme.textDisabled,
            style = AppTheme.appTypography.body2,
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = state.description,
            onValueChange = { viewModel.onDescriptionChanged(it) },
            interactionSource = descriptionInteractionSource,
            modifier = Modifier
                .aspectRatio(1.92f),
            shape = RoundedCornerShape(12.dp),
            textStyle = AppTheme.appTypography.subTitle2.copy(color = AppTheme.colorScheme.textPrimary),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Default
            ),
            singleLine = false,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppTheme.colorScheme.primary,
                unfocusedBorderColor = AppTheme.colorScheme.outline,
                cursorColor = AppTheme.colorScheme.primary
            )
        )

        Spacer(modifier = Modifier.height(16.dp))
        Attachments(
            state.attachments,
            state.attachmentsFailed,
            state.attachmentUploading,
            onAttachmentPinClicked = { focusManager.clearFocus(true) },
            viewModel::onAttachmentAdded,
            viewModel::onAttachmentRemoved
        )
        Spacer(modifier = Modifier.height(16.dp))

        PrimaryButton(
            label = stringResource(id = R.string.support_btn_submit),
            enabled = state.title.isNotEmpty() && state.description.isNotEmpty() && !state.submitting,
            showLoader = state.submitting,
            onClick = { viewModel.submitSupportRequest() },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
        )

    }
}

@Composable
fun Attachments(
    attachments: List<File>,
    failedAttachments: List<File>,
    uploading: File?,
    onAttachmentPinClicked:() -> Unit,
    onAttachmentAdded: (List<File>) -> Unit,
    onAttachmentRemoved: (File) -> Unit
) {

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri>? ->
        val mediaFiles = mutableListOf<File>()
        uris?.let { selectedUris ->
            for (uri in selectedUris) {
                val fileName = getFileName(context, uri)
                val mediaFile = File(context.filesDir, fileName)
                mediaFile.copyFrom(context, uri)
                mediaFiles.add(mediaFile)
            }
            onAttachmentAdded(mediaFiles.toList())
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .motionClickEvent {
                launcher.launch("*/*")
                onAttachmentPinClicked()
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_attach_file),
            contentDescription = "Attachment media",
            tint = AppTheme.colorScheme.textSecondary,
            modifier = Modifier.rotate(45f)
        )

        Text(
            text = stringResource(R.string.support_label_attachment),
            color = AppTheme.colorScheme.textSecondary,
            style = AppTheme.appTypography.body2,
            modifier = Modifier.padding(start = 4.dp)
        )
    }

    if (attachments.isNotEmpty()) {
        Spacer(modifier = Modifier.padding(top = 8.dp))
    }
    attachments.forEach { attachment ->
        AttachmentItem(
            attachment,
            isUploading = uploading == attachment,
            isFailed = failedAttachments.contains(attachment),
            onAttachmentRemoved
        )

    }

}

@Composable
fun AttachmentItem(
    attachment: File,
    isUploading: Boolean,
    isFailed: Boolean = false,
    onAttachmentRemoved: (File) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .animateContentSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AppTheme.colorScheme.containerLow)
        ) {
            AsyncImage(
                model = attachment,
                contentDescription = "Attachment Images",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.padding(start = 8.dp))
        val elapseFileName = "${attachment.name.take(8)}....${attachment.name.takeLast(8)}"
        Text(
            text = if (attachment.name.length > 25) elapseFileName else attachment.name,
            color = if (isFailed) AppTheme.colorScheme.alertColor else AppTheme.colorScheme.textSecondary,
            style = AppTheme.appTypography.label1,
            modifier = Modifier
                .padding(start = 4.dp)
                .weight(1f)
        )
        if (isUploading) {
            CircularProgressIndicator(
                color = AppTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = 2.dp, top = 2.dp)
                    .size(20.dp)
            )
        } else {
            Icon(
                Icons.Default.Close,
                tint = if (isFailed) AppTheme.colorScheme.alertColor else AppTheme.colorScheme.textSecondary,
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 2.dp, top = 2.dp)
                    .size(20.dp)
                    .motionClickEvent {
                        onAttachmentRemoved(attachment)
                    }
            )
        }
    }

}


private fun getFileName(context: Context, uri: Uri): String {
    var fileName = "media_file"
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index == -1) return@use
            val displayName = it.getString(index)
            if (!displayName.isNullOrBlank()) {
                fileName = displayName
            }
        }
    }
    return fileName
}

fun File.copyFrom(context: Context, imageUri: Uri) {
    FileInputStream(
        context.contentResolver.openFileDescriptor(imageUri, "r")!!.fileDescriptor
    ).use { input ->
        this.outputStream().use { output ->
            input.copyTo(output)
        }
    }
}

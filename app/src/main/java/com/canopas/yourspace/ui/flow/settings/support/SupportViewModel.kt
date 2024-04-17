package com.canopas.yourspace.ui.flow.settings.support

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.yourspace.data.service.support.ApiSupportService
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.ui.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SupportViewModel @Inject constructor(
    private val appNavigator: AppNavigator,
    private val appDispatchers: AppDispatcher,
    private val apiSupportService: ApiSupportService
) : ViewModel() {

    private val _state = MutableStateFlow(SupportState())
    val state = _state.asStateFlow()

    private val attachmentsToUpload = mutableListOf<File>()
    private val uploadedAttachments = mutableMapOf<File, Uri?>()

    fun popBackStack() {
        appNavigator.navigateBack()
    }

    fun onTitleChanged(title: String) {
        _state.value = state.value.copy(title = title)
    }

    fun onDescriptionChanged(description: String) {
        _state.value = state.value.copy(description = description)
    }

    fun onAttachmentAdded(files: List<File>) {
        val attachments = state.value.attachments.toMutableList()
        files.forEach {
            if (it.length() > 5 * 1024 * 1024) {
                val failed = state.value.attachmentsFailed.toMutableList()
                failed.add(it)
                _state.value =
                    state.value.copy(attachmentSizeLimitExceed = true, attachmentsFailed = failed)
                return
            }
        }

        attachments.addAll(files)
        attachmentsToUpload.addAll(files)
        _state.value =
            state.value.copy(attachments = attachments, attachmentSizeLimitExceed = false)
        uploadPendingAttachments()
    }

    private fun uploadPendingAttachments() {
        val fileCopy = attachmentsToUpload.toList()
        attachmentsToUpload.clear()
        fileCopy.forEach { file ->
            viewModelScope.launch(appDispatchers.IO) {
                try {
                    uploadingAttachment(file)
                    val uri = apiSupportService.uploadImage(file)
                    uploadedAttachment(file, uri)
                } catch (e: Exception) {
                    uploadedAttachment(file, null)
                    _state.emit(state.value.copy(error = e.message))
                }
            }
        }
    }

    private fun uploadingAttachment(file: File) {
        val uploading = state.value.attachmentUploading.toMutableList()
        uploading.add(file)
        _state.value = state.value.copy(attachmentUploading = uploading)
    }

    private fun uploadedAttachment(file: File, uri: Uri?) {
        val uploading = state.value.attachmentUploading.toMutableList()
        uploading.remove(file)
        val failed = state.value.attachmentsFailed.toMutableList()
        if (uri == null) {
            failed.add(file)
        }
        uploadedAttachments[file] = uri
        _state.value = state.value.copy(attachmentUploading = uploading, attachmentsFailed = failed)
    }

    fun onAttachmentRemoved(file: File) {
        val attachments = state.value.attachments.toMutableList()
        attachments.remove(file)
        val failed = state.value.attachmentsFailed.toMutableList()
        failed.remove(file)
        uploadedAttachments.remove(file)
        _state.value = state.value.copy(attachments = attachments, attachmentsFailed = failed)
    }

    fun submitSupportRequest() = viewModelScope.launch(appDispatchers.IO) {
        _state.emit(
            state.value.copy(
                submitting = true,
                error = null,
                attachmentSizeLimitExceed = false
            )
        )

        try {
            val attachments = uploadedAttachments.mapNotNull { it.value }
            apiSupportService.submitSupportRequest(
                title = state.value.title,
                description = state.value.description,
                attachments = attachments
            )
            _state.emit(state.value.copy(requestSent = true, submitting = false))
        } catch (e: Exception) {
            Timber.e(e, "Error submitting support request")
            _state.emit(state.value.copy(error = e.message, submitting = false))
        }
    }

    fun resetErrorState() {
        _state.value = state.value.copy(error = null, attachmentSizeLimitExceed = false)
    }

    fun dismissSuccessPopup() {
        _state.value = state.value.copy(requestSent = false)
        appNavigator.navigateBack()
    }
}

data class SupportState(
    val submitting: Boolean = false,
    val title: String = "",
    val description: String = "",
    val requestSent: Boolean = false,
    val attachmentUploading: List<File> = emptyList(),
    val attachments: List<File> = emptyList(),
    val attachmentSizeLimitExceed: Boolean = false,
    val attachmentsFailed: List<File> = emptyList(),
    val error: String? = null
)

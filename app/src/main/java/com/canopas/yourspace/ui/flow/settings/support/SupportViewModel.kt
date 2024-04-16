package com.canopas.yourspace.ui.flow.settings.support

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.service.support.ApiSupportService
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.ui.navigation.AppNavigator
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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
                    _state.emit(state.value.copy(attachmentUploading = file))
                    val uri = apiSupportService.uploadImage(file)
                    _state.emit(state.value.copy(attachmentUploading = null))
                    uploadedAttachments[file] = uri
                } catch (e: Exception) {
                    val failed = state.value.attachmentsFailed.toMutableList()
                    failed.add(file)
                    uploadedAttachments[file] = null
                    _state.emit(
                        state.value.copy(
                            error = e.message,
                            attachmentUploading = null,
                            attachmentsFailed = failed
                        )
                    )
                }
            }
        }
    }

    fun onAttachmentRemoved(file: File) {
        val attachments = state.value.attachments.toMutableList()
        attachments.remove(file)
        val failed = state.value.attachmentsFailed.toMutableList()
        failed.remove(file)
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
    val attachmentUploading: File? = null,
    val attachments: List<File> = emptyList(),
    val attachmentSizeLimitExceed: Boolean = false,
    val attachmentsFailed: List<File> = emptyList(),
    val error: String? = null
)
package com.canopas.yourspace.ui.flow.settings.support

import com.canopas.yourspace.MainCoroutineRule
import com.canopas.yourspace.data.service.support.ApiSupportService
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.domain.utils.NetworkUtils
import com.canopas.yourspace.ui.navigation.AppNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@ExperimentalCoroutinesApi
class SupportViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val appNavigator = mock<AppNavigator>()
    private val testDispatcher = AppDispatcher(IO = UnconfinedTestDispatcher())
    private val apiSupportService = mock<ApiSupportService>()
    private val networkUtils = mock<NetworkUtils>()

    private lateinit var viewModel: SupportViewModel

    @Before
    fun setUp() {
        viewModel = SupportViewModel(
            appNavigator = appNavigator,
            appDispatchers = testDispatcher,
            apiSupportService = apiSupportService,
            networkUtils = networkUtils
        )
    }

    @Test
    fun `should call navigateBack on popBackStack`() {
        viewModel.popBackStack()
        verify(appNavigator).navigateBack()
    }

    @Test
    fun `should update title on onTitleChanged`() {
        val title = "title"
        viewModel.onTitleChanged(title)
        assert(viewModel.state.value.title == title)
    }

    @Test
    fun `should update description on onDescriptionChanged`() {
        val description = "description"
        viewModel.onDescriptionChanged(description)
        assert(viewModel.state.value.description == description)
    }

    @Test
    fun `should update attachments on onAttachmentAdded`() {
        val files = listOf(mock<File>())
        viewModel.onAttachmentAdded(files)
        assert(viewModel.state.value.attachments == files)
    }

    @Test
    fun `should set attachmentSizeLimitExceed to true if file size is greater than 5MB`() {
        val files = listOf(
            mock<File>()
                .apply { whenever(length()).thenReturn(5 * 1024 * 1024 + 1) }
        )
        viewModel.onAttachmentAdded(files)
        assert(viewModel.state.value.attachmentSizeLimitExceed)
    }

    @Test
    fun `should update failed attachments on onAttachmentAdded`() {
        val files = listOf(
            mock<File>()
                .apply { whenever(length()).thenReturn(5 * 1024 * 1024 + 1) }
        )
        viewModel.onAttachmentAdded(files)
        assert(viewModel.state.value.attachmentsFailed == files)
    }

    @Test
    fun `should update attachmentUploading on onAttachmentAdded`() = runTest {
        val files = listOf(mock<File>())
        whenever(apiSupportService.uploadImage(files[0])).doSuspendableAnswer {
            withContext(Dispatchers.IO) {
                delay(1000)
                null
            }
        }
        viewModel.onAttachmentAdded(files)
        assert(viewModel.state.value.attachmentUploading == files)
    }

    @Test
    fun `should update attachmentsFailed on onAttachmentAdded`() = runTest {
        val files = listOf(mock<File>())
        whenever(apiSupportService.uploadImage(files[0])).thenThrow(RuntimeException())
        viewModel.onAttachmentAdded(files)
        assert(viewModel.state.value.attachmentsFailed == files)
    }

    @Test
    fun `should update error on onAttachmentAdded`() = runTest {
        val files = listOf(mock<File>())
        whenever(apiSupportService.uploadImage(files[0])).thenThrow(RuntimeException("error"))
        viewModel.onAttachmentAdded(files)
        assert(viewModel.state.value.error == "error")
    }

    @Test
    fun `should remove file from uploading on uploadedAttachment`() = runTest {
        val files = listOf(mock<File>())
        viewModel.onAttachmentAdded(files)
        whenever(apiSupportService.uploadImage(files[0])).thenReturn(null)
        assert(viewModel.state.value.attachmentUploading.isEmpty())
    }

    @Test
    fun `should add file to failed on uploadedAttachment`() = runTest {
        val files = listOf(mock<File>())
        viewModel.onAttachmentAdded(files)
        whenever(apiSupportService.uploadImage(files[0])).thenReturn(null)
        assert(viewModel.state.value.attachmentsFailed == files)
    }

    @Test
    fun `should remove file from attachment on AttachmentRemoved`() {
        val file = mock<File>()
        val files = listOf(file)
        viewModel.onAttachmentAdded(files)
        viewModel.onAttachmentRemoved(file)
        assert(viewModel.state.value.attachments.isEmpty())
    }

    @Test
    fun `should remove file from failed on AttachmentRemoved`() = runTest {
        val file = mock<File>()
        val files = listOf(file)

        whenever(apiSupportService.uploadImage(files[0])).thenReturn(null)
        viewModel.onAttachmentAdded(files)
        assert(viewModel.state.value.attachmentsFailed.isNotEmpty())

        viewModel.onAttachmentRemoved(file)
        assert(viewModel.state.value.attachmentsFailed.isEmpty())
    }

    @Test
    fun `should set submitting state to true on submitSupportRequest`() = runTest {
        whenever(
            apiSupportService.submitSupportRequest(
                "title",
                "description",
                emptyList()
            )
        ).doSuspendableAnswer {
            withContext(Dispatchers.IO) {
                delay(1000)
                Unit
            }
        }
        viewModel.onTitleChanged("title")
        viewModel.onDescriptionChanged("description")
        viewModel.submitSupportRequest()
        assert(viewModel.state.value.submitting)
    }

    @Test
    fun `should set submitting state to false on submitSupportRequest succeed`() = runTest {
        viewModel.onTitleChanged("title")
        viewModel.onDescriptionChanged("description")
        viewModel.submitSupportRequest()
        assert(!viewModel.state.value.submitting)
    }

    @Test
    fun `should set requestSent state to true on submitSupportRequest succeed`() = runTest {
        viewModel.onTitleChanged("title")
        viewModel.onDescriptionChanged("description")
        viewModel.submitSupportRequest()
        assert(viewModel.state.value.requestSent)
    }

    @Test
    fun `should set error on submitSupportRequest failed`() = runTest {
        whenever(
            apiSupportService.submitSupportRequest(
                "title",
                "description",
                emptyList()
            )
        ).thenThrow(RuntimeException("error"))
        viewModel.onTitleChanged("title")
        viewModel.onDescriptionChanged("description")
        viewModel.submitSupportRequest()
        assert(viewModel.state.value.error == "error")
    }

    @Test
    fun `should reset error state on resetErrorState`() {
        viewModel.resetErrorState()
        assert(viewModel.state.value.error == null)
    }

    @Test
    fun `should dismiss success popup on dismissSuccessPopup`() {
        viewModel.dismissSuccessPopup()
        assert(!viewModel.state.value.requestSent)
    }

    @Test
    fun `should navigate back on dismissSuccessPopup`() {
        viewModel.dismissSuccessPopup()
        verify(appNavigator).navigateBack()
    }
}

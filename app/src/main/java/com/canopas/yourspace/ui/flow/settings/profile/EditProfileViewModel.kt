package com.canopas.yourspace.ui.flow.settings.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.yourspace.data.models.space.ApiSpace
import com.canopas.yourspace.data.models.user.ApiUser
import com.canopas.yourspace.data.models.user.LOGIN_TYPE_APPLE
import com.canopas.yourspace.data.models.user.LOGIN_TYPE_GOOGLE
import com.canopas.yourspace.data.repository.SpaceRepository
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.storage.LocationCache
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.domain.utils.ConnectivityObserver
import com.canopas.yourspace.ui.navigation.AppDestinations
import com.canopas.yourspace.ui.navigation.AppNavigator
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val navigator: AppNavigator,
    private val appDispatcher: AppDispatcher,
    private val spaceRepository: SpaceRepository,
    private val authService: AuthService,
    private val locationCache: LocationCache,
    private val connectivityObserver: ConnectivityObserver
) : ViewModel() {

    private val _state = MutableStateFlow(EditProfileState())
    val state = _state.asStateFlow()

    private var user: ApiUser? = null

    init {
        checkInternetConnection()
        getUser()
        fetchAdminSpaces()
    }

    private fun getUser() = viewModelScope.launch(appDispatcher.IO) {
        _state.emit(_state.value.copy(loading = true))
        user = authService.getUser()
        _state.emit(
            _state.value.copy(
                loading = false,
                firstName = user?.first_name,
                lastName = user?.last_name,
                email = user?.email,
                profileUrl = user?.profile_image,
                enableEmail = user?.auth_type != LOGIN_TYPE_GOOGLE && user?.auth_type != LOGIN_TYPE_APPLE
            )
        )
    }

    fun popBackStack() {
        navigator.navigateBack()
    }

    fun saveUser() = viewModelScope.launch(appDispatcher.IO) {
        if (state.value.saving || user == null) return@launch

        val newUser = user!!.copy(
            first_name = _state.value.firstName?.trim(),
            last_name = _state.value.lastName?.trim(),
            profile_image = _state.value.profileUrl,
            email = _state.value.email?.trim()
        )

        try {
            _state.emit(_state.value.copy(saving = true))
            authService.updateUser(newUser)
            _state.emit(_state.value.copy(saving = false))
            navigator.navigateBack()
        } catch (e: Exception) {
            Timber.e(e, "Failed to save user")
            _state.emit(_state.value.copy(saving = false, error = e))
        }
    }

    private fun onChange() {
        val validFirstName = (_state.value.firstName?.trim()?.length ?: 0) >= 3
        val validEmail = (_state.value.email?.trim()?.length ?: 0) >= 3

        val isValid = validFirstName && (validEmail)

        val changes = state.value.firstName?.trim() != user?.first_name ||
            state.value.lastName?.trim() != user?.last_name ||
            state.value.email?.trim() != user?.email ||
            state.value.profileUrl != user?.profile_image

        _state.value = _state.value.copy(allowSave = isValid && changes)
    }

    fun resetErrorState() {
        _state.value = _state.value.copy(error = null)
    }

    fun showProfileChooser(show: Boolean = true) {
        _state.value = _state.value.copy(showProfileChooser = show)
    }

    fun onProfileImageChanged(profileUri: Uri?) {
        profileUri?.let { uri ->
            uploadProfileImage(uri)
        } ?: run {
            _state.value = _state.value.copy(profileUrl = null)
            onChange()
        }
    }

    private fun uploadProfileImage(uri: Uri) = viewModelScope.launch(appDispatcher.IO) {
        try {
            val storage = FirebaseStorage.getInstance()
            val storageRef = storage.reference
            val fileName = "IMG_${System.currentTimeMillis()}.jpg"
            val imageRef = storageRef.child("profile_images/${user?.id}/$fileName")
            val uploadTask = imageRef.putFile(uri)
            uploadTask.addOnProgressListener {
                _state.value = _state.value.copy(isImageUploadInProgress = true)
            }.addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    _state.value = _state.value.copy(
                        profileUrl = uri.toString(),
                        isImageUploadInProgress = false
                    )
                    onChange()
                }
            }.addOnFailureListener {
                Timber.e(it, "Failed to upload profile image")
                _state.value = _state.value.copy(profileUrl = null, isImageUploadInProgress = false, error = it)
                onChange()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to upload profile image")
            _state.emit(_state.value.copy(isImageUploadInProgress = false, error = e))
        }
    }

    fun onFirstNameChanged(firstName: String) {
        _state.value = _state.value.copy(firstName = firstName)
        onChange()
    }

    fun onLastNameChanged(lastName: String) {
        _state.value = _state.value.copy(lastName = lastName)
        onChange()
    }

    fun onEmailChanged(email: String) {
        _state.value = _state.value.copy(email = email)
        onChange()
    }

    fun showDeleteAccountConfirmation(show: Boolean) {
        _state.value = _state.value.copy(showDeleteAccountConfirmation = show)
    }

    fun showAdminChangeDialog(show: Boolean) {
        _state.value = _state.value.copy(showAdminChangeDialog = show)
    }

    private fun fetchAdminSpaces() = viewModelScope.launch(appDispatcher.IO) {
        try {
            val userId = authService.currentUser?.id ?: return@launch
            val userSpaceList = spaceRepository.getUserSpaces(userId).firstOrNull() ?: return@launch
            val spaceWithMember = mutableListOf<ApiSpace>()

            for (space in userSpaceList) {
                val spaceDetail = spaceRepository.getSpaceInfo(space.id)
                if (spaceDetail!!.members.size > 1 && space.admin_id == userId) {
                    spaceWithMember.add(space)
                }
            }
            _state.emit(_state.value.copy(adminGroupList = spaceWithMember))
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch admin spaces")
            _state.emit(_state.value.copy(error = e))
        }
    }

    fun deleteAccount() = viewModelScope.launch(appDispatcher.IO) {
        try {
            val userId = authService.currentUser?.id ?: return@launch
            val isAdmin = spaceRepository.isUserAdminOfAnySpace(userId)

            if (isAdmin) {
                _state.emit(
                    _state.value.copy(
                        showDeleteAccountConfirmation = false,
                        showAdminChangeDialog = true
                    )
                )
                return@launch
            }

            _state.emit(
                _state.value.copy(
                    deletingAccount = true,
                    showDeleteAccountConfirmation = false
                )
            )
            spaceRepository.deleteUserSpaces()
            authService.deleteAccount()
            locationCache.clear()
            navigator.navigateTo(
                AppDestinations.signIn.path,
                clearStack = true
            )
            _state.emit(_state.value.copy(deletingAccount = false))
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete account")
            _state.emit(_state.value.copy(deletingAccount = false, error = e))
        }
    }

    fun checkInternetConnection() {
        viewModelScope.launch(appDispatcher.IO) {
            connectivityObserver.observe().collectLatest { status ->
                _state.emit(
                    _state.value.copy(
                        connectivityStatus = status
                    )
                )
            }
        }
    }
}

data class EditProfileState(
    val saving: Boolean = false,
    val loading: Boolean = false,
    val allowSave: Boolean = false,
    val enablePhone: Boolean = false,
    val enableEmail: Boolean = false,
    val showProfileChooser: Boolean = false,
    val showDeleteAccountConfirmation: Boolean = false,
    val deletingAccount: Boolean = false,
    val error: Exception? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val profileUrl: String? = null,
    val isImageUploadInProgress: Boolean = false,
    val connectivityStatus: ConnectivityObserver.Status = ConnectivityObserver.Status.Available,
    val showAdminChangeDialog: Boolean = false,
    val adminGroupList: List<ApiSpace> = emptyList()
)

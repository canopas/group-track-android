package com.canopas.yourspace.ui.flow.auth.methods

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.canopas.yourspace.MainCoroutineRule
import com.canopas.yourspace.data.models.user.ApiUser
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.service.auth.FirebaseAuthService
import com.canopas.yourspace.data.storage.UserPreferences
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.domain.utils.ConnectivityObserver
import com.canopas.yourspace.ui.flow.auth.SignInMethodViewModel
import com.canopas.yourspace.ui.navigation.AppNavigator
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class SignInMethodViewModelTest {
    private val testDispatcher = AppDispatcher(IO = UnconfinedTestDispatcher())

    @get:Rule var rule: TestRule = InstantTaskExecutorRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private lateinit var viewModel: SignInMethodViewModel

    private val navigator = mock<AppNavigator>()
    private val firebaseAuth = mock<FirebaseAuthService>()
    private val authService = mock<AuthService>()
    private val userPreferences = mock<UserPreferences>()
    private val connectivityObserver = mock<ConnectivityObserver>()
    private val currentUser = ApiUser(first_name = "first", last_name = "last")

    @Before
    fun setup() {
        whenever(connectivityObserver.observe()).thenReturn(flowOf(ConnectivityObserver.Status.Available))
        whenever(authService.currentUser).thenReturn(currentUser)

        viewModel = SignInMethodViewModel(
            navigator,
            firebaseAuth,
            authService,
            testDispatcher,
            userPreferences,
            connectivityObserver
        )
    }

    @Test
    fun `proceedGoogleSignIn should show loading`() = runTest {
        val account = mock<GoogleSignInAccount>()
        whenever(account.idToken).thenReturn("token")
        whenever(firebaseAuth.signInWithGoogleAuthCredential("token"))
            .doSuspendableAnswer {
                withContext(Dispatchers.IO) { delay(1000) }
                mock()
            }

        viewModel.proceedGoogleSignIn(account)
        assert(viewModel.state.value.showGoogleLoading)
    }

    @Test
    fun `proceedGoogleSignIn should invoke verifiedGoogleLogin`() = runTest {
        val account = mock<GoogleSignInAccount>()
        whenever(account.idToken).thenReturn("token")
        whenever(firebaseAuth.currentUserUid).thenReturn("uid")
        whenever(firebaseAuth.signInWithGoogleAuthCredential("token"))
            .thenReturn("firebaseToken")
        viewModel.proceedGoogleSignIn(account)
        verify(authService).verifiedGoogleLogin("uid", "firebaseToken", account)
    }

    @Test
    fun `proceedGoogleSignIn should navigate to onboard screen`() = runTest {
        val account = mock<GoogleSignInAccount>()
        whenever(account.idToken).thenReturn("token")
        whenever(firebaseAuth.currentUserUid).thenReturn("uid")
        whenever(firebaseAuth.signInWithGoogleAuthCredential("token"))
            .thenReturn("firebaseToken")

        whenever(authService.verifiedGoogleLogin("uid", "firebaseToken", account))
            .thenReturn(true)
        viewModel.proceedGoogleSignIn(account)
        verify(navigator).navigateTo("set-pin", "sign-in", true)
    }

    @Test
    fun `proceedGoogleSignIn should show error`() = runTest {
        val account = mock<GoogleSignInAccount>()
        whenever(account.idToken).thenReturn("token")
        whenever(firebaseAuth.signInWithGoogleAuthCredential("token"))
            .thenThrow(RuntimeException())
        viewModel.proceedGoogleSignIn(account)
        assert(viewModel.state.value.error != null)
    }

    @Test
    fun `proceedGoogleSignIn should set  showGoogleLoading to false`() = runTest {
        val account = mock<GoogleSignInAccount>()
        whenever(account.idToken).thenReturn("token")
        whenever(firebaseAuth.signInWithGoogleAuthCredential("token"))
            .thenThrow(RuntimeException())
        viewModel.proceedGoogleSignIn(account)
        assert(!viewModel.state.value.showGoogleLoading)
    }

    @Test
    fun `resetErrorState should set error to null`() = runTest {
        viewModel.resetErrorState()
        assert(viewModel.state.value.error == null)
    }
}

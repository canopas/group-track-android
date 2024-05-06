package com.canopas.yourspace.ui.flow.geofence.addplace.locate

import android.location.Location
import androidx.lifecycle.SavedStateHandle
import com.canopas.yourspace.MainCoroutineRule
import com.canopas.yourspace.data.repository.SpaceRepository
import com.canopas.yourspace.data.service.location.LocationManager
import com.canopas.yourspace.data.service.place.ApiPlaceService
import com.canopas.yourspace.data.storage.UserPreferences
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.ui.flow.geofence.add.locate.LocateOnMapViewModel
import com.canopas.yourspace.ui.navigation.AppNavigator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class LocateOnMapViewModelTest {

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()
    private val appNavigator = mock<AppNavigator>()
    private val locationManager = mock<LocationManager>()
    private val testDispatcher = AppDispatcher(IO = UnconfinedTestDispatcher())
    private val spaceRepository = mock<SpaceRepository>()
    private val placeService = mock<ApiPlaceService>()
    private val userPreferences = mock<UserPreferences>()
    private val savedStateHandle = mock<SavedStateHandle>()
    private lateinit var viewModel: LocateOnMapViewModel

    @Before
    fun setUp() {
        viewModel = LocateOnMapViewModel(
            savedStateHandle,
            appNavigator,
            locationManager,
            testDispatcher,
            placeService,
            spaceRepository,
            userPreferences
        )
    }

    @Test
    fun `popBackStack should navigate back`() {
        viewModel.popBackStack()
        verify(appNavigator).navigateBack()
    }

    @Test
    fun `init should set defaultLocation`() = runTest {
        val location = mock<Location>()
        whenever(locationManager.getLastLocation()).thenReturn(location)
        viewModel = LocateOnMapViewModel(
            savedStateHandle,
            appNavigator,
            locationManager,
            testDispatcher,
            placeService,
            spaceRepository,
            userPreferences
        )
        assert(viewModel.state.value.defaultLocation == location)
    }
}

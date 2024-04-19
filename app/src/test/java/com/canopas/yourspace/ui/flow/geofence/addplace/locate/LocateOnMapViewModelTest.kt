package com.canopas.yourspace.ui.flow.geofence.addplace.locate

import android.location.Location
import com.canopas.yourspace.MainCoroutineRule
import com.canopas.yourspace.data.service.location.LocationManager
import com.canopas.yourspace.data.utils.AppDispatcher
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
    private lateinit var viewModel: LocateOnMapViewModel

    @Before
    fun setUp() {
        viewModel = LocateOnMapViewModel(appNavigator, locationManager, testDispatcher)
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
        viewModel = LocateOnMapViewModel(appNavigator, locationManager, testDispatcher)
        assert(viewModel.state.value.defaultLocation == location)
    }
}

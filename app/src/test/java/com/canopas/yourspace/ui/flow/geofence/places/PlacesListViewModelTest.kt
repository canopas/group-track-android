package com.canopas.yourspace.ui.flow.geofence.places

import com.canopas.yourspace.MainCoroutineRule
import com.canopas.yourspace.data.models.place.ApiPlace
import com.canopas.yourspace.data.repository.SpaceRepository
import com.canopas.yourspace.data.service.place.ApiPlaceService
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.ui.navigation.AppDestinations
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
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class PlacesListViewModelTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private val appNavigator = mock<AppNavigator>()
    private val appDispatcher = AppDispatcher(IO = UnconfinedTestDispatcher())
    private val spaceRepository = mock<SpaceRepository>()
    private val placeService = mock<ApiPlaceService>()

    private lateinit var viewModel: PlacesListViewModel

    @Before
    fun setup() {
        init()
    }

    private fun init() {
        viewModel = PlacesListViewModel(appNavigator, appDispatcher, spaceRepository, placeService)
    }

    @Test
    fun `should set placesLoading true when getPlaces is called`() = runTest {
        whenever(spaceRepository.currentSpaceId).thenReturn("spaceId")
        whenever(placeService.getPlaces("spaceId")).doSuspendableAnswer {
            withContext(Dispatchers.IO) {
                delay(1000)
                emptyList()
            }
        }

        init()
        assert(viewModel.state.value.placesLoading)
    }

    @Test
    fun `should set placesLoading false when getPlaces is called`() = runTest {
        whenever(spaceRepository.currentSpaceId).thenReturn("spaceId")
        whenever(placeService.getPlaces("spaceId")).thenReturn(emptyList())

        init()
        assert(!viewModel.state.value.placesLoading)
    }

    @Test
    fun `should set places when getPlaces is called`() = runTest {
        val place = mock<ApiPlace>()
        val places = listOf(place)
        whenever(spaceRepository.currentSpaceId).thenReturn("spaceId")
        whenever(placeService.getPlaces("spaceId")).thenReturn(places)

        init()
        assert(viewModel.state.value.places.isNotEmpty())
    }

    @Test
    fun `should set error when getPlaces throws exception`() = runTest {
        whenever(spaceRepository.currentSpaceId).thenReturn("spaceId")
        whenever(placeService.getPlaces("spaceId")).thenThrow(RuntimeException("error"))

        init()
        assert(viewModel.state.value.error == "error")
    }

    @Test
    fun `should navigate back when navigateBack is called`() = runTest {
        viewModel.navigateBack()
        verify(appNavigator).navigateBack()
    }

    @Test
    fun `should navigate to add place when navigateToAddPlace is called`() = runTest {
        viewModel.navigateToAddPlace()
        verify(appNavigator).navigateTo(AppDestinations.LocateOnMap.setArgs("").path)
    }

    @Test
    fun `should show place added popup when showPlaceAddedPopup is called`() = runTest {
        viewModel.showPlaceAddedPopup(12.0, 34.0, "name")
        assert(viewModel.state.value.placeAdded)
        assert(viewModel.state.value.addedPlaceLat == 12.0)
        assert(viewModel.state.value.addedPlaceLng == 34.0)
        assert(viewModel.state.value.addedPlaceName == "name")
    }

    @Test
    fun `should dismiss place added popup when dismissPlaceAddedPopup is called`() = runTest {
        viewModel.dismissPlaceAddedPopup()
        assert(!viewModel.state.value.placeAdded)
        assert(viewModel.state.value.addedPlaceLat == 0.0)
        assert(viewModel.state.value.addedPlaceLng == 0.0)
        assert(viewModel.state.value.addedPlaceName == "")
    }

    @Test
    fun `should navigate to locate on map when selectedSuggestion is called`() = runTest {
        viewModel.selectedSuggestion("name")
        verify(appNavigator).navigateTo(AppDestinations.LocateOnMap.setArgs("name").path)
    }

    @Test
    fun `should show delete place confirmation when showDeletePlaceConfirmation is called`() = runTest {
        val place = mock<ApiPlace>()
        viewModel.showDeletePlaceConfirmation(place)
        assert(viewModel.state.value.placeToDelete == place)
    }

    @Test
    fun `should dismiss delete place confirmation when dismissDeletePlaceConfirmation is called`() = runTest {
        viewModel.dismissDeletePlaceConfirmation()
        assert(viewModel.state.value.placeToDelete == null)
    }

    @Test
    fun `should load places when showPlaceAddedPopup is called`() = runTest {
        clearInvocations(placeService)
        viewModel.showPlaceAddedPopup(12.0, 34.0, "name")
        verify(placeService).getPlaces(spaceRepository.currentSpaceId)
    }

    @Test
    fun `should delete place when onDeletePlace is called`() = runTest {
        val place = mock<ApiPlace>()
        viewModel.showDeletePlaceConfirmation(place)
        viewModel.onDeletePlace()
        verify(placeService).deletePlace(spaceRepository.currentSpaceId, place.id)
    }

    @Test
    fun `should load places when onDeletePlace is called`() = runTest {
        val place = mock<ApiPlace>()
        clearInvocations(placeService)
        viewModel.showDeletePlaceConfirmation(place)
        viewModel.onDeletePlace()
        verify(placeService).deletePlace(spaceRepository.currentSpaceId, place.id)
        verify(placeService).getPlaces(spaceRepository.currentSpaceId)
    }

    @Test
    fun `should not delete place when onDeletePlace is called and placeToDelete is null`() = runTest {
        viewModel.onDeletePlace()
        verify(placeService, never()).deletePlace(spaceRepository.currentSpaceId, "")
    }

    @Test
    fun `should update deletingPlaces when onDeletePlace is called`() = runTest {
        val place = mock<ApiPlace>()
        viewModel.showDeletePlaceConfirmation(place)
        viewModel.onDeletePlace()
        assert(!viewModel.state.value.deletingPlaces.contains(place))
    }

    @Test
    fun `should remove place from deletingPlaces when onDeletePlace is called and exception is thrown`() = runTest {
        val place = mock<ApiPlace>()
        viewModel.showDeletePlaceConfirmation(place)
        whenever(placeService.deletePlace(spaceRepository.currentSpaceId, place.id)).thenThrow(RuntimeException("error"))
        viewModel.onDeletePlace()
        assert(!viewModel.state.value.deletingPlaces.contains(place))
    }

    @Test
    fun `should set error when onDeletePlace is called and exception is thrown`() = runTest {
        val place = mock<ApiPlace>()
        viewModel.showDeletePlaceConfirmation(place)
        whenever(placeService.deletePlace(spaceRepository.currentSpaceId, place.id)).thenThrow(RuntimeException("error"))
        viewModel.onDeletePlace()
        assert(viewModel.state.value.error == "error")
    }
}

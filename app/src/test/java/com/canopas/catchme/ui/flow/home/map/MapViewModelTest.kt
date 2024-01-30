package com.canopas.catchme.ui.flow.home.map

import com.canopas.catchme.MainCoroutineRule
import com.canopas.catchme.data.models.user.ApiUser
import com.canopas.catchme.data.repository.SpaceRepository
import com.canopas.catchme.data.storage.UserPreferences
import com.canopas.catchme.data.utils.AppDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class MapViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val spaceRepository = mock<SpaceRepository>()
    private val userPreferences = mock<UserPreferences>()
    private val testDispatcher = AppDispatcher(IO = UnconfinedTestDispatcher())

    private lateinit var viewModel: MapViewModel

    private fun setUp() {
        viewModel = MapViewModel(
            spaceRepository = spaceRepository,
            userPreferences = userPreferences,
            appDispatcher = testDispatcher
        )
    }

    @Test
    fun `when userPreferences currentSpaceState emits, then listenMemberLocation is called`() =
        runTest {
            val user = ApiUser(id = "user1")
            val flow = flow {
                emit("space1")
            }
            whenever(userPreferences.currentUser).thenReturn(user)
            whenever(userPreferences.currentSpaceState).thenReturn(flow)
            whenever(spaceRepository.getMemberWithLocation()).thenReturn(flowOf(emptyList()))
            setUp()
            verify(spaceRepository).getMemberWithLocation()
        }
}

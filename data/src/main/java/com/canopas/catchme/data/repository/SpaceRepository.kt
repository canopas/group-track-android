package com.canopas.catchme.data.repository

import android.util.Log
import com.canopas.catchme.data.models.space.ApiSpace
import com.canopas.catchme.data.models.space.SPACE_MEMBER_ROLE_MEMBER
import com.canopas.catchme.data.models.space.SpaceInfo
import com.canopas.catchme.data.models.user.UserInfo
import com.canopas.catchme.data.service.auth.AuthService
import com.canopas.catchme.data.service.location.ApiLocationService
import com.canopas.catchme.data.service.space.ApiSpaceService
import com.canopas.catchme.data.service.user.ApiUserService
import com.canopas.catchme.data.storage.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SpaceRepository @Inject constructor(
    private val authService: AuthService,
    private val spaceService: ApiSpaceService,
    private val userService: ApiUserService,
    private val locationService: ApiLocationService,
    private val userPreferences: UserPreferences
) {
    var currentSpaceId: String
        get() = userPreferences.currentSpace ?: ""
        set(value) {
            userPreferences.currentSpace = value
        }

    suspend fun getAllSpaceInfo(): Flow<List<SpaceInfo>> {
        val userId = authService.currentUser?.id ?: ""
        return getUserSpaces(userId).map { spaces ->
            spaces.filterNotNull().map { space ->
                val members =
                    spaceService.getMemberBySpaceId(space.id).firstOrNull()
                        ?.mapNotNull { userService.getUser(it.user_id) }
                        ?.map {
                            val location = locationService.getCurrentLocation(it.id).firstOrNull()
                            UserInfo(it, location)
                        } ?: emptyList()

                SpaceInfo(space, members)
            }
        }
    }

    private suspend fun getCurrentSpace(): ApiSpace? {
        val spaceId = userPreferences.currentSpace

        if (spaceId.isNullOrEmpty()) {
            val userId = authService.currentUser?.id ?: ""
            return getUserSpaces(userId).firstOrNull()?.sortedBy { it?.created_at }?.firstOrNull()
        }
        return getSpace(spaceId)
    }

    private suspend fun getUserSpaces(userId: String) =
        spaceService.getSpaceMemberByUserId(userId).map {
            it.map { spaceMember ->
                val spaceId = spaceMember.space_id
                val space = spaceService.getSpace(spaceId)
                space
            }
        }


    private suspend fun getSpace(spaceId: String): ApiSpace? = spaceService.getSpace(spaceId)

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun getMemberWithLocation(): Flow<List<UserInfo>> {
        val currentSpace = getCurrentSpace() ?: return emptyFlow()
        return spaceService.getMemberBySpaceId(currentSpace.id)
            .map { members ->
                members.mapNotNull { userService.getUser(it.user_id) }
            }.flatMapLatest { users ->
                val flows = users.map { user ->
                    locationService.getCurrentLocation(user.id)
                        .map {
                            UserInfo(user, it)
                        }
                }
                combine(flows) { it.toList() }
            }
    }
}

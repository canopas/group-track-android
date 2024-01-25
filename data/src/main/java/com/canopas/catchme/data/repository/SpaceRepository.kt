package com.canopas.catchme.data.repository

import com.canopas.catchme.data.models.space.ApiSpace
import com.canopas.catchme.data.models.user.UserInfo
import com.canopas.catchme.data.service.auth.AuthService
import com.canopas.catchme.data.service.location.ApiLocationService
import com.canopas.catchme.data.service.space.ApiSpaceService
import com.canopas.catchme.data.service.user.ApiUserService
import com.canopas.catchme.data.storage.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
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

    private val _members = MutableStateFlow<List<UserInfo>>(emptyList())
    val members = _members.asStateFlow()

    private suspend fun getCurrentSpace(): ApiSpace? {
        val spaceId = userPreferences.currentSpace

        if (spaceId.isNullOrEmpty()) {
            val userId = authService.currentUser?.id ?: ""
            return getUserSpaces(userId).sortedBy { it?.created_at }.firstOrNull()
        }
        return getSpace(spaceId)
    }

    private suspend fun getUserSpaces(userId: String): List<ApiSpace?> {
        return spaceService.getSpaceMemberByUserId(userId).map {
            it.map { spaceMember ->
                val spaceId = spaceMember.space_id
                val space = spaceService.getSpace(spaceId)
                space
            }
        }.first()
    }

    private suspend fun getSpace(spaceId: String): ApiSpace? = spaceService.getSpace(spaceId)

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun listenMemberWithLocation() = withContext(Dispatchers.IO) {
        val currentSpace = getCurrentSpace() ?: return@withContext
        spaceService.getMemberBySpaceId(currentSpace.id).map { member ->
            val currentMembers = members.value
            member.filter { it.user_id !in currentMembers.map { it.user.id } }.mapNotNull {
                userService.getUser(it.user_id)
            }
        }.flatMapLatest { users ->
            val flows = users.map { user ->
                locationService.getCurrentLocation(user.id)
                    .map {
                        UserInfo(user, it)
                    }
            }
            combine(flows) { it.toList() }
        }.collectLatest { userInfos ->
            _members.emit(userInfos)
        }
    }
}

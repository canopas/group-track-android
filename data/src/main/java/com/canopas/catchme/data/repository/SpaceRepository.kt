package com.canopas.catchme.data.repository

import com.canopas.catchme.data.models.space.ApiSpace
import com.canopas.catchme.data.models.space.SpaceInfo
import com.canopas.catchme.data.models.user.UserInfo
import com.canopas.catchme.data.service.auth.AuthService
import com.canopas.catchme.data.service.location.ApiLocationService
import com.canopas.catchme.data.service.space.ApiSpaceService
import com.canopas.catchme.data.service.space.SpaceInvitationService
import com.canopas.catchme.data.service.user.ApiUserService
import com.canopas.catchme.data.storage.UserPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import javax.inject.Inject

class SpaceRepository @Inject constructor(
    private val authService: AuthService,
    private val spaceService: ApiSpaceService,
    private val invitationService: SpaceInvitationService,
    private val userService: ApiUserService,
    private val locationService: ApiLocationService,
    private val userPreferences: UserPreferences
) {

    var currentSpaceId: String
        get() = userPreferences.currentSpace ?: ""
        set(value) {
            userPreferences.currentSpace = value
        }

    suspend fun createSpaceAndGetInviteCode(spaceName: String): String {
        val spaceId = spaceService.createSpace(spaceName)
        val generatedCode = invitationService.createInvitation(spaceId)
        currentSpaceId = spaceId
        return generatedCode
    }

    suspend fun joinSpace(spaceId: String) {
        spaceService.joinSpace(spaceId)
        currentSpaceId = spaceId
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun getAllSpaceInfo(): Flow<List<SpaceInfo>> {
        val userId = authService.currentUser?.id ?: ""
        return getUserSpaces(userId).flatMapLatest { spaces ->
            val flows = spaces.filterNotNull().map { space ->
                spaceService.getMemberBySpaceId(space.id)
                    .map { members ->
                        members.mapNotNull { member ->
                            val user = userService.getUser(member.user_id)
                            user?.let { UserInfo(user, isLocationEnable = member.location_enabled) }
                        }
                    }.map { members ->
                        SpaceInfo(space, members)
                    }
            }
            combine(flows) { it.toList() }
        }
    }

    private suspend fun getCurrentSpace(): ApiSpace? {
        val spaceId = currentSpaceId

        if (spaceId.isEmpty()) {
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

    suspend fun getSpace(spaceId: String): ApiSpace? = spaceService.getSpace(spaceId)

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun getMemberWithLocation(): Flow<List<UserInfo>> {
        if (currentSpaceId.isEmpty()) return emptyFlow()
        return spaceService.getMemberBySpaceId(currentSpaceId)
            .map { members ->
                members.filter { it.location_enabled }
                    .mapNotNull { userService.getUser(it.user_id) }
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

    suspend fun getInviteCode(spaceId: String): String? {
        val code = invitationService.getSpaceInviteCode(spaceId)
        if (code?.isExpired == true) {
            return invitationService.regenerateInvitationCode(spaceId)
        }
        return code?.code
    }

    suspend fun enableLocation(spaceId: String, userId: String, locationEnabled: Boolean) {
        spaceService.enableLocation(spaceId, userId, locationEnabled)
    }
}

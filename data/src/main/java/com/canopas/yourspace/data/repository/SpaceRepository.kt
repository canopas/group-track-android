package com.canopas.yourspace.data.repository

import com.canopas.yourspace.data.models.space.ApiSpace
import com.canopas.yourspace.data.models.space.SpaceInfo
import com.canopas.yourspace.data.models.user.UserInfo
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.service.location.ApiLocationService
import com.canopas.yourspace.data.service.place.ApiPlaceService
import com.canopas.yourspace.data.service.space.ApiSpaceService
import com.canopas.yourspace.data.service.space.SpaceInvitationService
import com.canopas.yourspace.data.service.user.ApiUserService
import com.canopas.yourspace.data.storage.UserPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

class SpaceRepository @Inject constructor(
    private val authService: AuthService,
    private val spaceService: ApiSpaceService,
    private val placeService: ApiPlaceService,
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

    suspend fun regenerateInviteCode(spaceId: String): String {
        val inviteCode = invitationService.getSpaceInviteCode(spaceId)
        inviteCode?.let {
            invitationService.regenerateInvitationCode(spaceId)
        }
        return inviteCode?.code ?: ""
    }

    suspend fun joinSpace(spaceId: String) {
        spaceService.joinSpace(spaceId)
        currentSpaceId = spaceId

        val userId = authService.currentUser?.id ?: return
        val memberIds = getMemberBySpaceId(spaceId)?.map { it.user_id }
            ?: emptyList()

        placeService.joinUserToExistingPlaces(userId, spaceId, memberIds)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun getAllSpaceInfo(): Flow<List<SpaceInfo>> {
        val userId = authService.currentUser?.id ?: ""
        return getUserSpaces(userId).flatMapLatest { spaces ->
            val filterSpaces = spaces.filterNotNull()
            if (filterSpaces.isEmpty()) return@flatMapLatest flowOf(emptyList())
            val flows = filterSpaces.map { space ->
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

    suspend fun getCurrentSpaceInfo(): SpaceInfo? {
        val currentSpace = getCurrentSpace() ?: return null
        val members = spaceService.getMemberBySpaceId(currentSpace.id)
            .map { members ->
                members.mapNotNull { member ->
                    val user = userService.getUser(member.user_id)
                    user?.let { UserInfo(user, isLocationEnable = member.location_enabled) }
                }
            }.firstOrNull() ?: emptyList()

        return SpaceInfo(currentSpace, members)
    }

    suspend fun getSpaceInfo(spaceId: String): SpaceInfo? {
        val space = getSpace(spaceId) ?: return null
        val members = spaceService.getMemberBySpaceId(space.id)
            .map { members ->
                members.mapNotNull { member ->
                    val user = userService.getUser(member.user_id)
                    user?.let { UserInfo(user, isLocationEnable = member.location_enabled) }
                }
            }.firstOrNull() ?: emptyList()

        return SpaceInfo(space, members)
    }

    suspend fun getCurrentSpace(): ApiSpace? {
        val spaceId = currentSpaceId

        if (spaceId.isEmpty()) {
            val userId = authService.currentUser?.id
            return userId?.let { getUserSpaces(it).firstOrNull()?.sortedBy { it.created_at }?.firstOrNull() }
        }
        return getSpace(spaceId)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getUserSpaces(userId: String): Flow<List<ApiSpace>> {
        if (userId.isEmpty()) return emptyFlow()
        return spaceService.getSpaceMemberByUserId(userId).flatMapMerge { members ->

            if (members.isEmpty()) return@flatMapMerge flowOf(emptyList())
            val spaceFlows: List<Flow<ApiSpace?>> = members.map { apiSpaceMember ->
                spaceService.getSpaceFlow(apiSpaceMember.space_id)
            }
            combine(spaceFlows) { spaces ->
                spaces.filterNotNull()
            }
        }
    }

    suspend fun getSpace(spaceId: String): ApiSpace? = spaceService.getSpace(spaceId)

    suspend fun getMemberBySpaceId(spaceId: String) =
        spaceService.getMemberBySpaceId(spaceId).firstOrNull()

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun getMemberWithLocation(): Flow<List<UserInfo>> {
        if (currentSpaceId.isEmpty()) return emptyFlow()
        return spaceService.getMemberBySpaceId(currentSpaceId)
            .flatMapLatest { members ->
                if (members.isEmpty()) return@flatMapLatest flowOf(emptyList())
                val flows = members
                    .mapNotNull { member ->
                        val user = userService.getUser(member.user_id)
                        val session = userService.getUserSession(member.user_id)
                        user?.let {
                            locationService.getCurrentLocation(user.id)
                                .map {
                                    UserInfo(
                                        user,
                                        it.firstOrNull(),
                                        member.location_enabled,
                                        session
                                    )
                                }
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

    suspend fun getCurrentSpaceInviteCodeExpireTime(spaceId: String): Long? {
        val code = invitationService.getSpaceInviteCode(spaceId)
        return code?.created_at
    }

    suspend fun enableLocation(spaceId: String, userId: String, locationEnabled: Boolean) {
        spaceService.enableLocation(spaceId, userId, locationEnabled)
    }

    suspend fun deleteUserSpaces() {
        val userId = authService.currentUser?.id
        val allSpace = userId?.let { getUserSpaces(it).firstOrNull() } ?: emptyList()
        val ownSpace = allSpace.filter { it.admin_id == userId }
        val joinedSpace = allSpace.filter { it.admin_id != userId }

        ownSpace.forEach { space ->
            deleteSpace(space.id)
        }

        if (userId != null) {
            joinedSpace.forEach { space ->
                spaceService.removeUserFromSpace(space.id, userId)
            }
        }
    }

    private suspend fun updateUserSpaceId(userId: String, spaceId: String) {
        val user = userService.getUser(userId) ?: return

        val updatedSpaceIds = user.space_ids?.toMutableList()?.apply {
            remove(spaceId)
        } ?: return

        user.copy(space_ids = updatedSpaceIds).let {
            userService.updateUser(it)
        }
    }

    suspend fun deleteSpace(spaceId: String) {
        invitationService.deleteInvitations(spaceId)
        spaceService.deleteSpace(spaceId)
        val userId = authService.currentUser?.id
        currentSpaceId =
            userId?.let { getUserSpaces(it).firstOrNull()?.sortedBy { it.created_at }?.firstOrNull()?.id }
                ?: ""

        if (userId != null) {
            updateUserSpaceId(userId, spaceId)
        }
    }

    suspend fun leaveSpace(spaceId: String) {
        val userId = authService.currentUser?.id
        if (userId != null) {
            spaceService.removeUserFromSpace(spaceId, userId)
        }
        if (userId != null) {
            updateUserSpaceId(userId, spaceId)
        }
    }

    suspend fun updateSpace(newSpace: ApiSpace) {
        spaceService.updateSpace(newSpace)
    }

    suspend fun isUserAdminOfAnySpace(userId: String): Boolean {
        val spaces = getUserSpaces(userId).firstOrNull() ?: return false
        return spaces.any { space ->
            space.admin_id == userId &&
                (spaceService.getMemberBySpaceId(space.id).firstOrNull()?.size ?: 1) > 1
        }
    }

    suspend fun removeUserFromSpace(spaceId: String, userId: String) {
        spaceService.removeUserFromSpace(spaceId, userId)
        val user = userService.getUser(userId)
        val updatedSpaceIds = user?.space_ids?.toMutableList()?.apply {
            remove(spaceId)
        } ?: return

        user.copy(space_ids = updatedSpaceIds).let {
            userService.updateUser(it)
        }
    }

    suspend fun changeSpaceAdmin(spaceId: String, newAdminId: String) {
        try {
            spaceService.changeAdmin(spaceId, newAdminId)
        } catch (e: Exception) {
            Timber.e(e, "Failed to change space admin")
            throw e
        }
    }

    suspend fun generateAndDistributeSenderKeysForExistingSpaces(spaceIds: List<String>) {
        try {
            spaceService.generateAndDistributeSenderKeysForExistingSpaces(spaceIds)
        } catch (e: Exception) {
            Timber.e(e, "Failed to generate and distribute sender keys")
        }
    }
}

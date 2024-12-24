package com.canopas.yourspace.data.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.canopas.yourspace.data.models.user.ApiUser
import com.canopas.yourspace.data.models.user.ApiUserSession
import com.canopas.yourspace.data.storage.UserPreferences.PreferencesKey.KEY_USER_CURRENT_SPACE
import com.canopas.yourspace.data.storage.UserPreferences.PreferencesKey.KEY_USER_JSON
import com.canopas.yourspace.data.storage.UserPreferences.PreferencesKey.KEY_USER_SESSION_JSON
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

const val PREF_USER_PREFERENCES = "your_space_user_preferences"

@Singleton
class UserPreferences @Inject constructor(
    @Named(PREF_USER_PREFERENCES) private val preferencesDataStore: DataStore<Preferences>
) {

    private val userJsonAdapter: JsonAdapter<ApiUser> =
        Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build().adapter(ApiUser::class.java)
    private val userSessionJsonAdapter: JsonAdapter<ApiUserSession> =
        Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            .adapter(ApiUserSession::class.java)

    object PreferencesKey {
        val INTRO_SHOWN = booleanPreferencesKey("intro_shown")
        val ONBOARD_SHOWN = booleanPreferencesKey("onboard_shown")

        val KEY_USER_JSON = stringPreferencesKey("current_user")
        val KEY_USER_SESSION_JSON = stringPreferencesKey("user_session")
        val KEY_USER_CURRENT_SPACE = stringPreferencesKey("user_current_space")

        val IS_FCM_REGISTERED = booleanPreferencesKey("is_fcm_registered")
        val LAST_BATTERY_DIALOG_DATE = stringPreferencesKey("last_battery_dialog_date")

        val KEY_USER_MAP_STYLE = stringPreferencesKey("user_map_style")
        val KEY_BATTERY_OPTIMIZATION = booleanPreferencesKey("battery_optimization_enabled")
    }

    suspend fun isIntroShown(): Boolean {
        return preferencesDataStore.data.first()[PreferencesKey.INTRO_SHOWN] ?: false
    }

    suspend fun setIntroShown(value: Boolean) {
        preferencesDataStore.edit { preferences ->
            preferences[PreferencesKey.INTRO_SHOWN] = value
        }
    }

    suspend fun getLastBatteryDialogDate(): String? {
        return preferencesDataStore.data.first()[PreferencesKey.LAST_BATTERY_DIALOG_DATE]
    }

    suspend fun setLastBatteryDialogDate(value: String) {
        preferencesDataStore.edit { preferences ->
            preferences[PreferencesKey.LAST_BATTERY_DIALOG_DATE] = value
        }
    }

    suspend fun isOnboardShown(): Boolean {
        return preferencesDataStore.data.first()[PreferencesKey.ONBOARD_SHOWN] ?: false
    }

    fun setOnboardShown(value: Boolean) = runBlocking {
        preferencesDataStore.edit { preferences ->
            preferences[PreferencesKey.ONBOARD_SHOWN] = value
        }
    }

    var currentUserSessionState = preferencesDataStore.data.map { _ ->
        preferencesDataStore.data.first()[KEY_USER_SESSION_JSON]?.let {
            return@let userSessionJsonAdapter.fromJson(it)
        }
    }

    var currentUser: ApiUser?
        get() = runBlocking {
            preferencesDataStore.data.first()[KEY_USER_JSON]?.let {
                val user = userJsonAdapter.fromJson(it)
                return@let user
            }
        }
        set(newUser) = runBlocking {
            if (newUser == null) {
                preferencesDataStore.edit {
                    it.remove(KEY_USER_JSON)
                }
            } else {
                preferencesDataStore.edit { preferences ->
                    preferences[KEY_USER_JSON] = userJsonAdapter.toJson(newUser)
                }
            }
        }

    var currentUserSession: ApiUserSession?
        get() = runBlocking {
            preferencesDataStore.data.first()[KEY_USER_SESSION_JSON]?.let {
                return@let userSessionJsonAdapter.fromJson(it)
            }
        }
        set(newSession) = runBlocking {
            if (newSession == null) {
                preferencesDataStore.edit {
                    it.remove(KEY_USER_SESSION_JSON)
                }
            } else {
                preferencesDataStore.edit { preferences ->
                    preferences[KEY_USER_SESSION_JSON] = userSessionJsonAdapter.toJson(newSession)
                }
            }
        }

    var currentSpaceState = preferencesDataStore.data.map { preferences ->
        preferences[KEY_USER_CURRENT_SPACE] ?: ""
    }

    var currentSpace: String?
        get() = runBlocking {
            preferencesDataStore.data.first()[KEY_USER_CURRENT_SPACE]
        }
        set(newSpace) = runBlocking {
            if (newSpace == null) {
                preferencesDataStore.edit {
                    it.remove(KEY_USER_CURRENT_SPACE)
                }
            } else {
                preferencesDataStore.edit { preferences ->
                    preferences[KEY_USER_CURRENT_SPACE] = newSpace
                }
            }
        }

    var isFCMRegistered: Boolean
        get() = runBlocking {
            preferencesDataStore.data.first()[PreferencesKey.IS_FCM_REGISTERED] ?: false
        }
        set(registered) = runBlocking {
            preferencesDataStore.edit { preferences ->
                preferences[PreferencesKey.IS_FCM_REGISTERED] = registered
            }
        }

    var currentMapStyle: String?
        get() = runBlocking {
            preferencesDataStore.data.first()[PreferencesKey.KEY_USER_MAP_STYLE]
        }
        set(newStyle) = runBlocking {
            if (newStyle == null) {
                preferencesDataStore.edit {
                    it.remove(PreferencesKey.KEY_USER_MAP_STYLE)
                }
            } else {
                preferencesDataStore.edit { preferences ->
                    preferences[PreferencesKey.KEY_USER_MAP_STYLE] = newStyle
                }
            }
        }

    var isBatteryOptimizationEnabled: Boolean
        get() = runBlocking {
            preferencesDataStore.data.first()[PreferencesKey.KEY_BATTERY_OPTIMIZATION] ?: false
        }
        set(value) = runBlocking {
            preferencesDataStore.edit { preferences ->
                preferences[PreferencesKey.KEY_BATTERY_OPTIMIZATION] = value
            }
        }
}

package com.canopas.catchme.data.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.canopas.catchme.data.models.auth.ApiUser
import com.canopas.catchme.data.models.auth.ApiUserSession
import com.canopas.catchme.data.storage.UserPreferences.PreferencesKey.KEY_USER_JSON
import com.canopas.catchme.data.storage.UserPreferences.PreferencesKey.KEY_USER_SESSION_JSON
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

const val PREF_USER_PREFERENCES = "catch_me_user_preferences"
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
        val KEY_USER_JSON = stringPreferencesKey("current_user")
        val KEY_USER_SESSION_JSON = stringPreferencesKey("user_session")

    }

    suspend fun isIntroShown(): Boolean {
        return preferencesDataStore.data.first()[PreferencesKey.INTRO_SHOWN] ?: false
    }

    suspend fun setIntroShown(value: Boolean) {
        preferencesDataStore.edit { preferences ->
            preferences[PreferencesKey.INTRO_SHOWN] = value
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
}
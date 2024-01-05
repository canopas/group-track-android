package com.canopas.catchme.data.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

const val  PREF_USER_PREFERENCES = "catch_mew_user_preferences"
@Singleton
class UserPreferences @Inject constructor(
    @Named(PREF_USER_PREFERENCES) private val preferencesDataStore: DataStore<Preferences>
){

    object PreferencesKey {
        val INTRO_SHOWN = booleanPreferencesKey("intro_shown")
    }

    suspend fun isIntroShown(): Boolean {
        return preferencesDataStore.data.first()[PreferencesKey.INTRO_SHOWN] ?: false
    }

   suspend fun setIntroShown(value: Boolean) {
        preferencesDataStore.edit { preferences ->
            preferences[PreferencesKey.INTRO_SHOWN] = value
        }
    }

}
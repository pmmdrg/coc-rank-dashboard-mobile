package com.cocrank.dashboard.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class UserPreferences(private val context: Context) {

    companion object {
        val KEY_PLAYER_TAG = stringPreferencesKey("player_tag")
        val KEY_API_TOKEN = stringPreferencesKey("api_token")
        val KEY_PROXY_URL = stringPreferencesKey("proxy_url")
        val KEY_IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val KEY_CLAN_DATA_JSON = stringPreferencesKey("clan_data_json")
        val KEY_LEAGUE_HISTORY_JSON = stringPreferencesKey("league_history_json")
        val KEY_PREVIOUS_SEASON_JSON = stringPreferencesKey("previous_season_json")
        val KEY_ACTIVE_SEASON_INDEX = androidx.datastore.preferences.core.intPreferencesKey("active_season_index")
    }

    val playerTag: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_PLAYER_TAG] ?: ""
    }

    val apiToken: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_API_TOKEN] ?: ""
    }

    val proxyUrl: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_PROXY_URL] ?: ""
    }

    val isDarkTheme: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_IS_DARK_THEME] ?: true // Mặc định là Dark Theme đậm chất gaming
    }

    val clanDataJson: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_CLAN_DATA_JSON] ?: ""
    }

    val leagueHistoryJson: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_LEAGUE_HISTORY_JSON] ?: ""
    }

    val previousSeasonJson: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_PREVIOUS_SEASON_JSON] ?: ""
    }

    val activeSeasonIndex: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_ACTIVE_SEASON_INDEX] ?: 0
    }

    suspend fun savePlayerTag(tag: String) {
        val formatted = if (tag.isNotBlank() && !tag.startsWith("#")) "#$tag" else tag
        context.dataStore.edit { preferences ->
            preferences[KEY_PLAYER_TAG] = formatted.trim().uppercase()
        }
    }

    suspend fun saveApiToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_API_TOKEN] = token.trim()
        }
    }

    suspend fun saveProxyUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PROXY_URL] = url.trim()
        }
    }

    suspend fun setDarkTheme(isDark: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_IS_DARK_THEME] = isDark
        }
    }

    suspend fun saveClanDataJson(json: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CLAN_DATA_JSON] = json
        }
    }

    suspend fun saveLeagueHistoryJson(json: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LEAGUE_HISTORY_JSON] = json
        }
    }

    suspend fun savePreviousSeasonJson(json: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PREVIOUS_SEASON_JSON] = json
        }
    }

    suspend fun saveActiveSeasonIndex(index: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ACTIVE_SEASON_INDEX] = index
        }
    }
}

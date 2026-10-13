package com.cocrank.dashboard.ui.screens.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cocrank.dashboard.data.local.AppDatabase
import com.cocrank.dashboard.data.local.UserPreferences
import com.cocrank.dashboard.data.repository.RankRepository
import com.cocrank.dashboard.domain.model.Player
import com.cocrank.dashboard.domain.model.RankChangesSnapshot
import com.cocrank.dashboard.domain.model.RankingStats
import com.cocrank.dashboard.domain.model.RatingCategory
import com.cocrank.dashboard.domain.model.Season
import com.cocrank.dashboard.domain.usecase.RankingCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val season: Season? = null,
    val stats: RankingStats? = null,
    val rankChanges: RankChangesSnapshot? = null,
    val previousPlayers: List<Player> = emptyList(),
    val filteredPlayers: List<Player> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: RatingCategory? = null,
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
    val errorMessage: String? = null,
    val showConfigDialog: Boolean = false,
    val savedPlayerTag: String = "",
    val savedApiToken: String = "",
    val savedProxyUrl: String = "",
    val isDarkTheme: Boolean = true
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val userPreferences = UserPreferences(application)
    val repository = RankRepository(database, userPreferences)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        // Observe preferences
        viewModelScope.launch {
            userPreferences.isDarkTheme.collectLatest { isDark ->
                _uiState.update { it.copy(isDarkTheme = isDark) }
            }
        }
        viewModelScope.launch {
            userPreferences.playerTag.collectLatest { tag ->
                _uiState.update { it.copy(savedPlayerTag = tag) }
            }
        }
        viewModelScope.launch {
            userPreferences.apiToken.collectLatest { token ->
                _uiState.update { it.copy(savedApiToken = token) }
            }
        }
        viewModelScope.launch {
            userPreferences.proxyUrl.collectLatest { proxy ->
                _uiState.update { it.copy(savedProxyUrl = proxy) }
            }
        }

        // Observe latest season from local DB
        viewModelScope.launch {
            repository.latestSeason.collectLatest { season ->
                if (season != null) {
                    val normalized = RankingCalculator.normalizeSeason(season)
                    val stats = RankingCalculator.getRankingStats(normalized)
                    _uiState.update { state ->
                        val changes = if (state.previousPlayers.isNotEmpty()) {
                            RankingCalculator.calculateRankChanges(
                                currentSeason = normalized,
                                previousPlayers = state.previousPlayers,
                                myPlayerTag = state.savedPlayerTag
                            )
                        } else state.rankChanges

                        state.copy(
                            season = normalized,
                            stats = stats,
                            rankChanges = changes,
                            filteredPlayers = filterPlayers(normalized.players, state.searchQuery, state.selectedCategory)
                        )
                    }
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            val all = state.season?.players ?: emptyList()
            state.copy(
                searchQuery = query,
                filteredPlayers = filterPlayers(all, query, state.selectedCategory)
            )
        }
    }

    fun onCategorySelected(category: RatingCategory?) {
        _uiState.update { state ->
            val all = state.season?.players ?: emptyList()
            state.copy(
                selectedCategory = category,
                filteredPlayers = filterPlayers(all, state.searchQuery, category)
            )
        }
    }

    private fun filterPlayers(
        players: List<Player>,
        query: String,
        category: RatingCategory?
    ): List<Player> {
        return players.filter { player ->
            val matchesQuery = query.isBlank() ||
                    player.name.contains(query, ignoreCase = true) ||
                    (player.playerTag?.contains(query, ignoreCase = true) == true)
            val matchesCategory = category == null || player.rating == category
            matchesQuery && matchesCategory
        }
    }

    fun syncData() {
        val currentSeason = _uiState.value.season
        if (currentSeason != null) {
            _uiState.update { it.copy(previousPlayers = currentSeason.players) }
        }

        viewModelScope.launch {
            val tag = userPreferences.playerTag.first()
            if (tag.isBlank()) {
                _uiState.update { it.copy(showConfigDialog = true) }
                return@launch
            }

            _uiState.update { it.copy(isSyncing = true, syncMessage = "Đang đồng bộ CoC API...", errorMessage = null) }

            val result = repository.syncWithApi(
                overrideTag = tag,
                onProgress = { msg -> _uiState.update { it.copy(syncMessage = msg) } }
            )

            result.onSuccess { newSeason ->
                val prev = _uiState.value.previousPlayers
                val changes = RankingCalculator.calculateRankChanges(
                    currentSeason = newSeason,
                    previousPlayers = prev,
                    myPlayerTag = tag
                )
                _uiState.update {
                    it.copy(
                        season = newSeason,
                        stats = RankingCalculator.getRankingStats(newSeason),
                        rankChanges = changes,
                        filteredPlayers = filterPlayers(newSeason.players, it.searchQuery, it.selectedCategory),
                        isSyncing = false,
                        syncMessage = null,
                        errorMessage = null
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        syncMessage = null,
                        errorMessage = err.message ?: "Đồng bộ thất bại"
                    )
                }
            }
        }
    }

    fun savePlayerTag(tag: String) {
        viewModelScope.launch {
            userPreferences.savePlayerTag(tag)
            _uiState.update { it.copy(showConfigDialog = false) }
            syncData()
        }
    }

    fun saveConfig(tag: String, token: String, proxy: String) {
        viewModelScope.launch {
            userPreferences.savePlayerTag(tag)
            userPreferences.saveApiToken(token)
            userPreferences.saveProxyUrl(proxy)
            _uiState.update { it.copy(showConfigDialog = false) }
            syncData()
        }
    }

    fun openConfigDialog() {
        _uiState.update { it.copy(showConfigDialog = true) }
    }

    fun dismissConfigDialog() {
        _uiState.update { it.copy(showConfigDialog = false) }
    }

    fun setDarkTheme(isDark: Boolean) {
        viewModelScope.launch {
            userPreferences.setDarkTheme(isDark)
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

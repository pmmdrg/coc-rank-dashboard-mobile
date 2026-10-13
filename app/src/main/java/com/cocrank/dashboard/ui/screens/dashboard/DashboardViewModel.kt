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

import com.cocrank.dashboard.domain.model.ClanData
import com.cocrank.dashboard.domain.model.LeagueHistoryItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class DashboardUiState(
    val season: Season? = null,
    val currentSeason: Season? = null,
    val previousSeason: Season? = null,
    val allSeasons: List<Season> = emptyList(),
    val activeSeasonIndex: Int = 0,
    val stats: RankingStats? = null,
    val clanData: ClanData? = null,
    val leagueHistory: List<LeagueHistoryItem> = emptyList(),
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
    private val gson = Gson()

    private val _uiState = MutableStateFlow(
        DashboardUiState(
            clanData = repository.getSampleClanData(),
            leagueHistory = repository.getSampleLeagueHistory()
        )
    )
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
                _uiState.update { state ->
                    val curSeason = state.season
                    if (curSeason != null && tag.isNotBlank()) {
                        val seasonWithTag = curSeason.copy(myPlayerId = tag)
                        val normalized = RankingCalculator.normalizeSeason(seasonWithTag)
                        val stats = RankingCalculator.getRankingStats(normalized, tag)
                        state.copy(
                            savedPlayerTag = tag,
                            season = normalized,
                            stats = stats,
                            filteredPlayers = filterPlayers(normalized.players, state.searchQuery, state.selectedCategory)
                        )
                    } else {
                        state.copy(savedPlayerTag = tag)
                    }
                }
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

        // Observe cached ClanData
        viewModelScope.launch {
            userPreferences.clanDataJson.collectLatest { json ->
                if (json.isNotBlank()) {
                    try {
                        val cachedClan = gson.fromJson(json, ClanData::class.java)
                        _uiState.update { it.copy(clanData = cachedClan) }
                    } catch (_: Exception) {}
                }
            }
        }

        // Observe cached LeagueHistory
        viewModelScope.launch {
            userPreferences.leagueHistoryJson.collectLatest { json ->
                if (json.isNotBlank()) {
                    try {
                        val listType = object : TypeToken<List<LeagueHistoryItem>>() {}.type
                        val cachedHistory: List<LeagueHistoryItem> = gson.fromJson(json, listType)
                        _uiState.update { it.copy(leagueHistory = cachedHistory) }
                    } catch (_: Exception) {}
                }
            }
        }

        // Observe cached PreviousSeason
        viewModelScope.launch {
            userPreferences.previousSeasonJson.collectLatest { json ->
                if (json.isNotBlank()) {
                    try {
                        val cachedPrev = gson.fromJson(json, Season::class.java)
                        _uiState.update { state ->
                            val cur = state.season ?: state.currentSeason
                            val all = listOfNotNull(cur, cachedPrev)
                            state.copy(previousSeason = cachedPrev, allSeasons = all)
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        // Observe activeSeasonIndex
        viewModelScope.launch {
            userPreferences.activeSeasonIndex.collectLatest { idx ->
                _uiState.update { it.copy(activeSeasonIndex = idx) }
            }
        }

        // Observe latest season from local DB
        viewModelScope.launch {
            repository.latestSeason.collectLatest { season ->
                if (season != null) {
                    val activeTag = _uiState.value.savedPlayerTag.ifBlank { season.myPlayerId }
                    val seasonWithTag = if (activeTag.isNotBlank()) season.copy(myPlayerId = activeTag) else season
                    val normalized = RankingCalculator.normalizeSeason(seasonWithTag)
                    val stats = RankingCalculator.getRankingStats(normalized, activeTag)
                    _uiState.update { state ->
                        val changes = if (state.previousPlayers.isNotEmpty()) {
                            RankingCalculator.calculateRankChanges(
                                currentSeason = normalized,
                                previousPlayers = state.previousPlayers,
                                myPlayerTag = activeTag
                            )
                        } else state.rankChanges

                        val all = listOfNotNull(normalized, state.previousSeason)
                        val displayed = if (state.activeSeasonIndex == 1 && state.previousSeason != null) state.previousSeason else normalized

                        state.copy(
                            season = displayed,
                            currentSeason = normalized,
                            allSeasons = all,
                            stats = RankingCalculator.getRankingStats(displayed, activeTag),
                            rankChanges = changes,
                            filteredPlayers = filterPlayers(displayed.players, state.searchQuery, state.selectedCategory)
                        )
                    }
                }
            }
        }

        // Tự động gọi API Supercell khi khởi động ứng dụng nếu đã lưu playerTag
        viewModelScope.launch {
            val tag = userPreferences.playerTag.first()
            if (tag.isNotBlank()) {
                syncData()
            }
        }
    }

    /**
     * Chọn mùa giải hiển thị: 0 (Mùa hiện tại), 1 (Mùa trước đó)
     */
    fun selectSeasonIndex(index: Int) {
        val all = _uiState.value.allSeasons
        if (index in all.indices) {
            val target = all[index]
            val tag = _uiState.value.savedPlayerTag.ifBlank { target.myPlayerId }
            val normalized = RankingCalculator.normalizeSeason(target)
            val stats = RankingCalculator.getRankingStats(normalized, tag)
            _uiState.update { state ->
                state.copy(
                    activeSeasonIndex = index,
                    season = normalized,
                    stats = stats,
                    filteredPlayers = filterPlayers(normalized.players, state.searchQuery, state.selectedCategory)
                )
            }
            viewModelScope.launch {
                userPreferences.saveActiveSeasonIndex(index)
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

            _uiState.update { it.copy(isSyncing = true, syncMessage = "Đang đồng bộ Supercell API...", errorMessage = null) }

            val result = repository.syncAllWithApi(
                overrideTag = tag,
                onProgress = { msg -> _uiState.update { it.copy(syncMessage = msg) } }
            )

            result.onSuccess { syncRes ->
                val prev = _uiState.value.previousPlayers
                val activeIndex = _uiState.value.activeSeasonIndex
                val displayedSeason = if (activeIndex in syncRes.allSeasons.indices) syncRes.allSeasons[activeIndex] else syncRes.currentSeason
                val changes = RankingCalculator.calculateRankChanges(
                    currentSeason = displayedSeason,
                    previousPlayers = prev,
                    myPlayerTag = tag
                )

                _uiState.update {
                    it.copy(
                        season = displayedSeason,
                        currentSeason = syncRes.currentSeason,
                        previousSeason = syncRes.previousSeason,
                        allSeasons = syncRes.allSeasons,
                        stats = RankingCalculator.getRankingStats(displayedSeason, tag),
                        clanData = syncRes.clanData ?: it.clanData,
                        leagueHistory = if (syncRes.leagueHistory.isNotEmpty()) syncRes.leagueHistory else it.leagueHistory,
                        rankChanges = changes,
                        filteredPlayers = filterPlayers(displayedSeason.players, it.searchQuery, it.selectedCategory),
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

    fun syncClan() {
        val clanTag = _uiState.value.clanData?.tag
            ?: _uiState.value.stats?.myPlayer?.clanTag
            ?: _uiState.value.season?.players?.firstOrNull { it.isMe }?.clanTag
        if (clanTag.isNullOrBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncMessage = "Đang tải dữ liệu Clan...") }
            val res = repository.getClanData(clanTag)
            res.onSuccess { clan ->
                _uiState.update {
                    it.copy(clanData = clan, isSyncing = false, syncMessage = null)
                }
                userPreferences.saveClanDataJson(gson.toJson(clan))
            }.onFailure { err ->
                _uiState.update {
                    it.copy(isSyncing = false, syncMessage = null, errorMessage = err.message ?: "Không thể tải Clan")
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

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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

                        val currentWithTag = state.currentSeason?.let {
                            RankingCalculator.normalizeSeason(it.copy(myPlayerId = tag))
                        }
                        val previousWithTag = state.previousSeason?.let {
                            RankingCalculator.normalizeSeason(it.copy(myPlayerId = tag))
                        }
                        val all = listOfNotNull(currentWithTag, previousWithTag).ifEmpty { state.allSeasons }

                        state.copy(
                            savedPlayerTag = tag,
                            season = normalized,
                            currentSeason = currentWithTag ?: state.currentSeason,
                            previousSeason = previousWithTag ?: state.previousSeason,
                            allSeasons = all,
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
                            val current = state.currentSeason ?: (if (state.activeSeasonIndex == 0) state.season else null)
                            val all = listOfNotNull(current, cachedPrev)
                            val displayed = if (state.activeSeasonIndex == 1) cachedPrev else (current ?: state.season ?: cachedPrev)
                            val tag = state.savedPlayerTag.ifBlank { displayed.myPlayerId }
                            val displayedWithTag = if (tag.isNotBlank()) displayed.copy(myPlayerId = tag) else displayed
                            val normalized = RankingCalculator.normalizeSeason(displayedWithTag)
                            val stats = RankingCalculator.getRankingStats(normalized, tag)

                            state.copy(
                                previousSeason = cachedPrev,
                                currentSeason = current ?: state.currentSeason,
                                allSeasons = all,
                                season = normalized,
                                stats = stats,
                                filteredPlayers = filterPlayers(normalized.players, state.searchQuery, state.selectedCategory)
                            )
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        // Observe activeSeasonIndex
        viewModelScope.launch {
            userPreferences.activeSeasonIndex.collectLatest { idx ->
                _uiState.update { state ->
                    if (state.activeSeasonIndex == idx && state.season != null) return@update state
                    val target = when (idx) {
                        0 -> state.currentSeason ?: state.allSeasons.getOrNull(0)
                        1 -> state.previousSeason ?: state.allSeasons.getOrNull(1)
                        else -> state.allSeasons.getOrNull(idx)
                    }
                    if (target == null) {
                        state.copy(activeSeasonIndex = idx)
                    } else {
                        val tag = state.savedPlayerTag.ifBlank { target.myPlayerId }
                        val seasonWithTag = if (tag.isNotBlank()) target.copy(myPlayerId = tag) else target
                        val normalized = RankingCalculator.normalizeSeason(seasonWithTag)
                        val stats = RankingCalculator.getRankingStats(normalized, tag)
                        val all = listOfNotNull(state.currentSeason, state.previousSeason).ifEmpty { state.allSeasons }
                        state.copy(
                            activeSeasonIndex = idx,
                            season = normalized,
                            allSeasons = all,
                            stats = stats,
                            filteredPlayers = filterPlayers(normalized.players, state.searchQuery, state.selectedCategory)
                        )
                    }
                }
            }
        }

        // Observe cached RankChanges
        viewModelScope.launch {
            userPreferences.rankChangesJson.collectLatest { json ->
                if (json.isNotBlank()) {
                    try {
                        val cachedChanges = gson.fromJson(json, RankChangesSnapshot::class.java)
                        _uiState.update { state ->
                            if (state.rankChanges == null || state.rankChanges.changes.isEmpty()) {
                                state.copy(rankChanges = cachedChanges)
                            } else state
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        // Observe cached PreviousPlayers
        viewModelScope.launch {
            userPreferences.previousPlayersJson.collectLatest { json ->
                if (json.isNotBlank()) {
                    try {
                        val listType = object : TypeToken<List<Player>>() {}.type
                        val cachedPlayers: List<Player> = gson.fromJson(json, listType)
                        _uiState.update { state ->
                            state.copy(previousPlayers = cachedPlayers)
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        // Observe latest season from local DB
        viewModelScope.launch {
            repository.latestSeason.collectLatest { season ->
                if (season != null) {
                    val activeTag = _uiState.value.savedPlayerTag.ifBlank { season.myPlayerId }
                    val seasonWithTag = if (activeTag.isNotBlank()) season.copy(myPlayerId = activeTag) else season
                    val normalizedCurrent = RankingCalculator.normalizeSeason(seasonWithTag)
                    _uiState.update { state ->
                        val all = listOfNotNull(normalizedCurrent, state.previousSeason)
                        val displayed = if (state.activeSeasonIndex == 1 && state.previousSeason != null) {
                            state.previousSeason
                        } else {
                            normalizedCurrent
                        }
                        val displayedWithTag = if (activeTag.isNotBlank()) displayed.copy(myPlayerId = activeTag) else displayed
                        val normalizedDisplayed = RankingCalculator.normalizeSeason(displayedWithTag)

                        state.copy(
                            season = normalizedDisplayed,
                            currentSeason = normalizedCurrent,
                            allSeasons = all,
                            stats = RankingCalculator.getRankingStats(normalizedDisplayed, activeTag),
                            filteredPlayers = filterPlayers(normalizedDisplayed.players, state.searchQuery, state.selectedCategory)
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
        _uiState.update { state ->
            val target = when (index) {
                0 -> state.currentSeason ?: state.allSeasons.getOrNull(0)
                1 -> state.previousSeason ?: state.allSeasons.getOrNull(1)
                else -> state.allSeasons.getOrNull(index)
            } ?: return@update state

            val tag = state.savedPlayerTag.ifBlank { target.myPlayerId }
            val seasonWithTag = if (tag.isNotBlank()) target.copy(myPlayerId = tag) else target
            val normalized = RankingCalculator.normalizeSeason(seasonWithTag)
            val stats = RankingCalculator.getRankingStats(normalized, tag)
            val all = listOfNotNull(state.currentSeason, state.previousSeason).ifEmpty { state.allSeasons }

            state.copy(
                activeSeasonIndex = index,
                season = normalized,
                allSeasons = all,
                stats = stats,
                filteredPlayers = filterPlayers(normalized.players, state.searchQuery, state.selectedCategory)
            )
        }
        viewModelScope.launch {
            userPreferences.saveActiveSeasonIndex(index)
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
        viewModelScope.launch {
            val tag = userPreferences.playerTag.first()
            if (tag.isBlank()) {
                _uiState.update { it.copy(showConfigDialog = true) }
                return@launch
            }

            _uiState.update { it.copy(isSyncing = true, syncMessage = "Đang đồng bộ Supercell API...", errorMessage = null) }

            // Lấy danh sách snapshot của lần đồng bộ trước làm mốc so sánh (baseline)
            // Ưu tiên:
            // 1) previousPlayers trong state nếu đã có người chơi có rank hợp lệ
            // 2) currentSeason?.players trong state nếu có người chơi có rank hợp lệ
            val baselinePlayers = _uiState.value.previousPlayers.filter { it.rank > 0 }.ifEmpty {
                _uiState.value.currentSeason?.players?.filter { it.rank > 0 } ?: emptyList()
            }
            val previousUpdatedAt = _uiState.value.rankChanges?.updatedAt

            val result = repository.syncAllWithApi(
                overrideTag = tag,
                onProgress = { msg -> _uiState.update { it.copy(syncMessage = msg) } }
            )

            result.onSuccess { syncRes ->
                val activeIndex = _uiState.value.activeSeasonIndex
                val displayedSeason = if (activeIndex == 1 && syncRes.previousSeason != null) syncRes.previousSeason else syncRes.currentSeason
                val tagToUse = tag.ifBlank { displayedSeason.myPlayerId }
                val seasonWithTag = if (tagToUse.isNotBlank()) displayedSeason.copy(myPlayerId = tagToUse) else displayedSeason
                val normalizedDisplayed = RankingCalculator.normalizeSeason(seasonWithTag)

                val newRankedCurrent = syncRes.currentSeason
                val changes = if (baselinePlayers.isNotEmpty()) {
                    RankingCalculator.calculateRankChanges(
                        currentSeason = newRankedCurrent,
                        previousPlayers = baselinePlayers,
                        myPlayerTag = tag,
                        previousUpdatedAt = previousUpdatedAt
                    )
                } else {
                    RankChangesSnapshot(
                        seasonId = newRankedCurrent.leagueSeasonId,
                        groupTag = newRankedCurrent.leagueGroupTag,
                        updatedAt = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
                        previousUpdatedAt = null,
                        changes = emptyList()
                    )
                }

                // Ghi nhận ngay trạng thái vừa đồng bộ làm mốc so sánh cho lần đồng bộ kế tiếp
                val newPlayers = newRankedCurrent.players.filter { it.rank > 0 }
                if (newPlayers.isNotEmpty()) {
                    userPreferences.savePreviousPlayersJson(gson.toJson(newPlayers))
                }
                userPreferences.saveRankChangesJson(gson.toJson(changes))

                _uiState.update {
                    it.copy(
                        season = normalizedDisplayed,
                        currentSeason = syncRes.currentSeason,
                        previousSeason = syncRes.previousSeason,
                        allSeasons = syncRes.allSeasons,
                        stats = RankingCalculator.getRankingStats(normalizedDisplayed, tagToUse),
                        clanData = syncRes.clanData ?: it.clanData,
                        leagueHistory = if (syncRes.leagueHistory.isNotEmpty()) syncRes.leagueHistory else it.leagueHistory,
                        previousPlayers = if (newPlayers.isNotEmpty()) newPlayers else it.previousPlayers,
                        rankChanges = changes,
                        filteredPlayers = filterPlayers(normalizedDisplayed.players, it.searchQuery, it.selectedCategory),
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

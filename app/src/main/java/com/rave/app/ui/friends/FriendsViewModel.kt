package com.rave.app.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rave.app.core.TimeProvider
import com.rave.app.data.cloud.AuthRepository
import com.rave.app.data.cloud.CloudUser
import com.rave.app.data.cloud.CompetitionRepository
import com.rave.app.data.cloud.FeedItem
import com.rave.app.data.cloud.FeedRepository
import com.rave.app.data.cloud.Friend
import com.rave.app.data.cloud.FriendsRepository
import com.rave.app.data.cloud.ProfileRepository
import com.rave.app.data.cloud.SharedStreak
import com.rave.app.data.cloud.SharedStreakRepository
import com.rave.app.data.cloud.UserProfile
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.domain.model.DistanceUnit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LeaderboardEntry(val uid: String, val name: String, val weekKm: Double, val isMe: Boolean)

data class FriendsUiState(
    val loading: Boolean = true,
    val cloudAvailable: Boolean = false,
    val user: CloudUser? = null,
    val me: UserProfile? = null,
    val friends: List<Friend> = emptyList(),
    val requestCount: Int = 0,
    val invitationCount: Int = 0,
    val shared: List<SharedStreak> = emptyList(),
    val leaderboard: List<LeaderboardEntry> = emptyList(),
    val feed: List<FeedItem> = emptyList(),
    val unit: DistanceUnit = DistanceUnit.KM,
    /** Friends already nudged today (uid set). */
    val nudged: Set<String> = emptySet(),
)

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val friends: FriendsRepository,
    profiles: ProfileRepository,
    private val shared: SharedStreakRepository,
    private val feed: FeedRepository,
    competitions: CompetitionRepository,
    settings: SettingsRepository,
    private val time: TimeProvider,
) : ViewModel() {
    private val nudged = MutableStateFlow<Set<String>>(emptySet())
    private val _messages = Channel<NudgeResult>(Channel.BUFFERED)
    val messages: Flow<NudgeResult> = _messages.receiveAsFlow()

    private val social = combine(friends.observeFriends(), friends.observeRequests(), shared.observeActive(), shared.observeInvitations(),
        competitions.observeInvitations()) { f, r, s, si, ci -> Social(f, r.incoming.size, s, si.size + ci.size) }

    val state: StateFlow<FriendsUiState> = combine(
        auth.currentUser, profiles.observeMyProfile(), social, feed.observeFeed(), combine(settings.settings, nudged) { a, b -> a to b },
    ) { user, me, soc, feedItems, (prefs, nudgedSet) ->
        val weekStart = time.weekStart().toEpochDay()
        val board = (soc.friends.map { LeaderboardEntry(it.uid, it.name, it.weekKm, false) } +
            listOfNotNull(me?.let { LeaderboardEntry(it.uid, it.name, it.weekKmFor(weekStart), true) }))
            .sortedByDescending { it.weekKm }
        FriendsUiState(
            loading = false,
            cloudAvailable = auth.isAvailable,
            user = user,
            me = me,
            friends = soc.friends,
            requestCount = soc.requests,
            invitationCount = soc.invitations,
            shared = soc.shared,
            leaderboard = board,
            feed = feedItems,
            unit = prefs.unit,
            nudged = nudgedSet,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FriendsUiState(cloudAvailable = auth.isAvailable))

    private data class Social(val friends: List<Friend>, val requests: Int, val shared: List<SharedStreak>, val invitations: Int)

    fun myUid(): String? = auth.currentUser.value?.uid

    fun nudge(friendUid: String, reason: String, targetId: String?) {
        viewModelScope.launch {
            val result = friends.nudge(friendUid, reason, targetId)
            nudged.update { it + friendUid }
            _messages.send(
                when {
                    result.isFailure -> NudgeResult.FAILED
                    result.getOrDefault(false) -> NudgeResult.SENT
                    else -> NudgeResult.ALREADY
                },
            )
        }
    }

    fun cheer(item: FeedItem) {
        viewModelScope.launch { feed.toggleCheer(item) }
    }
}

enum class NudgeResult { SENT, ALREADY, FAILED }

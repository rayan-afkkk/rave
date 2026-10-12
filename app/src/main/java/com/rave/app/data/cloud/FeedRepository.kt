package com.rave.app.data.cloud

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.rave.app.data.local.entity.ActivityEntity
import com.rave.app.domain.model.ActivityType
import com.rave.app.domain.model.LatLngPoint
import com.rave.app.domain.tracking.Polyline
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class FeedItem(
    val id: String,
    val ownerUid: String,
    val ownerName: String,
    val type: ActivityType,
    val name: String,
    val distanceM: Double,
    val durationS: Long,
    val startTime: Long,
    /** Privacy-trimmed, simplified route segments. */
    val segments: List<List<LatLngPoint>>,
    val cheers: List<String>,
)

/**
 * Friends' finished activities. Each `feed/{id}` doc carries an `audience` array (owner +
 * friends at publish time) so reads can be secured with `request.auth.uid in audience`.
 * Only privacy-trimmed, simplified polylines are uploaded — never raw GPS traces.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class FeedRepository @Inject constructor(
    private val firebase: FirebaseProvider,
    private val auth: AuthRepository,
) {
    fun observeFeed(): Flow<List<FeedItem>> = auth.currentUser.flatMapLatest { user ->
        if (user == null || firebase.firestore == null) flowOf(emptyList())
        else firebase.requireFirestore().collection(FEED)
            .whereArrayContains("audience", user.uid)
            .orderBy("startTime", Query.Direction.DESCENDING)
            .limit(40)
            .observe()
            .map { s -> s.documents.mapNotNull { it.toFeedItem() } }
            .catch { emit(emptyList()) }
    }

    suspend fun publish(
        remoteId: String,
        activity: ActivityEntity,
        ownerName: String,
        segments: List<List<LatLngPoint>>,
        audience: List<String>,
    ) {
        val uid = firebase.requireUid()
        val encoded = segments.map { Polyline.encode(Polyline.simplify(it, toleranceM = 6.0)) }
        firebase.requireFirestore().collection(FEED).document(remoteId).set(
            mapOf(
                "ownerUid" to uid, "ownerName" to ownerName, "type" to activity.type.name, "name" to activity.name,
                "distanceM" to activity.distanceM, "durationS" to activity.durationS, "startTime" to activity.startTime,
                "polylines" to encoded, "audience" to (audience + uid).distinct(), "autoDetected" to activity.autoDetected,
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
            SetOptions.merge(),
        ).await()
    }

    suspend fun remove(remoteId: String) {
        runCatching { firebase.requireFirestore().collection(FEED).document(remoteId).delete().await() }
    }

    /** Cheer / un-cheer. Rules only allow adding or removing your own uid. */
    suspend fun toggleCheer(item: FeedItem): Result<Unit> = runCatching {
        val me = firebase.requireUid()
        val value = if (me in item.cheers) FieldValue.arrayRemove(me) else FieldValue.arrayUnion(me)
        firebase.requireFirestore().collection(FEED).document(item.id).update("cheers", value).await()
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toFeedItem(): FeedItem? {
        if (!exists()) return null
        return FeedItem(
            id = id,
            ownerUid = str("ownerUid"),
            ownerName = str("ownerName"),
            type = runCatching { ActivityType.valueOf(str("type")) }.getOrDefault(ActivityType.WALK),
            name = str("name"),
            distanceM = double("distanceM"),
            durationS = long("durationS"),
            startTime = long("startTime"),
            segments = stringList("polylines").map { Polyline.decode(it) },
            cheers = stringList("cheers"),
        )
    }

    companion object {
        const val FEED = "feed"
    }
}

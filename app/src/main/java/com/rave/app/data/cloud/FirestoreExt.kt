package com.rave.app.data.cloud

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/* Snapshot listeners as cold Flows. Errors (e.g. permission denied) close the flow. */

fun DocumentReference.observe(): Flow<DocumentSnapshot> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) close(error) else if (snapshot != null) trySend(snapshot)
    }
    awaitClose { registration.remove() }
}

fun Query.observe(): Flow<QuerySnapshot> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) close(error) else if (snapshot != null) trySend(snapshot)
    }
    awaitClose { registration.remove() }
}

fun DocumentSnapshot.double(field: String): Double = getDouble(field) ?: getLong(field)?.toDouble() ?: 0.0
fun DocumentSnapshot.int(field: String): Int = getLong(field)?.toInt() ?: 0
fun DocumentSnapshot.long(field: String): Long = getLong(field) ?: 0L
fun DocumentSnapshot.str(field: String): String = getString(field).orEmpty()

@Suppress("UNCHECKED_CAST")
fun DocumentSnapshot.stringList(field: String): List<String> = (get(field) as? List<String>).orEmpty()

@Suppress("UNCHECKED_CAST")
fun DocumentSnapshot.map(field: String): Map<String, Any?> = (get(field) as? Map<String, Any?>).orEmpty()

/** Firestore `in` / `whereIn` queries accept at most 30 values. */
fun <T> List<T>.firestoreChunks(): List<List<T>> = chunked(30)

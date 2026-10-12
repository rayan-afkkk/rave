package com.rave.app.data.cloud

import android.content.Context
import android.provider.ContactsContract
import com.rave.app.domain.social.PhoneHasher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Opt-in "friends from contacts". Phone numbers are read on-device, hashed with
 * [PhoneHasher] and only the hashes are sent in a query. Raw contacts never leave the phone.
 * Requires READ_CONTACTS (asked with an explanation first).
 */
@Singleton
class ContactsMatcher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profiles: ProfileRepository,
) {
    suspend fun findFriendsInContacts(): List<UserProfile> {
        val hashes = withContext(Dispatchers.IO) { readHashes() }
        if (hashes.isEmpty()) return emptyList()
        return profiles.findByPhoneHashes(hashes)
    }

    private fun readHashes(): List<String> {
        val result = mutableSetOf<String>()
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            null, null, null,
        )?.use { c ->
            val idx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (c.moveToNext() && idx >= 0) {
                c.getString(idx)?.let { PhoneHasher.hash(it) }?.let(result::add)
            }
        }
        return result.toList()
    }
}

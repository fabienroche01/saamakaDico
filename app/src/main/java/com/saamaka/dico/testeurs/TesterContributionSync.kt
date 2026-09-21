package com.saamaka.dico.testeurs

import android.content.Context
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.saamaka.dico.testeurs.repository.CorrectionStore
import com.saamaka.dico.testeurs.repository.DeletionProposalStore
import com.saamaka.dico.testeurs.repository.NewEntryProposalStore

data class TesterSyncResult(
    val success: Boolean,
    val sentCount: Int = 0,
    val message: String
)

class TesterContributionSync(
    private val context: Context,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val preferences = context.getSharedPreferences(
        "saamaka_tester_sync",
        Context.MODE_PRIVATE
    )

    fun lastSyncAtMillis(): Long =
        preferences.getLong(KEY_LAST_SYNC_AT, 0L)

    fun pendingCount(): Int {
        val since = lastSyncAtMillis()
        val corrections = CorrectionStore(context).all().count { it.createdAt > since && it.hasChanges() }
        val validations = ReviewStore(context).all().count {
            it.createdAt > since && it.action == ReviewActionType.VALIDATED
        }
        val deletions = DeletionProposalStore(context).all().count { it.createdAt > since }
        val newEntries = NewEntryProposalStore(context).all().count { it.createdAt > since }
        return corrections + validations + deletions + newEntries
    }

    fun syncAll(
        testerName: String,
        onComplete: (TesterSyncResult) -> Unit
    ) {
        val existingUser = auth.currentUser
        if (existingUser != null) {
            performSync(existingUser.uid, existingUser.email.orEmpty(), testerName, onComplete)
            return
        }

        auth.signInAnonymously()
            .addOnSuccessListener { result ->
                val user = result.user
                if (user == null) {
                    onComplete(TesterSyncResult(false, message = "Connexion Firebase impossible."))
                } else {
                    performSync(user.uid, user.email.orEmpty(), testerName, onComplete)
                }
            }
            .addOnFailureListener { error ->
                onComplete(
                    TesterSyncResult(
                        false,
                        message = error.localizedMessage ?: "Connexion Firebase impossible."
                    )
                )
            }
    }

    fun cancelDeletion(entryId: Int) {
        val user = auth.currentUser ?: return
        firestore.collection(COLLECTION_TESTERS)
            .document(user.uid)
            .collection(COLLECTION_ITEMS)
            .document("deletion_${entryId}")
            .delete()
    }

    fun cancelNewEntry(localId: String) {
        val user = auth.currentUser ?: return
        firestore.collection(COLLECTION_TESTERS)
            .document(user.uid)
            .collection(COLLECTION_ITEMS)
            .document("new_${safeId(localId)}")
            .delete()
    }

    private fun performSync(
        uid: String,
        email: String,
        testerName: String,
        onComplete: (TesterSyncResult) -> Unit
    ) {
        val since = lastSyncAtMillis()
        val now = System.currentTimeMillis()
        val items = mutableListOf<Pair<String, Map<String, Any?>>>()

        CorrectionStore(context).all()
            .filter { it.createdAt > since && it.hasChanges() }
            .forEach { proposal ->
                items += "correction_${proposal.entryId}" to mapOf(
                    "type" to "correction",
                    "status" to "pending",
                    "entryId" to proposal.entryId,
                    "frenchCurrent" to proposal.frenchCurrent,
                    "saamakaCurrent" to proposal.saamakaCurrent,
                    "frenchProposed" to proposal.frenchProposed,
                    "saamakaProposed" to proposal.saamakaProposed,
                    "categoryCurrent" to proposal.categoryCurrent,
                    "categoryProposed" to proposal.categoryProposed,
                    "comment" to proposal.comment,
                    "testerName" to proposal.testerName.ifBlank { testerName },
                    "createdAtMillis" to proposal.createdAt,
                    "updatedAt" to Timestamp.now()
                )
            }

        ReviewStore(context).all()
            .filter { it.createdAt > since && it.action == ReviewActionType.VALIDATED }
            .forEach { action ->
                items += "validation_${action.entryId}" to mapOf(
                    "type" to "validation",
                    "status" to "pending",
                    "entryId" to action.entryId,
                    "frenchCurrent" to action.frenchCurrent,
                    "saamakaCurrent" to action.saamakaCurrent,
                    "reviewer" to action.reviewer.ifBlank { testerName },
                    "createdAtMillis" to action.createdAt,
                    "updatedAt" to Timestamp.now()
                )
            }

        DeletionProposalStore(context).all()
            .filter { it.createdAt > since }
            .forEach { proposal ->
                items += "deletion_${proposal.entryId}" to mapOf(
                    "type" to "deletion",
                    "status" to "pending",
                    "entryId" to proposal.entryId,
                    "saamaka" to proposal.saamaka,
                    "french" to proposal.french,
                    "english" to proposal.english,
                    "dutch" to proposal.dutch,
                    "reason" to proposal.reason,
                    "comment" to proposal.comment,
                    "testerName" to proposal.testerName.ifBlank { testerName },
                    "createdAtMillis" to proposal.createdAt,
                    "updatedAt" to Timestamp.now()
                )
            }

        NewEntryProposalStore(context).all()
            .filter { it.createdAt > since }
            .forEach { proposal ->
                items += "new_${safeId(proposal.localId)}" to mapOf(
                    "type" to "new_entry",
                    "status" to "pending",
                    "localId" to proposal.localId,
                    "saamaka" to proposal.saamaka,
                    "french" to proposal.french,
                    "english" to proposal.english,
                    "dutch" to proposal.dutch,
                    "category" to proposal.category,
                    "comment" to proposal.comment,
                    "audioFileName" to proposal.audioFileName.orEmpty(),
                    "testerName" to proposal.testerName.ifBlank { testerName },
                    "createdAtMillis" to proposal.createdAt,
                    "updatedAt" to Timestamp.now()
                )
            }

        if (items.isEmpty()) {
            onComplete(TesterSyncResult(true, 0, "Aucune nouvelle contribution à envoyer."))
            return
        }

        val testerDoc = firestore.collection(COLLECTION_TESTERS).document(uid)
        val chunks = items.chunked(BATCH_SIZE)

        fun commitChunk(index: Int) {
            if (index >= chunks.size) {
                testerDoc.set(
                    mapOf(
                        "uid" to uid,
                        "email" to email,
                        "testerName" to testerName,
                        "lastSyncAt" to Timestamp.now(),
                        "clientVersion" to BuildConfig.VERSION_NAME
                    ),
                    SetOptions.merge()
                ).addOnSuccessListener {
                    preferences.edit().putLong(KEY_LAST_SYNC_AT, now).apply()
                    onComplete(
                        TesterSyncResult(
                            true,
                            items.size,
                            "${items.size} contribution(s) envoyée(s)."
                        )
                    )
                }.addOnFailureListener { error ->
                    onComplete(
                        TesterSyncResult(
                            false,
                            message = error.localizedMessage ?: "Échec de synchronisation."
                        )
                    )
                }
                return
            }

            val batch = firestore.batch()
            chunks[index].forEach { (documentId, data) ->
                val ref = testerDoc.collection(COLLECTION_ITEMS).document(documentId)
                batch.set(
                    ref,
                    data + mapOf(
                        "ownerUid" to uid,
                        "clientVersion" to BuildConfig.VERSION_NAME
                    ),
                    SetOptions.merge()
                )
            }

            batch.commit()
                .addOnSuccessListener { commitChunk(index + 1) }
                .addOnFailureListener { error ->
                    onComplete(
                        TesterSyncResult(
                            false,
                            message = error.localizedMessage ?: "Échec de synchronisation."
                        )
                    )
                }
        }

        commitChunk(0)
    }

    private fun safeId(value: String): String =
        value.replace(Regex("[^A-Za-z0-9_-]"), "_").take(120)

    private companion object {
        const val KEY_LAST_SYNC_AT = "last_sync_at"
        const val COLLECTION_TESTERS = "tester_contributions"
        const val COLLECTION_ITEMS = "items"
        const val BATCH_SIZE = 400
    }
}

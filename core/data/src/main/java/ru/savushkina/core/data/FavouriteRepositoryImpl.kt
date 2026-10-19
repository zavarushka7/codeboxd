package ru.savushkina.core.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import ru.savushkina.core.common.runCatchingCancellable
import ru.savushkina.core.domain.FavouriteRepository
import ru.savushkina.core.model.AddFavouriteResult
import ru.savushkina.core.model.FavouriteFour
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavouriteRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : FavouriteRepository {

    override fun observeFavourites(userId: String): Flow<List<String>> = callbackFlow {
        val listener = firestore.collection(COLLECTION)
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val ids = snapshot?.get("itemIds") as? List<*> ?: emptyList<Any>()
                trySend(ids.mapNotNull { it as? String })
            }
        awaitClose { listener.remove() }
    }

    override suspend fun addFavourite(
        userId: String,
        itemId: String
    ): Result<AddFavouriteResult> = runCatchingCancellable {
        val docRef = firestore.collection(COLLECTION).document(userId)
        firestore.runTransaction { tx ->
            val snapshot = tx.get(docRef)
            val current = (snapshot.get("itemIds") as? List<*>)
                ?.mapNotNull { it as? String }
                .orEmpty()

            when {
                current.contains(itemId) -> AddFavouriteResult.AlreadyExists
                current.size >= FavouriteFour.MAX_SIZE -> AddFavouriteResult.LimitReached
                else -> {
                    tx.set(
                        docRef,
                        mapOf("itemIds" to (current + itemId)),
                        SetOptions.merge()
                    )
                    AddFavouriteResult.Added
                }
            }
        }.await()
    }

    override suspend fun removeFavourite(
        userId: String,
        itemId: String
    ): Result<Unit> = runCatchingCancellable {
        val docRef = firestore.collection(COLLECTION).document(userId)
        firestore.runTransaction { tx ->
            val snapshot = tx.get(docRef)
            val current = (snapshot.get("itemIds") as? List<*>)
                ?.mapNotNull { it as? String }
                .orEmpty()
            tx.set(
                docRef,
                mapOf("itemIds" to current.filterNot { it == itemId }),
                SetOptions.merge()
            )
        }.await()
        Unit
    }

    private companion object {
        const val COLLECTION = "favourites"
    }
}
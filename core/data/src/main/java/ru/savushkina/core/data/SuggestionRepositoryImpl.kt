package ru.savushkina.core.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import ru.savushkina.core.common.runCatchingCancellable
import ru.savushkina.core.domain.SuggestionRepository
import ru.savushkina.core.model.ItemSuggestion
import ru.savushkina.core.model.SuggestionStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SuggestionRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : SuggestionRepository {

    override suspend fun submitSuggestion(
        suggestion: ItemSuggestion
    ): Result<String> = runCatchingCancellable {
        val name = suggestion.name.trim()
        val type = suggestion.type.trim()
        val description = suggestion.description.trim()

        require(name.isNotBlank()) { "Имя не может быть пустым" }
        require(name.length <= ItemSuggestion.NAME_MAX) {
            "Имя слишком длинное (макс. ${ItemSuggestion.NAME_MAX})"
        }
        require(type.isNotBlank()) { "Тип не может быть пустым" }
        require(type.length <= ItemSuggestion.TYPE_MAX) {
            "Тип слишком длинный (макс. ${ItemSuggestion.TYPE_MAX})"
        }
        require(description.length <= ItemSuggestion.DESCRIPTION_MAX) {
            "Описание слишком длинное (макс. ${ItemSuggestion.DESCRIPTION_MAX})"
        }
        require(suggestion.authorUid.isNotBlank()) { "Нет автора заявки" }

        val data = mapOf(
            "authorUid" to suggestion.authorUid,
            "authorName" to suggestion.authorName,
            "name" to name,
            "type" to type,
            "description" to description,
            "createdAt" to Timestamp.now(),
            "status" to SuggestionStatus.PENDING.name
        )
        firestore.collection(COLLECTION).add(data).await().id
    }

    private companion object {
        const val COLLECTION = "suggestions"
    }
}
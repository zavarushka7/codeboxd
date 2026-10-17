package ru.savushkina.feature.collection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.savushkina.core.domain.AuthRepository
import ru.savushkina.core.domain.CollectionRepository
import ru.savushkina.core.model.UserCollection
import javax.inject.Inject

@HiltViewModel
class CollectionsListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    collectionRepository: CollectionRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    /** null или пустой — свой профиль, иначе чужой. */
    private val targetUserId: String? = savedStateHandle.get<String>("userId")

    val currentUserId: String?
        get() = authRepository.currentUserId

    val isOwnCollections: Boolean
        get() = targetUserId.isNullOrBlank() || targetUserId == currentUserId

    private val _collections = MutableStateFlow<List<UserCollection>>(emptyList())
    val collections = _collections.asStateFlow()

    init {
        val uid = targetUserId?.takeIf { it.isNotBlank() }
            ?: authRepository.currentUserId

        if (uid != null) {
            viewModelScope.launch {
                collectionRepository.observeCollections(uid).collect {
                    _collections.value = it
                }
            }
        }
    }
}
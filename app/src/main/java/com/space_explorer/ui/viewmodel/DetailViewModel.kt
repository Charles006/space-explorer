package com.space_explorer.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.space_explorer.domain.error.AstronomyError
import com.space_explorer.domain.repository.AstronomyRepository
import com.space_explorer.ui.navigation.DetailDestination
import com.space_explorer.ui.state.DetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the Detail screen.
 *
 * Inputs:
 *   * `astronomyId` (from [SavedStateHandle]) — typed as ISO date so it
 *     doubles as the API key.
 *
 * Behaviour:
 *   * Fetches the APOD detail once on init and on [retry].
 *   * Observes the favorites stream so the star toggle stays accurate even
 *     when the user backs out, edits favorites elsewhere, and returns.
 *   * Reports toggle failures to the user via [DetailUiState.errorMessage]
 *     instead of swallowing them — this was a real bug where a rapid double
 *     tap would silently fail and the UI would diverge from the DB state.
 */
@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: AstronomyRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val astronomyId: String =
        checkNotNull(savedStateHandle.get<String>(DetailDestination.ARG_ID)) {
            "Detail screen launched without ${DetailDestination.ARG_ID}"
        }

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        observeFavoriteState()
        loadDetail()
    }

    /** Re-fetch the detail. Invoked from the error retry button. */
    fun retry() = loadDetail()

    /** Toggle favorite state. Errors are surfaced via [DetailUiState.errorMessage]. */
    fun toggleFavorite() {
        val current = _uiState.value.astronomy ?: run {
            _uiState.update { it.copy(errorMessage = "No hay contenido cargado para marcar.") }
            return
        }
        viewModelScope.launch {
            runCatching { repository.toggleFavorite(current) }
                .onFailure { throwable ->
                    _uiState.update { it.copy(errorMessage = throwable.userMessage()) }
                }
        }
    }

    /** Dismiss any user-visible error so the snackbar can be re-shown later. */
    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // region ── Internal ──────────────────────────────────────────────────

    private fun observeFavoriteState() {
        repository.observeFavoriteIds()
            .onEach { ids ->
                _uiState.update { current ->
                    val updated = current.astronomy?.copy(isFavorite = astronomyId in ids)
                    current.copy(astronomy = updated)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadDetail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.getAstronomyByDate(astronomyId)
                .onSuccess { astronomy ->
                    _uiState.update { it.copy(astronomy = astronomy, isLoading = false) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = throwable.userMessage())
                    }
                }
        }
    }

    private fun Throwable.userMessage(): String = when (this) {
        is AstronomyError -> userMessage
        else -> message ?: "Error desconocido"
    }

    // endregion
}

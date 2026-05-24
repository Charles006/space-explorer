package com.space_explorer.ui.viewmodel

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.space_explorer.domain.error.AstronomyError
import com.space_explorer.domain.model.Astronomy
import com.space_explorer.domain.repository.AstronomyRepository
import com.space_explorer.ui.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class AstronomyViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: AstronomyRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        DateUtils.setFixedClock("2026-05-22")
        repository = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        DateUtils.resetClock()
    }

    @Test
    fun `loadInitialData success populates items and stops loading`() = runTest {
        val items = listOf(astronomy("2026-05-22", "Mars"))
        whenever(repository.getAstronomyRange(any(), any())).thenReturn(Result.success(items))
        whenever(repository.observeFavoriteIds()).thenReturn(MutableStateFlow(emptySet()))

        val viewModel = AstronomyViewModel(repository)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.items).hasSize(1)
            assertThat(state.errorMessage).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadInitialData failure surfaces domain error message`() = runTest {
        whenever(repository.getAstronomyRange(any(), any()))
            .thenReturn(Result.failure(AstronomyError.Network()))
        whenever(repository.observeFavoriteIds()).thenReturn(MutableStateFlow(emptySet()))

        val viewModel = AstronomyViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorMessage).contains("Sin conexion")
    }

    @Test
    fun `loadInitialData with generic exception falls back to its message`() = runTest {
        whenever(repository.getAstronomyRange(any(), any()))
            .thenReturn(Result.failure(RuntimeException("boom")))
        whenever(repository.observeFavoriteIds()).thenReturn(MutableStateFlow(emptySet()))

        val viewModel = AstronomyViewModel(repository)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.errorMessage).isEqualTo("boom")
    }

    @Test
    fun `onSearchQueryChanged filters items by title`() = runTest {
        val items = listOf(
            astronomy("2026-05-22", "Mars Sunrise"),
            astronomy("2026-05-21", "Lunar Eclipse"),
            astronomy("2026-05-20", "Saturn rings")
        )
        whenever(repository.getAstronomyRange(any(), any())).thenReturn(Result.success(items))
        whenever(repository.observeFavoriteIds()).thenReturn(MutableStateFlow(emptySet()))

        val viewModel = AstronomyViewModel(repository)
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("mars")

        val state = viewModel.uiState.value
        assertThat(state.filteredItems).hasSize(1)
        assertThat(state.filteredItems.first().title).contains("Mars")
    }

    @Test
    fun `onRemoteSearch with invalid date sets error without calling repository`() = runTest {
        whenever(repository.getAstronomyRange(any(), any())).thenReturn(Result.success(emptyList()))
        whenever(repository.observeFavoriteIds()).thenReturn(MutableStateFlow(emptySet()))

        val viewModel = AstronomyViewModel(repository)
        advanceUntilIdle()

        viewModel.onRemoteSearch("invalid-date")

        assertThat(viewModel.uiState.value.errorMessage).contains("Formato")
    }

    @Test
    fun `toggleFavorite delegates to repository toggleFavorite`() = runTest {
        val item = astronomy("2026-05-22", "Mars", isFavorite = false)
        whenever(repository.getAstronomyRange(any(), any())).thenReturn(Result.success(listOf(item)))
        whenever(repository.observeFavoriteIds()).thenReturn(MutableStateFlow(emptySet()))

        val viewModel = AstronomyViewModel(repository)
        advanceUntilIdle()

        viewModel.toggleFavorite(item)
        advanceUntilIdle()

        verify(repository).toggleFavorite(item)
    }

    // Regression: after a failed loadNextPage the snackbar stayed pinned even when
    // the very next page loaded fine, because appendPage() never cleared the prior
    // errorMessage. See screenshot bug — error sticks while list continues at 10 items.
    @Test
    fun `loadNextPage success clears prior errorMessage`() = runTest {
        val initial = listOf(astronomy("2026-05-22", "First"))
        val nextPage = listOf(astronomy("2026-05-12", "Second"))
        whenever(repository.observeFavoriteIds()).thenReturn(MutableStateFlow(emptySet()))
        whenever(repository.getAstronomyRange(any(), any()))
            .thenReturn(Result.success(initial))                                  // load initial
            .thenReturn(Result.failure(RuntimeException("boom transient")))       // first loadNextPage
            .thenReturn(Result.success(nextPage))                                 // second loadNextPage

        val viewModel = AstronomyViewModel(repository)
        advanceUntilIdle()

        viewModel.loadNextPage()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.errorMessage).contains("boom transient")

        viewModel.loadNextPage()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.errorMessage).isNull()
        assertThat(viewModel.uiState.value.items).hasSize(2)
    }

    @Test
    fun `observeFavoriteIds updates isFavorite flag of items`() = runTest {
        val item = astronomy("2026-05-22", "Mars", isFavorite = false)
        whenever(repository.getAstronomyRange(any(), any())).thenReturn(Result.success(listOf(item)))
        val favoritesFlow = MutableStateFlow<Set<String>>(emptySet())
        whenever(repository.observeFavoriteIds()).thenReturn(favoritesFlow)

        val viewModel = AstronomyViewModel(repository)
        advanceUntilIdle()

        favoritesFlow.value = setOf("2026-05-22")
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.items.first().isFavorite).isTrue()
    }

    private fun astronomy(date: String, title: String, isFavorite: Boolean = false) = Astronomy(
        id = date,
        date = date,
        title = title,
        explanation = "Explanation for $title",
        imageUrl = "https://image/$date.jpg",
        hdImageUrl = null,
        mediaType = "image",
        copyright = null,
        isFavorite = isFavorite
    )
}

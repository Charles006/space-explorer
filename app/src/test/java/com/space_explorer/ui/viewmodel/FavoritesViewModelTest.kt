package com.space_explorer.ui.viewmodel

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.space_explorer.domain.model.Astronomy
import com.space_explorer.domain.repository.AstronomyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: AstronomyRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState exposes favorites from repository`() = runTest {
        val favorites = listOf(
            astronomy("2026-05-22", "Mars"),
            astronomy("2026-05-21", "Lunar Eclipse"),
        )
        whenever(repository.getFavorites()).thenReturn(flowOf(favorites))

        val viewModel = FavoritesViewModel(repository)

        viewModel.uiState.test {
            // Skip initial loading state if present
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()
            assertThat(state.favorites).hasSize(2)
            assertThat(state.isLoading).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `removeFavorite calls repository`() = runTest {
        val favorite = astronomy("2026-05-22", "Mars")
        whenever(repository.getFavorites()).thenReturn(flowOf(listOf(favorite)))

        val viewModel = FavoritesViewModel(repository)
        advanceUntilIdle()

        viewModel.removeFavorite(favorite)
        advanceUntilIdle()

        verify(repository).removeFavorite("2026-05-22")
    }

    @Test
    fun `onSearchQueryChanged filters favorites locally`() = runTest {
        val favorites = listOf(
            astronomy("2026-05-22", "Mars Sunrise"),
            astronomy("2026-05-21", "Lunar Eclipse"),
            astronomy("2026-05-20", "Saturn rings"),
        )
        whenever(repository.getFavorites()).thenReturn(MutableStateFlow(favorites))

        val viewModel = FavoritesViewModel(repository)

        viewModel.uiState.test {
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()

            viewModel.onSearchQueryChanged("lunar")

            state = awaitItem()
            while (state.favorites.size != 1) state = awaitItem()
            assertThat(state.favorites.first().title).contains("Lunar")
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun astronomy(date: String, title: String) = Astronomy(
        id = date,
        date = date,
        title = title,
        explanation = "Explanation for $title",
        imageUrl = "https://image/$date.jpg",
        hdImageUrl = null,
        videoUrl = null,
        mediaType = "image",
        copyright = null,
        isFavorite = true,
    )
}

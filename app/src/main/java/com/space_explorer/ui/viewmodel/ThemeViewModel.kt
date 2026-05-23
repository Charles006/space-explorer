package com.space_explorer.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.space_explorer.data.preferences.ThemeMode
import com.space_explorer.data.preferences.ThemePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themePreferences: ThemePreferences
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = themePreferences.themeMode

    fun toggleTheme(isCurrentlyDark: Boolean) {
        val next = if (isCurrentlyDark) ThemeMode.LIGHT else ThemeMode.DARK
        themePreferences.setThemeMode(next)
    }

    fun setMode(mode: ThemeMode) {
        themePreferences.setThemeMode(mode)
    }
}

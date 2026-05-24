package com.space_explorer.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.space_explorer.domain.error.AstronomyError

@Composable
fun rememberErrorText(error: AstronomyError?): String? =
    error?.let { stringResource(it.messageRes, *it.messageArgs.toTypedArray()) }

package com.space_explorer.ui.components

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.SubcomposeAsyncImage

/**
 * Plays a NASA APOD video (YouTube / Vimeo embed) inside the app without
 * launching an external browser.
 *
 * UX flow:
 *   1. Show the thumbnail (if any) with a centered play button.
 *   2. On tap, swap to an in-process [WebView] loading the embed URL wrapped
 *      in [buildEmbedHtml] (autoplay + playsinline).
 *
 * The WebView is destroyed on leaving the composition to prevent background
 * playback and the classic WebView-leaks-the-Activity problem.
 *
 * Why WebView instead of ExoPlayer / Media3: NASA APOD videos are YouTube or
 * Vimeo embeds. Both providers' ToS forbid extracting the underlying stream,
 * so the official path is to render the provider's player — which is what an
 * iframe-in-WebView does. For the rare direct MP4, WebView also plays it via
 * HTML5 `<video>`, so coverage is universal with zero new dependencies.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EmbeddedVideoPlayer(
    embedUrl: String,
    thumbnailUrl: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    // rememberSaveable keyed on embedUrl so navigating between different
    // videos resets the play state, but recomposition for unrelated reasons
    // (e.g. parent updates) preserves it.
    var isPlaying by rememberSaveable(embedUrl) { mutableStateOf(false) }
    val webViewHolder = remember { mutableStateOf<WebView?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("embedded_video_player")
    ) {
        if (!isPlaying) {
            ThumbnailWithPlayButton(
                thumbnailUrl = thumbnailUrl,
                contentDescription = contentDescription,
                onPlay = { isPlaying = true }
            )
        } else {
            EmbeddedWebView(
                embedUrl = embedUrl,
                onWebViewCreated = { webViewHolder.value = it }
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewHolder.value?.apply {
                loadUrl("about:blank")
                onPause()
                removeAllViews()
                destroy()
            }
        }
    }
}

// region ── Internals ──────────────────────────────────────────────────────

@Composable
private fun ThumbnailWithPlayButton(
    thumbnailUrl: String,
    contentDescription: String,
    onPlay: () -> Unit
) {
    if (thumbnailUrl.isNotBlank()) {
        SubcomposeAsyncImage(
            model = thumbnailUrl,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onPlay)
            .testTag("embedded_video_play_button"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.PlayCircle,
            contentDescription = "Reproducir video",
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(80.dp)
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun EmbeddedWebView(
    embedUrl: String,
    onWebViewCreated: (WebView) -> Unit
) {
    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .testTag("embedded_video_webview"),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    loadWithOverviewMode = true
                    useWideViewPort = true
                }
                // Needed for fullscreen and certain provider scripts.
                webChromeClient = WebChromeClient()
                loadDataWithBaseURL(
                    "https://www.youtube.com",
                    buildEmbedHtml(embedUrl),
                    "text/html",
                    "utf-8",
                    null
                )
                onWebViewCreated(this)
            }
        }
    )
}

// endregion

/**
 * Wraps an embed [url] in a minimal HTML page with `autoplay=1` and
 * `playsinline=1` query parameters appended (without overriding existing
 * values). `internal` so it stays unit-testable from the same package.
 */
internal fun buildEmbedHtml(url: String): String {
    val withAutoplay = appendQueryParamIfMissing(url, "autoplay", "1")
    val withPlaysInline = appendQueryParamIfMissing(withAutoplay, "playsinline", "1")
    return """
        <!doctype html>
        <html>
          <head>
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <style>
              html, body, iframe { margin: 0; padding: 0; border: 0; height: 100vh; width: 100vw; background: #000; }
            </style>
          </head>
          <body>
            <iframe src="$withPlaysInline"
                    allow="autoplay; encrypted-media; picture-in-picture"
                    allowfullscreen></iframe>
          </body>
        </html>
    """.trimIndent()
}

/** Adds `key=value` to [url]'s query string only if `key=` is not already present. */
private fun appendQueryParamIfMissing(url: String, key: String, value: String): String {
    if (url.contains("$key=")) return url
    val separator = if (url.contains('?')) '&' else '?'
    return "$url$separator$key=$value"
}

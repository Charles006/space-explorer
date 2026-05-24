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

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EmbeddedVideoPlayer(
    embedUrl: String,
    thumbnailUrl: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
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
                webChromeClient = WebChromeClient()
                loadDataWithBaseURL(
                    "https://www.youtube-nocookie.com",
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

internal fun buildEmbedHtml(url: String): String {
    val embedUrl = normalizeToEmbedUrl(url)
    val withAutoplay = appendQueryParamIfMissing(embedUrl, "autoplay", "1")
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

internal fun normalizeToEmbedUrl(url: String): String {
    YOUTUBE_EMBED_REGEX.find(url)?.let { match ->
        val id = match.groupValues[1]
        val preservedQuery = match.groupValues[2]
        return YOUTUBE_NOCOOKIE_EMBED.format(id) + preservedQuery
    }
    YOUTUBE_WATCH_REGEX.find(url)?.let { match ->
        return YOUTUBE_NOCOOKIE_EMBED.format(match.groupValues[1])
    }
    YOUTU_BE_REGEX.find(url)?.let { match ->
        return YOUTUBE_NOCOOKIE_EMBED.format(match.groupValues[1])
    }
    VIMEO_REGEX.find(url)?.let { match ->
        return VIMEO_EMBED.format(match.groupValues[1])
    }
    return url
}

private val YOUTUBE_EMBED_REGEX =
    Regex("""(?:https?://)?(?:www\.)?youtube(?:-nocookie)?\.com/embed/([A-Za-z0-9_-]+)(\?[^\s]*)?""")
private val YOUTUBE_WATCH_REGEX =
    Regex("""(?:https?://)?(?:www\.)?youtube\.com/watch\?v=([A-Za-z0-9_-]+)""")
private val YOUTU_BE_REGEX =
    Regex("""(?:https?://)?(?:www\.)?youtu\.be/([A-Za-z0-9_-]+)""")
private val VIMEO_REGEX =
    Regex("""(?:https?://)?(?:www\.)?vimeo\.com/(\d+)""")

private const val YOUTUBE_NOCOOKIE_EMBED = "https://www.youtube-nocookie.com/embed/%s"
private const val VIMEO_EMBED = "https://player.vimeo.com/video/%s"

private fun appendQueryParamIfMissing(url: String, key: String, value: String): String {
    if (url.contains("$key=")) return url
    val separator = if (url.contains('?')) '&' else '?'
    return "$url$separator$key=$value"
}

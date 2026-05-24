package com.space_explorer.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.space_explorer.core.Constants
import com.space_explorer.domain.model.Astronomy
import com.space_explorer.ui.util.DateUtils

/**
 * List item card displaying an APOD with a cover image, title, date and a
 * favorite toggle.
 *
 * Composition concerns:
 *   * The full card is `clickable` to open the detail screen.
 *   * The favorite button has its own click region so taps don't bubble up
 *     into navigation — this is enforced by [IconButton] which absorbs taps.
 *   * Video items show a `PlayCircle` overlay so users see they will open
 *     external content; static images do not get the overlay.
 */
@Composable
fun ApodCard(
    astronomy: Astronomy,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable(onClick = onClick)
            .testTag("apod_card_${astronomy.id}"),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column {
            CardCover(astronomy)
            CardFooter(astronomy = astronomy, onToggleFavorite = onToggleFavorite)
        }
    }
}

// region ── Internals ──────────────────────────────────────────────────────

@Composable
private fun CardCover(astronomy: Astronomy) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(Constants.APOD_CARD_COVER_ASPECT_RATIO)
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
    ) {
        // Short-circuit: skip the Coil request entirely when we know the URL is
        // unusable (NASA sometimes returns blank urls). This avoids noisy Coil
        // error logs and a wasted HTTP roundtrip.
        if (astronomy.imageUrl.isBlank()) {
            CoverError()
        } else {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(astronomy.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = astronomy.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = { CoverPlaceholder() },
                error = { CoverError() }
            )
        }

        if (!astronomy.isImage) {
            VideoOverlay()
        }
    }
}

@Composable
private fun CoverPlaceholder() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxSize()
    ) {}
}

@Composable
private fun CoverError() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Icon(
            imageVector = Icons.Outlined.PlayCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun VideoOverlay() {
    Surface(
        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Outlined.PlayCircle,
                contentDescription = "Video",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(56.dp)
            )
        }
    }
}

@Composable
private fun CardFooter(astronomy: Astronomy, onToggleFavorite: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = astronomy.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = DateUtils.prettyPrint(astronomy.date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        FavoriteToggle(astronomy, onToggleFavorite)
    }
}

@Composable
private fun FavoriteToggle(astronomy: Astronomy, onToggle: () -> Unit) {
    IconButton(
        onClick = onToggle,
        modifier = Modifier.testTag("favorite_button_${astronomy.id}")
    ) {
        Icon(
            imageVector = if (astronomy.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
            contentDescription = if (astronomy.isFavorite) "Quitar de favoritos" else "Agregar a favoritos",
            tint = if (astronomy.isFavorite) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

// endregion

// region ── Previews ───────────────────────────────────────────────────────

private class AstronomyPreviewProvider : PreviewParameterProvider<Astronomy> {
    override val values = sequenceOf(
        sampleAstronomy(id = "1", title = "Mars Sunrise", mediaType = "image", isFavorite = false),
        sampleAstronomy(id = "2", title = "Saturn's Rings", mediaType = "video", isFavorite = true)
    )

    private fun sampleAstronomy(
        id: String,
        title: String,
        mediaType: String,
        isFavorite: Boolean
    ) = Astronomy(
        id = id,
        title = title,
        date = "2024-01-1$id",
        imageUrl = "https://example.com/$id.jpg",
        hdImageUrl = if (mediaType == "image") "https://example.com/$id-hd.jpg" else null,
        videoUrl = if (mediaType == "video") "https://youtube.com/embed/$id" else null,
        explanation = "Preview text for $title",
        copyright = "NASA",
        mediaType = mediaType,
        isFavorite = isFavorite
    )
}

@Preview(showBackground = true)
@Composable
private fun ApodCardPreview(
    @PreviewParameter(AstronomyPreviewProvider::class) astronomy: Astronomy
) {
    ApodCard(
        astronomy = astronomy,
        onClick = {},
        onToggleFavorite = {}
    )
}

// endregion

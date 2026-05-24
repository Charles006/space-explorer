package com.space_explorer.core

object Constants {
    /** Number of items requested per page from the NASA APOD API. */
    const val PAGE_SIZE: Int = 10

    /**
     * Distance (in items) from the end of the list at which the next page is
     * prefetched. Tuned for a balance between perceived smoothness and bandwidth.
     */
    const val PAGINATION_PREFETCH_DISTANCE: Int = 3

    /** Scroll threshold (item index) to show the "scroll to top" FAB. */
    const val SCROLL_TO_TOP_THRESHOLD: Int = 5

    /** HTTP connection and read timeout, in seconds. */
    const val HTTP_TIMEOUT_SECONDS: Long = 20L

    /** Keep [kotlinx.coroutines.flow.SharingStarted.WhileSubscribed] alive after the last subscriber, in ms. */
    const val STATE_FLOW_STOP_TIMEOUT_MS: Long = 5_000L

    /** Strict ISO-8601 date pattern enforced for NASA APOD queries. */
    const val ISO_DATE_PATTERN: String = "yyyy-MM-dd"

    /** Length of an ISO-8601 date used as a heuristic before regex validation. */
    const val ISO_DATE_LENGTH: Int = 10

    /** Aspect ratio for the hero image on the detail screen. */
    const val DETAIL_COVER_ASPECT_RATIO: Float = 4f / 3f

    /** Aspect ratio for cover images inside list cards. */
    const val APOD_CARD_COVER_ASPECT_RATIO: Float = 16f / 10f
}

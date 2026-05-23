package com.space_explorer.ui.navigation

sealed interface Destination {
    val route: String
}

object HomeDestination : Destination {
    override val route: String = "home"
}

object FavoritesDestination : Destination {
    override val route: String = "favorites"
}

object DetailDestination : Destination {
    const val ARG_ID = "astronomyId"
    override val route: String = "detail/{$ARG_ID}"
    fun build(id: String): String = "detail/$id"
}

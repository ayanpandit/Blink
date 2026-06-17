package com.ayanpandey.blink.core.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")

    data object Scanner : Screen("scanner")

    data object Pdf : Screen("pdf?uri={uri}") {
        fun createRoute(uri: String) = "pdf?uri=$uri"
    }

    data object Word : Screen("word?uri={uri}") {
        fun createRoute(uri: String) = "word?uri=$uri"
    }

    data object Excel : Screen("excel?uri={uri}") {
        fun createRoute(uri: String) = "excel?uri=$uri"
    }

    data object Ppt : Screen("ppt?uri={uri}") {
        fun createRoute(uri: String) = "ppt?uri=$uri"
    }

    data object Text : Screen("text?uri={uri}") {
        fun createRoute(uri: String) = "text?uri=$uri"
    }
}

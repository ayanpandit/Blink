package com.ayanpandey.blink.core.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    data object Home : Screen("home")

    data object Scanner : Screen("scanner")

    data object Pdf : Screen("pdf?uri={uri}") {
        fun createRoute(uri: String) = "pdf?uri=${Uri.encode(uri)}"
    }

    data object Word : Screen("word?uri={uri}") {
        fun createRoute(uri: String) = "word?uri=${Uri.encode(uri)}"
    }

    data object Excel : Screen("excel?uri={uri}") {
        fun createRoute(uri: String) = "excel?uri=${Uri.encode(uri)}"
    }

    data object Ppt : Screen("ppt?uri={uri}") {
        fun createRoute(uri: String) = "ppt?uri=${Uri.encode(uri)}"
    }

    data object Text : Screen("text?uri={uri}") {
        fun createRoute(uri: String) = "text?uri=${Uri.encode(uri)}"
    }

    data object Metadata : Screen("metadata?uri={uri}") {
        fun createRoute(uri: String) = "metadata?uri=${Uri.encode(uri)}"
    }

    data object Viewer : Screen("viewer?uri={uri}") {
        fun createRoute(uri: String) = "viewer?uri=${Uri.encode(uri)}"
    }
}

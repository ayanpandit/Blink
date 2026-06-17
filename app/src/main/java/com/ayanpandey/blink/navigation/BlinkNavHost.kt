package com.ayanpandey.blink.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ayanpandey.blink.core.navigation.Screen
import com.ayanpandey.blink.domain.di.DomainContainer
import com.ayanpandey.blink.feature.excel.ExcelScreen
import com.ayanpandey.blink.feature.home.HomeScreen
import com.ayanpandey.blink.feature.home.MetadataScreen
import com.ayanpandey.blink.feature.home.MetadataViewModel
import com.ayanpandey.blink.feature.pdf.PdfScreen
import com.ayanpandey.blink.feature.ppt.PptScreen
import com.ayanpandey.blink.feature.scanner.ScannerScreen
import com.ayanpandey.blink.feature.text.TextScreen
import com.ayanpandey.blink.feature.word.WordScreen

@Suppress("LongMethod")
@Composable
fun BlinkNavHost(
    navController: NavHostController,
    appContainer: DomainContainer,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier,
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToScanner = { navController.navigate(Screen.Scanner.route) },
                onFileSelected = { uri -> navController.navigate(Screen.Metadata.createRoute(uri)) },
                logger = appContainer.logger,
            )
        }
        composable(Screen.Scanner.route) {
            ScannerScreen()
        }
        composable(
            route = Screen.Pdf.route,
            arguments = listOf(navArgument("uri") { type = NavType.StringType }),
        ) { backStackEntry ->
            val uri = backStackEntry.arguments?.getString("uri") ?: ""
            PdfScreen(uri = uri)
        }
        composable(
            route = Screen.Word.route,
            arguments = listOf(navArgument("uri") { type = NavType.StringType }),
        ) { backStackEntry ->
            val uri = backStackEntry.arguments?.getString("uri") ?: ""
            WordScreen(uri = uri)
        }
        composable(
            route = Screen.Excel.route,
            arguments = listOf(navArgument("uri") { type = NavType.StringType }),
        ) { backStackEntry ->
            val uri = backStackEntry.arguments?.getString("uri") ?: ""
            ExcelScreen(uri = uri)
        }
        composable(
            route = Screen.Ppt.route,
            arguments = listOf(navArgument("uri") { type = NavType.StringType }),
        ) { backStackEntry ->
            val uri = backStackEntry.arguments?.getString("uri") ?: ""
            PptScreen(uri = uri)
        }
        composable(
            route = Screen.Text.route,
            arguments = listOf(navArgument("uri") { type = NavType.StringType }),
        ) { backStackEntry ->
            val uri = backStackEntry.arguments?.getString("uri") ?: ""
            TextScreen(uri = uri)
        }
        composable(
            route = Screen.Metadata.route,
            arguments = listOf(navArgument("uri") { type = NavType.StringType }),
        ) { backStackEntry ->
            val uri = backStackEntry.arguments?.getString("uri") ?: ""
            appContainer.logger.d("BlinkNavHost", "Metadata route received | uri=$uri")
            val viewModel =
                androidx.lifecycle.viewmodel.compose.viewModel {
                    MetadataViewModel(appContainer.fileResolver)
                }
            MetadataScreen(
                uriString = uri,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onOpenClick = { targetUri ->
                    appContainer.logger.d("BlinkNavHost",
                        "onOpenClick | targetUri=$targetUri")
                    val route = Screen.Viewer.createRoute(targetUri)
                    appContainer.logger.d("BlinkNavHost",
                        "onOpenClick | generatedRoute=$route")
                    navController.navigate(route)
                }
            )
        }
        composable(
            route = Screen.Viewer.route,
            arguments = listOf(navArgument("uri") { type = NavType.StringType }),
        ) { backStackEntry ->
            val uri = backStackEntry.arguments?.getString("uri") ?: ""
            appContainer.logger.d("BlinkNavHost",
                "Viewer route received | uri=$uri | uriLength=${uri.length}")
            val viewModel = androidx.lifecycle.viewmodel.compose.viewModel {
                com.ayanpandey.blink.feature.viewer.ViewerViewModel(
                    appContainer.documentViewer,
                    emptyList()
                )
            }
            com.ayanpandey.blink.feature.viewer.ViewerScreen(
                uriString = uri,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

package com.ayanpandey.blink.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ayanpandey.blink.core.navigation.Screen
import com.ayanpandey.blink.feature.excel.ExcelScreen
import com.ayanpandey.blink.feature.home.HomeScreen
import com.ayanpandey.blink.feature.pdf.PdfScreen
import com.ayanpandey.blink.feature.ppt.PptScreen
import com.ayanpandey.blink.feature.scanner.ScannerScreen
import com.ayanpandey.blink.feature.text.TextScreen
import com.ayanpandey.blink.feature.word.WordScreen

@Composable
fun BlinkNavHost(
    navController: NavHostController,
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
                onNavigateToPdf = { uri -> navController.navigate(Screen.Pdf.createRoute(uri)) },
                onNavigateToWord = { uri -> navController.navigate(Screen.Word.createRoute(uri)) },
                onNavigateToExcel = { uri -> navController.navigate(Screen.Excel.createRoute(uri)) },
                onNavigateToPpt = { uri -> navController.navigate(Screen.Ppt.createRoute(uri)) },
                onNavigateToText = { uri -> navController.navigate(Screen.Text.createRoute(uri)) },
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
    }
}

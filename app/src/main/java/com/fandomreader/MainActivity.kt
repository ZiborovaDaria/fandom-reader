package com.fandomreader

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fandomreader.feature.importbook.ImportRoute
import com.fandomreader.feature.library.AppThemePreferencesRepository
import com.fandomreader.feature.library.FilterBuilderRoute
import com.fandomreader.feature.library.FilterResultsRoute
import com.fandomreader.feature.library.LibraryRoute
import com.fandomreader.feature.library.SettingsRoute
import com.fandomreader.feature.library.WorkMetaRoute
import com.fandomreader.domain.model.LibraryFilterCriteria
import com.fandomreader.feature.reader.ReaderRoute
import com.fandomreader.ui.theme.AppThemeMode
import com.fandomreader.ui.theme.FandomReaderTheme
import dagger.hilt.android.AndroidEntryPoint
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var appThemePreferences: AppThemePreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = AndroidColor.TRANSPARENT,
                darkScrim = AndroidColor.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = AndroidColor.TRANSPARENT,
                darkScrim = AndroidColor.TRANSPARENT,
            ),
        )
        setContent {
            val themeMode by appThemePreferences.themeMode.collectAsStateWithLifecycle(
                initialValue = AppThemeMode.System,
            )
            val darkTheme = when (themeMode) {
                AppThemeMode.Light -> false
                AppThemeMode.Dark -> true
                AppThemeMode.System -> isSystemInDarkTheme()
            }
            FandomReaderTheme(darkTheme = darkTheme) {
                FandomReaderNav()
            }
        }
    }
}

@Composable
fun FandomReaderNav() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "library") {
        composable("library") {
            LibraryRoute(
                onOpenWork = { nav.navigate("work/$it") },
                onImport = { nav.navigate("import") },
                onOpenFilter = { nav.navigate("filters") },
                onOpenSettings = { nav.navigate("settings") },
            )
        }
        composable("filters") {
            FilterBuilderRoute(
                onApply = { criteria ->
                    val encoded = URLEncoder.encode(criteria.encode(), StandardCharsets.UTF_8.toString())
                    nav.navigate("filter-results/$encoded")
                },
                onBack = { nav.popBackStack() },
            )
        }
        composable(
            "filter-results/{criteria}",
            arguments = listOf(navArgument("criteria") { type = NavType.StringType }),
        ) { entry ->
            val raw = entry.arguments!!.getString("criteria")!!
            val decoded = URLDecoder.decode(raw, StandardCharsets.UTF_8.toString())
            val criteria = LibraryFilterCriteria.decode(decoded)
            FilterResultsRoute(
                criteria = criteria,
                onOpenWork = { nav.navigate("work/$it") },
                onBack = { nav.popBackStack() },
            )
        }
        composable("settings") {
            SettingsRoute(onBack = { nav.popBackStack() })
        }
        composable("import") {
            ImportRoute(onDone = { nav.popBackStack() })
        }
        composable(
            "work/{workId}",
            arguments = listOf(navArgument("workId") { type = NavType.LongType }),
        ) { entry ->
            val workId = entry.arguments!!.getLong("workId")
            WorkMetaRoute(
                workId = workId,
                onRead = { nav.navigate("reader/$it") },
                onBack = { nav.popBackStack() },
            )
        }
        composable(
            "reader/{workId}",
            arguments = listOf(navArgument("workId") { type = NavType.LongType }),
        ) { entry ->
            val workId = entry.arguments!!.getLong("workId")
            ReaderRoute(
                workId = workId,
                onBack = { nav.popBackStack() },
                onOpenMeta = {
                    val restored = nav.popBackStack("work/$workId", inclusive = false)
                    if (!restored) {
                        nav.navigate("work/$workId") {
                            launchSingleTop = true
                            popUpTo("reader/$workId") { inclusive = true }
                        }
                    }
                },
            )
        }
    }
}

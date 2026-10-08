package com.mhss.app.mybrain.presentation.main.components

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.mhss.app.mybrain.presentation.app_lock.AppLockManager
import com.mhss.app.mybrain.presentation.main.DashboardScreen
import com.mhss.app.mybrain.presentation.main.SettingsScreen
import com.mhss.app.mybrain.presentation.main.SpacesScreen
import com.mhss.app.ui.navigation.Screen
import com.mhss.app.mybrain.presentation.search.GlobalSearchScreen
import com.mhss.app.presentation.PermissionsScreen

import com.mhss.app.presentation.SettingsViewModel
import org.koin.androidx.compose.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mhss.app.preferences.domain.model.PrefsKey
import androidx.datastore.preferences.core.intPreferencesKey
import com.mhss.app.preferences.PrefsConstants
import com.mhss.app.mybrain.presentation.main.SpacesScreenGrid


@Composable
fun NavigationGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    mainNavController: NavHostController,
    startUpScreen: Screen,
    appLockManager: AppLockManager
) {
    NavHost(modifier = modifier, navController = navController, startDestination = startUpScreen){

        composable<Screen.DashboardScreen>(
            enterTransition = { fadeIn(tween(0)) },
            exitTransition = { fadeOut(tween(0)) },
        ) {
            DashboardScreen(mainNavController)
        }
        composable<Screen.SpacesScreen>(
            enterTransition = { fadeIn(tween(0)) },
            exitTransition = { fadeOut(tween(0)) },
        ) {

            val settingsViewModel: SettingsViewModel = koinViewModel()

            val menuStyle = settingsViewModel
                .getSettings(
                    PrefsKey.IntKey(PrefsConstants.MENU_STYLE_KEY),
                    0
                )
                .collectAsStateWithLifecycle(0)

            if (menuStyle.value == 1)
            {
                SpacesScreenGrid(mainNavController)
            } else {
                SpacesScreen(mainNavController)
            }
        }

        composable<Screen.PermissionsScreen>(
            enterTransition = { fadeIn(tween(0)) },
            exitTransition = { fadeOut(tween(0)) },
        ) {
            PermissionsScreen()
        }
        composable<Screen.SettingsScreen>(
            enterTransition = { fadeIn(tween(0)) },
            exitTransition = { fadeOut(tween(0)) },
        ) {
            SettingsScreen(mainNavController, appLockManager)
        }
        composable<Screen.GlobalSearchScreen>(
            enterTransition = { fadeIn(tween(0)) },
            exitTransition = { fadeOut(tween(0)) },
        ) {
            GlobalSearchScreen(mainNavController)
        }
    }
}
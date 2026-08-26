package com.radiantengineering.orbittime.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.radiantengineering.orbittime.ui.screens.common.OrbitPlaceholderScreen
import com.radiantengineering.orbittime.ui.screens.welcome.WelcomeScreen
import com.radiantengineering.orbittime.ui.screens.welcome.rememberWelcomeScreenStrings

/** Hosts the app's screens and owns the single [NavHostController] that routes between them. */
@Composable
fun OrbitNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = OrbitDestinations.WELCOME,
        modifier = modifier,
    ) {
        composable(OrbitDestinations.WELCOME) {
            WelcomeScreen(
                strings = rememberWelcomeScreenStrings(),
                onSignInClick = { navController.navigate(OrbitDestinations.SIGN_IN) },
                onCreateAccountClick = { navController.navigate(OrbitDestinations.CREATE_ACCOUNT) },
            )
        }
        composable(OrbitDestinations.SIGN_IN) {
            OrbitPlaceholderScreen(
                title = "Sign in",
                onBackClick = { navController.popBackStack() },
            )
        }
        composable(OrbitDestinations.CREATE_ACCOUNT) {
            OrbitPlaceholderScreen(
                title = "Create account",
                onBackClick = { navController.popBackStack() },
            )
        }
    }
}

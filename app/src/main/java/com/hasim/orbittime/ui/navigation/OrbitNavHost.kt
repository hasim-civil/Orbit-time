package com.hasim.orbittime.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.hasim.orbittime.ui.screens.auth.CreateAccountScreen
import com.hasim.orbittime.ui.screens.auth.SignInScreen
import com.hasim.orbittime.ui.screens.main.MainScreen
import com.hasim.orbittime.ui.screens.welcome.WelcomeScreen
import com.hasim.orbittime.ui.screens.welcome.rememberWelcomeScreenStrings

/** Hosts the app's screens and owns the single [NavHostController] that routes between them. */
@Composable
fun OrbitNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    // A signed-in session survives app restarts, so a returning user skips
    // straight past Welcome / Sign In to Home.
    val startDestination = remember {
        if (FirebaseAuth.getInstance().currentUser != null) {
            OrbitDestinations.HOME
        } else {
            OrbitDestinations.WELCOME
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
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
            SignInScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToCreateAccount = {
                    navController.navigate(OrbitDestinations.CREATE_ACCOUNT) {
                        popUpTo(OrbitDestinations.SIGN_IN) { inclusive = true }
                    }
                },
                onSignedIn = { navController.navigateToHomeClearingAuthStack() },
            )
        }
        composable(OrbitDestinations.CREATE_ACCOUNT) {
            CreateAccountScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToSignIn = {
                    navController.navigate(OrbitDestinations.SIGN_IN) {
                        popUpTo(OrbitDestinations.CREATE_ACCOUNT) { inclusive = true }
                    }
                },
                onAccountCreated = { navController.navigateToHomeClearingAuthStack() },
            )
        }
        composable(OrbitDestinations.HOME) {
            MainScreen(
                onLoggedOut = {
                    navController.navigate(OrbitDestinations.WELCOME) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
            )
        }
    }
}

private fun NavHostController.navigateToHomeClearingAuthStack() {
    navigate(OrbitDestinations.HOME) {
        popUpTo(OrbitDestinations.WELCOME) { inclusive = true }
    }
}

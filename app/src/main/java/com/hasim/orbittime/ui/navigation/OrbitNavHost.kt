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
import com.hasim.orbittime.ui.screens.auth.VerifyEmailScreen
import com.hasim.orbittime.ui.screens.main.MainScreen
import com.hasim.orbittime.ui.screens.welcome.WelcomeScreen
import com.hasim.orbittime.ui.screens.welcome.rememberWelcomeScreenStrings

/**
 * Where a currently-signed-in Firebase user (if any) belongs: signed out -> Welcome, signed in
 * but not yet verified -> the Verify Email gate, signed in and verified -> Home. Used both for
 * the app's cold-start destination and right after a sign-in/registration succeeds, so an
 * unverified account can never land on Home by either path. Google accounts are always
 * `isEmailVerified == true` from Firebase itself, so this never gates them.
 */
private fun destinationForCurrentUser(): String {
    val user = FirebaseAuth.getInstance().currentUser
    return when {
        user == null -> OrbitDestinations.WELCOME
        user.isEmailVerified -> OrbitDestinations.HOME
        else -> OrbitDestinations.VERIFY_EMAIL
    }
}

/** Hosts the app's screens and owns the single [NavHostController] that routes between them. */
@Composable
fun OrbitNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    // A signed-in session survives app restarts, so a returning verified user skips straight
    // past Welcome / Sign In to Home — and a returning unverified one lands on the Verify Email
    // gate instead of Home, exactly as if they'd just signed in.
    val startDestination = remember { destinationForCurrentUser() }

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
                onSignedIn = { navController.navigateAfterAuthClearingAuthStack() },
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
                // A brand-new email/password account is never pre-verified, so this always
                // lands on Verify Email in practice; routing through the same shared helper as
                // Sign In keeps a (Google, or already-verified) account from being sent there
                // needlessly.
                onAccountCreated = { navController.navigateAfterAuthClearingAuthStack() },
            )
        }
        composable(OrbitDestinations.VERIFY_EMAIL) {
            VerifyEmailScreen(
                onVerified = {
                    navController.navigate(OrbitDestinations.HOME) {
                        popUpTo(OrbitDestinations.WELCOME) { inclusive = true }
                    }
                },
                onSignedOut = {
                    navController.navigate(OrbitDestinations.WELCOME) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
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

/** Sends a just-authenticated user (sign-in or registration) to Home if verified, or the Verify
 * Email gate if not, clearing the Welcome/Sign In/Create Account back stack either way so the
 * back button never returns to a pre-auth screen. */
private fun NavHostController.navigateAfterAuthClearingAuthStack() {
    navigate(destinationForCurrentUser()) {
        popUpTo(OrbitDestinations.WELCOME) { inclusive = true }
    }
}

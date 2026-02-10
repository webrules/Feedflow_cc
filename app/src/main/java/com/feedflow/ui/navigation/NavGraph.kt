package com.feedflow.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.feedflow.ui.bookmarks.BookmarksScreen
import com.feedflow.ui.browser.InAppBrowserScreen
import com.feedflow.ui.browser.LoginBrowserScreen
import com.feedflow.ui.communities.CommunitiesScreen
import com.feedflow.ui.detail.ThreadDetailScreen
import com.feedflow.ui.home.SiteListScreen
import com.feedflow.ui.image.FullScreenImageScreen
import com.feedflow.ui.login.LoginScreen
import com.feedflow.ui.settings.SettingsScreen
import com.feedflow.ui.threads.ThreadListScreen
import java.net.URLDecoder

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            SiteListScreen(
                onSiteClick = { site ->
                    navController.navigate(Screen.Communities.createRoute(site.id))
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
                onBookmarksClick = {
                    navController.navigate(Screen.Bookmarks.route)
                },
                onLoginClick = {
                    navController.navigate(Screen.Login.route)
                }
            )
        }

        composable(
            route = Screen.Communities.route,
            arguments = listOf(navArgument("siteId") { type = NavType.StringType })
        ) { backStackEntry ->
            val siteId = backStackEntry.arguments?.getString("siteId") ?: return@composable
            CommunitiesScreen(
                siteId = siteId,
                onCommunityClick = { community ->
                    navController.navigate(Screen.ThreadList.createRoute(siteId, community.id))
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ThreadList.route,
            arguments = listOf(
                navArgument("siteId") { type = NavType.StringType },
                navArgument("communityId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val siteId = backStackEntry.arguments?.getString("siteId") ?: return@composable
            val encodedCommunityId = backStackEntry.arguments?.getString("communityId") ?: return@composable
            val communityId = URLDecoder.decode(encodedCommunityId, "UTF-8")
            ThreadListScreen(
                siteId = siteId,
                communityId = communityId,
                onThreadClick = { thread ->
                    navController.navigate(Screen.ThreadDetail.createRoute(siteId, thread.id))
                },
                onBackClick = { navController.popBackStack() },
                onHomeClick = { navController.popBackStack(Screen.Home.route, false) }
            )
        }

        composable(
            route = Screen.ThreadDetail.route,
            arguments = listOf(
                navArgument("siteId") { type = NavType.StringType },
                navArgument("threadId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val siteId = backStackEntry.arguments?.getString("siteId") ?: return@composable
            val encodedThreadId = backStackEntry.arguments?.getString("threadId") ?: return@composable
            val threadId = URLDecoder.decode(encodedThreadId, "UTF-8")
            ThreadDetailScreen(
                siteId = siteId,
                threadId = threadId,
                onBackClick = { navController.popBackStack() },
                onHomeClick = { navController.popBackStack(Screen.Home.route, false) },
                onLinkClick = { url ->
                    navController.navigate(Screen.Browser.createRoute(url))
                },
                onImageClick = { url ->
                    navController.navigate(Screen.FullScreenImage.createRoute(url))
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Bookmarks.route) {
            BookmarksScreen(
                onBackClick = { navController.popBackStack() },
                onThreadClick = { thread, serviceId ->
                    navController.navigate(Screen.ThreadDetail.createRoute(serviceId, thread.id))
                    // thread.id is URL-encoded inside createRoute
                },
                onUrlClick = { url ->
                    navController.navigate(Screen.Browser.createRoute(url))
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onBackClick = { navController.popBackStack() },
                onSiteLoginClick = { site, loginUrl ->
                    navController.navigate(Screen.LoginBrowser.createRoute(site.id, loginUrl))
                }
            )
        }

        composable(
            route = Screen.LoginBrowser.route,
            arguments = listOf(
                navArgument("siteId") { type = NavType.StringType },
                navArgument("url") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val siteId = backStackEntry.arguments?.getString("siteId") ?: return@composable
            val encodedUrl = backStackEntry.arguments?.getString("url") ?: return@composable
            val url = URLDecoder.decode(encodedUrl, "UTF-8")
            LoginBrowserScreen(
                siteId = siteId,
                url = url,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Browser.route,
            arguments = listOf(navArgument("url") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("url") ?: return@composable
            val url = URLDecoder.decode(encodedUrl, "UTF-8")
            InAppBrowserScreen(
                url = url,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.FullScreenImage.route,
            arguments = listOf(navArgument("url") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("url") ?: return@composable
            val url = URLDecoder.decode(encodedUrl, "UTF-8")
            FullScreenImageScreen(
                imageUrl = url,
                onDismiss = { navController.popBackStack() }
            )
        }
    }
}

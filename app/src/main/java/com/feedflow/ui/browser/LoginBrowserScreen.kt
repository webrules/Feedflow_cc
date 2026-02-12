package com.feedflow.ui.browser

import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.feedflow.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginBrowserScreen(
    siteId: String,
    url: String,
    onBackClick: () -> Unit,
    onLoginSuccess: (String) -> Unit = {},
    viewModel: LoginBrowserViewModel = hiltViewModel()
) {
    var isLoading by remember { mutableStateOf(true) }
    var pageTitle by remember { mutableStateOf("") }
    var webView: WebView? by remember { mutableStateOf(null) }
    var loginSaved by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = pageTitle.ifBlank { stringResource(R.string.login) },
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AndroidView(
                factory = { context ->
                    // Enable cookies globally for WebView
                    CookieManager.getInstance().apply {
                        setAcceptCookie(true)
                    }

                    WebView(context).apply {
                        // Accept third-party cookies for this WebView
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(
                                view: WebView?,
                                url: String?,
                                favicon: android.graphics.Bitmap?
                            ) {
                                isLoading = true
                            }

                            override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                                isLoading = false
                                pageTitle = view?.title ?: ""

                                // Always try to capture cookies on every page load
                                // This ensures we catch cookies regardless of redirect patterns
                                if (!loginSaved && finishedUrl != null) {
                                    val cookies = extractCookiesForSite(siteId)
                                    if (cookies.isNotEmpty() && isLoginSuccess(siteId, finishedUrl, cookies)) {
                                        val cookieHeader = cookies.joinToString("; ")
                                        viewModel.saveCookies(siteId, cookieHeader)
                                        loginSaved = true
                                        android.util.Log.d("LoginBrowser", "Saved ${cookies.size} cookies for $siteId: ${cookies.take(3)}")
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Login successful!")
                                            onLoginSuccess(siteId)
                                        }
                                    }
                                }
                            }
                        }
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            builtInZoomControls = true
                            displayZoomControls = false
                            userAgentString = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                        }

                        webView = this
                        loadUrl(url)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

private fun extractCookiesForSite(siteId: String): Set<String> {
    val cookieManager = CookieManager.getInstance()
    val domains = when (siteId) {
        "hackernews" -> listOf("https://news.ycombinator.com")
        "4d4y" -> listOf("https://www.4d4y.com", "https://4d4y.com")
        "v2ex" -> listOf("https://www.v2ex.com", "https://v2ex.com")
        "linux_do" -> listOf("https://linux.do")
        "zhihu" -> listOf("https://www.zhihu.com", "https://zhihu.com")
        "nodeseek" -> listOf("https://www.nodeseek.com", "https://nodeseek.com")
        else -> emptyList()
    }

    val allCookies = mutableSetOf<String>()
    for (domain in domains) {
        val cookieString = cookieManager.getCookie(domain)
        if (!cookieString.isNullOrBlank()) {
            cookieString.split(";").forEach { cookie ->
                allCookies.add(cookie.trim())
            }
        }
    }
    return allCookies
}

private fun isLoginSuccess(siteId: String, url: String, cookies: Set<String>): Boolean {
    // Check both URL patterns AND the presence of auth-related cookies
    val cookieNames = cookies.map { it.substringBefore("=").trim().lowercase() }.toSet()

    return when (siteId) {
        "hackernews" -> {
            // HN sets a "user" cookie on login
            cookieNames.contains("user") &&
                    (url.contains("ycombinator.com") && !url.contains("/login"))
        }
        "4d4y" -> {
            // 4D4Y Discuz sets session cookies; any page away from login page means success
            // Key cookies: cdb_sid, cdb_auth, cdb_cookietime
            val hasAuthCookie = cookieNames.any { it.contains("auth") || it.contains("sid") || it.contains("saltkey") }
            hasAuthCookie && url.contains("4d4y.com") && !url.contains("logging.php?action=login")
        }
        "v2ex" -> {
            // V2EX sets A2 cookie on login
            val hasAuthCookie = cookieNames.any { it == "a2" || it == "v2ex_tab" }
            hasAuthCookie && url.contains("v2ex.com") && !url.contains("/signin")
        }
        "linux_do" -> {
            // Discourse sets _t cookie
            val hasAuthCookie = cookieNames.any { it == "_t" || it.contains("_forum_session") }
            hasAuthCookie && url.contains("linux.do") && !url.contains("/login")
        }
        "zhihu" -> {
            // Zhihu sets z_c0 cookie on login
            val hasAuthCookie = cookieNames.contains("z_c0")
            hasAuthCookie && url.contains("zhihu.com") && !url.contains("/signin")
        }
        "nodeseek" -> {
            // NodeSeek sets session cookie on login
            val hasAuthCookie = cookieNames.any { it.contains("session") || it.contains("token") || it.contains("auth") }
            hasAuthCookie && url.contains("nodeseek.com") && !url.contains("/signIn")
        }
        else -> false
    }
}

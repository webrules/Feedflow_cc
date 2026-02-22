package com.feedflow.ui.browser

import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.feedflow.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudflareChallengeScreen(
    siteId: String,
    siteUrl: String,
    onBackClick: () -> Unit,
    onChallengeSolved: () -> Unit,
    viewModel: LoginBrowserViewModel = hiltViewModel()
) {
    var isLoading by remember { mutableStateOf(true) }
    var challengeSolved by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verifying...") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isLoading) {
                    Spacer(modifier = Modifier.height(32.dp))
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Checking security...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // WebView for solving Cloudflare challenge
                AndroidView(
                    factory = { context ->
                        CookieManager.getInstance().apply {
                            setAcceptCookie(true)
                        }

                        WebView(context).apply {
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    // Check if we have Cloudflare clearance cookies
                                    scope.launch {
                                        delay(2000) // Wait for challenge to complete

                                        val cookies = extractCloudflareCookies(siteUrl)
                                        val hasClearance = cookies.any { it.contains("cf_clearance") }

                                        if (hasClearance || !isChallengePage(url)) {
                                            // Save all cookies
                                            val cookieHeader = cookies.joinToString("; ")
                                            viewModel.saveCookies(siteId, cookieHeader)

                                            challengeSolved = true
                                            isLoading = false
                                            delay(500)
                                            onChallengeSolved()
                                        }
                                    }
                                }
                            }

                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 7a) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                            }

                            loadUrl(siteUrl)
                        }
                    },
                    modifier = if (challengeSolved) {
                        Modifier.height(1.dp) // Hide after solved
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    }
                )
            }
        }
    }
}

private fun extractCloudflareCookies(siteUrl: String): Set<String> {
    val cookieManager = CookieManager.getInstance()
    val domains = listOf(siteUrl, siteUrl.replace("https://", "https://www."))

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

private fun isChallengePage(url: String?): Boolean {
    if (url == null) return false
    return url.contains("/cdn-cgi/challenge") ||
            url.contains("/cdn-cgi/l/chk_captcha") ||
            url.contains("turnstile") ||
            url.contains("hcaptcha") ||
            url.contains("recaptcha")
}

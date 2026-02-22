package com.feedflow.ui.communities

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.feedflow.R
import com.feedflow.data.model.Community
import com.feedflow.data.model.ForumSite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunitiesScreen(
    siteId: String,
    onCommunityClick: (Community) -> Unit,
    onBackClick: () -> Unit,
    onLoginClick: ((String) -> Unit)? = null,
    onCloudflareChallenge: ((String, String) -> Unit)? = null,
    viewModel: CommunitiesViewModel = hiltViewModel()
) {
    val communities by viewModel.communities.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    val site = ForumSite.fromId(siteId)
    val isCloudflareError = error?.contains("403") == true || error?.contains("Cloudflare") == true

    // Refresh login status whenever this screen becomes visible
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshLoginStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(siteId) {
        site?.let { viewModel.loadCommunities(it) }
    }

    val showLoginButton = viewModel.requiresLogin() && !isLoggedIn && onLoginClick != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(site?.let { stringResource(it.displayNameRes) } ?: stringResource(R.string.communities))
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (showLoginButton) {
                        IconButton(onClick = { onLoginClick?.invoke(siteId) }) {
                            Icon(Icons.Default.Person, contentDescription = stringResource(R.string.login))
                        }
                    }
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
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
            when {
                isLoading && communities.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                error != null && communities.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = error ?: stringResource(R.string.error_loading),
                            color = MaterialTheme.colorScheme.error
                        )
                        if (isCloudflareError && onCloudflareChallenge != null && site != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            IconButton(
                                onClick = { onCloudflareChallenge(siteId, site.baseUrl) }
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = "Verify Security"
                                )
                            }
                            Text(
                                text = "Tap to verify security",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                communities.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.no_communities),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(communities) { community ->
                            CommunityItem(
                                community = community,
                                onClick = { onCommunityClick(community) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityItem(
    community: Community,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = community.name,
                style = MaterialTheme.typography.titleMedium
            )
        },
        supportingContent = if (community.description.isNotBlank()) {
            {
                Text(
                    text = community.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        } else null,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    )
}

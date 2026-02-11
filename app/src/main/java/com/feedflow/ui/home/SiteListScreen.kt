package com.feedflow.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.feedflow.R
import com.feedflow.data.model.ForumSite
import com.feedflow.ui.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SiteListScreen(
    onSiteClick: (ForumSite) -> Unit,
    onSettingsClick: () -> Unit,
    onBookmarksClick: () -> Unit,
    onLoginClick: () -> Unit,
    onCoverClick: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    siteListViewModel: SiteListViewModel = hiltViewModel()
) {
    val isDarkMode by settingsViewModel.isDarkMode.collectAsState()
    val language by settingsViewModel.language.collectAsState()
    val sites by siteListViewModel.sites.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    IconButton(onClick = onCoverClick) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = stringResource(R.string.ai_summary_toggle)
                        )
                    }
                    IconButton(onClick = onBookmarksClick) {
                        Icon(
                            Icons.Default.Bookmark,
                            contentDescription = stringResource(R.string.bookmarks)
                        )
                    }
                    IconButton(onClick = { settingsViewModel.toggleLanguage() }) {
                        Text(
                            text = if (language == "en") "EN" else "\u4E2D",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    IconButton(onClick = { settingsViewModel.toggleDarkMode() }) {
                        Icon(
                            painter = painterResource(
                                if (isDarkMode) R.drawable.ic_dark_mode else R.drawable.ic_light_mode
                            ),
                            contentDescription = stringResource(R.string.dark_mode)
                        )
                    }
                    IconButton(onClick = onLoginClick) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = stringResource(R.string.login)
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            SiteGrid(sites = sites, onSiteClick = onSiteClick)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SiteGrid(
    sites: List<ForumSite>,
    onSiteClick: (ForumSite) -> Unit
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        maxItemsInEachRow = 2,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        sites.forEach { site ->
            SiteCard(
                site = site,
                onClick = { onSiteClick(site) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SiteCard(
    site: ForumSite,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(120.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(site.iconRes),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(site.displayNameRes),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

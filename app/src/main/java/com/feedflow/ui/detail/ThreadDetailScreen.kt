package com.feedflow.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.feedflow.R
import com.feedflow.data.model.Comment
import com.feedflow.ui.common.LinkedTextView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadDetailScreen(
    siteId: String,
    threadId: String,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    onLinkClick: (String) -> Unit,
    onImageClick: (String) -> Unit,
    viewModel: ThreadDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    var replyText by remember { mutableStateOf("") }

    LaunchedEffect(siteId, threadId) {
        viewModel.loadThread(threadId, siteId)
    }

    val thread = (state as? ThreadDetailState.Loaded)?.thread
    val comments = (state as? ThreadDetailState.Loaded)?.comments ?: emptyList()
    val isLoading = state is ThreadDetailState.Loading
    val isLoadingMore = (state as? ThreadDetailState.Loaded)?.isLoadingMore ?: false
    val isFresh = (state as? ThreadDetailState.Loaded)?.isFresh ?: false
    val isBookmarked = (state as? ThreadDetailState.Loaded)?.isBookmarked ?: false
    val replyingTo = (state as? ThreadDetailState.Loaded)?.replyingTo
    val error = (state as? ThreadDetailState.Error)?.message

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = thread?.community?.name?.ifBlank { null }
                            ?: stringResource(R.string.communities)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (thread != null) {
                        Icon(
                            imageVector = if (isFresh) Icons.Default.CloudDone else Icons.Default.PhoneAndroid,
                            contentDescription = if (isFresh) "Latest content" else "Cached content",
                            tint = if (isFresh) Color(0xFF4CAF50) else Color(0xFFFF9800),
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 4.dp)
                        )
                    }
                    IconButton(onClick = { viewModel.toggleBookmark() }) {
                        Icon(
                            if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = stringResource(R.string.bookmarks)
                        )
                    }
                    IconButton(onClick = { viewModel.generateSummary() }) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = stringResource(R.string.ai_assistant)
                        )
                    }
                    IconButton(onClick = onHomeClick) {
                        Icon(Icons.Default.Home, contentDescription = stringResource(R.string.home))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (viewModel.getService()?.supportsPosting() == true) {
                ReplyBar(
                    replyText = replyText,
                    onReplyTextChange = { replyText = it },
                    replyingTo = replyingTo,
                    onCancelReply = { viewModel.selectReplyTarget(null) },
                    isLoading = isLoading,
                    onSend = {
                        if (replyText.isNotBlank()) {
                            viewModel.sendReply(replyText)
                            replyText = ""
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            when {
                isLoading && thread == null -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                error != null && thread == null -> {
                    Text(
                        text = error ?: stringResource(R.string.error_loading),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                thread != null -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Thread content
                        item {
                            ThreadContent(
                                thread = thread!!,
                                siteId = siteId,
                                onLinkClick = onLinkClick,
                                onImageClick = onImageClick
                            )
                        }

                        // Comments section (hide for RSS feeds which don't support comments)
                        if (siteId != "rss") {
                            item {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                                Text(
                                    text = stringResource(R.string.comments),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // Comments
                            if (comments.isEmpty()) {
                                item {
                                    Text(
                                        text = stringResource(R.string.no_comments),
                                        modifier = Modifier.padding(16.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                itemsIndexed(comments, key = { index, comment -> "${comment.id}_$index" }) { index, comment ->
                                    CommentItem(
                                        comment = comment,
                                        onReplyClick = { viewModel.selectReplyTarget(comment) },
                                        onLinkClick = onLinkClick,
                                        onImageClick = onImageClick
                                    )
                                    // Trigger loading next page when last comment appears
                                    if (index == comments.lastIndex) {
                                        LaunchedEffect(comments.size) {
                                            viewModel.loadMoreComments()
                                        }
                                    }
                                }
                            }
                        }

                        // Loading indicator for pagination
                        if (isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThreadContent(
    thread: com.feedflow.data.model.ForumThread,
    siteId: String,
    onLinkClick: (String) -> Unit,
    onImageClick: (String) -> Unit
) {
    val isLongForm = siteId in listOf("rss", "zhihu")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Author (hide for RSS feeds with no author)
        if (thread.author.username.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (thread.author.avatar.isNotBlank()) {
                    AsyncImage(
                        model = thread.author.avatar,
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(
                    text = thread.author.username,
                    style = MaterialTheme.typography.titleSmall
                )
                if (thread.timeAgo.isNotBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = thread.timeAgo,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Title
        Text(
            text = thread.title,
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Content
        LinkedTextView(
            content = thread.content,
            onLinkClick = onLinkClick,
            onImageClick = onImageClick,
            lineHeight = if (isLongForm) 28.sp else TextUnit.Unspecified
        )
    }
}

@Composable
private fun CommentItem(
    comment: Comment,
    onReplyClick: () -> Unit,
    onLinkClick: (String) -> Unit,
    onImageClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (comment.author.avatar.isNotBlank()) {
                        AsyncImage(
                            model = comment.author.avatar,
                            contentDescription = null,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = comment.author.username,
                        style = MaterialTheme.typography.labelMedium
                    )
                    if (comment.timeAgo.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = comment.timeAgo,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onReplyClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Reply,
                        contentDescription = stringResource(R.string.reply),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinkedTextView(
                content = comment.content,
                onLinkClick = onLinkClick,
                onImageClick = onImageClick
            )
        }
    }
}

@Composable
private fun ReplyBar(
    replyText: String,
    onReplyTextChange: (String) -> Unit,
    replyingTo: Comment?,
    onCancelReply: () -> Unit,
    isLoading: Boolean,
    onSend: () -> Unit
) {
    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            if (replyingTo != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${stringResource(R.string.replying_to)} ${replyingTo.author.username}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(
                        onClick = onCancelReply,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = replyText,
                    onValueChange = onReplyTextChange,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading,
                    placeholder = { Text(stringResource(R.string.write_reply)) },
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { if (!isLoading) onSend() }),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(
                        onClick = onSend,
                        enabled = replyText.isNotBlank()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.send)
                        )
                    }
                }
            }
        }
    }
}

---
name: add-screen
description: Scaffold a new Compose screen with ViewModel following Feedflow's MVVM pattern. Use when adding a new UI screen to the app.
user-invocable: true
argument-hint: [ScreenName]
---

# Add Screen Skill

Scaffold a new **$ARGUMENTS** screen with ViewModel for the Feedflow project.

## Steps

### 1. Gather requirements

Before writing code, determine:
- **Screen name**: PascalCase (e.g., "Profile", "Search", "Favorites")
- **Purpose**: what data does this screen show?
- **Navigation params**: what route arguments does it need?
- **Data source**: existing repository/service, or needs new data layer?

If any of these are unclear, ask the user before proceeding.

### 2. Create the ViewModel

Create `app/src/main/java/com/feedflow/ui/[name]/[Name]ViewModel.kt`

Follow the pattern from `ThreadListViewModel.kt`:

```kotlin
package com.feedflow.ui.[name]

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class [Name]ViewModel @Inject constructor(
    // Inject repositories, services, etc.
) : ViewModel() {

    // Primary data
    private val _data = MutableStateFlow<List<[DataType]>>(emptyList())
    val data: StateFlow<List<[DataType]>> = _data.asStateFlow()

    // Loading states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Pagination tracking
    private var currentPage = 1

    fun loadData(/* params */) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // Cache-first: load cached data for instant display
                // val cached = repository.getCached(key)
                // if (cached != null) {
                //     _data.value = cached
                //     _isLoading.value = false
                // }

                // Fetch fresh data
                val fresh = TODO("fetch from service/repository")
                _data.value = fresh
            } catch (e: Exception) {
                if (_data.value.isEmpty()) {
                    _error.value = e.message ?: "Failed to load"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Include loadMore() if the screen supports pagination:
    fun loadMore() {
        if (_isLoadingMore.value) return
        viewModelScope.launch {
            _isLoadingMore.value = true
            try {
                currentPage++
                val more = TODO("fetch next page")
                _data.value = _data.value + more
            } catch (e: Exception) {
                currentPage--
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun refresh() {
        currentPage = 1
        loadData(/* params */)
    }
}
```

### 3. Create the Composable Screen

Create `app/src/main/java/com/feedflow/ui/[name]/[Name]Screen.kt`

Follow the project's standard Compose pattern:

```kotlin
package com.feedflow.ui.[name]

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun [Name]Screen(
    // Navigation params (decoded from route):
    // paramId: String,
    onBackClick: () -> Unit,
    // Other navigation callbacks:
    // onItemClick: (Item) -> Unit,
    viewModel: [Name]ViewModel = hiltViewModel()
) {
    // Collect state
    val data by viewModel.data.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val error by viewModel.error.collectAsState()

    // Initial load
    LaunchedEffect(Unit) {
        viewModel.loadData(/* params */)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("[Screen Title]") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading && data.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                error != null && data.isEmpty() -> {
                    Text(
                        text = error ?: "Error",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(data) { item ->
                            // Item composable here
                        }
                        if (isLoadingMore) {
                            item {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                        .wrapContentWidth(Alignment.CenterHorizontally)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
```

### 4. Add navigation route

Edit `app/src/main/java/com/feedflow/ui/navigation/Screen.kt` — add a new sealed object:

```kotlin
object [Name] : Screen("[name]/{param}") {
    fun createRoute(param: String) =
        "[name]/${java.net.URLEncoder.encode(param, "UTF-8")}"
}
```

For screens with no parameters:
```kotlin
object [Name] : Screen("[name]")
```

### 5. Wire into NavGraph

Edit `app/src/main/java/com/feedflow/ui/navigation/NavGraph.kt` — add a composable entry:

```kotlin
composable(
    route = Screen.[Name].route,
    arguments = listOf(navArgument("param") { type = NavType.StringType })
) { backStackEntry ->
    val param = backStackEntry.arguments?.getString("param") ?: return@composable
    [Name]Screen(
        param = URLDecoder.decode(param, "UTF-8"),
        onBackClick = { navController.popBackStack() }
    )
}
```

### 6. Build and verify

Run `gradlew.bat assembleDebug` to verify everything compiles.

## Important conventions

- Use `@HiltViewModel` + `@Inject constructor` for ViewModels
- Expose state as `StateFlow` (not `LiveData`) — use `MutableStateFlow` + `asStateFlow()`
- Use `collectAsState()` in Compose to observe flows
- Use `LaunchedEffect` for triggering loads, not `init {}` in ViewModel
- URL-encode route parameters with `URLEncoder.encode(param, "UTF-8")`
- Use Material 3 components (`TopAppBar`, `Scaffold`, `MaterialTheme`)
- Follow the three-state UI pattern: loading spinner, error message, or content
- Cache-first loading: show cached data immediately, fetch fresh in background

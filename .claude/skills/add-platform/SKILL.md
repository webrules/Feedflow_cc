---
name: add-platform
description: Scaffold a new ForumService platform integration for Feedflow. Use when adding support for a new website or forum (e.g., Reddit, Lobsters, Lemmy).
user-invocable: true
argument-hint: [platform-name]
---

# Add Platform Skill

Scaffold a complete ForumService implementation for **$ARGUMENTS** in the Feedflow project.

## Steps

### 1. Gather requirements

Before writing code, determine:
- **Platform name**: display name (e.g., "Reddit")
- **Service ID**: lowercase key for the service map (e.g., "reddit")
- **Data source type**: REST API (Retrofit) or HTML scraping (OkHttp + Jsoup)
- **Auth required**: does this platform need login/cookies?
- **Categories**: what communities/sections does this platform have?

If any of these are unclear, ask the user before proceeding.

### 2. Create the service class

Create `app/src/main/java/com/feedflow/domain/service/[Name]Service.kt`

Follow the existing pattern from `HackerNewsService.kt` (for REST API) or `V2EXService.kt` / `FourD4YService.kt` (for HTML scraping):

```kotlin
package com.feedflow.domain.service

import com.feedflow.R
import com.feedflow.data.model.Comment
import com.feedflow.data.model.Community
import com.feedflow.data.model.ForumThread
import com.feedflow.data.model.ThreadDetailResult
import com.feedflow.data.model.User
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class [Name]Service @Inject constructor(
    // Dependencies: HackerNewsApi for REST, or OkHttpClient + EncryptionHelper for scraping
) : ForumService {

    override val name: String = "[Display Name]"
    override val id: String = "[service_id]"
    override val logo: Int = R.drawable.ic_[service_id]

    private val categories = listOf(
        Community("id", "Name", "Description", "[service_id]"),
        // ...
    )

    override suspend fun fetchCategories(): List<Community> = categories

    override suspend fun fetchCategoryThreads(
        categoryId: String,
        communities: List<Community>,
        page: Int
    ): List<ForumThread> {
        // Implement: fetch threads for the given category/page
        // Use pageSize = 20, calculate startIndex from page
        TODO("Implement thread fetching")
    }

    override suspend fun fetchThreadDetail(
        threadId: String,
        page: Int
    ): ThreadDetailResult {
        // Implement: fetch thread detail + comments
        TODO("Implement thread detail")
    }

    override suspend fun postComment(topicId: String, categoryId: String, content: String) {
        throw UnsupportedOperationException("Posting not supported for [Name]")
    }

    override suspend fun createThread(categoryId: String, title: String, content: String) {
        throw UnsupportedOperationException("Thread creation not supported for [Name]")
    }

    override fun getWebURL(thread: ForumThread): String {
        return "https://[domain]/[path]/${thread.id}"
    }

    // Override if this platform supports posting or requires login:
    // override fun supportsPosting(): Boolean = true
    // override fun requiresLogin(): Boolean = true

    // Private helper methods for mapping API/HTML data to domain models:
    private fun itemToThread(/* raw data */): ForumThread {
        TODO("Map raw data to ForumThread")
    }

    private fun itemToComment(/* raw data */): Comment {
        TODO("Map raw data to Comment")
    }
}
```

### 3. If using REST API: create API interface and DTOs

Create `app/src/main/java/com/feedflow/data/remote/api/[Name]Api.kt`:

```kotlin
package com.feedflow.data.remote.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface [Name]Api {
    @GET("[endpoint]")
    suspend fun getItems(@Query("page") page: Int): List<[DTO]>

    @GET("[endpoint]/{id}")
    suspend fun getItem(@Path("id") id: String): [DTO]?
}
```

Create DTOs in `app/src/main/java/com/feedflow/data/remote/dto/[Name]Dto.kt`.

Add a named Retrofit instance in `NetworkModule.kt`:
```kotlin
@Provides @Singleton @Named("[Name]")
fun provide[Name]Retrofit(client: OkHttpClient): Retrofit { ... }

@Provides @Singleton
fun provide[Name]Api(@Named("[Name]") retrofit: Retrofit): [Name]Api { ... }
```

### 4. Register in ServiceModule

Edit `app/src/main/java/com/feedflow/di/ServiceModule.kt`:

1. Add provider method(s):
   - For REST API services: one `@Provides @Named("[Name]")` returning `ForumService`
   - For scraping services: one concrete provider + one `@Named` ForumService provider (two-step pattern, see V2EX/4D4Y)

2. Add to `provideForumServiceMap`:
   - Add `@Named("[Name]")` parameter
   - Add `"[service_id]" to [name]Service` entry in the map

### 5. Add a logo drawable

Add `ic_[service_id].xml` or `ic_[service_id].png` to `app/src/main/res/drawable/`.
If no logo is available yet, inform the user they need to add one.

### 6. Build and verify

Run `gradlew.bat assembleDebug` to verify everything compiles.

### 7. Add the site to the home screen

The user may also need to add a site entry in `SiteListScreen.kt` or wherever sites are registered. Check if sites are hardcoded or loaded dynamically, and guide accordingly.

## Important conventions

- Use `@Singleton` scope on the service class
- Use `@Inject constructor` for DI
- Follow the existing naming pattern: `[Name]Service`, `[Name]Api`, `ic_[service_id]`
- Keep page size at 20 to match other services
- Use `TimeUtils.calculateTimeAgo()` for time formatting
- Use `HtmlUtils.cleanHtml()` for HTML content sanitization
- Handle errors gracefully — return empty lists, don't crash
- Use `coroutineScope { async { } }` for parallel fetches (see HackerNewsService)

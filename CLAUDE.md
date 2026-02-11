# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

This is a single-module Android project using Gradle (Kotlin DSL).

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK (minified + shrunk resources)
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run a single test class
./gradlew testDebugUnitTest --tests "com.feedflow.SomeTest"

# Run Android instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Clean build
./gradlew clean
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Architecture

**Feedflow** is an Android RSS/forum reader supporting multiple platforms: RSS, Hacker News, 4D4Y, V2EX, Linux.do (Discourse), and Zhihu.

### Layer Structure (Clean Architecture + MVVM)

- **`data/`** — Data layer: Room entities/DAOs, Retrofit APIs/DTOs, repositories, encryption, preferences
- **`domain/`** — Domain layer: `ForumService` interface implementations and parsers (RSS, OPML)
- **`ui/`** — UI layer: Jetpack Compose screens with Hilt-injected ViewModels
- **`di/`** — Dagger Hilt modules for dependency injection
- **`util/`** — Utilities (time formatting, network monitoring, GBK/HTML encoding)

### Core Abstraction: ForumService

`ForumService` (`domain/service/ForumService.kt`) is the central interface that all platform integrations implement. Each platform (RSS, HackerNews, Discourse, V2EX, 4D4Y, Zhihu) has its own service class.

Services are registered in `ServiceModule` as a `Map<String, ForumService>` keyed by site ID strings: `"rss"`, `"hackernews"`, `"linux_do"`, `"v2ex"`, `"4d4y"`, `"zhihu"`. To add a new platform, implement `ForumService` and register it in `ServiceModule`.

### Navigation

Navigation uses Jetpack Navigation Compose with a sealed `Screen` class (`ui/navigation/Screen.kt`). Routes use URL-encoded parameters. The nav graph is defined in `NavGraph.kt`.

Flow: **Home (SiteList) → Communities → ThreadList → ThreadDetail** with branches to Browser, Login, Bookmarks, Settings, and FullScreenImage.

### Dependency Injection (Hilt)

Four modules in `di/`:
- **`AppModule`** — Singletons: PreferencesManager, EncryptionHelper, NetworkMonitor, parsers
- **`DatabaseModule`** — Room database and DAOs
- **`NetworkModule`** — OkHttpClient, Retrofit instances (named: `@Named("HackerNews")`, `@Named("Discourse")`)
- **`ServiceModule`** — ForumService implementations (named by platform) and the service map

### Database

Room database (`FeedflowDatabase`, version 1) with 7 entities: CommunityEntity, SettingEntity, AiSummaryEntity, CachedTopicEntity, CachedThreadEntity, BookmarkEntity, UrlBookmarkEntity. Schema export is enabled — increment the version and provide a migration when modifying entities.

### Networking

Retrofit + OkHttp with Gson serialization. The Discourse client injects cookies from `EncryptionHelper` for authenticated requests. Some services (V2EX, 4D4Y, Zhihu) use OkHttp + Jsoup for HTML scraping instead of REST APIs.

## Tech Stack

- **Language:** Kotlin 1.9.22, Java 17 target
- **UI:** Jetpack Compose + Material 3, Coil for images, Accompanist for WebView
- **DI:** Dagger Hilt 2.48 with KSP
- **Database:** Room 2.6.1
- **Network:** Retrofit 2.9.0 + OkHttp 4.12.0, Jsoup 1.17.1 for HTML parsing
- **Serialization:** Kotlin Serialization + Gson (Gson for Retrofit, kotlinx for local)
- **AI:** Google Generative AI (Gemini) for content summaries
- **Min SDK:** 26, Target SDK: 34

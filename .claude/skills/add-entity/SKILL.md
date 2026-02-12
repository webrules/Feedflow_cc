---
name: add-entity
description: Scaffold a new Room database entity with DAO and repository, wired into Hilt DI. Use when adding a new database table to Feedflow.
user-invocable: true
argument-hint: [EntityName]
---

# Add Entity Skill

Scaffold a complete Room database entity, DAO, and repository for **$ARGUMENTS** in the Feedflow project.

## Steps

### 1. Gather requirements

Before writing code, determine:
- **Entity name**: PascalCase (e.g., "ReadHistory", "UserPreference")
- **Table name**: snake_case (e.g., "read_history", "user_preferences")
- **Fields**: what columns does this table need?
- **Primary key**: single field or composite?
- **Queries needed**: what data access patterns are required?

If any of these are unclear, ask the user before proceeding.

### 2. Create the Entity

Create `app/src/main/java/com/feedflow/data/local/db/entity/[Name]Entity.kt`

Follow the pattern from `BookmarkEntity.kt`:

```kotlin
package com.feedflow.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// For composite primary key:
@Entity(
    tableName = "[table_name]",
    primaryKeys = ["field1", "field2"]
)
data class [Name]Entity(
    val field1: String,
    val field2: String,
    val data: String,       // JSON-encoded complex data (if needed)
    val timestamp: Long
)

// For single auto-generated primary key:
// @Entity(tableName = "[table_name]")
// data class [Name]Entity(
//     @PrimaryKey(autoGenerate = true) val id: Int = 0,
//     val field1: String,
//     val timestamp: Long
// )
```

### 3. Create the DAO

Create `app/src/main/java/com/feedflow/data/local/db/dao/[Name]Dao.kt`

Follow the pattern from `BookmarkDao.kt`:

```kotlin
package com.feedflow.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.feedflow.data.local.db.entity.[Name]Entity
import kotlinx.coroutines.flow.Flow

@Dao
interface [Name]Dao {
    // Reactive query (for Compose UI observation)
    @Query("SELECT * FROM [table_name] ORDER BY timestamp DESC")
    fun getAll(): Flow<List<[Name]Entity>>

    // One-shot query (for suspend functions)
    @Query("SELECT * FROM [table_name] ORDER BY timestamp DESC")
    suspend fun getAllOnce(): List<[Name]Entity>

    // Filtered query
    @Query("SELECT * FROM [table_name] WHERE [field] = :param")
    suspend fun getByParam(param: String): [Name]Entity?

    // Existence check (both Flow and suspend variants)
    @Query("SELECT EXISTS(SELECT 1 FROM [table_name] WHERE [field] = :param)")
    fun existsFlow(param: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM [table_name] WHERE [field] = :param)")
    suspend fun exists(param: String): Boolean

    // Insert/upsert
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: [Name]Entity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<[Name]Entity>)

    // Delete
    @Query("DELETE FROM [table_name] WHERE [field] = :param")
    suspend fun delete(param: String)

    @Query("DELETE FROM [table_name]")
    suspend fun deleteAll()
}
```

### 4. Create the Repository (if needed)

Create `app/src/main/java/com/feedflow/data/repository/[Name]Repository.kt`

Follow the pattern from `BookmarkRepository.kt` or `CacheRepository.kt`:

```kotlin
package com.feedflow.data.repository

import com.feedflow.data.local.db.dao.[Name]Dao
import com.feedflow.data.local.db.entity.[Name]Entity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class [Name]Repository @Inject constructor(
    private val dao: [Name]Dao
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun getAll(): Flow<List<[DomainModel]>> {
        return dao.getAll().map { entities ->
            entities.map { /* entity to domain mapping */ }
        }
    }

    suspend fun save(item: [DomainModel]) {
        val entity = [Name]Entity(
            // map domain model to entity fields
            timestamp = System.currentTimeMillis()
        )
        dao.insert(entity)
    }

    suspend fun delete(id: String) {
        dao.delete(id)
    }

    suspend fun toggle(id: String): Boolean {
        val exists = dao.exists(id)
        if (exists) dao.delete(id)
        else dao.insert(/* new entity */)
        return !exists
    }
}
```

### 5. Register in FeedflowDatabase

Edit `app/src/main/java/com/feedflow/data/local/db/FeedflowDatabase.kt`:

1. Add the entity to the `@Database` entities array:
   ```kotlin
   @Database(
       entities = [
           // ... existing entities ...
           [Name]Entity::class
       ],
       version = N,  // INCREMENT the version number
       exportSchema = true
   )
   ```

2. Add abstract DAO getter:
   ```kotlin
   abstract fun [name]Dao(): [Name]Dao
   ```

3. Add a migration in the `companion object`:
   ```kotlin
   val MIGRATION_[N-1]_[N] = object : Migration(N-1, N) {
       override fun migrate(db: SupportSQLiteDatabase) {
           db.execSQL("""
               CREATE TABLE IF NOT EXISTS [table_name] (
                   [field1] TEXT NOT NULL,
                   [field2] TEXT NOT NULL,
                   timestamp INTEGER NOT NULL,
                   PRIMARY KEY([field1], [field2])
               )
           """)
       }
   }
   ```

4. Register the new migration in `DatabaseModule.kt`:
   ```kotlin
   .addMigrations(FeedflowDatabase.MIGRATION_1_2, FeedflowDatabase.MIGRATION_[N-1]_[N])
   ```

### 6. Register DAO in DatabaseModule

Edit `app/src/main/java/com/feedflow/di/DatabaseModule.kt` — add a provider:

```kotlin
@Provides
fun provide[Name]Dao(database: FeedflowDatabase): [Name]Dao {
    return database.[name]Dao()
}
```

### 7. Build and verify

Run `gradlew.bat assembleDebug` to verify everything compiles.

## Important conventions

- Always increment the database version when adding entities
- Always provide a `Migration` — never rely on destructive migration for released apps
- Use `OnConflictStrategy.REPLACE` for upsert behavior
- Provide both `Flow` and `suspend` variants for queries (Flow for Compose, suspend for one-off)
- Use `timestamp: Long` fields for cache invalidation and sorting
- Store complex objects as JSON strings in a `data: String` field
- Use composite primary keys when entities are scoped by serviceId
- Keep entity classes in `data/local/db/entity/` and DAOs in `data/local/db/dao/`

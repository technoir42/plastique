package io.plastique.watch

import androidx.room.RoomDatabase
import com.github.technoir42.rxjava2.extensions.mapError
import com.gojuno.koptional.Optional
import com.gojuno.koptional.toOptional
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.plastique.api.ApiException
import io.plastique.api.common.ErrorType
import io.plastique.api.nextCursor
import io.plastique.api.watch.WatchService
import io.plastique.api.watch.WatcherDto
import io.plastique.core.cache.CacheEntry
import io.plastique.core.cache.CacheEntryRepository
import io.plastique.core.cache.CacheHelper
import io.plastique.core.cache.CacheKey
import io.plastique.core.cache.DurationBasedCacheEntryChecker
import io.plastique.core.cache.toCacheKey
import io.plastique.core.db.createObservable
import io.plastique.core.json.adapters.NullFallbackAdapter
import io.plastique.core.paging.OffsetCursor
import io.plastique.core.paging.PagedData
import io.plastique.core.session.SessionManager
import io.plastique.core.session.requireUser
import io.plastique.users.UserNotFoundException
import io.plastique.users.UserRepository
import io.plastique.users.toUser
import io.reactivex.Observable
import io.reactivex.Single
import org.threeten.bp.Clock
import org.threeten.bp.Duration
import javax.inject.Inject

class WatcherRepository @Inject constructor(
    private val clock: Clock,
    private val database: RoomDatabase,
    private val watchService: WatchService,
    private val cacheEntryRepository: CacheEntryRepository,
    private val metadataConverter: NullFallbackAdapter,
    private val sessionManager: SessionManager,
    private val userRepository: UserRepository,
    private val watchDao: WatchDao
) {
    private val cacheHelper = CacheHelper(cacheEntryRepository, DurationBasedCacheEntryChecker(clock, CACHE_DURATION))

    fun getWatchers(username: String?): Observable<PagedData<List<Watcher>, OffsetCursor>> {
        return sessionManager.sessionChanges
            .firstOrError()
            .flatMapObservable { session ->
                val cacheUsername = username ?: session.requireUser().username
                val cacheKey = getCacheKey(cacheUsername)
                cacheHelper.createObservable(
                    cacheKey = cacheKey,
                    cachedData = getWatchersFromDb(cacheKey),
                    updater = fetchWatchers(username, cacheKey, cursor = null).ignoreElement())
            }
    }

    fun fetchWatchers(username: String?, cursor: OffsetCursor? = null): Single<Optional<OffsetCursor>> {
        return sessionManager.sessionChanges
            .firstOrError()
            .flatMap { session ->
                val cacheUsername = username ?: session.requireUser().username
                fetchWatchers(username, getCacheKey(cacheUsername), cursor)
            }
    }

    private fun fetchWatchers(username: String?, cacheKey: CacheKey, cursor: OffsetCursor?): Single<Optional<OffsetCursor>> {
        val offset = cursor?.offset ?: 0
        return if (username != null) {
            watchService.getWatchers(username = username, offset = offset, limit = WATCHERS_PER_PAGE)
        } else {
            watchService.getWatchers(offset = offset, limit = WATCHERS_PER_PAGE)
        }
            .map { watcherList ->
                val cacheMetadata = WatchersCacheMetadata(nextCursor = watcherList.nextCursor)
                val cacheEntry = CacheEntry(key = cacheKey, timestamp = clock.instant(), metadata = metadataConverter.toJson(cacheMetadata))
                persist(cacheEntry = cacheEntry, watchers = watcherList.results, replaceExisting = offset == 0)
                cacheMetadata.nextCursor.toOptional()
            }
            .mapError { error ->
                if (username != null && error is ApiException && error.errorData.type == ErrorType.InvalidRequest) {
                    UserNotFoundException(username, error)
                } else {
                    error
                }
            }
    }

    private fun getWatchersFromDb(cacheKey: CacheKey): Observable<PagedData<List<Watcher>, OffsetCursor>> {
        return database.createObservable("users", "watchers") {
            val watchers = watchDao.getWatchersByKey(cacheKey.value).map { it.toWatcher() }
            val nextCursor = getNextCursor(cacheKey)
            PagedData(watchers, nextCursor)
        }.distinctUntilChanged()
    }

    private fun persist(cacheEntry: CacheEntry, watchers: List<WatcherDto>, replaceExisting: Boolean) {
        val users = watchers.map { it.user }

        database.runInTransaction {
            userRepository.put(users)
            cacheEntryRepository.setEntry(cacheEntry)

            val startIndex = if (replaceExisting) {
                watchDao.deleteWatchersByKey(cacheEntry.key.value)
                1
            } else {
                watchDao.getMaxOrder(cacheEntry.key.value) + 1
            }

            val watcherEntities = watchers.mapIndexed { index, watcher ->
                WatcherEntity(key = cacheEntry.key.value, userId = watcher.user.id, order = startIndex + index)
            }
            watchDao.insertWatchers(watcherEntities)
        }
    }

    private fun getNextCursor(cacheKey: CacheKey): OffsetCursor? {
        val cacheEntry = cacheEntryRepository.getEntryByKey(cacheKey)
        val metadata = cacheEntry?.metadata?.let { metadataConverter.fromJson<WatchersCacheMetadata>(it) }
        return metadata?.nextCursor
    }

    private fun getCacheKey(username: String): CacheKey =
        "watchers-$username".toCacheKey()

    private companion object {
        private val CACHE_DURATION = Duration.ofHours(4)
        private const val WATCHERS_PER_PAGE = 50
    }
}

@JsonClass(generateAdapter = true)
data class WatchersCacheMetadata(
    @Json(name = "next_cursor")
    val nextCursor: OffsetCursor? = null
)

private fun WatcherEntityWithRelations.toWatcher(): Watcher =
    Watcher(user = user.toUser())

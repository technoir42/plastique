package io.plastique.statuses

import androidx.room.RoomDatabase
import com.github.technoir42.rxjava2.extensions.mapError
import com.gojuno.koptional.Optional
import com.gojuno.koptional.toOptional
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.plastique.api.ApiException
import io.plastique.api.common.ErrorType
import io.plastique.api.deviations.DeviationDto
import io.plastique.api.nextCursor
import io.plastique.api.statuses.StatusDto
import io.plastique.api.statuses.StatusService
import io.plastique.api.users.UserDto
import io.plastique.core.cache.CacheEntry
import io.plastique.core.cache.CacheEntryRepository
import io.plastique.core.cache.CacheHelper
import io.plastique.core.cache.CacheKey
import io.plastique.core.cache.MetadataValidatingCacheEntryChecker
import io.plastique.core.cache.toCacheKey
import io.plastique.core.db.createObservable
import io.plastique.core.json.adapters.NullFallbackAdapter
import io.plastique.core.paging.OffsetCursor
import io.plastique.core.paging.PagedData
import io.plastique.deviations.DeviationRepository
import io.plastique.users.UserNotFoundException
import io.plastique.users.UserRepository
import io.reactivex.Observable
import io.reactivex.Single
import org.threeten.bp.Clock
import org.threeten.bp.Duration
import javax.inject.Inject

class StatusRepositoryImpl @Inject constructor(
    private val clock: Clock,
    private val database: RoomDatabase,
    private val statusService: StatusService,
    private val statusDao: StatusDao,
    private val cacheEntryRepository: CacheEntryRepository,
    private val deviationRepository: DeviationRepository,
    private val userRepository: UserRepository,
    private val cacheMetadataConverter: NullFallbackAdapter
) : StatusRepository {

    fun getStatuses(params: StatusListLoadParams): Observable<PagedData<List<Status>, OffsetCursor>> {
        val cacheEntryChecker = MetadataValidatingCacheEntryChecker(clock, CACHE_DURATION) { serializedMetadata ->
            val metadata = cacheMetadataConverter.fromJson<StatusListCacheMetadata>(serializedMetadata)
            metadata?.params == params
        }
        val cacheHelper = CacheHelper(cacheEntryRepository, cacheEntryChecker)
        val cacheKey = getCacheKey(params)
        return cacheHelper.createObservable(
            cacheKey = cacheKey,
            cachedData = getStatusesFromDb(cacheKey),
            updater = fetch(params, cursor = null).ignoreElement())
    }

    fun fetch(params: StatusListLoadParams, cursor: OffsetCursor?): Single<Optional<OffsetCursor>> {
        val offset = cursor?.offset ?: 0
        return statusService.getStatuses(
            username = params.username,
            matureContent = params.matureContent,
            offset = offset,
            limit = STATUSES_PER_PAGE)
            .map { statusList ->
                val cacheMetadata = StatusListCacheMetadata(params, statusList.nextCursor)
                val cacheEntry = CacheEntry(
                    key = getCacheKey(params),
                    timestamp = clock.instant(),
                    metadata = cacheMetadataConverter.toJson(cacheMetadata))
                persist(cacheEntry = cacheEntry, statuses = statusList.results, replaceExisting = offset == 0)
                cacheMetadata.nextCursor.toOptional()
            }
            .mapError { error ->
                if (error is ApiException && error.errorData.type == ErrorType.InvalidRequest) {
                    UserNotFoundException(params.username, error)
                } else {
                    error
                }
            }
    }

    private fun persist(cacheEntry: CacheEntry, statuses: List<StatusDto>, replaceExisting: Boolean) {
        database.runInTransaction {
            cacheEntryRepository.setEntry(cacheEntry)
            put(statuses)

            val startIndex = if (replaceExisting) {
                statusDao.deleteLinksByKey(cacheEntry.key.value)
                1
            } else {
                statusDao.getMaxOrder(cacheEntry.key.value) + 1
            }

            val links = statuses.mapIndexed { index, status ->
                StatusLinkage(key = cacheEntry.key.value, statusId = status.id, order = startIndex + index)
            }
            statusDao.insertLinks(links)
        }
    }

    private fun getStatusesFromDb(cacheKey: CacheKey): Observable<PagedData<List<Status>, OffsetCursor>> {
        return database.createObservable("users", "deviation_images", "deviations", "statuses", "user_statuses") {
            val statuses = statusDao.getStatusesByKey(cacheKey.value).map { it.toStatus(clock.zone) }
            val nextCursor = getNextCursor(cacheKey)
            PagedData(statuses, nextCursor)
        }.distinctUntilChanged()
    }

    private fun getNextCursor(cacheKey: CacheKey): OffsetCursor? {
        val cacheEntry = cacheEntryRepository.getEntryByKey(cacheKey)
        val cacheMetadata = cacheEntry?.metadata?.let { cacheMetadataConverter.fromJson<StatusListCacheMetadata>(it) }
        return cacheMetadata?.nextCursor
    }

    override fun put(statuses: Collection<StatusDto>) {
        if (statuses.isEmpty()) {
            return
        }

        val deviations = mutableListOf<DeviationDto>()
        val users = mutableListOf<UserDto>()
        val flattenedStatuses = mutableListOf<StatusEntity>()
        statuses.forEach { status -> collectEntities(status, flattenedStatuses, deviations, users) }

        val uniqueDeviations = deviations.distinctBy { deviation -> deviation.id }
        val uniqueUsers = users.distinctBy { user -> user.id }
        val uniqueStatuses = flattenedStatuses.distinctBy { status -> status.id }
        database.runInTransaction {
            userRepository.put(uniqueUsers)
            deviationRepository.put(uniqueDeviations)
            statusDao.insertOrUpdate(uniqueStatuses)
        }
    }

    private fun collectEntities(
        status: StatusDto,
        statuses: MutableCollection<StatusEntity>,
        deviations: MutableCollection<DeviationDto>,
        users: MutableCollection<UserDto>
    ) {
        var shareType: ShareType = ShareType.None
        var sharedDeviationId: String? = null
        var sharedStatusId: String? = null
        users += status.author

        status.items.forEach { item ->
            when (item) {
                is StatusDto.EmbeddedItem.SharedDeviation -> {
                    shareType = ShareType.Deviation
                    item.deviation?.let { deviation ->
                        deviations += deviation
                        sharedDeviationId = deviation.id
                    }
                }

                is StatusDto.EmbeddedItem.SharedStatus -> {
                    shareType = ShareType.Status
                    item.status?.let { status ->
                        collectEntities(status, statuses, deviations, users)
                        sharedStatusId = status.id
                    }
                }
            }
        }

        statuses += status.toStatusEntity(
            shareType = shareType,
            sharedDeviationId = sharedDeviationId,
            sharedStatusId = sharedStatusId)
    }

    private fun getCacheKey(params: StatusListLoadParams): CacheKey =
        "statuses-${params.username}".toCacheKey()

    companion object {
        private val CACHE_DURATION = Duration.ofHours(2)
        private const val STATUSES_PER_PAGE = 20
    }
}

private fun StatusDto.toStatusEntity(shareType: ShareType, sharedDeviationId: String?, sharedStatusId: String?): StatusEntity {
    return StatusEntity(
        id = id,
        body = body,
        timestamp = timestamp,
        url = url,
        authorId = author.id,
        commentCount = commentCount,
        shareType = shareType,
        sharedDeviationId = sharedDeviationId,
        sharedStatusId = sharedStatusId)
}

@JsonClass(generateAdapter = true)
data class StatusListCacheMetadata(
    @Json(name = "params")
    val params: StatusListLoadParams,

    @Json(name = "next_cursor")
    val nextCursor: OffsetCursor?
)

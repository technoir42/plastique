package io.plastique.deviations.info

import androidx.room.RoomDatabase
import com.github.technoir42.rxjava2.extensions.mapError
import io.plastique.api.ApiException
import io.plastique.api.common.ErrorType
import io.plastique.api.deviations.DeviationMetadataDto
import io.plastique.api.deviations.DeviationService
import io.plastique.core.cache.CacheEntry
import io.plastique.core.cache.CacheEntryRepository
import io.plastique.core.cache.CacheHelper
import io.plastique.core.cache.CacheKey
import io.plastique.core.cache.DurationBasedCacheEntryChecker
import io.plastique.core.cache.toCacheKey
import io.plastique.deviations.DeviationNotFoundException
import io.plastique.deviations.DeviationRepository
import io.plastique.users.toUser
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.rxkotlin.Singles
import org.threeten.bp.Clock
import org.threeten.bp.Duration
import org.threeten.bp.ZoneId
import javax.inject.Inject

class DeviationInfoRepository @Inject constructor(
    private val clock: Clock,
    private val database: RoomDatabase,
    private val deviationService: DeviationService,
    private val deviationMetadataDao: DeviationMetadataDao,
    private val cacheEntryRepository: CacheEntryRepository,
    private val deviationRepository: DeviationRepository
) {
    private val cacheHelper = CacheHelper(cacheEntryRepository, DurationBasedCacheEntryChecker(clock, CACHE_DURATION))

    fun getDeviationInfo(deviationId: String): Observable<DeviationInfo> {
        val cacheKey = getCacheKey(deviationId)
        return cacheHelper.createObservable(
            cacheKey = cacheKey,
            cachedData = getDeviationInfoFromDb(deviationId),
            updater = fetch(deviationId, cacheKey))
    }

    private fun fetch(deviationId: String, cacheKey: CacheKey): Completable {
        return Singles.zip(
            deviationRepository.getDeviationTitleById(deviationId),
            deviationService.getMetadataByIds(listOf(deviationId))
                .mapError { error ->
                    if (error is ApiException && error.errorData.type == ErrorType.InvalidRequest && error.errorData.details.containsKey("deviationids")) {
                        DeviationNotFoundException(deviationId, error)
                    } else {
                        error
                    }
                }) { _, metadataResult -> persist(cacheKey, metadataResult.metadata) }
            .ignoreElement()
    }

    private fun getDeviationInfoFromDb(deviationId: String): Observable<DeviationInfo> {
        return deviationMetadataDao.getDeviationInfoById(deviationId)
            .filter { it.isNotEmpty() }
            .map { it.first().toDeviationInfo(clock.zone) }
            .distinctUntilChanged()
    }

    private fun persist(cacheKey: CacheKey, metadataList: List<DeviationMetadataDto>) {
        val entities = metadataList.map { it.toDeviationMetadataEntity() }
        database.runInTransaction {
            val cacheEntry = CacheEntry(key = cacheKey, timestamp = clock.instant())
            cacheEntryRepository.setEntry(cacheEntry)
            deviationMetadataDao.insertOrUpdate(entities)
        }
    }

    private fun getCacheKey(deviationId: String): CacheKey =
        "deviation-info-$deviationId".toCacheKey()

    companion object {
        private val CACHE_DURATION = Duration.ofHours(2)
    }
}

private fun DeviationInfoEntity.toDeviationInfo(timeZone: ZoneId): DeviationInfo = DeviationInfo(
    title = title,
    author = author.toUser(),
    publishTime = publishTime.atZone(timeZone),
    description = description,
    tags = tags)

private fun DeviationMetadataDto.toDeviationMetadataEntity(): DeviationMetadataEntity = DeviationMetadataEntity(
    deviationId = deviationId,
    description = description,
    tags = tags.map { it.name })

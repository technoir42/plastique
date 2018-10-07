package io.plastique.collections

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.plastique.api.collections.CollectionService
import io.plastique.core.paging.OffsetCursor
import io.plastique.deviations.DeviationCacheMetadataSerializer
import io.plastique.deviations.DeviationFetcher
import io.plastique.deviations.FetchParams
import io.plastique.deviations.FetchResult
import io.reactivex.Single
import javax.inject.Inject

@JsonClass(generateAdapter = true)
data class CollectionDeviationParams(
    @Json(name = "folder_id")
    val folderId: CollectionFolderId,

    @Json(name = "show_mature")
    override val showMatureContent: Boolean = false
) : FetchParams {
    override val showLiterature: Boolean get() = true

    override fun isSameAs(params: FetchParams): Boolean {
        if (params !is CollectionDeviationParams) return false
        return folderId == params.folderId &&
                showMatureContent == params.showMatureContent
    }

    override fun with(showMatureContent: Boolean, showLiterature: Boolean): FetchParams {
        return copy(showMatureContent = showMatureContent)
    }
}

class CollectionDeviationFetcher @Inject constructor(
    private val collectionService: CollectionService
) : DeviationFetcher<CollectionDeviationParams, OffsetCursor> {
    override fun getCacheKey(params: CollectionDeviationParams): String =
            "collection-folder-deviations-${params.folderId.id}"

    override fun createMetadataSerializer(): DeviationCacheMetadataSerializer =
            DeviationCacheMetadataSerializer(paramsType = CollectionDeviationParams::class.java, cursorType = OffsetCursor::class.java)

    override fun fetch(params: CollectionDeviationParams, cursor: OffsetCursor?): Single<FetchResult<OffsetCursor>> {
        val offset = cursor?.offset ?: 0
        return collectionService.getFolderContents(
                username = params.folderId.username,
                folderId = params.folderId.id,
                matureContent = params.showMatureContent,
                offset = offset,
                limit = 24)
                .map { deviationList ->
                    FetchResult(
                            deviations = deviationList.deviations,
                            nextCursor = if (deviationList.hasMore) OffsetCursor(deviationList.nextOffset!!) else null,
                            replaceExisting = offset == 0)
                }
    }
}

package io.plastique.api.feed

import androidx.annotation.IntRange
import io.plastique.api.common.AccessScope
import io.plastique.api.common.PagedListResult
import io.plastique.api.deviations.DeviationDto
import io.reactivex.Completable
import io.reactivex.Single
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface FeedService {
    @GET("feed/home")
    @AccessScope("feed")
    fun getHomeFeed(
        @Query("cursor") cursor: String?,
        @Query("mature_content") matureContent: Boolean
    ): Single<FeedElementList>

    @GET("feed/profile")
    @AccessScope("feed")
    fun getProfileFeed(
        @Query("cursor") cursor: String?,
        @Query("mature_content") matureContent: Boolean
    ): Single<FeedElementList>

    @GET("feed/notifications")
    @AccessScope("feed")
    fun getNotificationsFeed(
        @Query("cursor") cursor: String?,
        @Query("mature_content") matureContent: Boolean
    ): Single<FeedElementList>

    @GET("feed/home/{bucketid}")
    @AccessScope("feed")
    fun getBucket(
        @Path("bucketid") bucketId: String,
        @Query("offset") offset: Int,
        @Query("limit") @IntRange(from = 1, to = 120) limit: Int,
        @Query("mature_content") matureContent: Boolean
    ): Single<PagedListResult<DeviationDto>>

    @GET("feed/settings")
    @AccessScope("feed")
    fun getSettings(): Single<FeedSettingsDto>

    @POST("feed/settings/update")
    @FormUrlEncoded
    @AccessScope("feed")
    fun updateSettings(@FieldMap include: Map<String, Boolean>): Completable
}

package io.plastique.comments

import androidx.room.RoomDatabase
import com.github.technoir42.rxjava2.extensions.mapError
import com.gojuno.koptional.Optional
import com.gojuno.koptional.toOptional
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.plastique.api.ApiException
import io.plastique.api.comments.CommentDto
import io.plastique.api.comments.CommentList
import io.plastique.api.comments.CommentService
import io.plastique.api.comments.HideReason
import io.plastique.api.common.ErrorType
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
import io.plastique.users.UserNotFoundException
import io.plastique.users.UserRepository
import io.plastique.users.toUser
import io.reactivex.Observable
import io.reactivex.Single
import org.threeten.bp.Clock
import org.threeten.bp.Duration
import javax.inject.Inject

class CommentRepositoryImpl @Inject constructor(
    private val clock: Clock,
    private val database: RoomDatabase,
    private val commentDao: CommentDao,
    private val commentService: CommentService,
    private val cacheEntryRepository: CacheEntryRepository,
    private val metadataConverter: NullFallbackAdapter,
    private val userRepository: UserRepository
) : CommentRepository {

    private val cacheHelper = CacheHelper(cacheEntryRepository, DurationBasedCacheEntryChecker(clock, CACHE_DURATION))

    fun getComments(threadId: CommentThreadId): Observable<PagedData<List<Comment>, OffsetCursor>> {
        val cacheKey = threadId.cacheKey
        return cacheHelper.createObservable(
            cacheKey = cacheKey,
            cachedData = getCommentsFromDb(cacheKey),
            updater = fetchComments(threadId, cacheKey, cursor = null).ignoreElement())
    }

    fun fetchComments(threadId: CommentThreadId, cursor: OffsetCursor? = null): Single<Optional<OffsetCursor>> {
        return fetchComments(threadId, threadId.cacheKey, cursor)
    }

    private fun fetchComments(threadId: CommentThreadId, cacheKey: CacheKey, cursor: OffsetCursor?): Single<Optional<OffsetCursor>> {
        val offset = cursor?.offset ?: 0
        return getCommentList(threadId, null, COMMENTS_MAX_DEPTH, offset, COMMENTS_PER_PAGE)
            .map { commentList ->
                val cacheMetadata = CommentCacheMetadata(nextCursor = commentList.nextCursor)
                val cacheEntry = CacheEntry(
                    key = cacheKey,
                    timestamp = clock.instant(),
                    metadata = metadataConverter.toJson(cacheMetadata))
                persist(cacheEntry = cacheEntry, comments = commentList.comments, replaceExisting = offset == 0)
                cacheMetadata.nextCursor.toOptional()
            }

        // TODO: Ignore duplicates if offset changes
        // TODO: Load nested comments automatically
    }

    private fun getCommentsFromDb(cacheKey: CacheKey): Observable<PagedData<List<Comment>, OffsetCursor>> {
        return database.createObservable("users", "comments", "comment_linkage") {
            val commentsWithRelations = commentDao.getCommentsByKey(cacheKey.value)
            val comments = combineAndFilter(commentsWithRelations)
            val nextCursor = getNextCursor(cacheKey)
            PagedData(comments, nextCursor)
        }.distinctUntilChanged()
    }

    private fun combineAndFilter(commentsWithRelations: List<CommentEntityWithRelations>): List<Comment> {
        return commentsWithRelations.asSequence()
            .filter { !it.comment.isIgnored }
            .map { it.toComment() }
            .toList()
    }

    private fun getNextCursor(cacheKey: CacheKey): OffsetCursor? {
        val cacheEntry = cacheEntryRepository.getEntryByKey(cacheKey)
        val metadata = cacheEntry?.metadata?.let { metadataConverter.fromJson<CommentCacheMetadata>(it) }
        return metadata?.nextCursor
    }

    private fun getCommentList(threadId: CommentThreadId, parentCommentId: String?, maxDepth: Int, offset: Int, pageSize: Int): Single<CommentList> =
        when (threadId) {
            is CommentThreadId.Deviation -> commentService.getCommentsOnDeviation(
                deviationId = threadId.deviationId,
                parentCommentId = parentCommentId,
                maxDepth = maxDepth,
                offset = offset,
                limit = pageSize)

            is CommentThreadId.Profile -> commentService.getCommentsOnProfile(
                username = threadId.username,
                parentCommentId = parentCommentId,
                maxDepth = maxDepth,
                offset = offset,
                limit = pageSize)
                .mapError { error ->
                    if (error is ApiException && error.errorData.type == ErrorType.InvalidRequest) {
                        UserNotFoundException(threadId.username, error)
                    } else {
                        error
                    }
                }

            is CommentThreadId.Status -> commentService.getCommentsOnStatus(
                statusId = threadId.statusId,
                parentCommentId = parentCommentId,
                maxDepth = maxDepth,
                offset = offset,
                limit = pageSize)
        }

    override fun put(comments: Collection<CommentDto>) {
        if (comments.isEmpty()) {
            return
        }
        val entities = comments.map { it.toCommentEntity() }
        val users = comments.asSequence()
            .map { it.author }
            .distinctBy { it.id }
            .toList()

        database.runInTransaction {
            userRepository.put(users)
            commentDao.insertOrUpdate(entities)
        }
    }

    private fun persist(cacheEntry: CacheEntry, comments: List<CommentDto>, replaceExisting: Boolean) {
        database.runInTransaction {
            put(comments)
            cacheEntryRepository.setEntry(cacheEntry)

            val startIndex = if (replaceExisting) {
                commentDao.deleteLinks(cacheEntry.key.value)
                1
            } else {
                commentDao.maxOrder(cacheEntry.key.value) + 1
            }

            val links = comments.mapIndexed { index, comment ->
                CommentLinkage(key = cacheEntry.key.value, commentId = comment.id, order = startIndex + index)
            }
            commentDao.insertLinks(links)
        }
    }

    private val CommentEntity.isIgnored: Boolean
        get() = hidden == HideReason.HIDDEN_AS_SPAM && numReplies == 0

    companion object {
        private val CACHE_DURATION = Duration.ofHours(1)
        private const val COMMENTS_PER_PAGE = 50
        private const val COMMENTS_MAX_DEPTH = 5
    }
}

@JsonClass(generateAdapter = true)
data class CommentCacheMetadata(
    @Json(name = "next_cursor")
    val nextCursor: OffsetCursor? = null
)

private val CommentList.nextCursor: OffsetCursor?
    get() = if (hasMore) OffsetCursor(nextOffset!!) else null

private val CommentThreadId.cacheKey: CacheKey
    get() = when (this) {
        is CommentThreadId.Deviation -> "comments-deviation-$deviationId".toCacheKey()
        is CommentThreadId.Profile -> "comments-profile-$username".toCacheKey()
        is CommentThreadId.Status -> "comments-status-$statusId".toCacheKey()
    }

private fun CommentDto.toCommentEntity(): CommentEntity = CommentEntity(
    id = id,
    parentId = parentId,
    authorId = author.id,
    datePosted = datePosted,
    numReplies = numReplies,
    hidden = hidden,
    text = text)

private fun CommentEntityWithRelations.toComment(): Comment = Comment(
    id = comment.id,
    parentId = comment.parentId,
    author = author.toUser(),
    datePosted = comment.datePosted,
    text = comment.text)

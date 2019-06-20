package io.plastique.feed

import android.text.method.LinkMovementMethod
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.HtmlCompat
import androidx.core.text.htmlEncode
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.github.technoir42.android.extensions.inflate
import com.google.android.flexbox.FlexboxLayoutManager
import com.hannesdorfmann.adapterdelegates4.ListDelegationAdapter
import io.plastique.comments.CommentThreadId
import io.plastique.common.FeedHeaderView
import io.plastique.core.lists.BaseAdapterDelegate
import io.plastique.core.lists.ItemSizeCallback
import io.plastique.core.lists.ListItem
import io.plastique.core.lists.LoadingIndicatorItemDelegate
import io.plastique.core.lists.OnViewHolderClickListener
import io.plastique.core.text.RichTextView
import io.plastique.core.time.ElapsedTimeFormatter
import io.plastique.deviations.DeviationActionsView
import io.plastique.deviations.list.ImageHelper
import io.plastique.glide.GlideRequests
import io.plastique.statuses.ShareObjectId
import io.plastique.statuses.ShareUiModel
import io.plastique.statuses.ShareView
import io.plastique.statuses.StatusActionsView
import io.plastique.statuses.isDeleted
import io.plastique.users.User
import io.plastique.util.dimensionRatio

private class CollectionUpdateItemDelegate(
    private val glide: GlideRequests,
    private val elapsedTimeFormatter: ElapsedTimeFormatter,
    private val itemSizeCallback: ItemSizeCallback,
    private val onDeviationClick: OnDeviationClickListener,
    private val onViewHolderClickListener: OnViewHolderClickListener
) : BaseAdapterDelegate<CollectionUpdateItem, ListItem, CollectionUpdateItemDelegate.ViewHolder>() {

    override fun isForViewType(item: ListItem): Boolean = item is CollectionUpdateItem

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val view = parent.inflate(R.layout.item_feed_collection_update)
        return ViewHolder(view, glide, itemSizeCallback, onDeviationClick, onViewHolderClickListener)
    }

    override fun onBindViewHolder(item: CollectionUpdateItem, holder: ViewHolder, position: Int, payloads: List<Any>) {
        holder.headerView.time = elapsedTimeFormatter.format(item.date)
        holder.headerView.setUser(item.user, glide)
        holder.folderNameView.text = holder.itemView.resources.getString(R.string.feed_collection_folder_name, item.folderName, item.addedCount)
        holder.folderItemsAdapter.update(item.folderItems)
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
        (holder as ViewHolder).run { folderItemsView.layoutManager = layoutManager }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        (holder as ViewHolder).run { folderItemsView.layoutManager = null }
    }

    class ViewHolder(
        itemView: View,
        glide: GlideRequests,
        itemSizeCallback: ItemSizeCallback,
        onDeviationClick: OnDeviationClickListener,
        private val onClickListener: OnViewHolderClickListener
    ) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val headerView: FeedHeaderView = itemView.findViewById(R.id.header)
        val folderNameView: TextView = itemView.findViewById(R.id.folder_name)
        val folderItemsView: RecyclerView = itemView.findViewById(R.id.thumbnails)
        val folderItemsAdapter: DeviationsAdapter = DeviationsAdapter(folderItemsView.context, glide, itemSizeCallback, onDeviationClick)
        val layoutManager: FlexboxLayoutManager = FlexboxLayoutManager(folderItemsView.context)

        init {
            headerView.setOnUserClickListener(this)
            folderNameView.setOnClickListener(this)
            folderItemsView.adapter = folderItemsAdapter
            folderItemsView.itemAnimator = DefaultItemAnimator().apply { supportsChangeAnimations = false }

            // TODO: Thumbnail click listener
        }

        override fun onClick(view: View) {
            onClickListener.onViewHolderClick(this, view)
        }
    }
}

private class ImageDeviationItemDelegate(
    private val glide: GlideRequests,
    private val elapsedTimeFormatter: ElapsedTimeFormatter,
    private val onViewHolderClickListener: OnViewHolderClickListener
) : BaseAdapterDelegate<ImageDeviationItem, ListItem, ImageDeviationItemDelegate.ViewHolder>() {

    override fun isForViewType(item: ListItem): Boolean = item is ImageDeviationItem

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val view = parent.inflate(R.layout.item_feed_deviation_image)
        return ViewHolder(view, onViewHolderClickListener, ImageHelper.getMaxWidth(parent))
    }

    override fun onBindViewHolder(item: ImageDeviationItem, holder: ViewHolder, position: Int, payloads: List<Any>) {
        holder.headerView.time = elapsedTimeFormatter.format(item.date)
        holder.headerView.setUser(item.user, glide)
        holder.titleView.text = item.title
        holder.actionsView.render(item.actionsState)

        val preview = ImageHelper.choosePreview(item.preview, item.content, holder.maxImageWidth)
        val previewSize = ImageHelper.calculateOptimalPreviewSize(preview, holder.maxImageWidth)
        (holder.previewView.layoutParams as ConstraintLayout.LayoutParams).dimensionRatio = previewSize.dimensionRatio

        glide.load(preview.url)
            .override(previewSize.width, previewSize.height)
            .centerCrop()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(holder.previewView)
    }

    class ViewHolder(
        itemView: View,
        private val onClickListener: OnViewHolderClickListener,
        val maxImageWidth: Int
    ) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val headerView: FeedHeaderView = itemView.findViewById(R.id.header)
        val previewView: ImageView = itemView.findViewById(R.id.deviation_preview)
        val titleView: TextView = itemView.findViewById(R.id.deviation_title)
        val actionsView: DeviationActionsView = itemView.findViewById(R.id.deviation_actions)

        init {
            headerView.setOnUserClickListener(this)
            previewView.setOnClickListener(this)
            titleView.setOnClickListener(this)
            actionsView.setOnFavoriteClickListener(this)
            actionsView.setOnCommentsClickListener(this)
            actionsView.setOnShareClickListener(this)
        }

        override fun onClick(view: View) {
            onClickListener.onViewHolderClick(this, view)
        }
    }
}

private class LiteratureDeviationItemDelegate(
    private val glide: GlideRequests,
    private val elapsedTimeFormatter: ElapsedTimeFormatter,
    private val onViewHolderClickListener: OnViewHolderClickListener
) : BaseAdapterDelegate<LiteratureDeviationItem, ListItem, LiteratureDeviationItemDelegate.ViewHolder>() {

    override fun isForViewType(item: ListItem): Boolean = item is LiteratureDeviationItem

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val view = parent.inflate(R.layout.item_feed_deviation_literature)
        return ViewHolder(view, onViewHolderClickListener)
    }

    override fun onBindViewHolder(item: LiteratureDeviationItem, holder: ViewHolder, position: Int, payloads: List<Any>) {
        holder.headerView.time = elapsedTimeFormatter.format(item.date)
        holder.headerView.setUser(item.user, glide)
        holder.titleView.text = item.title
        holder.excerptView.text = item.excerpt.value
        holder.actionsView.render(item.actionsState)
    }

    class ViewHolder(itemView: View, private val onClickListener: OnViewHolderClickListener) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val headerView: FeedHeaderView = itemView.findViewById(R.id.header)
        val titleView: TextView = itemView.findViewById(R.id.deviation_title)
        val excerptView: RichTextView = itemView.findViewById(R.id.deviation_excerpt)
        val actionsView: DeviationActionsView = itemView.findViewById(R.id.deviation_actions)

        init {
            headerView.setOnUserClickListener(this)
            titleView.setOnClickListener(this)
            excerptView.setOnClickListener(this)
            actionsView.setOnFavoriteClickListener(this)
            actionsView.setOnCommentsClickListener(this)
            actionsView.setOnShareClickListener(this)
        }

        override fun onClick(view: View) {
            onClickListener.onViewHolderClick(this, view)
        }
    }
}

private class MultipleDeviationsItemDelegate(
    private val glide: GlideRequests,
    private val elapsedTimeFormatter: ElapsedTimeFormatter,
    private val gridItemSizeCallback: ItemSizeCallback,
    private val onDeviationClick: OnDeviationClickListener,
    private val onViewHolderClickListener: OnViewHolderClickListener
) : BaseAdapterDelegate<MultipleDeviationsItem, ListItem, MultipleDeviationsItemDelegate.ViewHolder>() {

    override fun isForViewType(item: ListItem): Boolean = item is MultipleDeviationsItem

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val view = parent.inflate(R.layout.item_feed_multiple_deviations)
        return ViewHolder(view, glide, gridItemSizeCallback, onDeviationClick, onViewHolderClickListener)
    }

    override fun onBindViewHolder(item: MultipleDeviationsItem, holder: ViewHolder, position: Int, payloads: List<Any>) {
        holder.headerView.time = elapsedTimeFormatter.format(item.date)
        holder.headerView.setUser(item.user, glide)
        val resources = holder.itemView.resources
        holder.descriptionView.text = resources.getString(R.string.feed_multiple_deviations_submitted_description,
            resources.getQuantityString(R.plurals.common_deviations, item.submittedTotal, item.submittedTotal))
        holder.adapter.update(item.items)
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
        (holder as ViewHolder).run { deviationsView.layoutManager = holder.layoutManager }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        (holder as ViewHolder).run { deviationsView.layoutManager = null }
    }

    class ViewHolder(
        itemView: View,
        glide: GlideRequests,
        gridItemSizeCallback: ItemSizeCallback,
        onDeviationClick: OnDeviationClickListener,
        private val onClickListener: OnViewHolderClickListener
    ) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val headerView: FeedHeaderView = itemView.findViewById(R.id.header)
        val descriptionView: TextView = itemView.findViewById(R.id.description)
        val deviationsView: RecyclerView = itemView.findViewById(R.id.deviations)
        val adapter: DeviationsAdapter = DeviationsAdapter(itemView.context, glide, gridItemSizeCallback, onDeviationClick)
        val layoutManager: FlexboxLayoutManager = FlexboxLayoutManager(deviationsView.context)

        init {
            deviationsView.adapter = adapter
            deviationsView.itemAnimator = DefaultItemAnimator().apply { supportsChangeAnimations = false }
            headerView.setOnUserClickListener(this)
        }

        override fun onClick(view: View) {
            onClickListener.onViewHolderClick(this, view)
        }
    }
}

private class StatusItemDelegate(
    private val glide: GlideRequests,
    private val elapsedTimeFormatter: ElapsedTimeFormatter,
    private val onViewHolderClickListener: OnViewHolderClickListener
) : BaseAdapterDelegate<StatusUpdateItem, ListItem, StatusItemDelegate.ViewHolder>() {

    override fun isForViewType(item: ListItem): Boolean = item is StatusUpdateItem

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val view = parent.inflate(R.layout.item_feed_status)
        return ViewHolder(view, onViewHolderClickListener)
    }

    override fun onBindViewHolder(item: StatusUpdateItem, holder: ViewHolder, position: Int, payloads: List<Any>) {
        holder.headerView.time = elapsedTimeFormatter.format(item.date)
        holder.headerView.setUser(item.user, glide)
        holder.statusTextView.text = item.text.value
        holder.actionsView.render(item.actionsState)

        holder.shareView.setShare(item.share, glide, elapsedTimeFormatter)
        holder.shareView.setOnClickListener(if (!item.share.isDeleted) holder else null)
    }

    class ViewHolder(itemView: View, private val onClickListener: OnViewHolderClickListener) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val headerView: FeedHeaderView = itemView.findViewById(R.id.header)
        val statusTextView: RichTextView = itemView.findViewById(R.id.status_text)
        val shareView: ShareView = itemView.findViewById(R.id.status_share)
        val actionsView: StatusActionsView = itemView.findViewById(R.id.status_actions)

        init {
            headerView.setOnUserClickListener(this)
            statusTextView.setOnClickListener(this)
            actionsView.setOnCommentsClickListener(this)
            actionsView.setOnShareClickListener(this)

            statusTextView.movementMethod = LinkMovementMethod.getInstance()
        }

        override fun onClick(view: View) {
            onClickListener.onViewHolderClick(this, view)
        }
    }
}

private class UsernameChangeItemDelegate(
    private val glide: GlideRequests,
    private val elapsedTimeFormatter: ElapsedTimeFormatter,
    private val onViewHolderClickListener: OnViewHolderClickListener
) : BaseAdapterDelegate<UsernameChangeItem, ListItem, UsernameChangeItemDelegate.ViewHolder>() {

    override fun isForViewType(item: ListItem): Boolean = item is UsernameChangeItem

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val view = parent.inflate(R.layout.item_feed_username_change)
        return ViewHolder(view, onViewHolderClickListener)
    }

    override fun onBindViewHolder(item: UsernameChangeItem, holder: ViewHolder, position: Int, payloads: List<Any>) {
        holder.headerView.time = elapsedTimeFormatter.format(item.date)
        holder.headerView.setUser(item.user, glide)
        holder.descriptionView.text = HtmlCompat.fromHtml(holder.itemView.resources.getString(R.string.feed_username_change_description,
            item.formerName.htmlEncode()), 0)
    }

    private class ViewHolder(itemView: View, private val onClickListener: OnViewHolderClickListener) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val headerView: FeedHeaderView = itemView.findViewById(R.id.header)
        val descriptionView: TextView = itemView.findViewById(R.id.description)

        init {
            headerView.setOnUserClickListener(this)
        }

        override fun onClick(view: View) {
            onClickListener.onViewHolderClick(this, view)
        }
    }
}

class FeedAdapter(
    glide: GlideRequests,
    elapsedTimeFormatter: ElapsedTimeFormatter,
    gridItemSizeCallback: ItemSizeCallback,
    private val onCollectionFolderClick: OnCollectionFolderClickListener,
    private val onCommentsClick: OnCommentsClickListener,
    private val onFavoriteClick: OnFavoriteClickListener,
    private val onDeviationClick: OnDeviationClickListener,
    private val onShareClick: OnShareClickListener,
    private val onStatusClick: OnStatusClickListener,
    private val onUserClick: OnUserClickListener
) : ListDelegationAdapter<List<ListItem>>(), OnViewHolderClickListener {

    init {
        delegatesManager.addDelegate(CollectionUpdateItemDelegate(glide, elapsedTimeFormatter, gridItemSizeCallback, onDeviationClick, this))
        delegatesManager.addDelegate(ImageDeviationItemDelegate(glide, elapsedTimeFormatter, this))
        delegatesManager.addDelegate(LiteratureDeviationItemDelegate(glide, elapsedTimeFormatter, this))
        delegatesManager.addDelegate(MultipleDeviationsItemDelegate(glide, elapsedTimeFormatter, gridItemSizeCallback, onDeviationClick, this))
        delegatesManager.addDelegate(StatusItemDelegate(glide, elapsedTimeFormatter, this))
        delegatesManager.addDelegate(UsernameChangeItemDelegate(glide, elapsedTimeFormatter, this))
        delegatesManager.addDelegate(LoadingIndicatorItemDelegate())
    }

    @Suppress("ComplexMethod")
    override fun onViewHolderClick(holder: RecyclerView.ViewHolder, view: View) {
        val position = holder.adapterPosition
        if (position == RecyclerView.NO_POSITION) return
        val item = items[position]
        when {
            view is FeedHeaderView -> {
                require(item is FeedListItem)
                onUserClick(item.user)
            }
            view.id == R.id.deviation_actions_comments -> {
                require(item is DeviationItem)
                onCommentsClick(CommentThreadId.Deviation(item.deviationId))
            }
            view.id == R.id.deviation_actions_favorite -> {
                require(item is DeviationItem)
                onFavoriteClick(item.id, !item.actionsState.isFavorite)
            }
            view.id == R.id.deviation_actions_share -> {
                require(item is DeviationItem)
                onShareClick(ShareObjectId.Deviation(item.deviationId))
            }
            view.id == R.id.status_actions_comments -> {
                require(item is StatusUpdateItem)
                onCommentsClick(CommentThreadId.Status(item.statusId))
            }
            view.id == R.id.status_actions_share -> {
                require(item is StatusUpdateItem)
                onShareClick(item.shareObjectId)
            }
            view.id == R.id.deviation_preview || view.id == R.id.deviation_title || view.id == R.id.deviation_excerpt -> {
                require(item is DeviationItem)
                onDeviationClick(item.deviationId)
            }
            view.id == R.id.status_text -> {
                val statusId = (item as StatusUpdateItem).statusId
                onStatusClick(statusId)
            }
            view.id == R.id.status_share -> {
                require(item is StatusUpdateItem)
                when (val share = item.share) {
                    is ShareUiModel.ImageDeviation -> onDeviationClick(share.deviationId)
                    is ShareUiModel.LiteratureDeviation -> onDeviationClick(share.deviationId)
                    is ShareUiModel.Status -> onStatusClick(share.statusId)
                    else -> throw IllegalStateException("Unexpected share $share")
                }
            }
            view.id == R.id.folder_name -> {
                require(item is CollectionUpdateItem)
                onCollectionFolderClick(item.user.name, item.folderId, item.folderName)
            }
        }
    }

    private val StatusUpdateItem.shareObjectId: ShareObjectId
        get() = when (share) {
            is ShareUiModel.None -> ShareObjectId.Status(statusId)
            is ShareUiModel.ImageDeviation -> ShareObjectId.Deviation(share.deviationId, statusId)
            is ShareUiModel.LiteratureDeviation -> ShareObjectId.Deviation(share.deviationId, statusId)
            is ShareUiModel.Status -> ShareObjectId.Status(share.statusId, statusId)
            else -> throw IllegalStateException("Nothing to share")
        }
}

typealias OnCollectionFolderClickListener = (username: String?, folderId: String, folderName: String) -> Unit
typealias OnCommentsClickListener = (threadId: CommentThreadId) -> Unit
typealias OnDeviationClickListener = (deviationId: String) -> Unit
typealias OnFavoriteClickListener = (deviationId: String, favorite: Boolean) -> Unit
typealias OnShareClickListener = (shareObjectId: ShareObjectId) -> Unit
typealias OnStatusClickListener = (statusId: String) -> Unit
typealias OnUserClickListener = (user: User) -> Unit

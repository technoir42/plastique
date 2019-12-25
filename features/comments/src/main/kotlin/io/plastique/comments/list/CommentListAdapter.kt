package io.plastique.comments.list

import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.github.technoir42.android.extensions.inflate
import com.hannesdorfmann.adapterdelegates4.ListDelegationAdapter
import io.plastique.comments.R
import io.plastique.core.lists.BaseAdapterDelegate
import io.plastique.core.lists.ListItem
import io.plastique.core.lists.LoadingIndicatorItemDelegate
import io.plastique.core.lists.OnViewHolderClickListener
import io.plastique.core.text.RichTextView
import io.plastique.core.time.ElapsedTimeFormatter
import io.plastique.glide.GlideRequests
import io.plastique.users.User

private class CommentItemDelegate(
    private val glide: GlideRequests,
    private val elapsedTimeFormatter: ElapsedTimeFormatter,
    private val onViewHolderClickListener: OnViewHolderClickListener
) : BaseAdapterDelegate<CommentItem, ListItem, CommentItemDelegate.ViewHolder>() {

    override fun isForViewType(item: ListItem): Boolean = item is CommentItem

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val view = parent.inflate(R.layout.item_comment)
        return ViewHolder(view, onViewHolderClickListener)
    }

    override fun onBindViewHolder(item: CommentItem, holder: ViewHolder, position: Int, payloads: List<Any>) {
        holder.avatarView.contentDescription =
            holder.itemView.resources.getString(R.string.common_avatar_description, item.comment.author.name)
        holder.authorView.text = item.comment.author.name
        holder.textView.text = item.comment.text.value
        holder.timeView.text = elapsedTimeFormatter.format(item.comment.datePosted)

        val replyingTo = if (item.comment.isReply) {
            holder.itemView.resources.getString(R.string.comments_replying_to, item.comment.parentAuthorName)
        } else {
            null
        }

        holder.replyingToView.text = replyingTo
        holder.replyingToView.isVisible = replyingTo != null
        holder.replyButton.isVisible = item.showReplyButton

        glide.load(item.comment.author.avatarUrl)
            .fallback(R.drawable.default_avatar_64dp)
            .circleCrop()
            .dontAnimate()
            .into(holder.avatarView)
    }

    class ViewHolder(
        itemView: View,
        private val onClickListener: OnViewHolderClickListener
    ) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val avatarView: ImageView = itemView.findViewById(R.id.comment_author_avatar)
        val authorView: TextView = itemView.findViewById(R.id.comment_author_name)
        val timeView: TextView = itemView.findViewById(R.id.comment_time)
        val replyingToView: TextView = itemView.findViewById(R.id.comment_replying_to)
        val textView: RichTextView = itemView.findViewById(R.id.comment_text)
        val replyButton: ImageButton = itemView.findViewById(R.id.comment_button_reply)

        init {
            itemView.setOnClickListener(this)
            authorView.setOnClickListener(this)
            avatarView.setOnClickListener(this)
            replyButton.setOnClickListener(this)
            replyingToView.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            onClickListener.onViewHolderClick(this, view)
        }
    }
}

internal class CommentListAdapter(
    glide: GlideRequests,
    elapsedTimeFormatter: ElapsedTimeFormatter,
    private val onReplyClick: OnReplyClickListener,
    private val onReplyingToClick: OnReplyingToClickListener,
    private val onUserClick: OnUserClickListener
) : ListDelegationAdapter<List<ListItem>>(), OnViewHolderClickListener {

    init {
        delegatesManager.addDelegate(CommentItemDelegate(glide, elapsedTimeFormatter, this))
        delegatesManager.addDelegate(LoadingIndicatorItemDelegate())
    }

    override fun onViewHolderClick(holder: RecyclerView.ViewHolder, view: View) {
        val position = holder.adapterPosition
        if (position == RecyclerView.NO_POSITION) return
        val item = items[position] as CommentItem
        if (holder is CommentItemDelegate.ViewHolder) {
            if (view === holder.avatarView || view === holder.authorView) {
                onUserClick(item.comment.author)
            } else if (view === holder.replyButton) {
                onReplyClick(item.comment.id)
            } else if (view === holder.replyingToView) {
                onReplyingToClick(item.comment.id)
            }
        }
    }

    fun findCommentPosition(commentId: String): Int {
        for ((index, item) in items.withIndex()) {
            if (item is CommentItem && item.comment.id == commentId) {
                return index
            }
        }
        return RecyclerView.NO_POSITION
    }
}

private typealias OnReplyClickListener = (commentId: String) -> Unit
private typealias OnReplyingToClickListener = (commentId: String) -> Unit
private typealias OnUserClickListener = (user: User) -> Unit

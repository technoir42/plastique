package io.plastique.deviations.info

import android.view.ViewGroup
import com.github.technoir42.android.extensions.layoutInflater
import io.plastique.core.lists.BaseListAdapter
import io.plastique.deviations.R
import io.plastique.deviations.databinding.ItemDeviationTagBinding

internal class TagListAdapter(
    private val onTagClick: OnTagClickListener
) : BaseListAdapter<String, TagListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDeviationTagBinding.inflate(parent.layoutInflater, parent, false)
        return ViewHolder(binding, onTagClick)
    }

    override fun onBindViewHolder(item: String, holder: ViewHolder, position: Int) {
        holder.binding.text1.text = holder.binding.root.resources.getString(R.string.common_hashtag, item)

        (holder.binding.text1.layoutParams as ViewGroup.MarginLayoutParams).apply {
            leftMargin = if (position != 0) holder.binding.root.resources.getDimensionPixelOffset(R.dimen.deviations_info_tag_spacing) else 0
        }
    }

    class ViewHolder(
        val binding: ItemDeviationTagBinding,
        onTagClick: OnTagClickListener
    ) : BaseListAdapter.ViewHolder<String>(binding.root) {

        init {
            binding.root.setOnClickListener { onTagClick(item) }
        }
    }
}

private typealias OnTagClickListener = (tag: String) -> Unit

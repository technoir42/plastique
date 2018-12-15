package io.plastique.feed

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.hannesdorfmann.adapterdelegates4.ListDelegationAdapter
import io.plastique.core.lists.ItemSizeCallback
import io.plastique.core.lists.ListItem
import io.plastique.core.lists.OnViewHolderClickListener
import io.plastique.deviations.list.DeviationItem
import io.plastique.deviations.list.GridImageDeviationItemDelegate
import io.plastique.deviations.list.GridLiteratureDeviationItemDelegate
import io.plastique.deviations.list.LayoutMode

class DeviationsAdapter(
    context: Context,
    itemSizeCallback: ItemSizeCallback,
    private val onDeviationClick: OnDeviationClickListener
) : ListDelegationAdapter<List<ListItem>>(), OnViewHolderClickListener {

    init {
        val layoutModeProvider = { LayoutMode.Grid }
        delegatesManager.addDelegate(GridImageDeviationItemDelegate(context, layoutModeProvider, itemSizeCallback, this))
        delegatesManager.addDelegate(GridLiteratureDeviationItemDelegate(context, layoutModeProvider, itemSizeCallback, this))
    }

    fun update(items: List<ListItem>) {
        if (this.items != items) {
            this.items = items
            notifyDataSetChanged()
        }
    }

    override fun onViewHolderClick(holder: RecyclerView.ViewHolder, view: View) {
        val position = holder.adapterPosition
        if (position == RecyclerView.NO_POSITION) return

        val item = items[position] as DeviationItem
        onDeviationClick(item.deviation.id)
    }
}
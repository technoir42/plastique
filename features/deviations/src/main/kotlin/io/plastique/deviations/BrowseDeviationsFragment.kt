package io.plastique.deviations

import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import io.plastique.core.ExpandableToolbarLayout
import io.plastique.core.FragmentListPagerAdapter
import io.plastique.core.MvvmFragment
import io.plastique.core.ScrollableToTop
import io.plastique.deviations.list.DailyDeviationsFragment
import io.plastique.deviations.list.HotDeviationsFragment
import io.plastique.deviations.list.LayoutMode
import io.plastique.deviations.list.PopularDeviationsFragment
import io.plastique.deviations.list.UndiscoveredDeviationsFragment
import io.plastique.deviations.tags.TagManager
import io.plastique.deviations.tags.TagManagerProvider
import io.plastique.deviations.tags.TagsView
import io.plastique.inject.getComponent
import io.plastique.main.MainPage
import io.plastique.util.SimpleOnTabSelectedListener

class BrowseDeviationsFragment : MvvmFragment<BrowseDeviationsViewModel>(), MainPage, ScrollableToTop, TagManagerProvider {
    private lateinit var expandableToolbarLayout: ExpandableToolbarLayout
    private lateinit var tagsView: TagsView
    private lateinit var pager: ViewPager
    private lateinit var pagerAdapter: FragmentListPagerAdapter
    private var switchLayoutMenuItem: MenuItem? = null
    private var layoutMode: LayoutMode = LayoutMode.DEFAULT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_browse_deviations, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        pagerAdapter = FragmentListPagerAdapter(requireContext(), childFragmentManager, *PAGES)
        pager = view.findViewById(R.id.pager)
        pager.adapter = pagerAdapter
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel.layoutMode
                .subscribe { layoutMode ->
                    this.layoutMode = layoutMode
                    switchLayoutMenuItem?.setIcon(getLayoutModeIconId(layoutMode))
                }
                .disposeOnDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_browse_deviations, menu)

        val filterItem = menu.findItem(R.id.deviations_action_filters)
        filterItem.setIcon(getFilterIconId(expandableToolbarLayout.isExpanded))

        switchLayoutMenuItem = menu.findItem(R.id.deviations_action_switch_layout)
        switchLayoutMenuItem!!.setIcon(getLayoutModeIconId(layoutMode))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.deviations_action_filters -> {
            expandableToolbarLayout.isExpanded = !expandableToolbarLayout.isExpanded
            animateFilterIcon(item)
            true
        }
        R.id.deviations_action_switch_layout_grid -> {
            viewModel.setLayoutMode(LayoutMode.Grid)
            true
        }
        R.id.deviations_action_switch_layout_flex -> {
            viewModel.setLayoutMode(LayoutMode.Flex)
            true
        }
        R.id.deviations_action_switch_layout_list -> {
            viewModel.setLayoutMode(LayoutMode.List)
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun getTitle(): Int = R.string.deviations_browse_title

    override fun createAppBarViews(parent: ExpandableToolbarLayout) {
        expandableToolbarLayout = parent

        val tabsView = TabLayout(parent.context)
        tabsView.id = R.id.browse_tabs
        tabsView.tabMode = TabLayout.MODE_SCROLLABLE
        tabsView.layoutParams = ExpandableToolbarLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, true)
        parent.addView(tabsView)

        tagsView = TagsView(parent.context)
        tagsView.id = R.id.browse_tags
        tagsView.layoutParams = ExpandableToolbarLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        parent.addView(tagsView)

        tabsView.setupWithViewPager(pager)
        tabsView.addOnTabSelectedListener(object : SimpleOnTabSelectedListener() {
            override fun onTabReselected(tab: TabLayout.Tab) {
                val fragment = pagerAdapter.getFragmentAtPosition(tab.position)
                if (fragment is ScrollableToTop) {
                    fragment.scrollToTop()
                }
            }
        })
    }

    override fun scrollToTop() {
        val currentFragment = pagerAdapter.getFragmentAtPosition(pager.currentItem)
        if (currentFragment is ScrollableToTop) {
            currentFragment.scrollToTop()
        }
    }

    override val tagManager: TagManager get() = tagsView

    override fun injectDependencies() {
        getComponent<DeviationsFragmentComponent>().inject(this)
    }

    private fun animateFilterIcon(item: MenuItem) {
        if (item.icon is Animatable) {
            item.icon.mutate()
            (item.icon as Animatable).start()

            val nextIconId = getFilterIconId(expandableToolbarLayout.isExpanded)
            AnimatedVectorDrawableCompat.clearAnimationCallbacks(item.icon)
            AnimatedVectorDrawableCompat.registerAnimationCallback(item.icon, object : Animatable2Compat.AnimationCallback() {
                override fun onAnimationEnd(drawable: Drawable) {
                    AnimatedVectorDrawableCompat.unregisterAnimationCallback(drawable, this)
                    item.setIcon(nextIconId)
                }
            })
        }
    }

    @DrawableRes
    private fun getFilterIconId(expanded: Boolean): Int = when {
        expanded -> R.drawable.ic_filters_collapse_24dp
        else -> R.drawable.ic_filters_expand_24dp
    }

    @DrawableRes
    private fun getLayoutModeIconId(layoutMode: LayoutMode): Int = when (layoutMode) {
        LayoutMode.Grid -> R.drawable.ic_layout_grid_24dp
        LayoutMode.Flex -> R.drawable.ic_layout_flex_24dp
        LayoutMode.List -> R.drawable.ic_layout_list_24dp
    }

    companion object {
        private val PAGES = arrayOf(
                FragmentListPagerAdapter.Page(R.string.deviations_browse_page_hot, HotDeviationsFragment::class.java),
                FragmentListPagerAdapter.Page(R.string.deviations_browse_page_popular, PopularDeviationsFragment::class.java),
                FragmentListPagerAdapter.Page(R.string.deviations_browse_page_undiscovered, UndiscoveredDeviationsFragment::class.java),
                FragmentListPagerAdapter.Page(R.string.deviations_browse_page_daily, DailyDeviationsFragment::class.java))
    }
}

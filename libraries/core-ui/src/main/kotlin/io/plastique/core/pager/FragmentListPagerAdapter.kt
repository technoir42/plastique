package io.plastique.core.pager

import android.content.Context
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.github.technoir42.android.extensions.instantiate
import java.lang.reflect.Field

class FragmentListPagerAdapter private constructor(
    private val context: Context,
    private val fragmentManager: FragmentManager,
    private val pages: List<Page>
) : FragmentStatePagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    constructor(activity: FragmentActivity, pages: List<Page>) : this(activity, activity.supportFragmentManager, pages)
    constructor(fragment: Fragment, pages: List<Page>) : this(fragment.requireContext(), fragment.childFragmentManager, pages)

    @Suppress("UNCHECKED_CAST")
    private val fragments = FIELD_FRAGMENTS.get(this) as List<Fragment>

    override fun getItem(position: Int): Fragment {
        val page = pages[position]
        val args = Bundle(page.args).apply {
            putBoolean(ARG_IN_VIEWPAGER, true)
        }
        return fragmentManager.fragmentFactory.instantiate(context, page.fragmentClass, args)
    }

    override fun getCount(): Int = pages.size

    override fun getPageTitle(position: Int): CharSequence? {
        return context.getString(pages[position].titleId)
    }

    fun getFragment(position: Int): Fragment? {
        return fragments[position]
    }

    data class Page(@StringRes val titleId: Int, val fragmentClass: Class<out Fragment>, val args: Bundle = Bundle.EMPTY)

    companion object {
        internal const val ARG_IN_VIEWPAGER = "in_viewpager"

        private val FIELD_FRAGMENTS: Field by lazy(LazyThreadSafetyMode.NONE) {
            FragmentStatePagerAdapter::class.java.getDeclaredField("mFragments").apply { isAccessible = true }
        }
    }
}

val Fragment.isInViewPager: Boolean
    get() = arguments?.getBoolean(FragmentListPagerAdapter.ARG_IN_VIEWPAGER, false) == true

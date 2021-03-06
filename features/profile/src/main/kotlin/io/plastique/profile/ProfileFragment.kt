package io.plastique.profile

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import androidx.core.view.isVisible
import io.plastique.core.BaseFragment
import io.plastique.core.mvvm.viewModel
import io.plastique.core.navigation.navigationContext
import io.plastique.inject.getComponent
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject

class ProfileFragment : BaseFragment(R.layout.fragment_profile) {
    @Inject lateinit var navigator: ProfileNavigator

    private val viewModel: ProfileViewModel by viewModel()

    private lateinit var signInButton: Button

    override fun onAttach(context: Context) {
        super.onAttach(context)
        navigator.attach(navigationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        signInButton = view.findViewById(R.id.button_sign_in)
        signInButton.setOnClickListener { navigator.openSignIn() }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel.state
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { renderState(it) }
            .disposeOnDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_profile, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.profile_action_view_watchers -> {
            navigator.openWatchers(null)
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun renderState(state: ProfileViewState) {
        signInButton.isVisible = state.showSignInButton
    }

    override fun injectDependencies() {
        getComponent<ProfileFragmentComponent>().inject(this)
    }
}

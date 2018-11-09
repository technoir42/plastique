package io.plastique.auth

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.fragment.app.DialogFragment
import io.plastique.auth.LoginEvent.ErrorDialogDismissedEvent
import io.plastique.core.MvvmActivity
import io.plastique.core.dialogs.MessageDialogFragment
import io.plastique.core.dialogs.OnDismissDialogListener
import io.plastique.core.dialogs.ProgressDialogController
import io.plastique.core.extensions.setActionBar
import io.plastique.core.extensions.showAllowingStateLoss
import io.plastique.inject.getComponent
import io.plastique.util.Animations
import io.reactivex.android.schedulers.AndroidSchedulers

class LoginActivity : MvvmActivity<LoginViewModel>(), OnDismissDialogListener {
    private lateinit var progressBar: ProgressBar
    private lateinit var webView: WebView
    private lateinit var progressDialogController: ProgressDialogController

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        setActionBar(R.id.toolbar) {
            setDisplayHomeAsUpEnabled(true)
        }

        progressBar = findViewById(R.id.progress)
        webView = findViewById(R.id.webview)
        webView.settings.javaScriptEnabled = true
        webView.webChromeClient = LoginWebChromeClient()
        webView.webViewClient = LoginWebViewClient()

        progressDialogController = ProgressDialogController(supportFragmentManager)

        viewModel.state
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { renderState(it) }
                .disposeOnDestroy()
    }

    override fun onDismissDialog(dialog: DialogFragment) {
        if (DIALOG_AUTH_ERROR == dialog.tag) {
            viewModel.dispatch(ErrorDialogDismissedEvent)
        }
    }

    private fun renderState(state: LoginViewState) = when (state) {
        LoginViewState.Initial -> Unit

        is LoginViewState.LoadUrl ->
            webView.loadUrl(state.authUrl)

        LoginViewState.InProgress ->
            progressDialogController.show(R.string.login_progress_title)

        LoginViewState.Success -> {
            progressDialogController.dismiss()
            finish()
        }

        LoginViewState.Error -> {
            progressDialogController.dismiss()
            val dialog = MessageDialogFragment.newInstance(R.string.common_error, R.string.login_error_message)
            dialog.showAllowingStateLoss(supportFragmentManager, DIALOG_AUTH_ERROR)
        }
    }

    override fun injectDependencies() {
        getComponent<AuthActivityComponent>().inject(this)
    }

    private fun setLoadProgress(progress: Int) {
        progressBar.progress = progress
        if (progress != 100) {
            progressBar.alpha = 1.0f
            progressBar.visibility = View.VISIBLE
        } else {
            Animations.fadeOut(progressBar, Animations.DURATION_SHORT)
        }
    }

    private inner class LoginWebChromeClient : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            setLoadProgress(newProgress)
        }
    }

    private inner class LoginWebViewClient : WebViewClient() {
        @Suppress("OverridingDeprecatedMember")
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            return viewModel.onRedirect(url)
        }
    }

    companion object {
        private const val DIALOG_AUTH_ERROR = "dialog.auth_error"

        fun createIntent(context: Context): Intent {
            return Intent(context, LoginActivity::class.java)
        }
    }
}

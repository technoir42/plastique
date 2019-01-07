package io.plastique.deviations

import io.plastique.core.session.Session
import io.plastique.core.session.SessionManager
import io.plastique.deviations.list.LayoutMode
import io.plastique.util.Preferences
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import javax.inject.Inject

class ContentSettings @Inject constructor(
    private val preferences: Preferences,
    private val sessionManager: SessionManager
) {
    val showLiterature: Boolean
        get() = preferences.getBoolean(PREF_SHOW_LITERATURE, true)

    val showLiteratureChanges: Observable<Boolean>
        get() = preferences.observable().getBoolean(PREF_SHOW_LITERATURE, true)

    val showMature: Boolean
        get() = preferences.getBoolean(PREF_SHOW_MATURE_CONTENT, false) && sessionManager.session is Session.User

    val showMatureChanges: Observable<Boolean>
        get() = Observables.combineLatest(
                preferences.observable().getBoolean(PREF_SHOW_MATURE_CONTENT, false),
                sessionManager.sessionChanges) { showMatureContent, session -> showMatureContent && session is Session.User }

    var layoutMode: LayoutMode
        get() = preferences.get(PREF_LAYOUT_MODE, LayoutMode.DEFAULT)
        set(value) = preferences.edit { put(PREF_LAYOUT_MODE, value) }

    val layoutModeChanges: Observable<LayoutMode>
        get() = preferences.observable().get(PREF_LAYOUT_MODE, LayoutMode.DEFAULT)

    companion object {
        private const val PREF_SHOW_LITERATURE = "content.show_literature"
        private const val PREF_SHOW_MATURE_CONTENT = "content.show_mature"
        private const val PREF_LAYOUT_MODE = "browse.layout_mode"
    }
}

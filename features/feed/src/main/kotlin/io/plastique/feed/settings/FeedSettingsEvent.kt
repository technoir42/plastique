package io.plastique.feed.settings

import com.sch.neon.Event

sealed class FeedSettingsEvent : Event() {
    data class FeedSettingsLoadedEvent(val settings: FeedSettings, val items: List<OptionItem>) : FeedSettingsEvent()
    data class LoadErrorEvent(val error: Throwable) : FeedSettingsEvent()
    data class SetEnabledEvent(val optionKey: String, val isEnabled: Boolean) : FeedSettingsEvent()
    object RetryClickEvent : FeedSettingsEvent()
}

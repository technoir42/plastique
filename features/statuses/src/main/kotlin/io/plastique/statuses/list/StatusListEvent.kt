package io.plastique.statuses.list

import com.sch.neon.Event
import io.plastique.core.lists.ListItem

sealed class StatusListEvent : Event() {
    data class ItemsChangedEvent(val items: List<ListItem>, val hasMore: Boolean) : StatusListEvent() {
        override fun toString(): String =
            "ItemsChangedEvent(items=${items.size}, hasMore=$hasMore)"
    }

    data class LoadErrorEvent(val error: Throwable) : StatusListEvent()

    object LoadMoreEvent : StatusListEvent()
    object LoadMoreStartedEvent : StatusListEvent()
    object LoadMoreFinishedEvent : StatusListEvent()
    data class LoadMoreErrorEvent(val error: Throwable) : StatusListEvent()

    object RefreshEvent : StatusListEvent()
    object RefreshFinishedEvent : StatusListEvent()
    data class RefreshErrorEvent(val error: Throwable) : StatusListEvent()

    object RetryClickEvent : StatusListEvent()
    object SnackbarShownEvent : StatusListEvent()

    data class ShowMatureChangedEvent(val showMature: Boolean) : StatusListEvent()
    data class UserChangedEvent(val userId: String?) : StatusListEvent()
}

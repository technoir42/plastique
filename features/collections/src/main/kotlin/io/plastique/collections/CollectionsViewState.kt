package io.plastique.collections

import io.plastique.core.content.ContentState
import io.plastique.core.lists.ListItem

data class CollectionsViewState(
    val params: FolderLoadParams,

    val contentState: ContentState,
    val signInNeeded: Boolean,

    val collectionItems: List<ListItem> = emptyList(),
    val items: List<ListItem> = emptyList(),
    val snackbarMessage: String? = null,

    val hasMore: Boolean = false,
    val loadingMore: Boolean = false,
    val refreshing: Boolean = false
) {
    val pagingEnabled: Boolean
        get() = contentState === ContentState.Content && hasMore && !loadingMore && !refreshing
}

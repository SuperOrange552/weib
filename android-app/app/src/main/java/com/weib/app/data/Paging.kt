package com.weib.app.data

data class PageSlice<T>(val items: List<T>, val page: Int, val totalPages: Int)

data class PagingState<T>(
    val items: List<T> = emptyList(),
    val page: Int = -1,
    val pageSize: Int = 20,
    val totalPages: Int = 1,
    val refreshing: Boolean = false,
    val appending: Boolean = false,
    val error: String? = null
) {
    val hasNext: Boolean get() = page + 1 < totalPages
}

object PagingReducer {
    fun <T> startRefresh(state: PagingState<T>): PagingState<T> =
        state.copy(page = -1, totalPages = 1, refreshing = true, appending = false, error = null)

    fun <T> startAppend(state: PagingState<T>): PagingState<T> =
        if (state.refreshing || state.appending || !state.hasNext) state
        else state.copy(appending = true, error = null)

    fun <T, K> pageLoaded(
        state: PagingState<T>,
        page: PageSlice<T>,
        keyOf: (T) -> K
    ): PagingState<T> {
        val merged = if (page.page == 0 || state.refreshing) page.items else
            (state.items + page.items).distinctBy(keyOf)
        return state.copy(
            items = merged,
            page = page.page,
            totalPages = page.totalPages.coerceAtLeast(page.page + 1),
            refreshing = false,
            appending = false,
            error = null
        )
    }

    fun <T> pageFailed(state: PagingState<T>, message: String): PagingState<T> =
        state.copy(refreshing = false, appending = false, error = message)
}

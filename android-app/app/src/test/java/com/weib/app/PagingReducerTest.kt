package com.weib.app

import com.weib.app.data.PageSlice
import com.weib.app.data.PagingReducer
import com.weib.app.data.PagingState
import org.junit.Assert.*
import org.junit.Test

class PagingReducerTest {
    data class Item(val id: Long)

    @Test fun refreshReplacesExistingItems() {
        val old = PagingState(items = listOf(Item(9)), page = 3)
        val loading = PagingReducer.startRefresh(old)
        val next = PagingReducer.pageLoaded(loading, PageSlice(listOf(Item(1)), 0, 2), Item::id)
        assertEquals(listOf(1L), next.items.map(Item::id))
        assertEquals(0, next.page)
        assertTrue(next.hasNext)
    }

    @Test fun appendDeduplicatesStableIds() {
        val old = PagingState(items = listOf(Item(1)), page = 0, totalPages = 2)
        val loading = PagingReducer.startAppend(old)
        val next = PagingReducer.pageLoaded(loading, PageSlice(listOf(Item(1), Item(2)), 1, 2), Item::id)
        assertEquals(listOf(1L, 2L), next.items.map(Item::id))
        assertFalse(next.hasNext)
    }

    @Test fun duplicateAppendStartIsIgnoredAndFailurePreservesItems() {
        val old = PagingState(items = listOf(Item(1)), page = 0, totalPages = 3, appending = true)
        assertSame(old, PagingReducer.startAppend(old))
        val failed = PagingReducer.pageFailed(old, "network")
        assertEquals(listOf(1L), failed.items.map(Item::id))
        assertEquals("network", failed.error)
        assertFalse(failed.appending)
    }
}

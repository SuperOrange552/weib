package com.weib.app

import com.weib.app.data.notification.NotificationDeduplicator
import org.junit.Assert.*
import org.junit.Test

class NotificationDeduplicatorTest {
    @Test fun duplicateEventOnlyNotifiesOnce() {
        val deduplicator = NotificationDeduplicator(100)
        assertTrue(deduplicator.accept("evt-1"))
        assertFalse(deduplicator.accept("evt-1"))
    }
}

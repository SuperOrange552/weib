package com.weib.app.data.notification

class NotificationDeduplicator(private val capacity: Int = 500) {
    private val ids = LinkedHashSet<String>()
    @Synchronized fun accept(eventId: String): Boolean {
        if (!ids.add(eventId)) return false
        while (ids.size > capacity) ids.remove(ids.first())
        return true
    }
}

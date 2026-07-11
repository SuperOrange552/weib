package com.weib.app

import com.weib.app.ui.AppDestination
import com.weib.app.ui.destinationsForRole
import com.weib.app.ui.theme.WeibPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UiContractTest {
    @Test fun webPaletteKeepsReadableBlackText() {
        assertEquals(0xFF0F172A, WeibPalette.Title)
        assertEquals(0xFF334155, WeibPalette.Body)
        assertEquals(0xFFF5F7FB, WeibPalette.Background)
    }

    @Test fun seekerAndBossHaveDifferentNavigationWithoutAdmin() {
        val seeker = destinationsForRole("seeker")
        val boss = destinationsForRole("boss")
        assertTrue(seeker.contains(AppDestination.Jobs))
        assertTrue(boss.contains(AppDestination.Dashboard))
        assertFalse((seeker + boss).any { it.route.contains("admin") })
    }
}
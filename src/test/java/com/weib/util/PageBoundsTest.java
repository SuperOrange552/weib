package com.weib.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageBoundsTest {
    @Test void defaultsToTwentyAndNormalizesNegativePage() {
        PageBounds bounds = PageBounds.of(-3, 0);
        assertEquals(0, bounds.page());
        assertEquals(20, bounds.size());
    }

    @Test void capsPageSizeAtFifty() {
        assertEquals(50, PageBounds.of(1, 500).size());
    }
}

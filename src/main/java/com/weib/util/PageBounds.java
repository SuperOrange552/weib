package com.weib.util;

public record PageBounds(int page, int size) {
    public static PageBounds of(int page, int size) {
        return new PageBounds(Math.max(page, 0), size < 1 ? 20 : Math.min(size, 50));
    }
}

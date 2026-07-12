package com.weib.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MapServiceTest {
    @Test
    void geocodeUriEncodesChineseAddressExactlyOnce() {
        var uri = MapService.buildGeocodeUri("上海市松江区九泾路168号", null, "test-key");

        assertThat(uri.getRawQuery()).contains("address=%E4%B8%8A%E6%B5%B7");
        assertThat(uri.getRawQuery()).doesNotContain("%25E4%25B8%258A");
        assertThat(uri.getRawQuery()).contains("key=test-key");
    }
}

package com.weib.session;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionRegistryWiringTest {
    @Test void productionConstructorIsExplicitlyAutowiredWhenTestConstructorsAlsoExist() throws Exception {
        var constructor = SessionRegistryService.class.getConstructor(
                StringRedisTemplate.class, ObjectMapper.class, SecuritySessionNotifier.class);
        assertTrue(constructor.isAnnotationPresent(Autowired.class));
    }
}

package com.weib.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class SessionRegistryServiceTest {
    @Test
    void mobileLoginReplacesOnlyPreviousMobileSlot() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ObjectMapper mapper = new ObjectMapper();
        LoginSlot previous = new LoginSlot("m1", "SEEKER", "MOBILE", 1000L, "d1");
        when(redis.execute(any(DefaultRedisScript.class), eq(List.of("login:slot:7:MOBILE")),
                anyString(), anyString())).thenReturn(mapper.writeValueAsString(previous));
        SessionRegistryService registry = new SessionRegistryService(redis, mapper,
                Clock.fixed(Instant.ofEpochMilli(2000L), ZoneOffset.UTC));

        SessionReplacement replacement = registry.register(7L, ClientType.MOBILE, "m2", "BOSS", "d2");

        assertNotNull(replacement.replaced());
        assertEquals("m1", replacement.replaced().sid());
        assertEquals("m2", replacement.current().sid());
        assertEquals("BOSS", replacement.current().activeRole());
    }

    @Test
    void firstLoginHasNoReplacement() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(DefaultRedisScript.class), anyList(), anyString(), anyString())).thenReturn(null);
        SessionRegistryService registry = new SessionRegistryService(redis, new ObjectMapper(), Clock.systemUTC());

        assertNull(registry.register(8L, ClientType.WEB, "w1", "SEEKER", "browser").replaced());
    }

    @Test
    void onlyTheSidStoredInTheTerminalSlotRemainsValid() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ObjectMapper mapper = new ObjectMapper();
        when(redis.opsForValue()).thenReturn(mock(org.springframework.data.redis.core.ValueOperations.class));
        when(redis.opsForValue().get("login:slot:9:WEB"))
                .thenReturn(mapper.writeValueAsString(new LoginSlot("w2", "BOSS", "WEB", 3L, "browser")));
        SessionRegistryService registry = new SessionRegistryService(redis, mapper, Clock.systemUTC());

        assertTrue(registry.isCurrent(9L, ClientType.WEB, "w2"));
        assertFalse(registry.isCurrent(9L, ClientType.WEB, "w1"));

        registry.invalidate(9L, ClientType.WEB);
        verify(redis).delete("login:slot:9:WEB");
    }

    @Test
    void replacementAndGlobalInvalidationPublishSecurityEvents() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ObjectMapper mapper = new ObjectMapper();
        SecuritySessionNotifier notifier = mock(SecuritySessionNotifier.class);
        LoginSlot previous = new LoginSlot("old", "SEEKER", "MOBILE", 1L, "d1");
        when(redis.execute(any(DefaultRedisScript.class), anyList(), anyString(), anyString()))
                .thenReturn(mapper.writeValueAsString(previous));
        SessionRegistryService registry = new SessionRegistryService(redis, mapper, Clock.systemUTC(), notifier);

        registry.register(10L, ClientType.MOBILE, "new", "BOSS", "d2");
        registry.invalidateAll(10L, SessionInvalidationReason.PASSWORD_CHANGED);

        verify(notifier).forceLogout(eq(10L), eq(previous), eq(SessionInvalidationReason.KICKED));
        verify(notifier).forceLogout(eq(10L), isNull(), eq(SessionInvalidationReason.PASSWORD_CHANGED));
        verify(redis).delete(List.of("login:slot:10:WEB", "login:slot:10:MOBILE"));
    }
}

package com.weib.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class SessionRegistryService {
    static final long SLOT_TTL_SECONDS = Duration.ofHours(2).toSeconds();
    private static final DefaultRedisScript<String> REPLACE_SCRIPT = new DefaultRedisScript<>(
            "local old = redis.call('GET', KEYS[1]); "
                    + "redis.call('SET', KEYS[1], ARGV[1], 'EX', ARGV[2]); "
                    + "return old;",
            String.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public SessionRegistryService(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this(redis, objectMapper, Clock.systemUTC());
    }

    SessionRegistryService(StringRedisTemplate redis, ObjectMapper objectMapper, Clock clock) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public SessionReplacement register(Long userId, ClientType clientType, String sid,
                                       String activeRole, String deviceIdHash) {
        LoginSlot current = new LoginSlot(
                sid,
                activeRole.toUpperCase(Locale.ROOT),
                clientType.name(),
                clock.millis(),
                deviceIdHash);
        String oldJson = redis.execute(
                REPLACE_SCRIPT,
                List.of(key(userId, clientType)),
                write(current),
                String.valueOf(SLOT_TTL_SECONDS));
        return new SessionReplacement(read(oldJson).orElse(null), current);
    }

    public Optional<LoginSlot> current(Long userId, ClientType clientType) {
        return read(redis.opsForValue().get(key(userId, clientType)));
    }

    public boolean isCurrent(Long userId, ClientType clientType, String sid) {
        return current(userId, clientType).map(slot -> slot.sid().equals(sid)).orElse(false);
    }

    public void invalidate(Long userId, ClientType clientType) {
        redis.delete(key(userId, clientType));
    }

    public void invalidateAll(Long userId) {
        redis.delete(List.of(key(userId, ClientType.WEB), key(userId, ClientType.MOBILE)));
    }

    private String key(Long userId, ClientType clientType) {
        return "login:slot:" + userId + ":" + clientType.name();
    }

    private String write(LoginSlot slot) {
        try {
            return objectMapper.writeValueAsString(slot);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("无法序列化登录会话", e);
        }
    }

    private Optional<LoginSlot> read(String json) {
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, LoginSlot.class));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Redis 登录会话数据格式错误", e);
        }
    }
}

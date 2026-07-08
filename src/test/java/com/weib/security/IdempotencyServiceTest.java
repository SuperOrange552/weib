package com.weib.security;
import org.junit.jupiter.api.*;import org.springframework.data.redis.core.*;import java.time.Duration;import static org.junit.jupiter.api.Assertions.*;import static org.mockito.Mockito.*;
class IdempotencyServiceTest {
 StringRedisTemplate redis=mock(StringRedisTemplate.class); @SuppressWarnings("unchecked") ValueOperations<String,String> ops=mock(ValueOperations.class); IdempotencyService service;
 @BeforeEach void init(){when(redis.opsForValue()).thenReturn(ops);service=new IdempotencyService(redis);}
 @Test void firstRequestAcquires(){when(ops.get("idempotency:s:valid123")).thenReturn(null);when(ops.setIfAbsent("idempotency:s:valid123","PROCESSING",Duration.ofSeconds(30))).thenReturn(true);assertEquals(IdempotencyService.State.ACQUIRED,service.acquire("s","valid123"));}
 @Test void duplicatesAreDistinguished(){when(ops.get("idempotency:s:valid123")).thenReturn("PROCESSING","COMPLETED");assertEquals(IdempotencyService.State.PROCESSING,service.acquire("s","valid123"));assertEquals(IdempotencyService.State.COMPLETED,service.acquire("s","valid123"));}
 @Test void completionHasTenMinuteTtl(){service.complete("s","valid123");verify(ops).set("idempotency:s:valid123","COMPLETED",Duration.ofMinutes(10));}
 @Test void redisFailureDegrades(){when(ops.get(anyString())).thenThrow(new RuntimeException("down"));assertEquals(IdempotencyService.State.DEGRADED,service.acquire("s","valid123"));}
 @Test void rejectsUnsafeKeys(){assertThrows(IllegalArgumentException.class,()->service.acquire("s","bad key!"));}
}

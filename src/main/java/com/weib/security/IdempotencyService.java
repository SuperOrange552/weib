package com.weib.security;
import lombok.RequiredArgsConstructor;import org.slf4j.*;import org.springframework.data.redis.core.StringRedisTemplate;import org.springframework.stereotype.Service;import java.time.Duration;
@Service @RequiredArgsConstructor
public class IdempotencyService {
 private static final Logger log=LoggerFactory.getLogger(IdempotencyService.class); private final StringRedisTemplate redis;
 public enum State{ACQUIRED,PROCESSING,COMPLETED,DEGRADED}
 public State acquire(String scope,String key){validate(key);String k=redisKey(scope,key);try{String state=redis.opsForValue().get(k);if("PROCESSING".equals(state))return State.PROCESSING;if("COMPLETED".equals(state))return State.COMPLETED;Boolean ok=redis.opsForValue().setIfAbsent(k,"PROCESSING",Duration.ofSeconds(30));return Boolean.TRUE.equals(ok)?State.ACQUIRED:State.PROCESSING;}catch(RuntimeException e){log.warn("Idempotency Redis unavailable; database safeguards remain active: {}",e.getMessage());return State.DEGRADED;}}
 public void complete(String scope,String key){try{redis.opsForValue().set(redisKey(scope,key),"COMPLETED",Duration.ofMinutes(10));}catch(RuntimeException e){log.warn("Unable to complete idempotency marker: {}",e.getMessage());}}
 public void release(String scope,String key){try{redis.delete(redisKey(scope,key));}catch(RuntimeException ignored){}}
 private void validate(String key){if(key==null||!key.matches("^[A-Za-z0-9._:-]{8,128}$"))throw new IllegalArgumentException("非法幂等键");}
 private String redisKey(String scope,String key){return "idempotency:"+scope+":"+key;}
}
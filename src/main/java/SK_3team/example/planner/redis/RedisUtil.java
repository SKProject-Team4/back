package SK_3team.example.planner.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

// Redis: key:value값을 login 할 때 자동으로 저장되게

@RequiredArgsConstructor
@Service
public class RedisUtil {
    private final RedisTemplate<String, Object> redisTemplate;

    public void setData(String key, String value, Long expiredTime){
        redisTemplate.opsForValue().set(key, value, expiredTime, TimeUnit.MILLISECONDS);
    }

    public String getData(String key){
        return (String) redisTemplate.opsForValue().get(key);
    }

    public void deleteData(String key){
        redisTemplate.delete(key);
    }

    // 블랙리스트 등록
    public void setBlackList(String token, Long expiredTimeMs) {
        redisTemplate.opsForValue().set("blacklist:" + token, "logout", expiredTimeMs, TimeUnit.MILLISECONDS);
    }

    public boolean isBlackListed(String token) {
        return redisTemplate.hasKey("blacklist:" + token);
    }

    // logincheck
    public boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

}
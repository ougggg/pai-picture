package com.ouguofeng.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ouguofeng.service.CacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 通用缓存服务实现
 * 提供两级缓存：本地缓存（Caffeine）+ Redis分布式缓存
 */
@Slf4j
@Service
public class CacheServiceImpl implements CacheService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 本地缓存对象
     */
    private final Cache<String, String> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(1024)
            .maximumSize(10_000L)
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();

    /**
     * 默认过期时间（秒）
     */
    private static final int DEFAULT_EXPIRE_SECONDS = 600; // 10分钟

    /**
     * 锁的键前缀
     */
    private static final String LOCK_KEY_PREFIX = "cache:lock:";
    
    /**
     * 图片列表缓存键前缀
     */
    private static final String PICTURE_LIST_CACHE_KEY_PREFIX = "listPictureVOByPage:";

    @Override
    public String get(String key) {
        // 1. 先从本地缓存中查询
        String cachedValue = LOCAL_CACHE.getIfPresent(key);
        if (cachedValue != null) {
            return cachedValue;
        }
        
        // 2. 本地缓存未命中，查询 Redis 分布式缓存
        ValueOperations<String, String> opsForValue = stringRedisTemplate.opsForValue();
        cachedValue = opsForValue.get(key);
        if (cachedValue != null) {
            // 如果缓存命中，更新本地缓存
            LOCAL_CACHE.put(key, cachedValue);
        }
        return cachedValue;
    }

    @Override
    public void set(String key, String value, Integer expireSeconds) {
        if (expireSeconds == null) {
            expireSeconds = DEFAULT_EXPIRE_SECONDS;
        }
        
        // 添加随机时间，防止缓存雪崩
        int randomExpire = expireSeconds + RandomUtil.randomInt(0, expireSeconds / 2);
        
        // 1. 写入 Redis
        ValueOperations<String, String> opsForValue = stringRedisTemplate.opsForValue();
        opsForValue.set(key, value, randomExpire, TimeUnit.SECONDS);
        
        // 2. 写入本地缓存
        LOCAL_CACHE.put(key, value);
    }

    @Override
    public void delete(String key) {
        // 1. 删除本地缓存
        LOCAL_CACHE.invalidate(key);
        
        // 2. 删除 Redis 缓存
        stringRedisTemplate.delete(key);
    }

    @Override
    public void deleteByPattern(String pattern) {
        try {
            // 1. 清除本地缓存（全部清除，因为无法精确匹配）
            // 注意：这里为了简单，清除所有本地缓存，实际可以维护一个key列表
            
            // 2. 使用SCAN代替KEYS，避免阻塞Redis
            Set<String> keys = scanKeys(pattern);
            if (keys != null && !keys.isEmpty()) {
                // 分批删除，避免一次性删除过多键
                int batchSize = 100;
                Set<String> batch = new HashSet<>();
                for (String key : keys) {
                    batch.add(key);
                    LOCAL_CACHE.invalidate(key); // 同时清除本地缓存
                    if (batch.size() >= batchSize) {
                        stringRedisTemplate.delete(batch);
                        batch.clear();
                    }
                }
                if (!batch.isEmpty()) {
                    stringRedisTemplate.delete(batch);
                }
                log.info("清除缓存，模式: {}，共 {} 个缓存项", pattern, keys.size());
            }
        } catch (Exception e) {
            log.error("清除缓存失败，模式: {}", pattern, e);
        }
    }

    /**
     * 使用SCAN命令扫描匹配的键（非阻塞）
     * @param pattern 匹配模式
     * @return 匹配的键集合
     */
    private Set<String> scanKeys(String pattern) {
        Set<String> keys = new HashSet<>();
        try {
            ScanOptions options = ScanOptions.scanOptions()
                    .match(pattern)
                    .count(100) // 每次扫描100个键
                    .build();
            
            try (Cursor<String> cursor = stringRedisTemplate.scan(options)) {
                while (cursor.hasNext()) {
                    keys.add(cursor.next());
                }
            }
        } catch (Exception e) {
            log.error("扫描Redis键失败", e);
        }
        return keys;
    }

    @Override
    public String getLocalCache(String key) {
        return LOCAL_CACHE.getIfPresent(key);
    }

    @Override
    public void putLocalCache(String key, String value) {
        LOCAL_CACHE.put(key, value);
    }

    @Override
    public boolean tryLock(String lockKey, long timeout) {
        String fullLockKey = LOCK_KEY_PREFIX + lockKey;
        try {
            // 使用SET NX EX实现分布式锁
            Boolean result = stringRedisTemplate.opsForValue()
                    .setIfAbsent(fullLockKey, "1", timeout, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("获取分布式锁失败，lockKey: {}", lockKey, e);
            return false;
        }
    }

    @Override
    public void releaseLock(String lockKey) {
        String fullLockKey = LOCK_KEY_PREFIX + lockKey;
        try {
            stringRedisTemplate.delete(fullLockKey);
        } catch (Exception e) {
            log.error("释放分布式锁失败，lockKey: {}", lockKey, e);
        }
    }

    @Override
    public void clearPictureCache() {
        // 异步清除缓存，避免阻塞主流程
        CompletableFuture.runAsync(() -> {
            try {
                // 1. 清除本地缓存（快速操作）
                LOCAL_CACHE.invalidateAll();
                
                // 2. 使用SCAN代替KEYS，避免阻塞Redis
                Set<String> keys = scanKeys(PICTURE_LIST_CACHE_KEY_PREFIX + "*");
                if (keys != null && !keys.isEmpty()) {
                    // 分批删除，避免一次性删除过多键
                    int batchSize = 100;
                    Set<String> batch = new HashSet<>();
                    int count = 0;
                    for (String key : keys) {
                        batch.add(key);
                        if (batch.size() >= batchSize) {
                            stringRedisTemplate.delete(batch);
                            count += batch.size();
                            batch.clear();
                        }
                    }
                    if (!batch.isEmpty()) {
                        stringRedisTemplate.delete(batch);
                        count += batch.size();
                    }
                    log.info("清除图片列表缓存，共 {} 个缓存项", count);
                }
            } catch (Exception e) {
                log.error("清除Redis缓存失败", e);
            }
        });
    }
}


package com.ouguofeng.service;

/**
 * 通用缓存服务接口
 * 提供统一的缓存操作方法
 */
public interface CacheService {
    
    /**
     * 获取缓存值
     * @param key 缓存键
     * @return 缓存值，如果不存在返回null
     */
    String get(String key);
    
    /**
     * 设置缓存值
     * @param key 缓存键
     * @param value 缓存值
     * @param expireSeconds 过期时间（秒），如果为null则使用默认过期时间
     */
    void set(String key, String value, Integer expireSeconds);
    
    /**
     * 删除缓存
     * @param key 缓存键
     */
    void delete(String key);
    
    /**
     * 批量删除缓存（根据模式匹配）
     * @param pattern 匹配模式，如 "user:vo:*"
     */
    void deleteByPattern(String pattern);
    
    /**
     * 获取本地缓存中的值
     * @param key 缓存键
     * @return 缓存值，如果不存在返回null
     */
    String getLocalCache(String key);
    
    /**
     * 设置本地缓存
     * @param key 缓存键
     * @param value 缓存值
     */
    void putLocalCache(String key, String value);
    
    /**
     * 尝试获取分布式锁（防止缓存击穿）
     * @param lockKey 锁的键
     * @param timeout 超时时间（秒）
     * @return 是否获取到锁
     */
    boolean tryLock(String lockKey, long timeout);
    
    /**
     * 释放分布式锁
     * @param lockKey 锁的键
     */
    void releaseLock(String lockKey);
    
    /**
     * 清除图片列表缓存
     * 清除本地缓存和Redis缓存（异步执行，不阻塞）
     * 清除匹配 "listPictureVOByPage:*" 的所有缓存
     */
    void clearPictureCache();
}


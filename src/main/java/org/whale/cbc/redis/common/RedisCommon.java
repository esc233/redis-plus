package org.whale.cbc.redis.common;

import lombok.Getter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.whale.cbc.redis.config.Properties.RedisCommonProperties;
import org.whale.cbc.redis.service.LockService;
import org.whale.cbc.redis.service.PubSubService;
import org.whale.cbc.redis.service.QueueService;
import org.whale.cbc.redis.service.RedisService;

/**
 * @Author yakik
 * @DATE 2019/6/4
 * @DESCRIPTION :
 */
public class RedisCommon {
    @Getter
    private LockService lockService;
    @Getter
    private PubSubService pubSubService;
    @Getter
    private QueueService queueService;
    @Getter
    private RedisService redisService;
    @Getter
    private RedisCommonProperties redisCommonProperties;
    @Getter
    private StringRedisTemplate stringRedisTemplate;
    @Getter
    private RedisConnectionFactory redisConnectionFactory;

    public RedisCommon(RedisConnectionFactory redisConnectionFactory,RedisCommonProperties redisCommonProperties){
        init(redisConnectionFactory,redisCommonProperties);
    }

    private void init(RedisConnectionFactory redisConnectionFactory,RedisCommonProperties redisCommonProperties){
        this.stringRedisTemplate = new StringRedisTemplate();
        this.stringRedisTemplate.setConnectionFactory(redisConnectionFactory);
        this.stringRedisTemplate.afterPropertiesSet();
        this.redisCommonProperties = redisCommonProperties;
        this.redisConnectionFactory = redisConnectionFactory;
        BeanCache beanCache = new BeanCache(this.stringRedisTemplate,redisCommonProperties.getBeanCache().isCacheInit(),redisCommonProperties.getBeanCache().getRoot());
        RedisMessageListenerContainer redisMessageListenerContainer = new RedisMessageListenerContainer();
        redisMessageListenerContainer.setConnectionFactory(redisConnectionFactory);
        this.pubSubService = new PubSubService(this.stringRedisTemplate,redisMessageListenerContainer);
        this.lockService = new LockService(this.stringRedisTemplate,beanCache,
                redisCommonProperties.getRedisLock().getResolutionMillis(),
                redisCommonProperties.getRedisLock().getExpireMsecs(),
                redisCommonProperties.getRedisLock().getTimeoutMsecs(),
                redisCommonProperties.getRedisLock().getIsShowOldValue());
        this.lockService.setEnableLock(redisCommonProperties.getRedisLock().getEnableLock());
        this.queueService = new QueueService();
        this.queueService.setStringRedisTemplate(this.stringRedisTemplate);
        this.queueService = new QueueService();
        this.redisService = new RedisService(stringRedisTemplate,beanCache,lockService);
    }
}

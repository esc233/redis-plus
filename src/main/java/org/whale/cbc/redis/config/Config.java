package org.whale.cbc.redis.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.whale.cbc.redis.common.BeanCache;
import org.whale.cbc.redis.config.Properties.RedisCommonProperties;
import org.whale.cbc.redis.service.LockService;
import org.whale.cbc.redis.service.PubSubService;
import org.whale.cbc.redis.service.QueueService;
import org.whale.cbc.redis.service.RedisService;

/**
 * @Author thuglife
 * @DATE 2017/6/17
 * @DESCRIPTION :
 */
@Configuration
@ConditionalOnProperty(prefix = RedisCommonProperties.PROPERTEIS_PREFIX,value = "do-init",havingValue = "true",matchIfMissing = true)
@EnableConfigurationProperties({RedisCommonProperties.class})
public class Config {
    @Autowired
    private RedisCommonProperties redisCommonProperties;
    @Autowired
    private RedisConnectionFactory redisConnectionFactory;
    @Bean(initMethod = "init")
    @ConditionalOnMissingBean(BeanCache.class)
    public BeanCache beanCache(){
        return new BeanCache(stringRedisTemplate(),redisCommonProperties.getBeanCache().isCacheInit(),redisCommonProperties.getBeanCache().getRoot());
    }
    @Bean
    @ConditionalOnMissingBean(RedisMessageListenerContainer.class)
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory redisConnectionFactory){
        RedisMessageListenerContainer redisMessageListenerContainer = new RedisMessageListenerContainer();
        redisMessageListenerContainer.setConnectionFactory(redisConnectionFactory);
        return redisMessageListenerContainer;
    }
    @Bean
    @ConditionalOnMissingBean(PubSubService.class)
    public PubSubService pubSubService(RedisMessageListenerContainer redisMessageListenerContainer){
        return new PubSubService(stringRedisTemplate(),redisMessageListenerContainer);
    }

    @Bean
    @ConditionalOnMissingBean(LockService.class)
    public LockService lockService(BeanCache beanCache){
        LockService lockService= new LockService(stringRedisTemplate(),beanCache,
                redisCommonProperties.getRedisLock().getResolutionMillis(),
                redisCommonProperties.getRedisLock().getExpireMsecs(),
                redisCommonProperties.getRedisLock().getTimeoutMsecs());
        lockService.setEnableLock(redisCommonProperties.getRedisLock().getEnableLock());
        return lockService;
    }
    @Bean(initMethod = "init",destroyMethod = "shutdown")
    @ConditionalOnMissingBean(QueueService.class)
    public QueueService queueService(){
        QueueService queueService =  new QueueService();
        queueService.setStringRedisTemplate(stringRedisTemplate());
        return queueService;
    }
    @Bean
    @ConditionalOnMissingBean(StringRedisTemplate.class)
    public StringRedisTemplate stringRedisTemplate() {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }
    @Bean
    @ConditionalOnMissingBean(RedisService.class)
    public RedisService redisService(StringRedisTemplate stringRedisTemplate, BeanCache beanCache, LockService lockService){
        RedisService redisService=new RedisService(stringRedisTemplate,beanCache,lockService);
        return redisService;
    }
}

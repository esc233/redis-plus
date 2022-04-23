package org.whale.cbc.redis.config.Properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author yakik
 * @DATE 2018/9/16
 * @DESCRIPTION :
 */
@Data
@ConfigurationProperties(prefix = "redis-common")
public class RedisCommonProperties {
    public boolean doInit=true;
    public final static String PROPERTEIS_PREFIX = "redis-common";
    private final RedisCommonProperties.BeanCache beanCache =new RedisCommonProperties.BeanCache();
    private final RedisCommonProperties.RedisLock redisLock = new RedisCommonProperties.RedisLock();
    private final RedisCommonProperties.RedisQueue redisQueue = new RedisCommonProperties.RedisQueue();

    @Data
    public static class BeanCache{
        BeanCache(){

        }
        /**
         * 是否启动时初始化缓存
         */
        private boolean cacheInit = false;
        /**
         *扫描路径
         */
        private String root;
    }
    @Data
    public static class RedisLock{
        RedisLock(){

        }
        /**
         * 再次获取锁间隔
         */
        private int resolutionMillis =100;
        /**
         * 锁超时时间，防止线程在入锁以后，无限的执行等待
         */
        private int expireMsecs = 3000;
        /**
         * 锁等待时间，防止线程饥饿
         */
        private int timeoutMsecs = 1000;
        /**
         * 是否打印原先的旧值
         */
        private Boolean isShowOldValue = false;
        /**
         * 打印旧值排除key
         */
        private List<String> keyExclude = new ArrayList<String>();

        private Boolean enableLock = true;
    }

    @Data
    public static class RedisQueue{
        RedisQueue(){

        }
        private int timeOut=200;
    }

}

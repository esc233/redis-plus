package org.whale.cbc.redis.service;

import lombok.Setter;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.whale.cbc.redis.annotation.RedisKey;
import org.whale.cbc.redis.common.BeanCache;
import org.whale.cbc.redis.common.LogbackOldValue;
import org.whale.cbc.redis.common.RedisCbcConstants;
import org.whale.cbc.redis.util.RdbUtils;

import javax.annotation.PostConstruct;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * @Author thuglife
 * @DATE 2017/6/8
 * @DESCRIPTION : redis分布式锁
 */
public class LockService {
    private Logger log = LoggerFactory.getLogger(getClass());
    private final String lockKeyPre= RedisCbcConstants.LOCK_KEY_PRE + RedisCbcConstants.KEY_SEPARATOR;
    private int resolutionMillis =100;
    private int expireMsecs = 3000;
    private int timeoutMsecs = 1000;
    private Boolean isShowOldValue = true;
    private List<String> keyExclude;
    private StringRedisTemplate redisTemplate;
    private BeanCache beanCache;
    @Setter
    private boolean enableLock=true;

    public LockService(){

    }

    public LockService(StringRedisTemplate redisTemplate, BeanCache beanCache){
        this.redisTemplate=redisTemplate;
        this.beanCache=beanCache;
    }

    public LockService(StringRedisTemplate redisTemplate,BeanCache beanCache,int resolutionMillis,int expireMsecs,int timeoutMsecs){
        this.redisTemplate=redisTemplate;
        this.beanCache=beanCache;
        this.expireMsecs=expireMsecs;
        this.timeoutMsecs=timeoutMsecs;
        this.resolutionMillis=resolutionMillis;
    }

    public LockService(StringRedisTemplate redisTemplate,BeanCache beanCache,int resolutionMillis,int expireMsecs,int timeoutMsecs,boolean isShowOldValue){
        this.redisTemplate=redisTemplate;
        this.beanCache=beanCache;
        this.expireMsecs=expireMsecs;
        this.timeoutMsecs=timeoutMsecs;
        this.resolutionMillis=resolutionMillis;
        this.isShowOldValue=isShowOldValue;
    }

    @PostConstruct
    private void init(){

    }
    /**
     * 获得 lock.
     * 实现思路: 主要是使用了redis 的setnx命令,缓存了锁.
     * reids缓存的key是锁的key,所有的共享, value是锁的到期时间(注意:这里把过期时间放在value了,没有时间上设置其超时时间)
     * 执行过程:
     * 1.通过setnx尝试设置某个key的值,成功(当前没有这个锁)则返回,成功获得锁
     * 2.锁已经存在则获取锁的到期时间,和当前时间比较,超时的话,则设置新的值
     * @param key
     * @return true if lock is acquired, false acquire timeouted
     * @throws InterruptedException in case of thread interruption
     */

    public boolean lock(String key){
        return lock(key,this.timeoutMsecs,this.expireMsecs);
    }

    public boolean autoLock(String key){
        if(!enableLock){
            return true;
        }else {
            return lock(key);
        }
    }

    public<T> boolean autoLock(T t){
        if(!enableLock){
            return true;
        }else{
            return lock(t);
        }
    }

    /**
     * 不重试
     * @param t
     * @param <T>
     * @return
     */
    public<T> boolean lockAtOnce(T t){
        Class<?> clas = t.getClass();
        if(clas.isAnnotationPresent(RedisKey.class)){
            return lockAtOnce(beanCache.getKeyCache(t.getClass()).getKey(t));
        }else if(t instanceof String){
            return lockAtOnce((String)t);
        }
        return false;
    }

    public<T> boolean lockAtOnce(T t,int expireMsecs){
        Class<?> clas = t.getClass();
        if(clas.isAnnotationPresent(RedisKey.class)){
            return lockAtOnce(beanCache.getKeyCache(t.getClass()).getKey(t),expireMsecs);
        }else if(t instanceof String){
            return lockAtOnce((String)t,expireMsecs);
        }
        return false;
    }

    public boolean lockAtOnce(String key){
        if(doLock(key)){
            return true;
        }else {
            return false;
        }
    }

    public boolean lockAtOnce(String key,int expireMsecs){
        if(doLock(key,expireMsecs)){
            return true;
        }else {
            return false;
        }
    }

    private boolean doLock(String key){
        return doLock(key,this.expireMsecs);
    }

    private boolean doLock(String key,int expireMsecs){
        String lockKey = this.lockKeyPre+key;
        String expiresStr = (System.currentTimeMillis() + expireMsecs + 1)+"";
        if(redisTemplate.opsForValue().setIfAbsent(lockKey,expiresStr)){
            log.info("获取锁{}成功",lockKey);
            showOldValue(key);
            return true;
        }
        String currentValueStr = redisTemplate.opsForValue().get(lockKey);
        if (currentValueStr != null && Long.parseLong(currentValueStr) < System.currentTimeMillis()) {
            //判断是否为空，不为空的情况下，如果被其他线程设置了值，则第二个条件判断是过不去的
            //为空说其他线程释放了锁，下一循环去获取锁
            // lock is expired
            String oldValueStr = redisTemplate.opsForValue().getAndSet(lockKey, expiresStr);
            System.out.println("expiresStr:"+expiresStr);
            System.out.println("oldValueStr:"+oldValueStr);
            System.out.println("currentTimeMillis:"+System.currentTimeMillis());
            System.out.println("currentValueStr:"+currentValueStr);
            System.out.println("----:"+(System.currentTimeMillis()-Long.parseLong(currentValueStr)));
            //获取上一个锁到期时间，并设置现在的锁到期时间，
            //只有一个线程才能获取上一个线上的设置时间，因为jedis.getSet是同步的
            if (oldValueStr == null || oldValueStr.equals(currentValueStr)) {
                //防止误删（覆盖，因为key是相同的）了他人的锁——这里达不到效果，这里值会被覆盖，但是因为什么相差了很少的时间，所以可以接受
                //为空说明锁被其他线程释放，getSet操作获得了锁
                //[分布式的情况下]:如过这个时候，多个线程恰好都到了这里，但是只有一个线程的设置值和当前值相同，他才有权利获取锁
                // lock acquired
                log.info("旧锁超时，获取{}成功",lockKey);
                showOldValue(key);
                return true;
            }
        }
        return false;
    }
    /**
     *
     * @param key 上锁的key
     * @param timeoutMsecs 获取锁重试时间
     * @param expireMsecs 锁占用超时时间
     * @return
     */
    public boolean lock(String key,int timeoutMsecs,int expireMsecs){
        try {
            String lockKey = this.lockKeyPre+key;
            int timeout = timeoutMsecs;
            while (timeout>0){
                if(doLock(key,expireMsecs)){
                    return true;
                }
                int ran2 = (int) (Math.random()*(100-1)+1);
                timeout -= (resolutionMillis+ran2);
                /*
                    延迟100 毫秒,  这里使用随机时间可能会好一点,可以防止饥饿进程的出现,即,当同时到达多个进程,
                    只会有一个进程获得锁,其他的都用同样的频率进行尝试,后面有来了一些进行,也以同样的频率申请锁,这将可能导致前面来的锁得不到满足.
                    使用随机的等待时间可以一定程度上保证公平性
                 */
                try {
                    log.info("{}ms后再获取key",resolutionMillis+ran2);
                    Thread.sleep(resolutionMillis+ran2);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
            log.warn("获取{}超时",lockKey);
            return false;

        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }


    public<T> boolean lock(T t){
        Class<?> clas = t.getClass();
        if(clas.isAnnotationPresent(RedisKey.class)){
            return lock(beanCache.getKeyCache(t.getClass()).getKey(t));
        }else if(t instanceof String){
            return lock(t);
        }
        return false;
    }
    /**
     * 释放锁
     * @param key
     */
    public void unlock(String key) {
        try {
            log.info("释放锁:{}", key);
            String lockKey = this.lockKeyPre + key;
            redisTemplate.opsForValue().getOperations().delete(lockKey);
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public <T> void unlock(T t) {
        Class<?> clas = t.getClass();
        if(clas.isAnnotationPresent(RedisKey.class)){
            unlock(beanCache.getKeyCache(t.getClass()).getKey(t));
        }else {
            throw new RuntimeException("输入的泛型必须有RedisKey注释");
        }
    }

    public void showOldValue(String key){
        if (!isShowOldValue||Strings.isBlank(key)){
            return;
        }
        if(keyExclude!=null&&keyExclude.contains(key)){
            return;
        }
        try {
            String type = redisTemplate.opsForValue().getOperations().type(key).code();
            String table =key.split(RedisCbcConstants.KEY_SEPARATOR)[0];
            if(Strings.isBlank(table)){
                return;
            }
            switch (type){
                case "hash":
                    LogbackOldValue.info(log,table,"oldValue----->{}的旧值为:",key);

                    byte[] a=redisTemplate.opsForValue().getOperations().dump(key);
                    ByteBuffer buf = ByteBuffer.wrap(a);
                    List<byte[]> list= RdbUtils.readHashmapAsZipList(buf);
                    for(int i=0;i<list.size();i+=2){
                        LogbackOldValue.info(log,table,"oldValue----->key:{},field:{},value:{}",key,new String(list.get(i), "UTF-8"),new String(list.get(i+1), "UTF-8"));
                    }
                    break;
                case "string":
                    String value = redisTemplate.opsForValue().get(key);
                    LogbackOldValue.info(log,table,"oldValue----->{}的旧值为:{}",key,value);
            }
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

}

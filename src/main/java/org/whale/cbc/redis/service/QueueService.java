package org.whale.cbc.redis.service;

import com.google.common.base.Strings;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.whale.cbc.redis.handler.ConsumerHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * @Author thuglife
 * @DATE 2017/6/8
 * @DESCRIPTION :
 */
public class QueueService {
    class ConsumerThread implements Runnable{
        private String key;
        private ConsumerHandler consumerHandler;
        private BoundListOperations<String,String> boundListOperations;
        private boolean consumerIsRun = true;
        public ConsumerThread(String key, ConsumerHandler consumerHandler){
            this.consumerHandler = consumerHandler;
            this.key=key;
            this.boundListOperations=stringRedisTemplate.boundListOps(key);
        }

        public void stop(){
            this.consumerIsRun = false;
        }
        @Override
        public void run(){
            while (this.consumerIsRun&&isRun){
                if(boundListOperations.size()!=0) {
                    String value = boundListOperations.rightPop();
                    if (!Strings.isNullOrEmpty(value)) {
                        try {
                            consumerHandler.whenGetRecord(key, value);
                        } catch (Exception e) {
                            log.error(">>>redis消息队列消费异常，key:{},message:{}", new Object[]{key, value});
                            if (consumerHandler.whenRunFailed(key, value, e)) {
                                log.error(">>>redis消息队列回退重试，key:{},message:{}", new Object[]{key, value});
                                product(key, value);
                            }
                        }
                    }
                }else {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        continue;
                    }
                }
                continue;
            }
            log.info("结束订阅{}",key);
        }
    }
    private Logger log = LoggerFactory.getLogger(getClass());
    private ExecutorService executor;
    private boolean isRun = false;
    private boolean stoping =false;
    @Setter
    private StringRedisTemplate stringRedisTemplate;
    private List<ConsumerThread> consumerThreads = new ArrayList<ConsumerThread>();
    private void init(){
        this.executor = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>());
    }

    /**
     * 生产消息
     * @param key
     * @param value
     */
    public void product(String key,String value){
        stringRedisTemplate.opsForList().leftPush(key,value);
        log.info("队列:{}插入任务:{}",key,value);
    }

    /**
     * 生产消息
     * @param key
     * @param values
     */
    public void product(String key,String[] values){
        stringRedisTemplate.opsForList().leftPushAll(key,values);
        log.info("队列:{}插入任务:{}",key,values);
    }

    /**
     * 创造消费者
     * @param keys
     * @param consumerHandler
     */
    public boolean createConsumer(List<String> keys,ConsumerHandler consumerHandler){
        if(stoping){
            log.error("正在停止消费者稍后再试");
            return false;
        }else{
            if(!isRun){
                isRun=true;
            }
            if(executor==null){
                executor = Executors.newCachedThreadPool();
            }
            keys.forEach(key->{
                ConsumerThread consumerThread = new ConsumerThread(key,consumerHandler);
                this.executor.submit(consumerThread);
                consumerThreads.add(consumerThread);
            });
            return true;
        }

    }

    public boolean stopConsumerAll(){
        if(this.stoping){
            log.error("正在停止消费者稍后再试");
            return false;
        }else {
            this.stoping = true;
            for(ConsumerThread consumerThread:consumerThreads){
                consumerThread.stop();
            }
            consumerThreads.clear();
            this.stoping = false;
            return true;
        }
    }

    /**
     * 停止消费者
     */
    public void shutdown() {
        log.info("开始停止消费者");
        isRun = false;
        if (executor!= null) {
            executor.shutdown();
        }
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                log.debug("线程池超时关闭，强制结束线程");
            }
        } catch (InterruptedException ignored) {
            log.error("线程意外终止");
            Thread.currentThread().interrupt();
        }
        executor =null;

    }

    public boolean isStoping(){
        return this.stoping;
    }

}

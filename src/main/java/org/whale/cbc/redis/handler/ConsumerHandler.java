package org.whale.cbc.redis.handler;

/**
 * @Author huangs
 * @DATE 2017/5/16
 * @DESCRIPTION :消费者方法接口
 */
public interface ConsumerHandler {
    /**
     * 收到消息时方法
     * @param key
     * @param value
     */
    void whenGetRecord(String key,String value) throws Exception ;

    /**
     * 消费异常时方法,返回true时该record的重做，返回false时作废
     * @param key
     * @param value
     * @param exception
     */
    Boolean whenRunFailed(String key,String value, Exception exception);

}

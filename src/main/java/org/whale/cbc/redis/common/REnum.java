package org.whale.cbc.redis.common;

/**
 * @Author yakik
 * @DATE 2019/8/1
 * @DESCRIPTION :
 */
public interface REnum {
    REnum createEnum(String redisValue);
    String getRedisValue();
}

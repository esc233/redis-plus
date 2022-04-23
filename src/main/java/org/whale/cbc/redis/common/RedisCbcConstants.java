package org.whale.cbc.redis.common;

/**
 * @Author huangs
 * @DATE 2017/5/11
 * @DESCRIPTION :静态常量
 */
public class RedisCbcConstants {
    public final static String KEY_SEPARATOR = ":";
    public final static String LOCK_KEY_PRE = "LOCK";
    public final static String TMP_KEY_PRE = "TMP";
    public final static int READ_COMMITTED = 1;
    public final static int SERIALIZABLE  = 3;

    public final static String REDIS_TYPE_STRING = "string";
}

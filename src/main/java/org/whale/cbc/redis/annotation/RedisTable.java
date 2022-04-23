package org.whale.cbc.redis.annotation;

import java.lang.annotation.*;

/**
 * @Author huangs
 * @DATE 2017/5/11
 * @DESCRIPTION :标识该pojo类为该redis对应hash存储pojo
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
public @interface RedisTable {
    /** 表名*/
    String tableName() default "";
    /** 数据库表对应的序列名 */
    String sequence() default "";
    /** key */
    String key() default "";
}

package org.whale.cbc.redis.annotation;

import java.lang.annotation.*;

/**
 * @Author thuglife
 * @DATE 2017/7/28
 * @DESCRIPTION :
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
public @interface RedisKey {
    /** 前缀名*/
    String prefix() default "";
    /** key */
    String key() default "";
}

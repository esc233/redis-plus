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
    /** åįžå*/
    String prefix() default "";
    /** key */
    String key() default "";
}

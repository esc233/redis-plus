package org.whale.cbc.redis.annotation;

import java.lang.annotation.*;

/**
 * @Author thuglife
 * @DATE 2017/7/28
 * @DESCRIPTION :
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@Documented
public @interface RedisKeyParam {
    /** 替换key占位符符号 **/
    String keyParam() default "";
}

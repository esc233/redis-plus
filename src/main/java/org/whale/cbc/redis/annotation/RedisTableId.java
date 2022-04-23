package org.whale.cbc.redis.annotation;

import java.lang.annotation.*;

/**
 * @Author huangs
 * @DATE 2017/5/11
 * @DESCRIPTION :
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@Documented
public @interface RedisTableId {

	/** 替换key占位符符号 **/
	String keyParam() default "";
	/**副id*/
	boolean subId() default false;
}

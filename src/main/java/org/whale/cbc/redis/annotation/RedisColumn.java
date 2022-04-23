package org.whale.cbc.redis.annotation;

import java.lang.annotation.*;

/**
 * @Author huangs
 * @DATE 2017/5/11
 * @DESCRIPTION :标识pojo事务时需要watch的属性
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@Documented
public @interface RedisColumn {
    /**是否可为空*/
    boolean nullable() default true;
    /**是否可修改*/
    boolean editable() default true;
    /**是否唯一*//**唯一且不可修改的属性会另外以key-id的方式保存用于查找id*/
    boolean unique() default false;
    /**副id*/
    boolean subId() default false;
    /** 替换key占位符符号 **/
    String keyParam() default "";
    /** 在incr方法中是否可incr**/
    boolean incrable() default false;
    /**是否是redis属性**/
    boolean redisParam() default true;
    /**保留几位小数，存在redis已整数形式**/
    int digits() default 0;
}

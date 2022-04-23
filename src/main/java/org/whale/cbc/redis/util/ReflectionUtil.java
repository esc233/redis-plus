package org.whale.cbc.redis.util;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.whale.cbc.redis.common.REnum;
import org.whale.cbc.redis.testBean.enums.ValueEnum;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Date;

/**
 * @Author huangs
 * @DATE 2017/5/11
 * @DESCRIPTION :
 */
public class ReflectionUtil {
    private static final Logger logger = LoggerFactory.getLogger(ReflectionUtil.class);
    /**
     * 通过反射, 获得Class定义中声明的父类的泛型参数的类型.
     * 如无法找到, 返回Object.class.
     * eg.
     * public UserDao extends HibernateDao<User>
     *
     * @param clazz The class to introspect
     * @return the first generic declaration, or Object.class if cannot be determined
     */
    @SuppressWarnings("all")
    public static <T> Class<T> getSuperClassGenricType(final Class clazz) {
        return getSuperClassGenricType(clazz, 0);
    }

    /**
     * 通过反射, 获得定义Class时声明的父类的泛型参数的类型.
     * 如无法找到, 返回Object.class.
     *
     * 如public UserDao extends HibernateDao<User,Long>
     *
     * @param clazz clazz The class to introspect
     * @param index the Index of the generic ddeclaration,start from 0.
     * @return the index generic declaration, or Object.class if cannot be determined
     */
    @SuppressWarnings("all")
    public static Class getSuperClassGenricType(final Class clazz, final int index) {

        Type genType = clazz.getGenericSuperclass();

        if (!(genType instanceof ParameterizedType)) {
            logger.warn(clazz.getSimpleName() + "'s superclass not ParameterizedType");
            return Object.class;
        }

        Type[] params = ((ParameterizedType) genType).getActualTypeArguments();

        if (index >= params.length || index < 0) {
            logger.warn("Index: " + index + ", Size of " + clazz.getSimpleName() + "'s Parameterized Type: "
                    + params.length);
            return Object.class;
        }
        if (!(params[index] instanceof Class)) {
            logger.warn(clazz.getSimpleName() + " not set the actual class on superclass generic parameter");
            return Object.class;
        }

        return (Class) params[index];
    }
    /**
     * 将各种类转换为redis用的String型
     */
    public static String ToStringForRedis(Object param){
        if (param instanceof Boolean) {
            return (Boolean)param?"true":"false";
        } else if (param instanceof Date) {
            return TimeUtil.formatDate((Date)param,"yyyy-MM-dd HH:mm:ss");
        }else if(param instanceof BigDecimal){
            return Double.toString(((BigDecimal) param).doubleValue());
        } else if(param instanceof Duration){
            return ((Duration) param).getSeconds()+"";
        }else {
            return param.toString();
        }
    }
    /**
     * String 根据type转换对应object
     */
    public static Object ToObjectByType(String param,Type type){
        if(null==param||param.isEmpty())return null;
        if(type.equals(Long.class)){
            return Long.valueOf(param);
        }else if(type.equals(long.class)){
            return Long.valueOf(param).longValue();
        }else if(type.equals(Integer.class)){
            return Integer.valueOf(param);
        }else if(type.equals(int.class)){
            return Integer.valueOf(param).intValue();
        }else if(type.equals(String.class)){
            return param;
        }else if(type.equals(Date.class)){
            return TimeUtil.parseDateStr(param,"yyyy-MM-dd HH:mm:ss");
        }else if(type.equals(Boolean.class)){
            return param.trim().equals("true")?true:false;
        }else if(type.equals(Float.class)){
            return Float.valueOf(param);
        }else if(type.equals(Double.class)){
            return Double.valueOf(param);
        }else if(type.equals(BigDecimal.class)){
            return new BigDecimal(param.trim());
        }else if(type.equals(Duration.class)){
            return Duration.ofSeconds(Long.valueOf(param.trim()));
        }
//        else if(type.getClass().isInstance(REnum.class)){
//            try {
//                Method method=type.getClass().getMethod("createEnum",String.class);
//                return method.invoke(type.getClass().newInstance(),param.trim());
//            }catch (NoSuchMethodException|IllegalAccessException|InstantiationException|InvocationTargetException e){
//                return null;
//            }
//
//        }
        else{
            return param;
        }

    }
}

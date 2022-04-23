package org.whale.cbc.redis.util;

import org.whale.cbc.redis.common.REnum;
import org.whale.cbc.redis.tableBuild.Acolumn;
import org.whale.cbc.redis.tableBuild.Atable;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author thuglife
 * @DATE 2017/6/1
 * @DESCRIPTION :
 */
public class RedisUtil {
    public static Map<String,String> beanToMap(Object bean, Atable atable) throws IllegalAccessException{
        Map<String,String> param = new HashMap<String, String>();
        for (Acolumn acolumn:atable.getCols()){
            Object value = acolumn.getField().get(bean);
            if(null == value)continue;
            if(REnum.class.isAssignableFrom(acolumn.getField().getType())){
                REnum rEnum=(REnum)value;
                value=rEnum.getRedisValue();
            }
            if(acolumn.getAttrType()==BigDecimal.class&&((BigDecimal)value).compareTo(BigDecimal.ZERO)==0)value=0;
            if(acolumn.getDigits()!=null)value=Math.round(Double.valueOf(value.toString()).doubleValue()*acolumn.getDigits());
            param.put(acolumn.getAttrName(), ReflectionUtil.ToStringForRedis(value));
        }
        return param;
    }

    public static <T> T mapToBean(Map<String,String> map,T t,Atable atable) throws IllegalAccessException,InstantiationException{
        for (Acolumn acolumn:atable.getCols()){
            if(acolumn.getDigits()!=null&&map.get(acolumn.getAttrName())!=null)map.put(acolumn.getAttrName(),Double.toString(Double.valueOf(map.get(acolumn.getAttrName()))/acolumn.getDigits()));
            Object value=ReflectionUtil.ToObjectByType(map.get(acolumn.getAttrName()),acolumn.getAttrType());
            if(value!=null)acolumn.getField().set(t,value);
        }
        //atable.getIdColumn().getField().set(t,Long.valueOf(map.get(atable.getIdColumn().getAttrName())));
        return t;
    }
}

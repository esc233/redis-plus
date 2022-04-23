package org.whale.cbc.redis.tableBuild;

import lombok.Data;
import org.whale.cbc.redis.common.RedisCbcConstants;
import org.whale.cbc.redis.util.ReflectionUtil;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author thuglife
 * @DATE 2017/7/28
 * @DESCRIPTION :
 */
@Data
public class Akey {
    private Map<String,Field> params = new HashMap<String, Field>();
    private String prefix="";
    private String keyTemplate="";
    private Class<?> clazz;
    private String entityName;

    public <T> String getKey(T t){
        if(keyTemplate.isEmpty()||prefix.isEmpty()||params.size()==0){
            return null;
        }
        String _key=new String(keyTemplate);
        for(Map.Entry<String,Field> entry:params.entrySet()){
            try {
                Object value =entry.getValue().get(t);
                if(null==value)return null;
                _key=_key.replaceFirst(entry.getKey(),ReflectionUtil.ToStringForRedis(value));
            }catch (Exception e){
                e.printStackTrace();
                return null;
            }
        }
        return prefix+RedisCbcConstants.KEY_SEPARATOR+_key;
    }
}

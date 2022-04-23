package org.whale.cbc.redis.tableBuild;

import lombok.Data;
import org.whale.cbc.redis.util.ReflectionUtil;
import org.whale.cbc.redis.common.RedisCbcConstants;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Author huangs
 * @DATE 2017/5/11
 * @DESCRIPTION :
 */
@Data
public class Atable {

	/** table 实体名 */
	private String entityName;

	private String tableName;

	private Acolumn idColumn;

	private String keyTemplate;

	private List<Field> subIdFields;

	private List<Field> incrables;

	private Map<String,Field> keyFields;

	private Class<?> clazz;
	
	private List<Acolumn> cols;

	private String sequence;

	public List<Field> getFields(){
		List<Field> list = new ArrayList<Field>(cols.size());
		for(Acolumn col : cols){
			list.add(col.getField());
		}
		return list;
	}

	@Override
	public String toString() {
		return "Atable [entityName=" + entityName + ", clazz=" + clazz
				+ ",  cols=" + cols +  "]";
	}

	public <T> String getKey(T t){
		try {
			if(keyTemplate.isEmpty()){
				Object id = idColumn.getField().get(t);
				if(null==id)throw new Exception(idColumn.getAttrName()+"字段为空");
				return tableName+ RedisCbcConstants.KEY_SEPARATOR+idColumn.getAttrName()+ RedisCbcConstants.KEY_SEPARATOR+ReflectionUtil.ToStringForRedis(id);
			}
			String key=new String(keyTemplate);
			for(Map.Entry<String,Field> entry:keyFields.entrySet()){
				Object value =entry.getValue().get(t);
				if(null==value)return null;
				key=key.replaceFirst(entry.getKey(),ReflectionUtil.ToStringForRedis(value));
			}
			return tableName+ RedisCbcConstants.KEY_SEPARATOR+key;
		}catch (Exception e){
			e.printStackTrace();
		}
		return null;
	}

	public String getSubKey(String fieldName,Object value){
		return this.tableName+ RedisCbcConstants.KEY_SEPARATOR+fieldName+ RedisCbcConstants.KEY_SEPARATOR+ ReflectionUtil.ToStringForRedis(value);
	}

}

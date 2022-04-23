package org.whale.cbc.redis.tableBuild;

import lombok.Data;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
/**
 * @Author huangs
 * @DATE 2017/5/11
 * @DESCRIPTION :
 */
@Data
public class Acolumn {
	//字段类型 
	private Type attrType;
	//字段名
	private String attrName;
	//字段
	private Field field;
	//是否使用自增模式更新
	private Boolean incrable=false;

	private Integer digits;
}

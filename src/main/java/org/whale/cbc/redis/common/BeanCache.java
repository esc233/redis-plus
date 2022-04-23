package org.whale.cbc.redis.common;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.Assert;
import org.whale.cbc.redis.annotation.*;
import org.whale.cbc.redis.tableBuild.Acolumn;
import org.whale.cbc.redis.tableBuild.Akey;
import org.whale.cbc.redis.tableBuild.Atable;
import org.whale.cbc.redis.util.AnnotationUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author thuglife
 * @DATE 2017/5/25
 * @DESCRIPTION :
 */
public class BeanCache {
    private StringRedisTemplate redisTemplate;
    /**
     * bean缓存
     */
    @Getter
    private static ConcurrentHashMap<String,Atable> tableCache =new ConcurrentHashMap<>();
    /**
     key缓存
     */
    @Getter
    private static ConcurrentHashMap<String,Akey> keyCache =new ConcurrentHashMap<>();
    private Logger log = LoggerFactory.getLogger(getClass());
    private boolean cacheInit;
    private String root;
    private boolean isInit=false;
    public BeanCache(StringRedisTemplate redisTemplate, boolean cacheInit, String root){
        this.redisTemplate=redisTemplate;
        this.cacheInit=cacheInit;
        this.root=root;
    }

    public void init(){
        if(!cacheInit||isInit){
            return;
        }
        int count=0;
        log.info("开始缓存pojo类---->地址:{}",root);
        List<Class<?>> classes = AnnotationUtil.getClasses(root);
        for (Class clas :classes) {
            if(clas.isAnnotationPresent(RedisTable.class)){
                Atable atable=addCache(clas);
                count++;
            }else if(clas.isAnnotationPresent(RedisKey.class)){
                Akey akey=addkey(clas);
                count++;
            }
        }
        log.info("缓存pojo类结束,共缓存{}个pojo",count);
    }
    public Atable getBeanCache(Class<?> clazz){
        if(tableCache.containsKey(clazz.getName())){
            return tableCache.get(clazz.getName());
        }else {
            log.info("未找到{}的缓存,生成该缓存",clazz.getName());
            return addCache(clazz);
        }
    }

    public Akey getKeyCache(Class<?> clazz){
        if(keyCache.containsKey(clazz.getName())){
            return keyCache.get(clazz.getName());
        }else {
            log.info("未找到{}的缓存,生成该缓存",clazz.getName());
            return addkey(clazz);
        }
    }
    public Akey addkey(Class<?> clazz){
        log.info("开始缓存{}",clazz.getName());
        Akey akey = new Akey();
        akey.setClazz(clazz);
        akey.setEntityName(clazz.getName());
        RedisKey redisKey = clazz.getAnnotation(RedisKey.class);
        if(redisKey==null||redisKey.prefix().isEmpty()||redisKey.key().isEmpty()){
            throw new RuntimeException(clazz.getName()+"未设置key模板");
        }else{
            akey.setPrefix(redisKey.prefix());
            akey.setKeyTemplate(redisKey.key());
        }
        Map<String,Field> keyParamMap=new HashMap<String, Field>();
        Field[] fields = clazz.getDeclaredFields();
        for(Field field : fields) {
            field.setAccessible(true);
            if (field.getName().equals("serialVersionUID")){
                continue;
            }
            if(field.isAnnotationPresent(RedisKeyParam.class)){
                RedisKeyParam redisKeyParam = field.getAnnotation(RedisKeyParam.class);
                if(redisKeyParam.keyParam().isEmpty()){
                    throw new RuntimeException(clazz.getName()+"存在keyParam为空属性");
                }
                keyParamMap.put(redisKeyParam.keyParam(),field);
            }
        }
        akey.setParams(keyParamMap);
        keyCache.putIfAbsent(clazz.getName(),akey);
        return akey;
    }

    public Atable addCache(Class<?> clazz){

        log.info("开始缓存{}",clazz.getName());
        Atable atable = new Atable();
        atable.setClazz(clazz);
        atable.setEntityName(clazz.getName());
        RedisTable redisTable = clazz.getAnnotation(RedisTable.class);
        if(redisTable ==null||redisTable.tableName().isEmpty()){
            throw new RuntimeException(clazz.getName()+"未设置表名");
        }else if(redisTable ==null||redisTable.key().isEmpty()){
            throw new RuntimeException(clazz.getName()+"未设置key模板");
        }else{
            atable.setTableName(redisTable.tableName());
            atable.setKeyTemplate(redisTable.key());
            atable.setSequence(redisTable.sequence());
        }
        Field[] fields = clazz.getDeclaredFields();
        List<Acolumn> cols  = new ArrayList<Acolumn>();
        List<Field> subIdField = new ArrayList<Field>();
        Map<String,Field> keyParamMap=new HashMap<String, Field>();
        for(Field field : fields) {
            field.setAccessible(true);
            if (field.getName().equals("serialVersionUID")){
                continue;
            }
            if(Modifier.isStatic(field.getModifiers())){
                continue;
            }
            if (Modifier.isFinal(field.getModifiers())){
                continue;
            }
            Acolumn column = new Acolumn();
            column.setAttrName(field.getName());
            column.setAttrType(field.getType());
            column.setField(field);
            if(field.isAnnotationPresent(RedisTableId.class)){
                if(atable.getIdColumn()==null){
                    RedisTableId redisTableId = field.getAnnotation(RedisTableId.class);
                    if(!redisTableId.keyParam().isEmpty()){
                        keyParamMap.put(redisTableId.keyParam(),field);
                    }
                    atable.setIdColumn(column);
                    if(redisTableId.subId()){
                        subIdField.add(field);
                    }
                }
                else throw new RuntimeException(clazz.getName()+"有重复id");
            }else if(field.isAnnotationPresent(RedisColumn.class)){
                RedisColumn redisColumn =field.getAnnotation(RedisColumn.class);
                if(!redisColumn.redisParam()){
                    continue;
                }
                if(redisColumn.subId()){
                    subIdField.add(field);
                }
                if(!redisColumn.keyParam().isEmpty()){
                    keyParamMap.put(redisColumn.keyParam(),field);
                }
                if(redisColumn.incrable()){
                    column.setIncrable(true);
                }
                if(redisColumn.digits()!=0){
                    if(field.getType() == Double.class||field.getType() == double.class || field.getType() == float.class || field.getType() ==Float.class || field.getType() == BigDecimal.class)
                    {
                        column.setDigits(redisColumn.digits());
                    }
                    else {
                        throw new RuntimeException(clazz.getName()+"-->"+field.getName()+"不是浮点类型不能填写digits");
                    }
                }
            }
            cols.add(column);
        }
        if(atable.getIdColumn()!=null){
            Assert.isTrue(atable.getIdColumn().getAttrType().equals(Long.class),clazz.getName()+"id不是long型");
            if(atable.getSequence()!=null&&!atable.getSequence().isEmpty()){
                atable.setSequence(atable.getSequence());
                try {
                    log.info("检查并生成序列{}",atable.getSequence());
                    redisTemplate.opsForValue().setIfAbsent(atable.getSequence(),"100000");
                }catch (Exception e){
                    e.printStackTrace();
                    log.error(e.getMessage());
                    throw new RuntimeException(e.getMessage());
                }
            }
        }
        atable.setCols(cols);
        atable.setSubIdFields(subIdField);
        atable.setKeyFields(keyParamMap);
        tableCache.putIfAbsent(clazz.getName(),atable);

        log.info("结束缓存{}",clazz.getName());
        return atable;
    }
}

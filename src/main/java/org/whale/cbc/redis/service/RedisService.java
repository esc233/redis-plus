package org.whale.cbc.redis.service;

import lombok.Getter;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.*;
import org.springframework.util.StringUtils;
import org.whale.cbc.redis.annotation.RedisKey;
import org.whale.cbc.redis.annotation.RedisTable;
import org.whale.cbc.redis.common.BeanCache;
import org.whale.cbc.redis.tableBuild.Acolumn;
import org.whale.cbc.redis.tableBuild.Akey;
import org.whale.cbc.redis.tableBuild.Atable;
import org.whale.cbc.redis.util.RedisUtil;
import org.whale.cbc.redis.util.ReflectionUtil;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Author huangs
 * @DATE 2017/5/11
 * @DESCRIPTION :
 */
public class RedisService {

    private Logger log = LoggerFactory.getLogger(getClass());
    @Getter
    private StringRedisTemplate stringRedisTemplate;
    private BeanCache beanCache;
    private LockService lockService;

    public RedisService(StringRedisTemplate stringRedisTemplate,BeanCache beanCache,LockService lockService){
        this.stringRedisTemplate=stringRedisTemplate;
        this.beanCache=beanCache;
        this.lockService = lockService;
    }

    /**
     * 根据key取出map(不使用redis自带的hgetAll)
     * @param key
     * @return
     */
    public Map<String,String> hgetAll(String key){
        try {
            if(Strings.isBlank(key)||!exists(key))return null;
            BoundHashOperations boundHashOperations=stringRedisTemplate.boundHashOps(key);
            return boundHashOperations.entries();
//            byte[] a=stringRedisTemplate.dump(key);
//            ByteBuffer buf = ByteBuffer.wrap(a);
//            List<byte[]> list= RdbUtils.readHashmapAsZipList(buf);
//            Map<String,String> map=new HashMap<String, String>();
//            for(int i=0;i<list.size();i+=2){
//                map.put(new String(list.get(i), StandardCharsets.UTF_8),new String(list.get(i+1), StandardCharsets.UTF_8));
//            }
//            return map;
        }catch (Exception e){
            e.printStackTrace();
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    public Map<String,String> hgetAll(String key,Atable atable){
        try {
            if(Strings.isBlank(key)||!exists(key))return null;
            BoundHashOperations boundHashOperations=stringRedisTemplate.boundHashOps(key);
            Map<String,String> map=new HashMap<String, String>();
            atable.getCols().forEach(acolumn -> {
                Object value=boundHashOperations.get(acolumn.getAttrName());
                if(Objects.nonNull(value)) {
                    map.put(acolumn.getAttrName(),value.toString());
                }
            });
            return map;
        }catch (Exception e){
            e.printStackTrace();
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * 复制一个map到新的key
     * @param sources
     * @param target
     */
    public Boolean HMCopyKey(String sources,String target){
        if(lockService.autoLock(target)) {
            try {
                Map<String, String> map = hgetAll(sources);
                stringRedisTemplate.opsForHash().putAll(target,map);
                log.info("redisService-->HMCopyKey-->sources:{},target:{},value{}", sources, target, map);
                return true;
            }catch (Exception e){
                e.printStackTrace();
                log.error(e.getMessage());
                throw new RuntimeException(e.getMessage());
            }finally {
                lockService.unlock(target);
            }
        }
        return false;
    }

    public Boolean copyKey(String sources,String target){
        if(lockService.autoLock(target)) {
            try {
                String value = stringRedisTemplate.opsForValue().get(sources);
                stringRedisTemplate.opsForValue().set(target, value);
                log.info("redisService-->copyKey-->sources:{},target:{},value{}", sources, target, value);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                log.error(e.getMessage());
                throw new RuntimeException(e.getMessage());
            } finally {
                lockService.unlock(target);
            }
        }
        return false;
    }


    /**
     * 根据key取出value
     * @param key
     * @return
     */
    public String get(String key){
        try {
            return stringRedisTemplate.opsForValue().get(key);
        }catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    public <T> String get(T t){
        String key=getKey(t);
        if(key==null||key.isEmpty()){
            log.error("key为空");
            return null;
        }else {
            return get(key);
        }
    }

    public boolean exists(String key) {
        return stringRedisTemplate.hasKey(key);
    }

    /**
     * 根据key保存value
     * @param key
     * @param value
     * @return
     */
    public Boolean set(String key,String value){
        if(lockService.autoLock(key)) {
            try {
                stringRedisTemplate.opsForValue().set(key, value);
                log.debug("redisService-->set-->key:{},value:{}",key,value);
                return true;
            }catch (Exception e) {
                e.printStackTrace();
                log.error(e.getMessage());
                throw new RuntimeException(e.getMessage());
            } finally {
                lockService.unlock(key);
            }
        }
        return false;
    }

    public <T> Boolean set(T t,String value){
        String key=getKey(t);
        if(key==null||key.isEmpty()){
            log.error("key为空");
            return false;
        }else {
            return set(key,value);
        }
    }
    /**
     * 将POJO以hash的方式保存
     * @param t
     * @param <T>
     * @return key
     */
    public <T>  String saveBeanInHash(T t){
        try {
            final Atable atable = beanCache.getBeanCache(t.getClass());
            //检查是否有id，没有的话有序列生成
            if(atable.getIdColumn()!=null&&null==atable.getIdColumn().getField().get(t)){
                if(!atable.getSequence().isEmpty()){
                    Long id=stringRedisTemplate.opsForValue().increment(atable.getSequence(),1);
                    atable.getIdColumn().getField().set(t,id);
                }else {
                    throw new NullPointerException(t.getClass().getName()+">>>>id为空企且未设置序列");
                }
            }
            //获取key
            final String key=atable.getKey(t);
            if(null==key||key.isEmpty()){
                throw new RuntimeException("key创建失败");
            }
            if (exists(key)) {
                throw new RuntimeException("已经存在key" + key + "的数据，保存失败");
            }
            final Map<String, String> param = RedisUtil.beanToMap(t, atable);
            log.debug("redisService-->saveBeanInHash-->Transaction---->key:{},value:{}",key,param);
            List<Object> result=stringRedisTemplate.execute(new SessionCallback<List<Object>>() {
                @Override
                public List<Object> execute(RedisOperations operations) throws
                        DataAccessException {
                    operations.multi();
                    operations.opsForHash().putAll(key, param);
                    if (atable.getSubIdFields().size() > 0) {
                        for (Field subIdField : atable.getSubIdFields()) {
                            if (!param.get(subIdField.getName()).isEmpty()) {
                                String subKey = atable.getSubKey(subIdField.getName(), param.get(subIdField.getName()));
                                log.debug("redisService-->saveBeanInHash-->Transaction--->key:{},value:{}",subKey,key);
                                operations.opsForValue().set(subKey, key);
                            }
                        }
                    }
                    return operations.exec();
                }
            });
            if (null == result){
                log.warn(key + "保存异常，但部分事务已提交,请检查数据");
            }else {
                log.info(key + "保存成功");
            }
            return key;
        }catch (Exception e){
            e.printStackTrace();
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * 传入class返回序列下一个值
     * @param clazz
     * @return
     */
    public Long nextSeq(Class<?> clazz){
        return nextSeq(clazz,1L);
    }

    public Long nextSeq(Class<?> clazz,Long num){
        Atable atable = beanCache.getBeanCache(clazz);
        if(atable.getSequence()==null||atable.getSequence().isEmpty()){
            log.error("{}未设置sequence",clazz.getName());
            throw new NullPointerException(clazz.getName()+"未设置sequence");
        }
        try {
            if(!exists(atable.getSequence())){
                stringRedisTemplate.opsForValue().setIfAbsent(atable.getSequence(),"100000");
            }
            return stringRedisTemplate.opsForValue().increment(atable.getSequence(),num);
        }catch (Exception e){
            e.printStackTrace();
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    public <T>  Map<String,T> saveBeanInHash(List<T> list){
        if(null==list||list.size()==0){
            return null;
        }
        Map<String,T> map = new HashMap<String,T>();
        final Map<String,Map<String, String>> beanAndValue=new HashMap<String,Map<String, String>>();
        try {
            final Atable atable = beanCache.getBeanCache(list.get(0).getClass());
            for(T t:list) {
                //检查是否有id，没有的话有序列生成
                if (atable.getIdColumn()!=null&&null == atable.getIdColumn().getField().get(t)) {
                    if (!atable.getSequence().isEmpty()) {
                        Long id = stringRedisTemplate.opsForValue().increment(atable.getSequence(),1);
                        atable.getIdColumn().getField().set(t,id);
                    } else {
                        throw new NullPointerException(t.getClass().getName() + ">>>>id为空且未设置序列");
                    }
                }
                String key = atable.getKey(t);
                if (null == key || key.isEmpty()){
                    throw new RuntimeException("key创建失败");
                }
                if (exists(key)) {
                    log.info("报错key:{}",key);
                    throw new RuntimeException("已经存在key" + key + "的数据，保存失败");
                }
                Map<String, String> param = RedisUtil.beanToMap(t, atable);
                log.debug("redisService-->saveBeanInHash-->pipeline-->key:{},value:{}",key,param);
                beanAndValue.put(key,param);
                map.put(key,t);
            }
            List<Object> results = stringRedisTemplate.executePipelined(
                    new RedisCallback<Object>() {
                        @Override
                        public Object doInRedis(RedisConnection connection) throws DataAccessException {
                            StringRedisConnection stringRedisConn = (StringRedisConnection) connection;
                            for (Map.Entry<String, Map<String, String>> entry : beanAndValue.entrySet()) {
                                stringRedisConn.hMSet(entry.getKey(), entry.getValue());
                                if (atable.getSubIdFields().size() > 0) {
                                    for (Field subIdField : atable.getSubIdFields()) {
                                        if (!entry.getValue().get(subIdField.getName()).isEmpty()) {
                                            String subKey = atable.getSubKey(subIdField.getName(), entry.getValue().get(subIdField.getName()));
                                            log.debug("redisService-->saveBeanInHash-->pipeline--->key:{},value:{}", subKey, entry.getKey());
                                            stringRedisConn.set(atable.getSubKey(subIdField.getName(), entry.getValue().get(subIdField.getName())), entry.getKey());
                                        }
                                    }
                                }
                            }
                            return null;
                        }
            });
            log.info("保存成功");
            return map;
        }catch (Exception e){
            e.printStackTrace();
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }


    /**
     * 获取hash并转为pojo
     * @param key
     * @param clazz
     * @param <T>
     * @return
     */
    public <T> T getBeanInHash(String key,Class<T> clazz){
        try {
            T t=clazz.newInstance();
            Atable atable = beanCache.getBeanCache(clazz);
            Map<String,String> map=hgetAll(key,atable);
            if(map==null||map.size()==0){
                return null;
            }
            return RedisUtil.mapToBean(map,t,atable);
        }catch (Exception e){
            e.printStackTrace();
            log.error("getBean异常:{}",e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * 获取hash并转为pojo
     * @param t
     * @param <T>
     * @return
     */
    public <T> T getBeanInHash(T t){
        try {
            Atable atable = beanCache.getBeanCache(t.getClass());
            String key= getKey(t,atable);
            Map<String,String> map=hgetAll(key,atable);
            if(map==null||map.size()==0){
                return null;
            }
            T _t=(T)atable.getClazz().newInstance();
            return RedisUtil.mapToBean(map,_t,atable);
        }catch (Exception e){
            e.printStackTrace();
            log.error("getBean异常:{}",e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    public <T> List<T> getBeansInHash(List<String> keys,Class clazz){
        try {
            Atable atable = beanCache.getBeanCache(clazz);
            String[] fileNames = atable.getCols().stream().map(acolumn -> acolumn.getAttrName()).collect(Collectors.toList()).toArray(new String[atable.getCols().size()]);
            List<Object> result=stringRedisTemplate.execute(new RedisCallback<List<Object>>() {
                @Nullable
                @Override
                public List<Object> doInRedis(RedisConnection connection) throws DataAccessException {
                    StringRedisConnection stringRedisConn = (StringRedisConnection) connection;
                    stringRedisConn.openPipeline();
                    for (String key : keys) {
                        stringRedisConn.hMGet(key, fileNames);
                    }
                    return stringRedisConn.closePipeline();
                }
            });
            List<T> ts = new ArrayList<T>();
            for(Object o:result){
                List<byte[]> a=(List<byte[]>)o;
                Map<String, String> map = new HashMap<String, String>();
                for (int i = 0; i < a.size(); i++) {
                    if(Objects.isNull(atable.getCols().get(i).getAttrName())||Objects.isNull(a.get(i))){
                        continue;
                    }
                    map.put(atable.getCols().get(i).getAttrName(),new String((byte[])a.get(i)));
                };
                T _t = (T) atable.getClazz().newInstance();
                ts.add(RedisUtil.mapToBean(map,_t,atable));
            };
//            Map<String, String> map = new HashMap<String, String>();
//            for (int i = 0; i < result.size(); i++) {
//                map.put(atable.getCols().get(i).getAttrName(),new String((byte[])result.get(i)));
//                if ((i + 1) % atable.getCols().size() == 0) {
//                    T _t = (T) atable.getClazz().newInstance();
//                    ts.add(RedisUtil.mapToBean(map,_t,atable));
//                    map = new HashMap<String, String>();
//                }
//            }

            return ts;
        }catch (Exception e){
            e.printStackTrace();
            log.error("getBean异常:{}",e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * 更新hash,完全覆盖
     * @param t
     * @param <T>
     * @return
     */
    public <T> Boolean updateBeanOverWrite(T t){
        Atable atable = beanCache.getBeanCache(t.getClass());
        String key = getKey(t,atable);
        if(lockService.autoLock(key)){
            try {
                if(!exists(key)){
                    log.error("key:{}不存在，更新失败",key);
                    throw new RuntimeException("key:"+key+"不存在，更新失败");
                }
                Map<String,String> param = RedisUtil.beanToMap(t,atable);
                log.debug("redisService-->updateBeanInHash-->key:{},value:{}",key,param);
                stringRedisTemplate.opsForHash().putAll(key,param);
                log.info(key+"更新成功");
                return true;
            }catch (Exception e){
                e.printStackTrace();
                log.error(e.getMessage());
                log.error("{}更新失败",key);
                throw new RuntimeException(e.getMessage());
            }
        }
        return false;
    }

    /**
     * 更新hash默认上锁
     * @param t
     * @param <T>
     * @return
     */
    public <T> Boolean updateBeanInHashNotNull(T t){
        return updateBeanInHashNotNull(t,true);
    }

    /**
     * 更新hash默认不上锁
     * @param t
     * @param <T>
     * @return
     */
    public <T> Boolean updateBeanInHashNotNullWithoutLock(T t){
        return updateBeanInHashNotNull(t,false);
    }

    public void expire(String key,int seconds){
        stringRedisTemplate.expire(key,seconds,TimeUnit.SECONDS);
    }


    /**
     * 更新hash，为空的不更新,如果为增量字段用增量更新
     * @param t
     * @param <T>
     * @return
     */
    private <T> Boolean updateBeanInHashNotNull(final T t,boolean needLock){
        final Atable atable = beanCache.getBeanCache(t.getClass());
        final String key = getKey(t,atable);
        final Map<Acolumn,Long> incrMap=new HashMap<Acolumn, Long>();
        if(!needLock||lockService.autoLock(key)) {
            try {
                if(!exists(key)){
                    log.error("key:{}不存在，更新失败",key);
                    throw new RuntimeException("key:"+key+"不存在，更新失败");
                }
                BoundHashOperations hashOperations=stringRedisTemplate.boundHashOps(key);
                int count=1;
                try {
                    for (Acolumn acolumn : atable.getCols()) {
                        Object value = acolumn.getField().get(t);
                        if (value != null && !value.toString().isEmpty()) {
                            if (acolumn.getDigits() != null) {
                                value = Math.round(Double.valueOf(value.toString()).doubleValue() * acolumn.getDigits());
                            }
                            if (acolumn.getIncrable() && !StringUtils.isEmpty(hashOperations.get(acolumn.getAttrName()))) {
                                log.info("redisService-->updateBeanInHashNotNull-->Transaction-->-->incrkey:{},field:{},value:{}", key, acolumn.getAttrName(), value);
                                Long result = hashOperations.increment(acolumn.getAttrName(), Long.valueOf(value.toString()).longValue());
                                incrMap.put(acolumn, result);
                            } else {
                                log.info("redisService-->updateBeanInHashNotNull-->Transaction-->key:{},field:{},value:{}", key, acolumn.getAttrName(), value);
                                hashOperations.put(acolumn.getAttrName(), ReflectionUtil.ToStringForRedis(value));
                            }
                            count++;
                        }
                    }
                    if (incrMap.size() > 0) {
                        for (Map.Entry<Acolumn, Long> entry : incrMap.entrySet()) {
                            Long value=entry.getValue();
                            if (value != null){
                                Object _value=null;
                                if(null!=entry.getKey().getDigits()){
                                    Double doubleValue = value.doubleValue()/entry.getKey().getDigits();
                                    _value = ReflectionUtil.ToObjectByType(doubleValue.toString(), entry.getKey().getAttrType());
                                }else {
                                    _value = ReflectionUtil.ToObjectByType(value.toString(), entry.getKey().getAttrType());
                                }
                                entry.getKey().getField().set(t, _value);
                            }
                        }
                        incrMap.clear();
                    }
                    log.info(key + "更新成功");
                    return true;
                } catch (IllegalAccessException e){
                    throw new RuntimeException(e.getMessage());
                }
            } catch (Exception e) {
                e.printStackTrace();
                log.error(e.getMessage());
                log.error("{}更新失败，但部分事务已提交,请检查数据", key);
                throw new RuntimeException(e.getMessage());
            } finally {
                if(needLock){
                    lockService.unlock(key);
                }
            }
        }
        return false;
    }

    /**
     * 删除hash
     * @param t
     * @param <T>
     * @return
     */
    public <T> Boolean delBean(T t){
        Boolean success=false;
        Atable atable = beanCache.getBeanCache(t.getClass());
        String key=getKey(t,atable);
        try {
            log.debug("redisService-->delBean-->key:{}",key);
            stringRedisTemplate.delete(key);
            success =true;
            log.info("{}删除成功",key);
        }catch (Exception e){
            e.printStackTrace();
            log.error(e.getMessage());
            log.error("{}删除失败",key);
            throw new RuntimeException(e.getMessage());
        }
        return success;
    }

    /**
     * 根据key来删除
     * @param key
     * @return
     */
    public Boolean del(String key){
        log.debug("redisService-->del-->key:{}",key);
        stringRedisTemplate.delete(key);
        log.info("{}删除成功",key);
        return true;
    }

    /**
     * 根据正则删key
     * @param patten
     * @return
     */
    public Boolean delByPatten(String patten){
        Set<String> keys =stringRedisTemplate.keys(patten);
        stringRedisTemplate.delete(keys);
        return true;
    }

    /**
     * 增
     * @param key
     * @param num
     * @return
     */
    public Long incrBy(String key,Long num){
        log.debug("redisService-->incrBy-->key:{},num:{}",key,num);
        Long result = stringRedisTemplate.opsForValue().increment(key,num);
        return result;
    }

    public Long setnx(String key,String value){
        Long result=stringRedisTemplate.opsForValue().setIfAbsent(key,value)?1L:0L;
        return result;
    }

    /**
     * 减
     * @param key
     * @param num
     * @return
     */
    public Long decrBy(String key,Long num){
        log.debug("redisService-->decrBy-->key:{},num:{}",key,num);
        Long result = stringRedisTemplate.opsForValue().increment(key,-num);
        return result;
    }

    /**
     * 增
     * @param key
     * @param field
     * @param num
     * @return
     */
    public Long hincrBy(String key,String field,Long num){

        log.debug("redisService-->hincrBy-->key:{} field:{} incr:{}",key,field,num);
        return stringRedisTemplate.opsForHash().increment(key,field,num);
    }

    public Double hincrByFloat(String key,String field,double num){
            log.debug("redisService-->hincrBy-->key:{} field:{} incr:{}",key,field,num);
            double result = stringRedisTemplate.opsForHash().increment(key,field,num);
            return result;
    }

    /**
     * 按bean自增，返回key
     * @param t
     * @param <T>
     * @return
     */
//    public <T> String hincrBy(final T t){
//        final Atable atable = beanCache.getBeanCache(t.getClass());
//        final String key = getKey(t,atable);
//        if(lockService.autoLock(key)) {
//            try {
//                List<Object> results = stringRedisTemplate.executePipelined(
//                        new RedisCallback<Object>() {
//                            @Override
//                            public Object doInRedis(RedisConnection connection) throws DataAccessException {
//                                StringRedisConnection stringRedisConn = (StringRedisConnection) connection;
//                                try {
//                                    for(Field field:atable.getIncrables()){
//                                        Object value=field.get(t);
//                                        if(value!=null){
//                                            log.debug("redisService-->hincrBy-->key:{} field:{} incr:{}",key,field,(Long)value);
//                                            stringRedisConn.hIncrBy(key,field.getName(),(Long)value);
//                                        }
//                                    }
//                                }catch (IllegalAccessException e){
//                                    throw new RuntimeException(e.getMessage());
//                                }
//
//                                return null;
//                            }
//                        });
//
//                return key;
//            } catch (Exception e) {
//                e.printStackTrace();
//                log.error(e.getMessage());
//                log.error("{}更新失败", key);
//                throw new RuntimeException(e.getMessage());
//            } finally {
//                lockService.unlock(key);
//            }
//        }
//        return null;
//    }

    /**
     *将一个或多个值插入到列表头部
     * @param key
     * @param value
     */
    public void lpush(String key,String... value){
        log.debug("redisService-->lpush-->key:{} value:{}",key,value);
        stringRedisTemplate.opsForList().leftPushAll(key,value);
    }

    /**
     * 移出并获取列表的第一个元素， 如果列表没有元素会阻塞列表直到等待超时或发现可弹出元素为止。
     * @param timeout
     * @param key
     * @return
     */
    public String blpop(int timeout,String key){
        log.debug("redisService-->blpop-->timeout:{} keys:{}",timeout,key);
        String result=stringRedisTemplate.opsForList().leftPop(key,timeout,TimeUnit.SECONDS);
        return result;
    }

    /**
     * 获取列表长度
     * @param key
     * @return
     */
    public Long llen(String key){
        Long result=stringRedisTemplate.opsForList().size(key);
        return result;
    }

    /**
     * 移出并获取列表的最后一个元素， 如果列表没有元素会阻塞列表直到等待超时或发现可弹出元素为止。
     * @param timeout
     * @param key
     * @return
     */
    public String brpop(int timeout,String key){
        String result=stringRedisTemplate.opsForList().rightPop(key,timeout,TimeUnit.SECONDS);
        return result;
    }

    /**
     * 根据正则获取key
     * @param patten
     * @return
     */
    public Set<String> keys(String patten){
        Set<String> result=stringRedisTemplate.keys(patten);
        return result;
    }

    /**
     * 清空redis
     */
    public void flushAll(){
        stringRedisTemplate.execute(new RedisCallback<Object>() {
            @Override
            public String doInRedis(RedisConnection connection) throws DataAccessException {
                connection.flushDb();
                return "ok";
            }
        });
    }

    /**
     * 向集合添加一个或多个成员
     * @param key
     * @param value
     */
    public void sadd(String key,String... value){
        stringRedisTemplate.opsForSet().add(key,value);
    }

    public <T>void sadd(T t,String... value){
        String key=getKey(t);
        if(key==null||key.isEmpty()){
            log.error("key为空");
        }else {
             sadd(key,value);
        }
    }



    /**
     * 获取集合的成员数
     * @param key
     * @return
     */
    public Long scard(String key){
        Long value=stringRedisTemplate.opsForSet().size(key);
        return value;
    }

    /**
     * 判断 member 元素是否是集合 key 的成员
     * @param key
     * @param member
     * @return
     */
    public Boolean sismember(String key,String  member ){
        boolean result=stringRedisTemplate.opsForSet().isMember(key,member);
        return result;
    }

    /**
     * 返回集合中的所有成员
     * @param key
     * @return
     */
    public Set<String> smemmbers(String key){
        Set<String> result=stringRedisTemplate.opsForSet().members(key);
        return result;
    }

    /**
     * 获取key，当id为空时，通过subId获取id
     * @param t
     * @param atable
     * @param <T>
     * @return
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     */
    private <T> String getKey(T t,Atable atable){
        try {
            String key=atable.getKey(t);
            if(key==null){
                for(Field subIdfield:atable.getSubIdFields()){
                    Object subId = subIdfield.get(t);
                    if(null!=subId){
                        String subKey=atable.getSubKey(subIdfield.getName(),subId);
                        key = stringRedisTemplate.opsForValue().get(subKey);
                        if(key==null||key.isEmpty()){
                            continue;
                        }
                        break;
                    }
                }
            }
            return key;
        }catch (Exception e){
            log.error("key获取失败");
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * 根据akey获取key
     * @param t
     * @param akey
     * @param <T>
     * @return
     */
    private  <T> String getKey(T t,Akey akey){
        try {
            String key=akey.getKey(t);
            return key;
        }catch (Exception e){
            log.error("key获取失败");
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * redis hset
     * @param key
     * @param field
     * @param value
     */
    public void hset(String key,String field,String value){
        if(lockService.autoLock(key)){
            try {
                stringRedisTemplate.opsForHash().put(key,field,value);
                log.debug("redisService--->hset-->key:{},field:{},value:{}",key,field,value);
            }catch (Exception e){
                e.printStackTrace();
                log.error(e.getMessage());
                throw new RuntimeException(e.getMessage());
            }finally {
                lockService.unlock(key);
            }
        }
    }

    /**
     * redis hsetnx
     * @param key
     * @param field
     * @param value
     * @return
     */
    public Long hsetnx(String key,String field,String value){
        if(lockService.autoLock(key)){
            try {
                Long result=stringRedisTemplate.opsForHash().putIfAbsent(key, field, value)?1L:0L;
                log.debug("redisService--->hsetnx-->key:{},field:{},value:{}",key,field,value);
                return result;
            }catch (Exception e){
                e.printStackTrace();
                log.error(e.getMessage());
                throw new RuntimeException(e.getMessage());
            }finally {
                lockService.unlock(key);
            }
        }
        return 0l;
    }

    /**
     * 根据redisTable或者RedisKey获取key
     * @param t
     * @param <T>
     * @return
     */
    public <T> String getKey(T t){
        Class<?> clas = t.getClass();
        if(clas.isAnnotationPresent(RedisTable.class)){
            return getKey(t,beanCache.getBeanCache(t.getClass()));
        }else if(clas.isAnnotationPresent(RedisKey.class)){
            return getKey(t,beanCache.getKeyCache(t.getClass()));
        }
        return null;
    }

    public String type(String key){
        try {
            String type=stringRedisTemplate.type(key).code();
            return type;
        }catch (Exception e){
            e.printStackTrace();
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    public <T> String getSequence(Class<T> clazz){
        Atable atable = beanCache.getBeanCache(clazz);
        return atable!=null&&atable.getSequence()!=null&&!atable.getSequence().isEmpty()?atable.getSequence():null;
    }

    public void zadd(String key,double score,String member ){
        try {
            stringRedisTemplate.opsForZSet().add(key,member,score);
        }catch (Exception e){
            e.printStackTrace();
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    public void zrem(String key,String... member ){
        try {
            stringRedisTemplate.opsForZSet().remove(key,member);
        }catch (Exception e){
            e.printStackTrace();
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    public Set<String> zrange (String key,long start,long end ){
        try {
            return stringRedisTemplate.opsForZSet().range(key,start,end);
        }catch (Exception e){
            e.printStackTrace();
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }finally {

        }
    }

    public Set<String> zrevrange (String key,long start,long end ){
        try {
            return stringRedisTemplate.opsForZSet().reverseRange(key,start,end);
        }catch (Exception e){
            e.printStackTrace();
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    public Set<ZSetOperations.TypedTuple<String>> zrangeWithScores (String key, long start, long end ){
        try {
            return stringRedisTemplate.opsForZSet().rangeWithScores(key,start,end);
        }catch (Exception e){
            e.printStackTrace();
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }finally {

        }
    }

    public Set<ZSetOperations.TypedTuple<String>> zrevrangeWithScores (String key,long start,long end ){
        try {
            return stringRedisTemplate.opsForZSet().reverseRangeWithScores(key,start,end);
        }catch (Exception e){
            e.printStackTrace();
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }finally {

        }
    }

    public Long zrank(String key,String member){
        try {
            return stringRedisTemplate.opsForZSet().rank(key,member);
        }catch (Exception e){
            e.printStackTrace();
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }finally {

        }
    }
}

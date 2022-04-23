# 1 引用方法

## 1.1 spring整合

### 1.1.1 maven引用
````
    <dependency>
        <groupId>redis.clients</groupId>
        <artifactId>jedis</artifactId>
        <version>2.6.2</version>
        <type>jar</type>
        <scope>compile</scope>
    </dependency>
    
    <dependency>
        <groupId>cz</groupId>
        <artifactId>redis-cbc</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
````    
### 1.1.2 spring.xml配置
````
    <bean id="jedisPoolConfig" class="redis.clients.jedis.JedisPoolConfig">
    		<property name="maxIdle" value="${redis.maxIdle}" />  
            <property name="maxTotal" value="${redis.maxTotal}" />  
            <property name="maxWaitMillis" value="${redis.maxWaitMillis}" />  
            <property name="testOnBorrow" value="${redis.testOnBorrow}" />
    </bean>
    
    <bean id="jedisPool" class="redis.clients.jedis.JedisPool" >
    		<constructor-arg name="poolConfig" ref="jedisPoolConfig" />
    		<constructor-arg name="host" value="${redis.host}" />
    		<constructor-arg name="port" value="${redis.port}" />
    		<constructor-arg name="timeout" value="3000" />
    </bean>
    
    <bean id="beanConverter" class="org.whale.cbc.redis.common.BeanCache" >
        <constructor-arg name="jedisPool" ref="jedisPool" />
        <constructor-arg name="cacheInit" value="${redis.cacheInit}" />
        <constructor-arg name="root" value="${redis.root}" />
    </bean>
````
## 1.2 springboot整合

### 1.2.1 maven引用
````
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    <dependency>
        <groupId>cz</groupId>
        <artifactId>redis-cbc</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
````    
### 1.2.2 config.java配置
````
    public class config {
        private String host;
        private int port;
        private int timeout;
        private String pass;
        private boolean cacheInit;//是否启动时初始化缓存
        private String root;//地址
        @Autowired
        ApplicationContext applicationContext;
        @Bean
        public JedisPoolConfig getRedisConfig(){
            return applicationContext.containsBean("JedisPoolConfig")?(JedisPoolConfig)applicationContext.getBean("JedisPoolConfig"):new JedisPoolConfig();
        }
    
        @Bean
        public JedisPool getJedisPool(){
            JedisPool jedisPool =pass==null||pass.isEmpty()?new JedisPool(getRedisConfig(),host,port,timeout):new JedisPool(getRedisConfig(),host,port,timeout,pass);
            return jedisPool;
        }
    
        @Bean
        public BeanCache getBeanCache(){
            BeanCache beanCache = new BeanCache(getJedisPool(),cacheInit,root);
            return beanCache;
        }
    
        public String getHost() {
            return host;
        }
    
        public void setHost(String host) {
            this.host = host;
        }
    
        public int getPort() {
            return port;
        }
    
        public void setPort(int port) {
            this.port = port;
        }
    
        public int getTimeout() {
            return timeout;
        }
    
        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }
    
        public String getPass() {
            return pass;
        }
    
        public void setPass(String pass) {
            this.pass = pass;
        }
    
        public boolean isCacheInit() {
            return cacheInit;
        }
    
        public void setCacheInit(boolean cacheInit) {
            this.cacheInit = cacheInit;
        }
    
        public String getRoot() {
            return root;
        }
    
        public void setRoot(String root) {
            this.root = root;
        }
    }
````
# 2 配置参数
````
    redis:
        host: 127.0.0.1 #地址
        port: 6379 #端口
        timeout: 1000 #超时时间
        #pass: 111111 #密码
        maxIdle: 6 #最大空闲数
        minIdle: 0 #最小空闲数
        maxTotal: 8 #最大连接数
        maxWaitMillis: -1 #获取连接时的最大等待毫秒数(如果设置为阻塞时BlockWhenExhausted),如果超时就抛异常, 小于零: 阻塞不确定的时间
        testOnBorrow: false #在获取连接的时候检查有效性
        testWhileIdle: false #在获取连接的时候检查有效性
        blockWhenExhausted: true #连接耗尽时是否阻塞, false报异常,ture阻塞直到超时
        lifo: false #是否启用后进先出
        minEvictableIdleTimeMillis: 1800000 #逐出连接的最小空闲时间 默认1800000毫秒(30分钟)
        root: org.whale #扫描pojo根目录
        cacheInit: true #启动时是否扫描pojo类
    redisLock:
      expireMsecs: 3000 #锁超时时间，防止线程在入锁以后，无限的执行等待
      timeoutMsecs: 1000 #锁等待时间，防止线程饥饿
    redisQueue:
      timeOut: 200
````
# 3 orm
     
# 3.1 @RedisTable

类标志，标志该类为redis pojo类型,会被缓存至内存，未标识@RedisTable类无法使用redisServer方法

    String tableName() default ""; 必填,该类在reids对应的表名
    String sequence() default ""; 选填,改类型pk的序列，当pojo save时缺乏pk会获取
    String key() default ""; 必填,该类在redis保存的key字段，保存redis时，对应的key为tableName:key,可使用占位符
 
 例子:
 
````
@RedisTable(tableName="WQ_FINANCIAL_ALLOT_INFO",key = "id:#id:name:#name",sequence = "SEQ_FINANCIAL_ALLOT_INFO")
````
</pre></code>

其中  key的占位符以‘#’开头

# 3.2 @RedisTableId

field标识，标志该field为该pojo类的pk,save pojo时，该字段为null的话会自动从序列获取值，若未填写序列则保存失败

    String keyParam() default ""; 选填，填充key中对应的占位符
    
例子:
````
    @RedisTableId(keyParam = "#id")
        private Long id;
````

其中keyParam为‘#id’，表名当获取该类的key时，字段id的值将替换@RedisTable中key字符串中的'#id'，如果该值为空，则该pojo的key获取失败

# 3.3 @RedisColumn

field标识,标识需要特殊需求的pojo字段

        boolean nullable() default true; 是否可为空，暂时不支持
        boolean editable() default true; 是否可编辑，暂时不支持
        boolean unique() default false; 是否唯一，暂时不支持
        boolean subId() default false; 是否是副id，为true时，save pojo时会存储一份key为tableName:field.name:field,value，value为该pojo的key
        String keyParam() default "";同3.2
        boolean incrable() default false;  在incr和updateBeanInHashNotNull方法中是否使用incr
        boolean redisParam() default true;/**是否是redis属性**/ 为false时该属性不存redis
例子: 同3.2

# 4 redisService使用

redisService 为操作redis主类，封装save，update等方法s
详细见src\main\java\org\whale\cbc\redis\service\RedisService.java注释

# 5 queueService使用

redis队列主类
详细见src\main\java\org\whale\cbc\redis\service\QueueService.java注释

# 6 PubSubService

redis广播订阅主类
详细见src\main\java\org\whale\cbc\redis\service\PubSubService.java注释

# 7 缓存配置

    root: org.whale #扫描pojo根目录
    cacheInit: true #启动时是否扫描pojo类
    
当cacheInit为true时会自动扫描root路径下的redis的pojo并缓存至内存中快速读取



 



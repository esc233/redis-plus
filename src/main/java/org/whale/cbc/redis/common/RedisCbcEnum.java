package org.whale.cbc.redis.common;

/**
 * @Author thuglife
 * @DATE 2017/8/7
 * @DESCRIPTION :
 */
public class RedisCbcEnum {
    public enum  DataType{
        NONE ("none"),
        STRING ("string"),
        LIST ("list"),
        SET ("set"),
        ZSET ("zset"),
        HASH ("hash");
        private String value;

        DataType(String val){
            this.value=val;
        }
        public String getValue() {
            return value;
        }
    }
}

package org.whale.cbc.redis.testBean.enums;

import org.whale.cbc.redis.common.REnum;

/**
 * @Author yakik
 * @DATE 2019/8/1
 * @DESCRIPTION :
 */
public enum ValueEnum implements REnum {
    IS_VALUE("1","有效"),NO_VALUE("0","无效");

    ValueEnum(String code,String str){
        this.code=code;
        this.str=str;
    }
    private final String code;

    private final String str;

    public String getCode() {
        return this.code;
    }

    public String getStr() {
        return str;
    }


    @Override
    public REnum createEnum(String redisValue) {
        for (ValueEnum s : values()) {
            if (redisValue.equals(s.code)) {
                return s;
            }
        }
        return null;
    }

    @Override
    public String getRedisValue() {
        return this.code;
    }
}

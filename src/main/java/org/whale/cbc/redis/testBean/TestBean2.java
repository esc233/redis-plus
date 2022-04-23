package org.whale.cbc.redis.testBean;

import lombok.Data;
import org.whale.cbc.redis.annotation.RedisKey;
import org.whale.cbc.redis.annotation.RedisKeyParam;

/**
 * @Author thuglife
 * @DATE 2017/7/28
 * @DESCRIPTION :
 */
@Data
@RedisKey(prefix = "test",key = "key1:#key1:key2:#key2")
public class TestBean2 {
    @RedisKeyParam(keyParam = "#key1")
    private String key1;
    @RedisKeyParam(keyParam = "#key2")
    private String key2;
}

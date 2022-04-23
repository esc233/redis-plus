package org.whale.cbc.redis.testBean;

import lombok.Data;
import org.whale.cbc.redis.annotation.RedisColumn;
import org.whale.cbc.redis.annotation.RedisTable;
import org.whale.cbc.redis.annotation.RedisTableId;
import org.whale.cbc.redis.common.DecimalDigits;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by yakik on 2017/5/14.
 */
@RedisTable(tableName="TEST",key = "id:#id:name:#name",sequence = "SEQ_FINANCIAL_ALLOT_INFO")
@Data
public class TestBean1 {
    @RedisTableId(keyParam = "#id")
    private Long id;
    @RedisColumn(keyParam = "#name")
    private String name;
    @RedisColumn(subId = true)
    private String subkey;

    private Long age;

    private Date date;
    @RedisColumn(incrable = true,digits = DecimalDigits.DIGIT_THREE)
    private BigDecimal addTest;
}

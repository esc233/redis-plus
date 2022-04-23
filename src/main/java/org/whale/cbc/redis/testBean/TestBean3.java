package org.whale.cbc.redis.testBean;

import lombok.Data;
import org.whale.cbc.redis.annotation.RedisTable;
import org.whale.cbc.redis.annotation.RedisTableId;
import org.whale.cbc.redis.testBean.enums.ValueEnum;

/**
 * @Author yakik
 * @DATE 2019/8/1
 * @DESCRIPTION :
 */
@RedisTable(tableName="TEST",key = "id3:#id",sequence = "SEQ_FINANCIAL_ALLOT_INFO")
@Data
public class TestBean3 {
    @RedisTableId(keyParam = "#id")
    private Long id;

    private ValueEnum valueEnum;
}

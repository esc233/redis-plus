package org.whale.cbc.redis.testBean;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.whale.cbc.redis.annotation.RedisColumn;
import org.whale.cbc.redis.annotation.RedisTable;
import org.whale.cbc.redis.annotation.RedisTableId;

import java.util.Date;
/**
 *
 *WQ_ACCOUNT_POSITION
 * @Date 2017-07-10
 */
@RedisTable(tableName="WQ_ACCOUNT_POSITION",key = "id:#id",sequence = "SEQ_ACCOUNT_POSITION")
@Data
public class AccountPositionModel {
	@RedisColumn(redisParam=false)
	private Logger logger = LoggerFactory.getLogger(AccountPositionModel.class);
	@RedisColumn(redisParam=false)
	private static final Long serialVersionUID = -1410714248140l;
	@RedisTableId(keyParam = "#id")
	private Long pkAccountPosition;

	private Long fkAccount;

	private String accountCode;
	private Long fkFinancialProduct;
	private Long fkFinancialAllotInfo;
	private String productCode;
	private String productName;
	private String stockCode;

	private String stockName;

	private Integer stockType;

	private Integer allottedInitStockCnt;

	private Integer repaidStockCnt;
	@RedisColumn(incrable=true)
	private Integer poolStockCnt;
	@RedisColumn(incrable=true)
	private Integer buyAppoIntegerCnt;
	@RedisColumn(incrable=true)
	private Integer saleAppoIntegerCnt;
	@RedisColumn(incrable=true)
	private Integer buyTradedCnt;
	@RedisColumn(incrable=true)
	private Integer saleTradedCnt;

	private Integer totalOvernight;
	private Integer totalOvernightDirection;
	private Integer yesterdayTotalOvernight;
	private Integer positionDate;

	private Integer isNewestData;

	private Integer createById;

	private Date createByTime;

	private Integer updateById;

	private Date updateByTime;

	private Integer isValid;

	private String teamCode;

	private Integer isPublicPool;

}
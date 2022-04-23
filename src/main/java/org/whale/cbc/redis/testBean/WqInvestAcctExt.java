package org.whale.cbc.redis.testBean;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.whale.cbc.redis.annotation.RedisColumn;
import org.whale.cbc.redis.annotation.RedisTable;
import org.whale.cbc.redis.annotation.RedisTableId;
import org.whale.cbc.redis.common.DecimalDigits;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * <p>
 * 投顾交易子账号扩展表redis
 * </p>
 *
 * @author jobob
 * @since 2019-05-28
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ToString
@RedisTable(tableName = "WQ_INVEST_ACCT_EXT",key = "ACCOUNT_CODE:#accountCode")
public class WqInvestAcctExt implements Serializable {

    private static final long serialVersionUID = 1L;

    public final static int TRANSFER_FEE_TYPE_SH=1;
    public final static int TRANSFER_FEE_TYPE_SH_SZ=2;
    public final static int TRANSFER_FEE_TYPE_NONE=3;


    public WqInvestAcctExt(){

    }



    /**
     * 主键
     */
    @RedisTableId()
    private Long pkInvestAcctExt;

    /**
     * FK子账号
     */
    private Long fkAccount;

    /**
     * 子账号编码（唯一，用于登录鉴权等）
     */
    @RedisColumn(keyParam = "#accountCode")
    private String accountCode;

    /**
     * 当前买入委托所属产品CODE，理财产品CODE（这个字段的变更，需要存入《投顾子账号买入委托所属产品变更流水表》）
     */
    private String productCodeForBuyAppoint;

    /**
     * 风控人员USER_NAME（SYS_USER的USER_NAME，这个字段是唯一索引）
     */
    private String fkSysUserNameRisk;

    /**
     * 方案实际开始日期（用数字表示年月日）
     -----------------------------------------------------------------------------------------------------------------------------
     这个投顾子账号可能今日创建好，但是实际开始日期可能是下个交易日，那么这个字段就是下个交易日。
     */
    private Integer startDate;

    /**
     * 方案实际结束日期（用数字表示年月日，如果是日的，那么结束日期先存入30000101；如果是月的，那么结束日期就是预计的结束日期，比如3-1开始，那么结束是3-31，续签什么的，在其他模块处理。）
     ------------------------------------------------------------------
     结案的时候，实际的结束日期可能不同，那么覆盖这个字段。
     */
    private Integer endDate;

    /**
     * 投顾期初保证金
     */
    private BigDecimal initCautionAmount;

    /**
     * 投顾期初融资款
     */
    private BigDecimal initFinancingAmount;

    /**
     * 杠杆倍数（一般10倍算很高了，之所以有3000，那是因为免费体验，1快保证金）
     */
    private Integer leverMultiple;

    /**
     * 投顾实时可用资金（投顾子账号和产品类似，调用相同的jar方法）
     -------------------------------------------------------------------------------------------
     融资买入下单：可用扣减：委托价格x委托数量、交易佣金、过户费、印花税
     冻结加：委托价格x委托数量、交易佣金、过户费、印花税

     部成： 可用不变
     冻结扣减：委托价格x这次部成的数量

     最终态：全成：冻结扣掉原交易佣金、过户费、印花税；修正可用（因为成交均价和委托价格有误差）
     部成已撤、撤单等：冻结扣减，未成的那部分x委托价格；冻结扣掉原佣金等；可用也要改。
     -------------------------------------------------------------------------------------------
     融资卖出下单：可用和冻结，无需增减。

     部成：冻结加：委托价格x这次部成的数量

     最终态（全成、部成已撤）：可用加上：均价x已成数量 - 交易佣金、过户费、印花税
     冻结扣减：委托价格x成交数量
     因为在最终态的时候，钱加到可用了，所以冻结也都扣减掉
     -------------------------------------------------------------------------------------------
     这么做的意义是为了总资产：可用+冻结+当前持仓数量x市价
     当前持仓数量：包含了部成的委托单的部成的数量。
     -------------------------------------------------------------------------------------------
     如果对“可用资金”进行冻结1000，那么“可用资金”扣减1000，“冻结资金”+1000，“可用资金”允许扣负。
     解冻1000，那么“冻结资金”-1000，“可用资金”+1000，要先判断“冻结资金”是否够扣，不允许“冻结资金”负数。
     */
    @RedisColumn(incrable = true,digits = DecimalDigits.DIGIT_THREE)
    private BigDecimal usableMoney;

    /**
     * 投顾实时冻结资金（投顾子账号和产品类似，调用相同的jar方法）
     -------------------------------------------------------------------------------------------
     融资买入下单：可用扣减：委托价格x委托数量、交易佣金、过户费、印花税
     冻结加：委托价格x委托数量、交易佣金、过户费、印花税

     部成： 可用不变
     冻结扣减：委托价格x这次部成的数量

     最终态：全成：冻结扣掉原交易佣金、过户费、印花税；修正可用（因为成交均价和委托价格有误差）
     部成已撤、撤单等：冻结扣减，未成的那部分x委托价格；冻结扣掉原佣金等；可用也要改。
     -------------------------------------------------------------------------------------------
     融资卖出下单：可用和冻结，无需增减。

     部成：冻结加：委托价格x这次部成的数量

     最终态（全成、部成已撤）：可用加上：均价x已成数量 - 交易佣金、过户费、印花税
     冻结扣减：委托价格x成交数量
     因为在最终态的时候，钱加到可用了，所以冻结也都扣减掉
     -------------------------------------------------------------------------------------------
     这么做的意义是为了总资产：可用+冻结+当前持仓数量x市价
     当前持仓数量：包含了部成的委托单的部成的数量。
     -------------------------------------------------------------------------------------------
     如果对“可用资金”进行冻结1000，那么“可用资金”扣减1000，“冻结资金”+1000，“可用资金”允许扣负。
     解冻1000，那么“冻结资金”-1000，“可用资金”+1000，要先判断“冻结资金”是否够扣，不允许“冻结资金”负数。
     */
    @RedisColumn(incrable = true,digits = DecimalDigits.DIGIT_THREE)
    private BigDecimal freezeMoney;

    /**
     * 补仓线比例（也叫警戒线比例，这个字段不是具体金额）
     */
    private BigDecimal coverLinePer;

    /**
     * 平仓线比例（这个字段不是具体金额）
     */
    private BigDecimal closeOutLinePer;

    /**
     * 盈利提取线比例（比如设置为1.05，那么表示 (期初保证金 + 期初融资款) x 1.05，默认1）
     */
    private BigDecimal profitExtractLinePer;

    /**
     * 补仓线金额
     */
    private BigDecimal coverLineAmount;

    /**
     * 平仓线金额
     */
    private BigDecimal closeOutLineAmount;

    /**
     * 盈利提取线金额
     */
    private BigDecimal profitExtractLineAmount;

    /**
     * 主板单票比例
     */
    private BigDecimal mainboardSingleStockPer;

    /**
     * 中小板单票比例
     */
    private BigDecimal smallSingleStockPer;

    /**
     * 创业板单票比例
     */
    private BigDecimal ventureSingleStockPer;

    /**
     * 科创板单票比例
     */
    private BigDecimal techSingleStockPer;

    /**
     * 主板总体比例
     */
    private BigDecimal mainboardOverallPer;

    /**
     * 中小板总体比例
     */
    private BigDecimal smallOverallPer;

    /**
     * 创业板总体比例
     */
    private BigDecimal ventureOverallPer;

    /**
     * 科创板总体比例
     */
    private BigDecimal techOverallPer;

    /**
     * 过户费收取规则（常量，1只收沪市，2深市、沪市均收，3都不收，默认2）
     */
    private Integer transferFeeType;

    /**
     * 下单审核规则（1.下单，2.回填）
     */
    private Integer orderAuditRule;

    /**
     * 前置风控限制（LIMIT_MS）
     --------------------------------------------------------------------------------
     用逗号分割存入数据库
     */
    private String preRiskLimit;

    /**
     * 允许交易的类别（STOCK,NATIONAL_DEBT,ETF）
     --------------------------------------------------------------------------------
     用逗号分割存入数据库
     */
    private String allowedTradeType;

    /**
     * 股票允许交易的类别（MAINBOARD,SMALL,VENTURE,TECH）
     主板，中小板，创业板，科创板

     --------------------------------------------------------------------------------
     用逗号分割存入数据库
     先根据股票代码，判断所属市场，6开头的就是上海的，0、3开头的就是深圳的，
     根据不同市场，再获取行情的不同字段，知道这个票属于什么证券类型。
     比如属于上海的主板，那么判断下这个MAINBOARD有没在这个字符串里面；
     比如属于深圳的中小板，那么判断下SMALL有没在这个字符串里面。
     如果找到，表示可以买入，如果找不到，则不允许买入。

     */
    private String stockAllowedType;

    /**
     * 股票风控限制（STOCK_ST,STOCK_UP,STOCK_DOWN,STOCK_NEW,LIMIT_BLOCK,DESIGNATE_PER,COVER_LINE_LIMIT,CLOSE_OUT_LINE_LIMIT）
     ST股票，触及涨停，触及跌停，新股，板块限制，单票比例限制，补仓线限买，平仓线限买
     --------------------------------------------------------------------------------
     用逗号分割存入数据库
     下单做买入的时候，每条规则执行对应的风控代码。
     补仓线限买，平仓线限买，下单的时候不管，只判断子账号状态
     由行情计算模块判断，如果有“补仓线限买”，那在资产小于补仓线时，修改子账号状态=2：停机
     由行情计算模块判断，如果有“平仓线限买”，那在资产小于平仓线时，修改子账号状态=2：停机

     */
    public final static String COVER_LINE_LIMIT="COVER_LINE_LIMIT";
    private String stockRiskLimit;

    /**
     * 公共指定单票的CODE（《WQ_INVEST_ACCT_COMM_DESIG_PER》的“COMM_DESIG_CODE”）
     */
    private String commDesigCode;



    /**
     * 账户截止上个交易日累计盈亏金额（因为是整个账户的盈亏，不是每个票的盈亏，所以要放这里。每日收盘清算的时候，都累加上今日盈亏）
     */
    private BigDecimal acctYestTotalProfitAmount;

    /**
     * 当前账户市值=循环累加这个子账号的所有持仓的当前市值
     -------------------------------------------------------------------------------------------------------
     行情计算模块，实时计算存到redis，无需同步到oracle，所以盘中oracle这个字段没有值；收盘清算的时候，会根据收盘时的值存入到oracle，此时有值。
     */
    private BigDecimal apAcctMarketValueAmount;


    /**
     * 当前账户总资产=当前账户市值+投顾实时可用资金+投顾实时冻结资金
     -------------------------------------------------------------------------------------------------------
     行情计算模块，实时计算存到redis，无需同步到oracle，所以盘中oracle这个字段没有值；收盘清算的时候，会根据收盘时的值存入到oracle，此时有值。
     */
    private BigDecimal apAcctTotalAssetAmount;

    /**
     * 当前账户风险系数=（当前账户总资产-投顾期初融资款）/当前账户市值
     保留5位小数点
     --------------------------------------------------------------------
     行情计算模块，实时计算存到redis，无需同步到oracle，所以盘中oracle这个字段没有值；收盘清算的时候，会根据收盘时的值存入到oracle，此时有值。
     */
    private BigDecimal apAcctRiskFactor;

    /**
     * 当前账户补仓线买入
     判断"股票风控限制"这个字段值，是不是有“COVER_LINE_LIMIT”，如果有，表示到了补仓线限买；如果没有，表示到了补仓线也不限买
     所以：
     如果有“COVER_LINE_LIMIT”，AP_ACCT_COVER_LINE_LIMIT=禁止；
     如果没有“COVER_LINE_LIMIT”，AP_ACCT_COVER_LINE_LIMIT=允许；
     --------------------------------------------------------------------
     行情计算模块，实时计算存到redis，无需同步到oracle，所以盘中oracle这个字段没有值；收盘清算的时候，会根据收盘时的值存入到oracle，此时有值。
     */
    private String apAcctCoverLineBuy;

    /**
     * 当前账户今日盈亏=循环累加这个子账号的所有持仓的当前今日盈亏
     --------------------------------------------------------------------
     行情计算模块，实时计算存到redis，无需同步到oracle，所以盘中oracle这个字段没有值；收盘清算的时候，会根据收盘时的值存入到oracle，此时有值。
     */
    private BigDecimal apAcctTodayProfitAmount;

    /**
     * 当前账户累计盈亏=当前账户今日盈亏+账户截止上个交易日累计盈亏金额
     --------------------------------------------------------------------
     行情计算模块，实时计算存到redis，无需同步到oracle，所以盘中oracle这个字段没有值；收盘清算的时候，会根据收盘时的值存入到oracle，此时有值。
     */
    private BigDecimal apAcctTotalProfitAmount;

    /**
     * 当前账户主板总体市值
     --------------------------------------------------------------------
     行情计算模块，实时计算存到redis，无需同步到oracle，所以盘中oracle这个字段没有值；收盘清算的时候，会根据收盘时的值存入到oracle，此时有值。
     */
    private BigDecimal apAcctMainboardMvAmount;

    /**
     * 当前账户主板总体买入委托的未成交金额
     --------------------------------------------------------------------
     投顾子账号持仓表的“当日买入委托的未成交金额”的持仓类型是主板的汇总（不包括当日买入委托的未成交佣金税等）
     */
    private BigDecimal apAcctMbBapotUndAmount;

    /**
     * 当前账户中小板总体市值
     --------------------------------------------------------------------
     行情计算模块，实时计算存到redis，无需同步到oracle，所以盘中oracle这个字段没有值；收盘清算的时候，会根据收盘时的值存入到oracle，此时有值。
     */
    private BigDecimal apAcctSmallMvAmount;

    /**
     * 当前账户中小板总体买入委托的未成交金额
     --------------------------------------------------------------------
     投顾子账号持仓表的“当日买入委托的未成交金额”的持仓类型是中小板的汇总（不包括当日买入委托的未成交佣金税等）
     */
    private BigDecimal apAcctSmBapotUndAmount;

    /**
     * 当前账户创业板总体市值
     --------------------------------------------------------------------
     行情计算模块，实时计算存到redis，无需同步到oracle，所以盘中oracle这个字段没有值；收盘清算的时候，会根据收盘时的值存入到oracle，此时有值。
     */
    private BigDecimal apAcctVentureMvAmount;

    /**
     * 当前账户创业板总体买入委托的未成交金额
     --------------------------------------------------------------------
     投顾子账号持仓表的“当日买入委托的未成交金额”的持仓类型是创业板的汇总（不包括当日买入委托的未成交佣金税等）
     */
    private BigDecimal apAcctVeBapotUndAmount;

    /**
     * 当前账户科创板总体市值
     --------------------------------------------------------------------
     行情计算模块，实时计算存到redis，无需同步到oracle，所以盘中oracle这个字段没有值；收盘清算的时候，会根据收盘时的值存入到oracle，此时有值。
     */
    private BigDecimal apAcctTechMvAmount;

    /**
     * 当前账户科创板买入委托的未成交金额
     --------------------------------------------------------------------
     投顾子账号持仓表的“当日买入委托的未成交金额”的持仓类型是科创板的汇总（不包括当日买入委托的未成交佣金税等）
     */
    private BigDecimal apAcctTeBapotUndAmount;

    /**
     * 全部交易日累计的买入成交金额（每日开盘计算报表的时候，都累加上每日资产表的成交额等；结案的时候，也做累加，所以在开盘计算报表的时候，当天结案的不累加成交额，避免重复累加。）
     */
    private BigDecimal allDaysBuyDealAmount;

    /**
     * 全部交易日累计的买入成交佣金税等（每日开盘计算报表的时候，都累加上每日资产表的成交额等；结案的时候，也做累加，所以在开盘计算报表的时候，当天结案的不累加成交额，避免重复累加。）
     */
    private BigDecimal allDaysBuyTaxAmount;

    /**
     * 全部交易日累计的卖出成交金额（每日开盘计算报表的时候，都累加上每日资产表的成交额等；结案的时候，也做累加，所以在开盘计算报表的时候，当天结案的不累加成交额，避免重复累加。）
     */
    private BigDecimal allDaysSaleDealAmount;

    /**
     * 全部交易日累计的卖出成交佣金税等（每日开盘计算报表的时候，都累加上每日资产表的成交额等；结案的时候，也做累加，所以在开盘计算报表的时候，当天结案的不累加成交额，避免重复累加。）
     */
    private BigDecimal allDaysSaleTaxAmount;

    @RedisColumn(redisParam = false)
    private String memo;

    /**
     * 本条记录系统用户创建者ID
     */
    @RedisColumn(redisParam = false)
    private Long createById;

    /**
     * 本条记录系统用户创建时间
     */
    @RedisColumn(redisParam = false)
    private Date createByTime;

    /**
     * 本条记录系统用户修改者ID
     */
    @RedisColumn(redisParam = false)
    private Long updateById;

    /**
     * 本条记录系统用户修改时间
     */
    @RedisColumn(redisParam = false)
    private Date updateByTime;

    /**
     * 删除状态位(0无效，1有效)
     */
    @RedisColumn(redisParam = false)
    private Integer isValid;


}

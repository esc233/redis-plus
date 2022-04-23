package org.whale.cbc.redis.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * @version 1.0
 * @author linwei
 * @data 2016年8月1日
 * @描述:资金账号报文独立文件日志
 */
public class LogbackOldValue {
	private static Logger log = LoggerFactory.getLogger(LogbackOldValue.class);
	/**
	 *
	 * @param tableName
	 * @param content
	 */
	public static void info(Logger logger,String tableName, String content,Object... objects) {
		if (tableName != null&&!tableName.isEmpty()) {
			MDC.put("logFileName",tableName);
			log.info(content,objects);
			MDC.clear();
		}
		logger.info(content,objects);
	}

	public static void debug(Logger logger,String tableName, String content,Object... objects) {
		if (tableName != null&&!tableName.isEmpty()) {
			MDC.put("logFileName", tableName);
			log.debug(content,objects);
			MDC.clear();
		}
		logger.debug(content,objects);
	}

	public static void error(Logger logger,String tableName, String content,Object... objects) {
		if (tableName != null && !tableName.isEmpty()) {
			MDC.put("logFileName", tableName);
			log.error(content,objects);
			MDC.clear();
		}
		logger.error(content,objects);
	}

}

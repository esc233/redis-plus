package org.whale.cbc.redis.util;


import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
/**
 * @Author huangs
 * @DATE 2017/5/11
 * @DESCRIPTION :
 */
public class TimeUtil {
	public static String getCurrDate(String sDateFormat) {
		Calendar gc = new GregorianCalendar();
		Date date = gc.getTime();
		SimpleDateFormat sf = new SimpleDateFormat(sDateFormat);
		String result = sf.format(date);
		return result;
	}

	public static String getCurrDate(Date date, String sDateFormat) {
		System.out.println(date.toString());
		SimpleDateFormat sf = new SimpleDateFormat(sDateFormat);
		String result = sf.format(date);
		return result;
	}

	public static long getSubDate(String startDate, String endDate) {
		if ((startDate == null) || (endDate == null)) {
			return 0L;
		}
		Calendar calendar = new GregorianCalendar();
		String format = "yyyyMMdd";
		if ((startDate != null) && (startDate.trim().length() == 6)) {
			format = "yyyyMM";
		}
		SimpleDateFormat bartDateFormat = new SimpleDateFormat(format);
		try {
			Date date = bartDateFormat.parse(startDate);
			calendar.setTime(date);
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
		Calendar calendar1 = new GregorianCalendar();
		String format1 = "yyyyMMdd";
		if ((endDate != null) && (endDate.trim().length() == 6)) {
			format1 = "yyyyMM";
		}
		SimpleDateFormat bartDateFormat1 = new SimpleDateFormat(format1);
		try {
			Date date = bartDateFormat1.parse(endDate);
			calendar1.setTime(date);
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
		return (calendar1.getTime().getTime() - calendar.getTime().getTime()) / 86400000L;
	}

	public static String fillUpDateStrToDayMax(String dateStr) {
		if (dateStr == null) {
			throw new RuntimeException("调用fillUpDateStrToDayMax()时，参数dateStr为空！");
		}
		return dateStr.trim() + " 23:59:59";
	}

	public static String fillUpDateStrToDayMin(String dateStr) {
		if (dateStr == null) {
			throw new RuntimeException("调用fillUpDateStrToDayMin()时，参数dateStr为空！");
		}
		return dateStr.trim() + " 00:00:00";
	}

	public static Date parseDateToDayMax(Date date) {
		if (date == null) {
			throw new RuntimeException("调用parseDateToDayMax()时，参数date为空！");
		}
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);

		calendar.set(11, 23);
		calendar.set(12, 59);
		calendar.set(13, 59);
		calendar.set(14, 999);
		return calendar.getTime();
	}

	public static Date parseDateToDayMin(Date date) {
		if (date == null) {
			throw new RuntimeException("调用parseDateToDayMin()时，参数date为空！");
		}
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);

		calendar.set(11, 0);
		calendar.set(12, 0);
		calendar.set(13, 0);
		calendar.set(14, 0);
		return calendar.getTime();
	}

	public static boolean isBetweenStartAndEndDate(Date startDate, Date endDate, Date compareDate) {
		if (startDate == null) {
			throw new RuntimeException("调用isBetweenStartAndEndDate()时，参数startDate为空！");
		}
		if (endDate == null) {
			throw new RuntimeException("调用isBetweenStartAndEndDate()时，参数endDate为空！");
		}
		if (compareDate == null) {
			compareDate = new Date();
		}
		long startDateTime = startDate.getTime();
		long endDateTime = endDate.getTime();
		long compareDateTime = compareDate.getTime();
		if (startDateTime > endDateTime) {
			throw new RuntimeException("调用isBetweenStartAndEndDate()时，startDate应比endDate小或等于！");
		}
		if ((compareDateTime >= startDateTime) && (compareDateTime <= endDateTime)) {
			return true;
		}
		return false;
	}

	public static boolean isGeStartAndLtEndDate(Date startDate, Date endDate, Date compareDate) {
		if (startDate == null) {
			throw new RuntimeException("调用isBetweenStartAndEndDate()时，参数startDate为空！");
		}
		if (endDate == null) {
			throw new RuntimeException("调用isBetweenStartAndEndDate()时，参数endDate为空！");
		}
		if (compareDate == null) {
			compareDate = new Date();
		}
		long startDateTime = startDate.getTime();
		long endDateTime = endDate.getTime();
		long compareDateTime = compareDate.getTime();
		if (startDateTime > endDateTime) {
			throw new RuntimeException("调用isBetweenStartAndEndDate()时，startDate应比endDate小或等于！");
		}
		if ((compareDateTime >= startDateTime) && (compareDateTime < endDateTime)) {
			return true;
		}
		return false;
	}

	public static boolean isBetweenStartAndEndDateAccuracyDay(Date startDate, Date endDate, Date compareDate) {
		if (startDate == null) {
			throw new RuntimeException("调用isBetweenStartAndEndDateByDay()时，参数startDate为空！");
		}
		if (endDate == null) {
			throw new RuntimeException("调用isBetweenStartAndEndDateByDay()时，参数endDate为空！");
		}
		if (compareDate == null) {
			compareDate = new Date();
		}
		return isBetweenStartAndEndDate(getDateForAccuracy(startDate, 5), getDateForAccuracy(endDate, 5), getDateForAccuracy(compareDate, 5));
	}

	public static boolean isGeStartAndLtEndDateAccuracyDay(Date startDate, Date endDate, Date compareDate) {
		if (startDate == null) {
			throw new RuntimeException("调用isBetweenStartAndEndDateByDay()时，参数startDate为空！");
		}
		if (endDate == null) {
			throw new RuntimeException("调用isBetweenStartAndEndDateByDay()时，参数endDate为空！");
		}
		if (compareDate == null) {
			compareDate = new Date();
		}
		return isGeStartAndLtEndDate(getDateForAccuracy(startDate, 5), getDateForAccuracy(endDate, 5), getDateForAccuracy(compareDate, 5));
	}

	public static Date addMonth(Date originalTime, int month) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(originalTime);
		calendar.add(2, month);
		return calendar.getTime();
	}

	public static Date addDay(Date originalTime, int day) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(originalTime);
		calendar.add(5, day);
		return calendar.getTime();
	}

	public static Date getDateForAccuracy(Date date, int accuracy) {
		Calendar calendar = Calendar.getInstance();
		if (date != null) {
			calendar.setTime(date);
		}
		switch (accuracy) {
		case 5:
			calendar.set(11, 0);
			calendar.set(12, 0);
			calendar.set(13, 0);
			calendar.set(14, 0);
			return calendar.getTime();
		case 11:
			calendar.set(12, 0);
			calendar.set(13, 0);
			calendar.set(14, 0);
			return calendar.getTime();
		case 12:
			calendar.set(13, 0);
			calendar.set(14, 0);
			return calendar.getTime();
		case 13:
			calendar.set(14, 0);
			return calendar.getTime();
		}
		return null;
	}

	public static Calendar getCalendarForAccuracy(Date date, int accuracy) {
		Calendar calendar = Calendar.getInstance();
		if (date != null) {
			calendar.setTime(date);
		}
		switch (accuracy) {
		case 5:
			calendar.set(11, 0);
			calendar.set(12, 0);
			calendar.set(13, 0);
			calendar.set(14, 0);
			return calendar;
		case 11:
			calendar.set(12, 0);
			calendar.set(13, 0);
			calendar.set(14, 0);
			return calendar;
		case 12:
			calendar.set(13, 0);
			calendar.set(14, 0);
			return calendar;
		case 13:
			calendar.set(14, 0);
			return calendar;
		}
		return null;
	}

	public static Calendar convertToCalendar(Date date) {
		if (date == null) {
			date = new Date();
		}
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		return calendar;
	}

	public static int getDaysByMillisecondDif(long startTimeMillisecond, long endTimeMillisecond) {
		long totalDifDay = (endTimeMillisecond - startTimeMillisecond) / 86400000L;
		return new Long(totalDifDay).intValue();
	}

	public static int getDaysByDateDif(Date startTime, Date endTime) {
		startTime = getDateForAccuracy(startTime, 5);
		endTime = getDateForAccuracy(endTime, 5);
		return getDaysByMillisecondDif(startTime.getTime(), endTime.getTime());
	}

	public static boolean isTheSameTimeByDate(Date startTime, Date endTime) {
		Calendar startCalendar = Calendar.getInstance();
		if (startTime != null) {
			startCalendar.setTime(startTime);
		}
		Calendar endCalendar = Calendar.getInstance();
		if (endTime != null) {
			endCalendar.setTime(endTime);
		}
		if (startCalendar.get(1) != endCalendar.get(1)) {
			return false;
		}
		if (startCalendar.get(2) != endCalendar.get(2)) {
			return false;
		}
		if (startCalendar.get(5) != endCalendar.get(5)) {
			return false;
		}
		return true;
	}

	public static int getMonthsByDateDif(Date startTime, Date endTime) {
		Calendar startCalendar = getCalendarForAccuracy(startTime, 5);
		Calendar endCalendar = getCalendarForAccuracy(endTime, 5);
		int yearDif = endCalendar.get(1) - startCalendar.get(1);
		int monthDif = endCalendar.get(2) - startCalendar.get(2);
		return yearDif * 12 + monthDif;
	}

	public static int compareToDay(Date startTime, Date endTime) {
		long difDays = getDaysByDateDif(startTime, endTime);
		if (difDays == 0L) {
			return 0;
		}
		if (difDays > 0L) {
			return 1;
		}
		return -1;
	}

	public static String formatDate(Date date, String datePattern) {
		String result = null;
		try {
			SimpleDateFormat sdf = new SimpleDateFormat(datePattern);
			result = sdf.format(date);
		} catch (Exception e) {
			throw new RuntimeException("日期格式化错误");
		}
		return result;
	}

	public static Date parseDateStr(String dateStr, String datePattern) {
		Date result = null;
		try {
			SimpleDateFormat sdf = new SimpleDateFormat(datePattern);
			result = sdf.parse(dateStr);
		} catch (Exception e) {
			throw new RuntimeException("日期转换失败");
		}
		return result;
	}

	public static long getSubDate(Date startDate, Date endDate) {
		Calendar calendar1 = new GregorianCalendar();
		calendar1.setTime(parseDateToDayMin(startDate));

		Calendar calendar2 = new GregorianCalendar();
		calendar2.setTime(parseDateToDayMin(endDate));

		return (calendar2.getTime().getTime() - calendar1.getTime().getTime()) / 86400000L;
	}

	public static int compareToOnlyDate(Date compareDate, Date anotherDate) {
		if (compareDate == null) {
			throw new RuntimeException("调用compareToOnlyDate()时，参数compareDate为空！");
		}
		if (anotherDate == null) {
			throw new RuntimeException("调用anotherDate()时，参数anotherDate为空！");
		}
		return parseDateToDayMin(compareDate).compareTo(parseDateToDayMin(anotherDate));
	}
	
	public static Integer getCurDate_yyyyMMdd() {
		return Integer.parseInt(getCurrDate("yyyyMMdd"));
	}

	public static void main(String[] args) {
		Calendar cal = Calendar.getInstance();
		cal.set(2015, 9, 21, 12, 20, 20);
		Date date1 = cal.getTime();

		cal.set(2015, 9, 22, 11, 11, 11);
		Date date2 = cal.getTime();
		//System.out.println(compareToOnlyDate(date1, date2));
		System.out.println(getCurDate_yyyyMMdd());
	}
}

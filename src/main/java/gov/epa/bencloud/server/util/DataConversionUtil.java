package gov.epa.bencloud.server.util;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataConversionUtil {
	private static final Logger log = LoggerFactory.getLogger(DataConversionUtil.class);
	
	public static Integer getFilterValueAsInteger(String filterValue) {

		Integer filterValueAsInteger = null;

		try {
			filterValueAsInteger = Integer.parseInt(filterValue);

		} catch (NumberFormatException e) {
			// ignore
		}

		return filterValueAsInteger;
	}

	public static Long getFilterValueAsLong(String filterValue) {

		Long filterValueAsLong = null;

		try {
			filterValueAsLong = Long.parseLong(filterValue);

		} catch (NumberFormatException e) {
			// ignore
		}

		return filterValueAsLong;
	}

	public static Double getFilterValueAsDouble(String filterValue) {

		Double filterValueAsDouble = null;

		try {
			filterValueAsDouble = Double.parseDouble(filterValue);

		} catch (NumberFormatException e) {
			// ignore
		}

		return filterValueAsDouble;
	}

	public static BigDecimal getFilterValueAsBigDecimal(String filterValue) {

		BigDecimal filterValueAsBigDecimal = null;

		try {
			filterValueAsBigDecimal = new BigDecimal(filterValue);

		} catch (Exception e) {
			// ignore
		}

		return filterValueAsBigDecimal;
	}

	public static Date getFilterValueAsDate(String filterValue, String dateFormatString ) {

		Date filterValueAsDate = null;

		SimpleDateFormat dateFormat = new SimpleDateFormat(dateFormatString);

		try {
			filterValueAsDate = dateFormat.parse(filterValue);

		} catch (ParseException e) {
			// ignore
		}

		return filterValueAsDate;
	}

}

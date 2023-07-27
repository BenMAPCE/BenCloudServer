package gov.epa.bencloud.server.util;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Methods for performing conversions of strings to other data types.
 */
public class DataConversionUtil {
	private static final Logger log = LoggerFactory.getLogger(DataConversionUtil.class);



	/**
	 * 
	 * @param filterValue
	 * @return the given filter value in short format.
	 */
	public static Short getFilterValueAsShort(String filterValue) {

		Short filterValueAsShort = null;

		try {
			filterValueAsShort = Short.parseShort(filterValue);

		} catch (NumberFormatException e) {
			// ignore
		}

		return filterValueAsShort;
	}
	
	/**
	 * 
	 * @param filterValue
	 * @return the given filter value in integer format.
	 */
	public static Integer getFilterValueAsInteger(String filterValue) {

		Integer filterValueAsInteger = null;

		try {
			filterValueAsInteger = Integer.parseInt(filterValue);

		} catch (NumberFormatException e) {
			// ignore
		}

		return filterValueAsInteger;
	}

	/**
	 * 
	 * @param filterValue
	 * @return the given filter value in long format.
	 */
	public static Long getFilterValueAsLong(String filterValue) {

		Long filterValueAsLong = null;

		try {
			filterValueAsLong = Long.parseLong(filterValue);

		} catch (NumberFormatException e) {
			// ignore
		}

		return filterValueAsLong;
	}

	/**
	 * 
	 * @param filterValue
	 * @return the given filter value in double format.
	 */
	public static Double getFilterValueAsDouble(String filterValue) {

		Double filterValueAsDouble = null;

		try {
			filterValueAsDouble = Double.parseDouble(filterValue);

		} catch (NumberFormatException e) {
			// ignore
		}

		return filterValueAsDouble;
	}

	/**
	 * 
	 * @param filterValue
	 * @return the given filter value in big decimal format.
	 */
	public static BigDecimal getFilterValueAsBigDecimal(String filterValue) {

		BigDecimal filterValueAsBigDecimal = null;

		try {
			filterValueAsBigDecimal = new BigDecimal(filterValue);

		} catch (Exception e) {
			// ignore
		}

		return filterValueAsBigDecimal;
	}

	/**
	 * 
	 * @param filterValue
	 * @param dateFormatString
	 * @return the given filter value in date format.
	 */
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

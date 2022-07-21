package gov.epa.bencloud.server.util;

/*
 * Methods for converting parameter values to other data types, or for setting null parameters to default values.
 */
public class ParameterUtil {

	/**
	 * 
	 * @param parametersValue
	 * @param defaultValue
	 * @return the parameter value as an integer, or the default value if parametersValue == null or cannot be formatted as an integer.
	 */
	public static int getParameterValueAsInteger(String parametersValue, int defaultValue) {

		int value = defaultValue;

		if (null != parametersValue) {
			try {
				value = Integer.parseInt(parametersValue);
			} catch (NumberFormatException e) {
				value = defaultValue;
			}
		}

		return value;
	}	

	/**
	 * 
	 * @param parametersValue
	 * @param defaultValue
	 * @return the parameter value as a string, or "" if parametersValue == null.
	 */
	public static String getParameterValueAsString(String parametersValue, String defaultValue) {

		String value = "";

		if (null != parametersValue) {
			value = parametersValue;
		}

		return value;
	}

	/**
	 * 
	 * @param parametersValue
	 * @param defaultValue
	 * @return the parameter value as a boolean, or the default value if parametersValue == null. 
	 */
	public static boolean getParameterValueAsBoolean(String parametersValue, boolean defaultValue) {

		boolean value = defaultValue;

		if (null != parametersValue) {
			try {
				value = Boolean.valueOf(parametersValue);
			} catch (Exception e) {
				value = defaultValue;
			}
		}

		return value;
	}

}

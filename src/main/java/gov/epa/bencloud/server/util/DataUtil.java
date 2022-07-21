package gov.epa.bencloud.server.util;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/*
 * Methods for converting seconds into human readable times.
 */
public class DataUtil {

	/**
	 * 
	 * @param start
	 * @param end
	 * @return the time in a human readable format (hours minutes seconds).
	 */
	public static String getHumanReadableTime(LocalDateTime start, LocalDateTime end) {
		
		if (null == start || null == end) {
			return "";
		}
		
		long totalSeconds = ChronoUnit.SECONDS.between(start, end);
		
		StringBuilder humanReadableTime = new StringBuilder();

		getHumanReadableTime(totalSeconds, humanReadableTime);
		return humanReadableTime.toString();
		
	}
	
	/**
	 * Formats the a number of seconds into a human readable format (hours minutes seconds).
	 * @param totalSeconds
	 * @param humanReadableTime
	 */
	public static void getHumanReadableTime(long totalSeconds, StringBuilder humanReadableTime) {
	
		long hours = ((totalSeconds/60) / 60);
		long minutes = ((totalSeconds / 60) % 60);
		long seconds = (totalSeconds % 60);

		if (hours > 0) {
			if (hours < 2) {
				humanReadableTime.append(hours).append(" hour").append(" ");
			} else {
				humanReadableTime.append(hours).append(" hours").append(" ");
			}
		}
		
		if (minutes > 0) {
			if (minutes < 2) {
				humanReadableTime.append(minutes).append(" minute").append(" ");
			} else {
				humanReadableTime.append(minutes).append(" minutes").append(" ");
			}
		}
		
		if (seconds > 0) {
			if (seconds < 2) {
				humanReadableTime.append(seconds).append(" second").append(" ");
			} else {
				humanReadableTime.append(seconds).append(" seconds").append(" ");
			}
		}
		
		if(hours==0 && minutes==0 && seconds==0) {
			humanReadableTime.append("0 seconds");
		}
	}

}

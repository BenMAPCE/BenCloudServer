package gov.epa.bencloud.api.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.pac4j.core.profile.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import gov.epa.bencloud.server.util.DataUtil;


/*
 * Representation of a health impact function task log
 * Used to log status/progress messages about a given task
 */
public class HIFTaskLog extends TaskLog {
	private static final Logger log = LoggerFactory.getLogger(HIFTaskLog.class);
	private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");
	
	private HIFTaskConfig hifTaskConfig = null;
	private List<String> logMessages = new ArrayList<String>();
	
	/**
	 * Default constructor
	 */
	public HIFTaskLog() {
		super();
	}
	
	/**
	 * Creates an hif task log object from a hifTaskConfig object
	 * @param hifTaskConfig
	 * @param userId 
	 */
	public HIFTaskLog(HIFTaskConfig hifTaskConfig, String userId) {
		super();
		this.hifTaskConfig = hifTaskConfig;
		this.setUserId(userId);
	}
	
	/**
	 * 
	 * @return the hif task configuration
	 */
	public HIFTaskConfig getHifTaskConfig() {
		return hifTaskConfig;
	}
	
	/**
	 * Sets the hif task configuration
	 * @param hifTaskConfig
	 */
	public void setHifTaskConfig(HIFTaskConfig hifTaskConfig) {
		this.hifTaskConfig = hifTaskConfig;
	}

	/**
	 * 
	 * @return hif task log messages
	 */
	public List<String> getLogMessages() {
		return logMessages;
	}

	/**
	 * Sets the hif task log messages
	 * @param logMessages
	 */
	public void setLogMessages(List<String> logMessages) {
		this.logMessages = logMessages;
	}
	
	/**
	 * Adds a messages to the hif task log messages
	 * @param message
	 */
	public void addMessage(String message) {
        logMessages.add(dtf.format(LocalDateTime.now()) + ": " + message); 
	}
	
	/**
	 * 
	 * @param userProfile
	 * @return string representation of the hif task log
	 */
	public String toString(Optional<UserProfile> userProfile) {
		StringBuilder b = new StringBuilder();
		b.append("-------------------------------\n");
		b.append("HEALTH IMPACT FUNCTION TASK LOG\n");
		b.append("-------------------------------\n\n");
		b.append("BenMAP Cloud application version: ")
			.append(getAppVersion())
			.append(", database version: ")
			.append(getDbVersion())
			.append("\n");
		b.append("User: ")
			.append(getUserId())
			.append("\n");
		b.append("Completed: ")
			.append(dtf.format(getDtEnd()))
			.append("\n");
		b.append("Total processing time: ")
			.append(DataUtil.getHumanReadableTime(getDtStart(), getDtEnd()))
			.append("\n");
		b.append("\n");

		b.append(hifTaskConfig.toString(userProfile));
		
		b.append("\nPROCESSING LOG\n\n");
		for(String msg : logMessages) {
			b.append(msg).append("\n");	
		}
		
		return b.toString();
	}

	/**
	 * 
	 * @return JSON string representation of the hif task log
	 */
	public String toJsonString() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		
		try {
			return mapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			log.error("Unable to create JSON", e);
			return "{\"error\" : \"Unable to create JSON\"}";
		}
	}
	
}

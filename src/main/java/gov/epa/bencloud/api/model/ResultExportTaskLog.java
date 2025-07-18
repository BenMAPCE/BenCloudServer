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
 * Representation of a grid import task log
 * Used to log status/progress messages about a given task
 */
public class ResultExportTaskLog extends TaskLog {
	private static final Logger log = LoggerFactory.getLogger(ResultExportTaskLog.class);
	private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");
	
	private ResultExportTaskConfig resultExportTaskConfig = null;
	private List<String> logMessages = new ArrayList<String>();
	
	/**
	 * Default constructor
	 */
	public ResultExportTaskLog() {
		super();
	}
	
	/**
	 * Creates an grid import task log object from a gridImportTaskConfig object
	 * @param resultExportTaskConfig
	 * @param userId 
	 */
	public ResultExportTaskLog(ResultExportTaskConfig resultExportTaskConfig, String userId) {
		super();
		this.resultExportTaskConfig = resultExportTaskConfig;
		this.setUserId(userId);
	}
	
	/**
	 * 
	 * @return the grid import task configuration
	 */
	public ResultExportTaskConfig getGridImportTaskConfig() {
		return resultExportTaskConfig;
	}
	
	/**
	 * Sets the grid import task configuration
	 * @param gridImportTaskConfig
	 */
	public void setGridImportTaskConfig(ResultExportTaskConfig resultExportTaskConfig) {
		this.resultExportTaskConfig = resultExportTaskConfig;
	}

	/**
	 * 
	 * @return grid import task log messages
	 */
	public List<String> getLogMessages() {
		return logMessages;
	}

	/**
	 * Sets the grid import task log messages
	 * @param logMessages
	 */
	public void setLogMessages(List<String> logMessages) {
		this.logMessages = logMessages;
	}
	
	/**
	 * Adds a messages to the grid import task log messages
	 * @param message
	 */
	public void addMessage(String message) {
        logMessages.add(dtf.format(LocalDateTime.now()) + ": " + message); 
	}
	
	/**
	 * 
	 * @param userProfile
	 * @return string representation of the grid import task log
	 */
	public String toString(Optional<UserProfile> userProfile) {
		StringBuilder b = new StringBuilder();
		b.append("-------------------------------\n");
		b.append("RESULT EXPORT TASK LOG\n");
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

		b.append(resultExportTaskConfig.toString(userProfile));
		
		b.append("\nPROCESSING LOG\n\n");
		for(String msg : logMessages) {
			b.append(msg).append("\n");	
		}
		
		return b.toString();
	}

	/**
	 * 
	 * @return JSON string representation of the grid import task log
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

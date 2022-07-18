package gov.epa.bencloud.api.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import gov.epa.bencloud.server.util.DataUtil;


/*
 * Representation of a valuation task log
 */
public class ValuationTaskLog extends TaskLog {
	private static final Logger log = LoggerFactory.getLogger(ValuationTaskLog.class);
	private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");
	
	private ValuationTaskConfig vfTaskConfig = null;
	private List<String> logMessages = new ArrayList<String>();
	
	/*
	 * Default constructor
	 */
	public ValuationTaskLog() {
		super();
	}
	
	/*
	 * Creates a valuation task log object from a valuation task configuration object
	 */
	public ValuationTaskLog(ValuationTaskConfig vfTaskConfig) {
		super();
		this.vfTaskConfig = vfTaskConfig;
	}
	
	/**
	 * 
	 * @return the valuation task configuration object.
	 */
	public ValuationTaskConfig getVfTaskConfig() {
		return vfTaskConfig;
	}

	/**
	 * Sets the valuation task configuration object.
	 * @param vfTaskConfig
	 */
	public void setHifTaskConfig(ValuationTaskConfig vfTaskConfig) {
		this.vfTaskConfig = vfTaskConfig;
	}

	/**
	 * 
	 * @return the list of valuation task log messages.
	 */
	public List<String> getLogMessages() {
		return logMessages;
	}
	
	/**
	 * Sets the valuation task log messages.
	 * @param logMessages
	 */
	public void setLogMessages(List<String> logMessages) {
		this.logMessages = logMessages;
	}
	
	/**
	 * Adds a messages to the valuation task log messages.
	 * @param message
	 */
	public void addMessage(String message) {
        logMessages.add(dtf.format(LocalDateTime.now()) + ": " + message); 
	}
	
	/*
	 * Returns a string representation of the valuation task log object
	 */
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("---------------------------\n");
		b.append("VALUATION FUNCTION TASK LOG\n");
		b.append("---------------------------\n\n");
		b.append("BenMAP Cloud application version: ")
			.append(getAppVersion())
			.append(", database version: ")
			.append(getDbVersion())
			.append("\n");
		b.append("User: ")
			.append("IN DEVELOPMENT")
			.append("\n");
		b.append("Completed: ")
			.append(dtf.format(getDtEnd()))
			.append("\n");
		b.append("Total processing time: ")
			.append(DataUtil.getHumanReadableTime(getDtStart(), getDtEnd()))
			.append("\n");
		b.append("\n");

		b.append(vfTaskConfig.toString());
		
		b.append("\nPROCESSING LOG\n\n");
		for(String msg : logMessages) {
			b.append(msg).append("\n");	
		}
		
		return b.toString();
	}

	/**
	 * 
	 * @return a string represenation of the JSON representation of the valuatino task log.
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

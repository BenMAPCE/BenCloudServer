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

public class ValuationTaskLog extends TaskLog {
	private static final Logger log = LoggerFactory.getLogger(ValuationTaskLog.class);
	private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");
	
	private ValuationTaskConfig vfTaskConfig = null;
	private List<String> logMessages = new ArrayList<String>();
	
	public ValuationTaskLog() {
		super();
	}
	
	public ValuationTaskLog(ValuationTaskConfig vfTaskConfig) {
		super();
		this.vfTaskConfig = vfTaskConfig;
	}
	
	public ValuationTaskConfig getVfTaskConfig() {
		return vfTaskConfig;
	}
	public void setHifTaskConfig(ValuationTaskConfig vfTaskConfig) {
		this.vfTaskConfig = vfTaskConfig;
	}
	public List<String> getLogMessages() {
		return logMessages;
	}
	public void setLogMessages(List<String> logMessages) {
		this.logMessages = logMessages;
	}
	
	public void addMessage(String message) {
        logMessages.add(dtf.format(LocalDateTime.now()) + ": " + message); 
	}
	
	
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

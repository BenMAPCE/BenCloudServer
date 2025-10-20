package gov.epa.bencloud.api.model;

import java.util.Map;
import java.util.Optional;

import org.pac4j.core.profile.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import gov.epa.bencloud.server.tasks.model.Task;

/*
 * Representation of a air quality import task configuration
 */

public class AirQualityImportTaskConfig {
	private static final Logger log = LoggerFactory.getLogger(AirQualityImportTaskConfig.class);
	
	public String name;
	public String groupName;
	public String userId;
	public Integer pollutantId;
	public String aqYear;
	public String source;
	public String dataType;
	public String description;
	public Integer gridId;
	public Map<String, Integer> csvFilestoreIds; //layerName and filestoreId

	/*
	 * Default constructor
	 */
	public AirQualityImportTaskConfig() {
		super();
	}

	/**
	 * Creates an air quality import configuration object from a task object
	 * @param task
	 */
	public AirQualityImportTaskConfig(Task task) {
		super();
		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode params = mapper.readTree(task.getParameters());
			this.name = task.getName();

			this.groupName = params.get("group name").asText();
			this.userId = params.get("userId").asText();
			pollutantId = params.get("pollutantId").asInt();
			aqYear = params.get("aqYear").asText();
			source = params.get("source").asText();
			dataType = params.get("dataType").asText();
			description = params.get("description").asText();
			gridId = params.get("gridId").asInt();

		ArrayNode filesArray = mapper.createArrayNode();
		for (Map.Entry<String, Integer> entry : csvFilestoreIds.entrySet()) {
			ObjectNode file = mapper.createObjectNode();
			file.put("layerName", entry.getKey());
			file.put("filestoreId", entry.getValue());
			filesArray.add(file);
		}

		JsonNode filesNode = params.get("files");
		for (JsonNode fileNode : filesNode) {
			csvFilestoreIds.put(fileNode.get("layerName").asText(),fileNode.get("filestoreId").asInt());
		}

		} catch (JsonMappingException e) {
			log.error("Error parsing task parameters", e);
		} catch (JsonProcessingException e) {
			log.error("Error processing task parameters", e);
		}
	}

	/**
	 * 
	 * @param userProfile
	 * @return string representation of the air quality import task configuration
	 */
	public String toString(Optional<UserProfile> userProfile) {
		StringBuilder b = new StringBuilder();
				
		b.append("Air Quality Group Name: ").append(name).append("\n\n");
				
		return b.toString();
	}

}

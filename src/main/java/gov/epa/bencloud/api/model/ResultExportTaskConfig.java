package gov.epa.bencloud.api.model;

import static gov.epa.bencloud.server.database.jooq.data.Tables.AIR_QUALITY_LAYER;
import static gov.epa.bencloud.server.database.jooq.data.Tables.GRID_DEFINITION;
import static gov.epa.bencloud.server.database.jooq.data.Tables.POPULATION_DATASET;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jooq.JSON;
import org.jooq.Record16;
import org.jooq.Record3;
import org.pac4j.core.profile.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import gov.epa.bencloud.api.AirQualityApi;
import gov.epa.bencloud.api.GridDefinitionApi;
import gov.epa.bencloud.api.PopulationApi;
import gov.epa.bencloud.api.function.EFunction;
import gov.epa.bencloud.api.util.AirQualityUtil;
import gov.epa.bencloud.api.util.ExposureUtil;
import gov.epa.bencloud.server.tasks.model.Task;

/*
 * Representation of a health impact function task configuration
 */

public class ResultExportTaskConfig {
	private static final Logger log = LoggerFactory.getLogger(ResultExportTaskConfig.class);
	
	public String name;
	public Integer filestoreId;
	public String userId;

	public Integer batchId;
	public Boolean includeHealthImpact;
	public Boolean includeValuation;
	public Boolean includeExposure;
	public String taskUuid;
	public String uuidType;
	public Integer[] gridIds;
	public Boolean isAdmin;
	
	/*
	 * Default constructor
	 */
	public ResultExportTaskConfig() {
		super();
	}

	/**
	 * Creates a result export configuration object from a task object
	 * @param task
	 */
	public ResultExportTaskConfig(Task task) {
		super();
		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode params = mapper.readTree(task.getParameters());
			this.name = task.getName();

		} catch (JsonMappingException e) {
			log.error("Error parsing task parameters", e);
		} catch (JsonProcessingException e) {
			log.error("Error processing task parameters", e);
		}
	}

	/**
	 * 
	 * @param userProfile
	 * @return string representation of the result export task configuration
	 */
	public String toString(Optional<UserProfile> userProfile) {
		StringBuilder b = new StringBuilder();
				
		b.append("Export Name: ").append(name).append("\n\n");

		//TODO: Add all fields here?
		
		return b.toString();
	}

	

	
}

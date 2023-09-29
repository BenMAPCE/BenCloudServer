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

public class ExposureTaskConfig {
	private static final Logger log = LoggerFactory.getLogger(ExposureTaskConfig.class);
	
	public String name;
	public Integer resultDatasetId = 0;
	public Integer aqBaselineId = 0;
	public Integer aqScenarioId = 0;
	public Integer popId = 0;
	public Integer popYear = 0;
	public List<ExposureConfig> exposureFunctions = new ArrayList<ExposureConfig>();
	public Integer gridDefinitionId = 0;
	/*
	 * Default constructor
	 */
	public ExposureTaskConfig() {
		super();
	}

	/**
	 * Creates an exposure task configuration object from a task object
	 * @param task
	 */
	public ExposureTaskConfig(Task task) {
		super();
		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode params = mapper.readTree(task.getParameters());
			JsonNode aqLayers = params.get("air_quality_data");
			this.name = task.getName();

			for (JsonNode aqLayer : aqLayers) {
				switch (aqLayer.get("type").asText().toLowerCase()) {
				case "baseline":
					this.aqBaselineId = aqLayer.get("id").asInt();
					Integer gridDefinitionId = AirQualityApi.getAirQualityLayerGridId(aqBaselineId);
					this.gridDefinitionId = gridDefinitionId;
					break;
				case "scenario":
					this.aqScenarioId = aqLayer.get("id").asInt();
					break;
				}
			}
			JsonNode popConfig = params.get("population");
			this.popId = popConfig.get("id").asInt();

			JsonNode functions = params.get("functions");
			System.out.println(functions);
			
			this.popYear = popConfig.get("year").asInt();


			for (JsonNode function : functions) {
				this.exposureFunctions.add(new ExposureConfig(function));
			}

		} catch (JsonMappingException e) {
			log.error("Error parsing task parameters", e);
		} catch (JsonProcessingException e) {
			log.error("Error processing task parameters", e);
		}
	}

	public List<Integer> getRequiredVariableIds() {

		Set<Integer> requiredVariableIds = new HashSet<Integer>();

		for (ExposureConfig eConfig : this.exposureFunctions) {
			if (eConfig.variable != null) {
				requiredVariableIds.add(eConfig.variable);
			}
		}

		List<Integer> requiredVariableIdsList = new ArrayList<Integer>();
		requiredVariableIdsList.addAll(requiredVariableIds);
		return requiredVariableIdsList;
	}

	/**
	 * 
	 * @param userProfile
	 * @return string representation of the hif task configuration
	 */
	public String toString(Optional<UserProfile> userProfile) {
		StringBuilder b = new StringBuilder();
		
		Record16<Integer, String, String, Short, Integer, Integer, String, String, String, String, String, LocalDateTime, String, String, String, JSON> baselineAq = AirQualityApi.getAirQualityLayerDefinition(aqBaselineId, userProfile);
		Record16<Integer, String, String, Short, Integer, Integer, String, String, String, String, String, LocalDateTime, String, String, String, JSON> scenarioAq = AirQualityApi.getAirQualityLayerDefinition(aqScenarioId, userProfile);
		
		b.append("Task Name: ").append(name).append("\n\n");
		
		Record3<String, Integer, String> populationInfo = PopulationApi.getPopulationDatasetInfo(popId);
		b.append("Analysis Year: ").append(popYear).append("\n\n");
		
		/*
		 * Pollutant
		 */
		String pollMetrics = AirQualityUtil.getPollutantMetricList((Integer)baselineAq.getValue("pollutant_id"));
		
		b.append("POLLUTANT\n\n");
		b.append("Pollutant Name: ")
			.append(baselineAq.getValue("pollutant_friendly_name"))
			.append("\nDefined Metrics: ")
			.append(pollMetrics)
			.append("\n\n");

		b.append("AIR QUALITY DATA\n\n");
		/*
		 * Pre-policy AQ
		 */
		b.append("Pre-policy Air Quality Surface\n");
		b.append("Name: ").append(baselineAq.getValue(AIR_QUALITY_LAYER.NAME)).append("\n");
		b.append("Year: ").append(baselineAq.getValue(AIR_QUALITY_LAYER.AQ_YEAR)).append("\n");
		b.append("Description: ").append(baselineAq.getValue(AIR_QUALITY_LAYER.DESCRIPTION)).append("\n");
		b.append("Source: ").append(baselineAq.getValue(AIR_QUALITY_LAYER.SOURCE)).append("\n");
		b.append("Data Type: ").append(baselineAq.getValue(AIR_QUALITY_LAYER.DATA_TYPE)).append("\n");
		
		Record3<String, Integer, Integer> gridDefinitionInfo = GridDefinitionApi.getGridDefinitionInfo(baselineAq.getValue(AIR_QUALITY_LAYER.GRID_DEFINITION_ID));
		b.append("Grid Definition: ").append(gridDefinitionInfo.getValue(GRID_DEFINITION.NAME)).append("\n");
		JSON metricStatisticsJson = baselineAq.getValue("metric_statistics", JSON.class);
		ObjectMapper mapper = new ObjectMapper();
		JsonNode metricStats = null;
		//If we can't parse the metric statistics, we'll just skip them for now
		try {
			metricStats = mapper.readTree(metricStatisticsJson.data());
			if(metricStats.isArray()) {
				for(JsonNode stat : metricStats) {
					b.append("Metric: ").append(stat.get("metric_name").asText()).append("\n");
					b.append("- Seasonal Metric: ").append(stat.get("seasonal_metric_name").asText("Null")).append("\n");
					b.append("- Annual Statistic: ").append(stat.get("annual_statistic_name").asText("Null")).append("\n");
					b.append("- Cell Count: ").append(stat.get("cell_count").asInt()).append("\n");
					b.append("- Min/Max/Mean: ")
						.append(stat.get("min_value"))
						.append("/")
						.append(stat.get("max_value"))
						.append("/")
						.append(stat.get("mean_value"))
						.append("\n");
				}
				
			}
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		/*
		 * Post-policy AQ
		 */
		b.append("\nPost-policy Air Quality Surface\n");
		b.append("Name: ").append(scenarioAq.getValue(AIR_QUALITY_LAYER.NAME)).append("\n");
		b.append("Year: ").append(scenarioAq.getValue(AIR_QUALITY_LAYER.AQ_YEAR)).append("\n");
		b.append("Description: ").append(scenarioAq.getValue(AIR_QUALITY_LAYER.DESCRIPTION)).append("\n");
		b.append("Source: ").append(scenarioAq.getValue(AIR_QUALITY_LAYER.SOURCE)).append("\n");
		b.append("Data Type: ").append(scenarioAq.getValue(AIR_QUALITY_LAYER.DATA_TYPE)).append("\n");
		gridDefinitionInfo = GridDefinitionApi.getGridDefinitionInfo(scenarioAq.getValue(AIR_QUALITY_LAYER.GRID_DEFINITION_ID));
		b.append("Grid Definition: ").append(gridDefinitionInfo.getValue(GRID_DEFINITION.NAME)).append("\n");
		metricStatisticsJson = scenarioAq.getValue("metric_statistics", JSON.class);

		//If we can't parse the metric statistics, we'll just skip them for now
		try {
			metricStats = mapper.readTree(metricStatisticsJson.data());
			if(metricStats.isArray()) {
				for(JsonNode stat : metricStats) {
					b.append("Metric: ").append(stat.get("metric_name").asText()).append("\n");
					b.append("- Seasonal Metric: ").append(stat.get("seasonal_metric_name").asText("Null")).append("\n");
					b.append("- Annual Statistic: ").append(stat.get("annual_statistic_name").asText("Null")).append("\n");
					b.append("- Cell Count: ").append(stat.get("cell_count").asInt()).append("\n");
					b.append("- Min/Max/Mean: ")
						.append(stat.get("min_value"))
						.append("/")
						.append(stat.get("max_value"))
						.append("/")
						.append(stat.get("mean_value"))
						.append("\n");
				}	
			}
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		/*
		 * Population
		 */
		b.append("\nPOPULATION DATA\n\n");
		b.append("Population Dataset: ").append(populationInfo.getValue(POPULATION_DATASET.NAME)).append("\n");
		b.append("Population Year: ").append(popYear).append("\n");
		b.append("Grid Definition: ").append(populationInfo.getValue(GRID_DEFINITION.NAME)).append("\n\n");
		
		/*
		 * Exposure Functions
		 */
		b.append("EXPOSURE FUNCTIONS\n\n");	
		//TODO: Add name of exposure function group here?
		b.append("Functions Selected: ").append(exposureFunctions.size()).append("\n\n");

		//exposure functions are sorted by population group name by default
		for(int i=0; i < exposureFunctions.size(); i++) {
			ExposureConfig function = exposureFunctions.get(i);
			b.append("Function ").append(i+1).append(":\n");
			b.append(function.toString());
			b.append("\n");
		}
		
		return b.toString();
	}

	

	
}

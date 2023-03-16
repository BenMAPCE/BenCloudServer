package gov.epa.bencloud.api.model;

import static gov.epa.bencloud.server.database.jooq.data.Tables.AIR_QUALITY_LAYER;
import static gov.epa.bencloud.server.database.jooq.data.Tables.GRID_DEFINITION;
import static gov.epa.bencloud.server.database.jooq.data.Tables.POPULATION_DATASET;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jooq.JSON;
import org.jooq.Record10;
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
import gov.epa.bencloud.api.util.AirQualityUtil;
import gov.epa.bencloud.server.tasks.model.Task;

/*
 * Representation of a batch of health impact and valuation function scenarios
 */

public class BatchTaskConfig {
	private static final Logger log = LoggerFactory.getLogger(BatchTaskConfig.class);
	
	public String name;
	public Integer gridDefinitionId = 0;
	public Integer aqBaselineId = 0;
	public Integer popId = 0;
	public String pollutantName;
	public Integer pollutantId = 0;
	public Boolean preserveLegacyBehavior = true;
	public List<Scenario> aqScenarios = new ArrayList<Scenario>();
	public List<BatchHIFGroup> batchHifGroups = new ArrayList<BatchHIFGroup>();
	
	/*
	 * Default constructor
	 */
	public BatchTaskConfig() {
		super();
	}

	/**
	 * Creates a batch task configuration object from the parameters json field in the task object
	 * @param task
	 */
	public BatchTaskConfig(Task task) {
		super();
		
		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode params = mapper.readTree(task.getParameters());
			System.out.println(params.toString());
			JsonNode config = params.get("configuration");

			this.name = params.get("name").asText();
			this.pollutantName = config.get("pollutant").asText();
			this.pollutantId = config.get("pollutant_id").asInt();
			this.gridDefinitionId = params.get("valuation_grid").asInt();
			this.aqBaselineId = config.get("pre_policy_aq_id").asInt();
			this.popId = config.get("population_id").asInt();

			JsonNode scenarios = config.get("scenarios");

			for (JsonNode scenario : scenarios) {
				this.aqScenarios.add(new Scenario(scenario));
			}

			JsonNode functionGroups = params.get("hif_function_groups");

			for (JsonNode functionGroup : functionGroups) {
				this.batchHifGroups.add(new BatchHIFGroup(functionGroup));
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
	 * @return string representation of the batch task configuration
	 */
	public String toString(Optional<UserProfile> userProfile) {
		StringBuilder b = new StringBuilder();
		
		Record10<Integer, String, String, Short, Integer, Integer, String, String, String, JSON> baselineAq = AirQualityApi.getAirQualityLayerDefinition(aqBaselineId, userProfile);
		
		b.append("Task Name: ").append(name).append("\n\n");
		
		Record3<String, Integer, String> populationInfo = PopulationApi.getPopulationDatasetInfo(popId);
		
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
		b.append("Source: ")
			.append("Model")
			.append("\n");		
		Record3<String, Integer, Integer> gridDefinitionInfo = GridDefinitionApi.getGridDefinitionInfo(baselineAq.getValue(AIR_QUALITY_LAYER.GRID_DEFINITION_ID));
		b.append("Grid Definition: ").append(gridDefinitionInfo.getValue(GRID_DEFINITION.NAME)).append("\n");
		//b.append("Columns: ").append(gridDefinitionInfo.getValue(GRID_DEFINITION.COL_COUNT)).append("\n");
		//b.append("Row: ").append(gridDefinitionInfo.getValue(GRID_DEFINITION.ROW_COUNT)).append("\n\n");		
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
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		
		Record10<Integer, String, String, Short, Integer, Integer, String, String, String, JSON> scenarioAq = null;
		/*
		 * Post-policy AQ
		 */
		b.append("\nPost-Policy Air Quality Surfaces selected: ").append(aqScenarios.size()).append("\n");
		for(Scenario scenario : aqScenarios) {
			scenarioAq = AirQualityApi.getAirQualityLayerDefinition(scenario.id, userProfile);
			b.append("\nPost-policy Air Quality Surface\n");
			b.append("Name: ").append(scenario.name).append("\n");
			b.append("Source: ")
				.append("Model")
				.append("\n");	
			gridDefinitionInfo = GridDefinitionApi.getGridDefinitionInfo(gridDefinitionId);
			b.append("Grid Definition: ").append(gridDefinitionInfo.getValue(GRID_DEFINITION.NAME)).append("\n");
			// b.append("Columns: ").append(gridDefinitionInfo.getValue(GRID_DEFINITION.COL_COUNT)).append("\n");
			// b.append("Row: ").append(gridDefinitionInfo.getValue(GRID_DEFINITION.ROW_COUNT)).append("\n\n");

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
				e.printStackTrace();
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
			b.append(scenario.toString(userProfile));
		}
		
		/*
		 * Population
		 */
		b.append("\nPOPULATION DATA\n\n");
		b.append("Population Dataset: ").append(populationInfo.getValue(POPULATION_DATASET.NAME)).append("\n");
		b.append("Grid Definition: ").append(populationInfo.getValue(GRID_DEFINITION.NAME)).append("\n\n");
		
		/*
		 * Health Impact Groups
		 */
		b.append("HEALTH IMPACT GROUPS\n\n");
		b.append("Health Effect Groups Analyzed:\n");

		for(BatchHIFGroup batchHifGroup : batchHifGroups) {
			b.append(batchHifGroup.toString(userProfile));
		}
		
		b.append("ADVANCED SETTINGS\n\n");
		b.append("BenMAP-CE Desktop Backward Compatibility Mode: ").append(preserveLegacyBehavior ? "Enabled" : "Disabled").append("\n");

		return b.toString();
	}
	
}

package gov.epa.bencloud.api.model;

import static gov.epa.bencloud.server.database.jooq.data.Tables.AIR_QUALITY_LAYER;
import static gov.epa.bencloud.server.database.jooq.data.Tables.GRID_DEFINITION;
import static gov.epa.bencloud.server.database.jooq.data.Tables.POPULATION_DATASET;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Arrays;

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
	public Boolean preserveLegacyBehavior = true;
	public List<Scenario> aqScenarios = new ArrayList<Scenario>();
	public List<HifGroup> hifGroups = new ArrayList<HifGroup>();
	
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
			JsonNode config = params.get("configuration");

			this.name = task.getName();
			this.pollutantName = config.get("pollutant").asText();
			this.gridDefinitionId = params.get("valuation_grid").asInt();
			this.aqBaselineId = config.get("pre_policy_aq_id").asInt();
			this.popId = config.get("population_id").asInt();

			JsonNode scenarios = config.get("scenarios");

			for (JsonNode scenario : scenarios) {
				this.aqScenarios.add(new Scenario(scenario));
			}

			JsonNode functionGroups = params.get("hif_function_groups");

			for (JsonNode functionGroup : functionGroups) {
				this.hifGroups.add(new HifGroup(functionGroup));
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
			b.append(scenario.toString());

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

		for(HifGroup hifGroup : hifGroups) {
			b.append(hifGroup.toString());
		}
		
		b.append("ADVANCED SETTINGS\n\n");
		b.append("BenMAP-CE Desktop Backward Compatibility Mode: ").append(preserveLegacyBehavior ? "Enabled" : "Disabled").append("\n");

		return b.toString();
	}
	
}


class Scenario {
    public Integer id = 0;
    public String name;
    public List<PopConfig> popConfigs = new ArrayList<PopConfig>();

	/**
	 * Creates an object from the task parameter object
	 * @param scenario
	 */
	public Scenario(JsonNode scenario) {
		id = scenario.has("post_policy_aq_id") ? scenario.get("post_policy_aq_id").asInt() : null;
		name = scenario.has("post_policy_aq") ? scenario.get("post_policy_aq").asText() : null;
		JsonNode popConfigsJson = scenario.has("population_configs") ? scenario.get("population_configs") : null;
		if(popConfigsJson != null) {
			for(JsonNode popConfig : popConfigsJson) {
				popConfigs.add(new PopConfig(popConfig));
			}
		}
	}

		/**
	 * 
	 * @param userProfile
	 * @return string representation of the aq scenario
	 */
	public String toString(Optional<UserProfile> userProfile) {
		StringBuilder b = new StringBuilder();
		b.append("Scenario id: ").append(id).append("\n");
		b.append("Population Configurations:\n");
		for(int i = 0; i < popConfigs.size(); i++) {
			b.append(popConfigs.get(i).toString(userProfile, i+1)).append("\n");
		}
		return b.toString();
	}
}

class PopConfig {
    public Integer popYear = null;
    public List<HifConfig> hifConfigs = new ArrayList<HifConfig>();

	/**
	 * Creates an object from the task parameter object
	 * @param popConfig
	 */
	public PopConfig(JsonNode popConfig) {
		popYear = popConfig.has("population_year") ? popConfig.get("population_year").asInt() : null;
		JsonNode hifConfigsJson = popConfig.has("hif_configs") ? popConfig.get("hif_configs") : null;
		if(hifConfigsJson != null) {
			for(JsonNode hifConfig : hifConfigsJson) {
				hifConfigs.add(new HifConfig(hifConfig));
			}
		}
	}

	/**
	 * 
	 * @param userProfile
	 * @return string representation of the population configuration
	 */
	public String toString(Optional<UserProfile> userProfile, Integer count) {
		StringBuilder b = new StringBuilder();
		b.append("Population Configuration (").append(count).append("):\n");
		b.append("Population year: ").append(popYear).append("\n");
		b.append("HIF Configurations:\n");
		for(int i = 0; i < hifConfigs.size(); i++) {
			b.append(hifConfigs.get(i).toString(userProfile, i+1)).append("\n");
		}
		return b.toString();
	}
}

class HifConfig {
    public Integer hifInstanceId = null;
    public Integer incidenceYear = null;
    public List<Integer> prevalenceYears = new ArrayList<Integer>();

	/**
	 * Creates an object from the task parameter object
	 * @param hifConfig
	 */
	public HifConfig(JsonNode hifConfig) {
		hifInstanceId = hifConfig.has("hif_instance_id") ? hifConfig.get("hif_instance_id").asInt() : null;
		incidenceYear = hifConfig.has("incidence_year") ? hifConfig.get("incidence_year").asInt() : null;
		String[] currentPrevalence = hifConfig.has("prevalence_years") ? hifConfig.get("prevalence_years").asText().split(",") : null;
		if(currentPrevalence != null) {	
			for(String year : currentPrevalence) {
				prevalenceYears.add(Integer.valueOf(year));
			}
		}
	}

	/**
	 * 
	 * @param userProfile
	 * @return string representation of the HIF configuration
	 */
	public String toString(Optional<UserProfile> userProfile, Integer count) {
		StringBuilder b = new StringBuilder();
		b.append("HIF Configuration (").append(count).append("):\n");
		b.append("HIF instance id: ").append(hifInstanceId).append("\n");
		b.append("Incidence year: ").append(incidenceYear).append("\n");
		b.append("Prevalance years: ").append(Arrays.toString(prevalenceYears.toArray())).append("\n");
		return b.toString();
	}
}

class HifGroup {
	public Integer id = null;
	public String name = null;
	public List<HIFConfig> hifs = new ArrayList<HIFConfig>();

	/**
	 * Creates an object from the task parameter object
	 * @param hifGroup
	 */
	public HifGroup(JsonNode hifGroup) {
		id = hifGroup.has("id") ? hifGroup.get("id").asInt() : null;
		name = hifGroup.has("name") ? hifGroup.get("name").asText() : null;
		JsonNode hifFunctions = hifGroup.has("hif_functions") ? hifGroup.get("hif_functions") : null;
		if(hifFunctions != null) {
			for(JsonNode function : hifFunctions) {
				hifs.add(new HIFConfig(function));
			}
		}
	}

	/**
	 * 
	 * @param userProfile
	 * @return string representation of the HIF group
	 */
	public String toString(Optional<UserProfile> userProfile) {
		StringBuilder b = new StringBuilder();
		b.append("Health impact group name: ").append(name).append("\n");
		b.append("Functions Selected: ").append(hifs.size()).append("\n\n");
		b.append("Health impact functions:\n");
		//hifs are sorted by endpoint group and endpoint by default
		for(int i=0; i < hifs.size(); i++) {
			HIFConfig hif = hifs.get(i);
			b.append("Function ").append(i+1).append(":\n");
			b.append(hif.toString()).append("\n");
		}
		b.append("\n");
		return b.toString();
	}
}

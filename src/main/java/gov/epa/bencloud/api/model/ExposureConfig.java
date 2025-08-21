package gov.epa.bencloud.api.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import gov.epa.bencloud.api.IncidenceApi;
import gov.epa.bencloud.api.util.ApiUtil;
import gov.epa.bencloud.api.util.JSONUtil;

/**
 * @author jimanderton
 * The ExposureConfig serves two purposes:
 * 1. It is provided to the ExposureTaskRunnable as part of the ExposureTaskConfig and is used to define the analysis run.
 * 2. It is serialized as part of the ExposureTaskLog and stored in the exposure_result_dataset table as a record of the analysis run.
 *    This is then used to generate "audit trail" reports.
 */
public class ExposureConfig {
	// Many of these fields may appear to duplicate those in the hifRecord.
	// They are here because they may be overridden by the user as part configuring the analysis.
	// This values within the hifRecord only serve as a default
	
	public Integer efInstanceId = null;
	public Integer efId = null;
	public Integer startAge = null;
	public Integer endAge = null;
	public Integer race = null;
	public Integer ethnicity = null;
	public Integer gender = null;
	public Integer metric = null;
	public Integer seasonalMetric = null; //TODO: remove once fully replaced by Timing
	public Integer metricStatistic = null; //TODO: remove once fully replaced by Timing

	public Integer timing = null;
		
	public Integer variable = null;
	public Integer startDay = null;
	public Integer endDay = null;
	public Integer totalDays = null;
	public int arrayIdx = 0;
	
	public Map<String, Object> efRecord = new HashMap<String, Object> (); //This map will contain the full Exposure function record from the db
	
	/**
	 * Creates an object from the task parameter object
	 * @param function
	 */
	public ExposureConfig(JsonNode function) {
		this.efId = JSONUtil.getInteger(function, "id");
		this.startAge = JSONUtil.getInteger(function, "start_age");
		this.endAge = JSONUtil.getInteger(function, "end_age");
		this.race = JSONUtil.getInteger(function, "race_id");
		this.ethnicity = JSONUtil.getInteger(function, "ethnicity_id");
		this.gender = JSONUtil.getInteger(function, "gender_id");
				
		this.variable = JSONUtil.getInteger(function, "variable");
	}
	
	/**
	 * Default constructor
	 */
	public ExposureConfig() {
		
	}

	/*
	 * Returns a string representation of the EFConfig object 
	 * 
	 */
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("Unique ID: ").append(efRecord.get("id")).append("\n");
		b.append("Population Group: ").append(efRecord.get("population_group")).append("\n");
		
		b.append("Start Age: ").append(startAge).append("\n");
		b.append("End Age: ").append(endAge).append("\n");
		
		b.append("Race: ").append(ApiUtil.getRaceNameLookup().getOrDefault(race, "")).append("\n");
		b.append("Ethnicity: ").append(ApiUtil.getEthnicityNameLookup().getOrDefault(ethnicity, "")).append("\n");
		b.append("Gender: ").append(ApiUtil.getGenderNameLookup().getOrDefault(gender, "")).append("\n");
		
		b.append("Pollutant: ").append(efRecord.getOrDefault("pollutant_friendly_name", efRecord.getOrDefault("pollutant_name", "Null"))).append("\n");
		b.append("Metric: ").append(efRecord.getOrDefault("metric_name", "")).append("\n");
		b.append("Seasonal Metric: ").append(efRecord.get("seasonal_metric_name") == null ? "Null" : efRecord.get("seasonal_metric_name")).append("\n"); 
		b.append("Metric Statistic: ").append(efRecord.getOrDefault("metric_statistic_name", "")).append("\n");
		
		b.append("Function: ").append(efRecord.get("function_text")).append("\n");

		b.append("Variable: ").append(ApiUtil.getVariableName( (Integer)efRecord.get("variable") ) ).append("\n");

		return b.toString();
	}
	

	/*
	 * ExposureConfig comparator
	 * Compares (lexicographically) two ExposureConfigs by population group name
	 * Returns:
	 * 		0 - if h1 = h2
	 * 		a negative value - if h1 < h2
	 * 		a positive value - if h1 > h2
	 */
	public static Comparator<ExposureConfig> ExposureConfigPopulationGroupComparator = new Comparator<ExposureConfig>() {
		public int compare(ExposureConfig e1, ExposureConfig e2) {
			//If we don't have both records, just bail as if they're equal. This shouldn't happen in this case.
			if(e1.efRecord == null || e2.efRecord == null) {
				return 0;
			}
			
			String popGroup1 = (e1.efRecord.get("population_group") == null ? "" : e1.efRecord.get("population_group").toString());

			String popGroup2 = (e2.efRecord.get("population_group") == null ? "" : e2.efRecord.get("population_group").toString());

			return popGroup1.compareTo(popGroup2);
		}
	};

}

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
 * The HIFConfig serves two purposes:
 * 1. It is provided to the HIFTaskRunnable as part of the HIFTaskConfig and is used to define the analysis run.
 * 2. It is serialized as part of the HIFTaskLog and stored in the hif_result_dataset table as a record of the analysis run.
 *    This is then used to generate "audit trail" reports.
 */
public class HIFConfig {
	// Many of these fields may appear to duplicate those in the hifRecord.
	// They are here because they may be overridden by the user as part configuring the analysis.
	// This values within the hifRecord only serve as a default
	
	public Integer hifInstanceId = null;
	public Integer hifId = null;
	public Integer startAge = null;
	public Integer endAge = null;
	public Integer race = null;
	public Integer ethnicity = null;
	public Integer gender = null;
	public Integer metric = null;
	public Integer seasonalMetric = null;
	public Integer metricStatistic = null;
	public Integer timing = null;
	
	//incidence and prevlance may use different race, ethnicity, and/or gender than 
	//the functions configuration due to data availability
	public Integer incidence = null;
	public String incidenceName = null;
	public Integer incidenceYear = null;
	public Integer incidenceRace = null;
	public Integer incidenceEthnicity = null;
	public Integer incidenceGender = null;
	
	public Integer prevalence = null;
	public String prevalenceName = null;
	public Integer prevalenceYear = null;
	public Integer prevalenceRace = null;
	public Integer prevalenceEthnicity = null;
	public Integer prevalenceGender = null;	
	
	public Integer variable = null;
	public Integer startDay = null;
	public Integer endDay = null;
	public Integer totalDays = null;
	public int arrayIdx = 0;

	public Integer heroId = null;
	public String epaHeroUrl = null;
	public String accessUrl = null;
	
	public Map<String, Object> hifRecord = new HashMap<String, Object> (); //This map will contain the full HIF record from the db
	
	public List<ValuationConfig> valuationFunctions = new ArrayList<ValuationConfig>();	
	/**
	 * Creates an object from the task parameter object
	 * @param function
	 */
	public HIFConfig(JsonNode function) {
		this.hifId = JSONUtil.getInteger(function, "id");
		this.startAge = JSONUtil.getInteger(function, "start_age");
		this.endAge = JSONUtil.getInteger(function, "end_age");
		this.race = JSONUtil.getInteger(function, "race_id");
		this.ethnicity = JSONUtil.getInteger(function, "ethnicity_id");
		this.gender = JSONUtil.getInteger(function, "gender_id");
		
		this.incidence = JSONUtil.getInteger(function, "incidence_dataset_id");
		this.incidenceYear = JSONUtil.getInteger(function, "incidence_year");
		this.incidenceRace = JSONUtil.getInteger(function, "incidence_race");
		this.incidenceEthnicity = JSONUtil.getInteger(function, "incidence_ethnicity");
		this.incidenceGender = JSONUtil.getInteger(function, "incidence_gender");
		
		this.prevalence = JSONUtil.getInteger(function, "prevalence_dataset_id");
		this.prevalenceYear = JSONUtil.getInteger(function, "prevalence_year");
		this.prevalenceRace = JSONUtil.getInteger(function, "prevalence_race");
		this.prevalenceEthnicity = JSONUtil.getInteger(function, "prevalence_ethnicity");
		this.prevalenceGender = JSONUtil.getInteger(function, "prevalence_gender");
		
		this.variable = JSONUtil.getInteger(function, "variable");		
		
		//TODO: Add code to allow user to specify metric, seasonal metric, and metric statistic?
		//this.timing = JSONUtil.getInteger(function, "timing_id"); //not in use for now
	}
	
	/**
	 * Default constructor
	 */
	public HIFConfig() {
		
	}

	/*
	 * Returns a string representation of the HIFConfig object 
	 * 
	 */
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("Unique ID: ").append(hifRecord.get("id")).append("\n");
		b.append("Health Effect Group: ").append(hifRecord.get("endpoint_group_name")).append("\n");
		b.append("Health Effect: ").append(hifRecord.get("endpoint_name")).append("\n");		
		b.append("Author: ").append(hifRecord.getOrDefault("author", "")).append("\n");
		b.append("Year: ").append(hifRecord.getOrDefault("function_year", "")).append("\n");
		
		b.append("Start Age: ").append(startAge).append("\n");
		b.append("End Age: ").append(endAge).append("\n");
		
		b.append("Race: ").append(ApiUtil.getRaceNameLookup().getOrDefault(race, "")).append("\n");
		b.append("Ethnicity: ").append(ApiUtil.getEthnicityNameLookup().getOrDefault(ethnicity, "")).append("\n");
		b.append("Gender: ").append(ApiUtil.getGenderNameLookup().getOrDefault(gender, "")).append("\n");
		
		b.append("Pollutant: ").append(hifRecord.getOrDefault("pollutant_friendly_name", hifRecord.getOrDefault("pollutant_name", "Null"))).append("\n");
		b.append("Metric: ").append(hifRecord.getOrDefault("metric_name", "")).append("\n");
		String tmpTiming = hifRecord.getOrDefault("timing_name", "").toString();
		if(tmpTiming.isEmpty()){
			b.append("Seasonal Metric: ").append(hifRecord.get("seasonal_metric_name") == null ? "Null" : hifRecord.get("seasonal_metric_name")).append("\n"); 
			b.append("Metric Statistic: ").append(hifRecord.getOrDefault("metric_statistic_name", "")).append("\n");//keep for backward compatibility
		}
		else{
			b.append("Timing: ").append(hifRecord.getOrDefault("timing_name", "")).append("\n");
		}		
		
		if(hifRecord.get("other_pollutants") != null) {
			b.append("Other Pollutants: ").append(hifRecord.get("other_pollutants")).append("\n");
		}
		if(hifRecord.get("qualifier") != null) {
			b.append("Qualifier: ").append(hifRecord.get("qualifier")).append("\n");
		}
		if(hifRecord.get("reference") != null) {
			b.append("Reference: ").append(hifRecord.get("reference")).append("\n");
		}		

		b.append("Function: ").append(hifRecord.get("function_text")).append("\n");
		b.append("Baseline Function: ").append(hifRecord.get("baseline_function_text")).append("\n");
		
		if(incidence != null && incidence != 0) {
			b.append("Incidence Dataset: ").append(IncidenceApi.getIncidenceDatasetName(incidence)).append(" (").append(incidenceYear).append(")\n");
			b.append("Incidence Race: ").append(ApiUtil.getRaceNameLookup().getOrDefault(incidenceRace, "")).append("\n");
			b.append("Incidence Ethnicity: ").append(ApiUtil.getEthnicityNameLookup().getOrDefault(incidenceEthnicity, "")).append("\n");
			b.append("Incidence Gender: ").append(ApiUtil.getGenderNameLookup().getOrDefault(incidenceGender, "")).append("\n");
		}
		if(prevalence != null && prevalence != 0) {
			b.append("Prevalence Dataset: ").append(IncidenceApi.getIncidenceDatasetName(prevalence)).append(" (").append(prevalenceYear).append(")\n");
			b.append("Prevalence Race: ").append(ApiUtil.getRaceNameLookup().getOrDefault(prevalenceRace, "")).append("\n");
			b.append("Prevalence Ethnicity: ").append(ApiUtil.getEthnicityNameLookup().getOrDefault(prevalenceEthnicity, "")).append("\n");
			b.append("Prevalence Gender: ").append(ApiUtil.getGenderNameLookup().getOrDefault(prevalenceGender, "")).append("\n");
		}
		
		b.append("Beta: ").append(hifRecord.get("beta")).append("\n");
		b.append("Beta Distribution: ").append(hifRecord.get("dist_beta")).append("\n");
		b.append("Standard Error: ").append(hifRecord.get("p1_beta")).append("\n");
		if(hifRecord.get("p2_beta") != null && (Double)hifRecord.get("p2_beta") != 0) {
			b.append("P2Beta: ").append(hifRecord.get("p2_beta")).append("\n");
		}
		
		if(hifRecord.get("name_a") != null  || hifRecord.get("name_c") != null || hifRecord.get("name_c") != null) {
			b.append("Variables\n");
			if(hifRecord.get("name_a") != null) {
				b.append("Name A: ").append(hifRecord.get("name_a")).append("\n")
				.append("Value A: ").append(hifRecord.getOrDefault("val_a", "")).append("\n");
			}
			if(hifRecord.get("name_b") != null) {
				b.append("Name B: ").append(hifRecord.get("name_b")).append("\n")
				.append("Value B: ").append(hifRecord.getOrDefault("val_b", "")).append("\n");
			}
			if(hifRecord.get("name_c") != null) {
				b.append("Name C: ").append(hifRecord.get("name_c")).append("\n")
				.append("Value C: ").append(hifRecord.getOrDefault("val_c", "")).append("\n");
			}
		}

		if(hifRecord.get("hero_id") != null) {
			b.append("HERO ID: ").append(hifRecord.get("hero_id")).append("\n")
			.append("Access Url: ").append(hifRecord.getOrDefault("access_url", "")).append("\n");
		}
		
		return b.toString();
	}
	

	/*
	 * HIFConfig comparator
	 * Compares (lexicographically) two HIFConfigs by endpoint group name, then endpoint name
	 * Returns:
	 * 		0 - if h1 = h2
	 * 		a negative value - if h1 < h2
	 * 		a positive value - if h1 > h2
	 */
	public static Comparator<HIFConfig> HifConfigEndpointGroupComparator = new Comparator<HIFConfig>() {
		public int compare(HIFConfig h1, HIFConfig h2) {
			//If we don't have both records, just bail as if they're equal. This shouldn't happen in this case.
			if(h1.hifRecord == null || h2.hifRecord == null) {
				return 0;
			}
			
			String groupAndEndpoint1 = (h1.hifRecord.get("endpoint_group_name") == null ? "" : h1.hifRecord.get("endpoint_group_name").toString()) + "|" +
					(h1.hifRecord.get("endpoint_name") == null ? "" : h1.hifRecord.get("endpoint_name").toString());
			
			String groupAndEndpoint2 = (h2.hifRecord.get("endpoint_group_name") == null ? "" : h2.hifRecord.get("endpoint_group_name").toString()) + "|" +
					(h2.hifRecord.get("endpoint_name") == null ? "" : h2.hifRecord.get("endpoint_name").toString());
			
			return groupAndEndpoint1.compareTo(groupAndEndpoint2);
		}
	};

}

package gov.epa.bencloud.api.model;

import java.util.Comparator;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import gov.epa.bencloud.api.IncidenceApi;
import gov.epa.bencloud.api.util.ApiUtil;

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
	
	public Integer hifId = null;
	public Integer startAge = null;
	public Integer endAge = null;
	public Integer race = null;
	public Integer ethnicity = null;
	public Integer gender = null;
	public Integer metric = null;
	public Integer seasonalMetric = null;
	public Integer metricStatistic = null;
	public Integer incidence = null;
	public Integer incidenceYear = null;
	public Integer prevalence = null;
	public Integer prevalenceYear = null;
	public Integer variable = null;
	public Integer startDay = null;
	public Integer endDay = null;
	public Integer totalDays = null;
	public int arrayIdx = 0;
	
	public Map<String, Object> hifRecord = null; //This map will contain the full HIF record
	
	/**
	 * Creates an object from the task parameter object
	 * @param function
	 */
	public HIFConfig(JsonNode function) {
		this.hifId = function.get("id").asInt();
		this.startAge = function.has("start_age") ? function.get("start_age").asInt() : null;
		this.endAge = function.has("end_age") ? function.get("end_age").asInt() : null;
		this.race = function.has("race_id") ? function.get("race_id").asInt() : null;
		this.ethnicity = function.has("ethnicity_id") ? function.get("ethnicity_id").asInt() : null;
		this.gender = function.has("gender_id") ? function.get("gender_id").asInt() : null;
		this.incidence = function.has("incidence_dataset_id") ? function.get("incidence_dataset_id").asInt() : null;
		this.incidenceYear = function.has("incidence_year") ? function.get("incidence_year").asInt() : null;
		this.prevalence = function.has("prevalence_dataset_id") ? function.get("prevalence_dataset_id").asInt() : null;
		this.prevalenceYear = function.has("prevalence_year") ? function.get("prevalence_year").asInt() : null;
		this.variable = function.has("variable") ? function.get("variable").asInt() : null;
		//TODO: Add code to allow user to specify metric, seasonal metric, and metric statistic?
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
		b.append("Seasonal Metric: ").append(hifRecord.get("seasonal_metric_name") == null ? "Null" : hifRecord.get("seasonal_metric_name")).append("\n"); 
		b.append("Metric Statistic: ").append(hifRecord.getOrDefault("metric_statistic_name", "")).append("\n");
		
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
		}
		if(prevalence != null && prevalence != 0) {
			b.append("Prevalence Dataset: ").append(IncidenceApi.getIncidenceDatasetName(prevalence)).append(" (").append(prevalenceYear).append(")\n");
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

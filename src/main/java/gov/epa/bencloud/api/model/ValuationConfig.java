package gov.epa.bencloud.api.model;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

/*
 * Representation of a valuation configuration
 */

public class ValuationConfig {
	public Integer hifId = null;
	public Integer hifInstanceId = null;
	public Integer vfId = null;
	
	public Map<String, Object> vfRecord = null; //This map will contain the full valuation record
	
	/*
	 * Creates the object from the task parameter object
	 */
	public ValuationConfig(JsonNode function) {
		this.hifId = function.get("hif_id").asInt();
		this.hifInstanceId = function.get("hif_isntance_id").asInt();
		this.vfId = function.get("vf_id").asInt();
	}
	
	/*
	 * Default constructor
	 */
	public ValuationConfig() {
		
	}

	/*
	 * Returns a string representation of a valuation configuration
	 */
	@Override
	public String toString() {
		//TODO: Add error handling for partially populated object
		
		StringBuilder b = new StringBuilder();

		if(vfRecord.get("qualifier") != null) {
			b.append("Qualifier: ").append(vfRecord.get("qualifier")).append("\n");
		}
		b.append("Health Effect Group: ").append(vfRecord.get("endpoint_group_name")).append("\n");
		b.append("Health Effect: ").append(vfRecord.get("endpoint_name")).append("\n");		
		b.append("Start Age: ").append(vfRecord.get("start_age")).append("\n");
		b.append("End Age: ").append(vfRecord.get("end_age")).append("\n");
		b.append("Function: ").append(vfRecord.get("function_text")).append("\n");		

		if(vfRecord.get("reference") != null) {
			b.append("Reference: ").append(vfRecord.get("reference")).append("\n");
		}	
		
		if(vfRecord.get("name_a") != null  || vfRecord.get("name_c") != null || vfRecord.get("name_c") != null || vfRecord.get("name_d") != null) {
			b.append("Variables\n");
			if(vfRecord.get("name_a") != null) {
				b.append("Name A: ").append(vfRecord.get("name_a")).append("\n")
				.append("Value A: ").append(vfRecord.getOrDefault("val_a", "")).append("\n")
				.append("Distribution A: ").append(vfRecord.get("dist_a")).append("\n")
				.append("P1A: ").append(vfRecord.get("p1a")).append("\n")
				.append("P2A: ").append(vfRecord.get("p2a")).append("\n");
			}
			if(vfRecord.get("name_b") != null) {
				b.append("Name B: ").append(vfRecord.get("name_b")).append("\n")
				.append("Value B: ").append(vfRecord.getOrDefault("val_b", "")).append("\n");
			}
			if(vfRecord.get("name_c") != null) {
				b.append("Name C: ").append(vfRecord.get("name_c")).append("\n")
				.append("Value C: ").append(vfRecord.getOrDefault("val_c", "")).append("\n");
			}
			if(vfRecord.get("name_d") != null) {
				b.append("Name D: ").append(vfRecord.get("name_d")).append("\n")
				.append("Value D: ").append(vfRecord.getOrDefault("val_d", "")).append("\n");
			}
		}
		
		return b.toString();
	}
}

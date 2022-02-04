package gov.epa.bencloud.api.model;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

public class ValuationConfig {
	public Integer hifId = null;
	public Integer vfId = null;
	
	public Map<String, Object> vfRecord = null; //This map will contain the full valuation record
	
	/*
	 * Creates the object from the task parameter object
	 */
	public ValuationConfig(JsonNode function) {
		this.hifId = function.get("hif_id").asInt();
		this.vfId = function.get("vf_id").asInt();
	}
	
	public ValuationConfig() {
		
	}

	@Override
	public String toString() {
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

		/*
<Valuation.Function.#2.(VSL.based.on.26.value-of-life.studies.with.Cessation.Lag.3%.d.r.)>
Dataset: EPA Standard Valuation Functions (2021)
Endpoint group: Mortality
Endpoint: Mortality, All Cause
Start Age: 0
End age: 99
Reference: 0
Function: A*AllGoodsIndex*B
Name A: mean VSL in 2015$
A: 8705114.255
Dist A: Weibull
P1A: 9648168.299
P2A: 1.509588003
Name B: 3% d.r. Cessation Lag
B: 0.90605998
Name C: 0
C: 0
Name D: 0
D: 0
</Valuation.Function.#2.(VSL.based.on.26.value-of-life.studies.with.Cessation.Lag.3%.d.r.)>
		 */
		
		return b.toString();
	}
}

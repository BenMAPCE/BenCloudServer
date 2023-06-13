package gov.epa.bencloud.api.model;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;
import org.pac4j.core.profile.UserProfile;

public class ScenarioHIFConfig {
    public Integer hifInstanceId = null;
    public Integer incidenceYear = null;
    public Integer prevalenceYear = null;


	/**
	 * Default constructor
	 */
	public ScenarioHIFConfig() {

	}

	/**
	 * Creates an object from the scenario hif config parameter object
	 * @param scenarioHifConfig
	 */
	public ScenarioHIFConfig(JsonNode scenarioHifConfig) {
		hifInstanceId = scenarioHifConfig.has("hif_instance_id") ? scenarioHifConfig.get("hif_instance_id").asInt() : null;
		incidenceYear = scenarioHifConfig.has("incidence_year") ? scenarioHifConfig.get("incidence_year").asInt() : null;
		prevalenceYear = scenarioHifConfig.has("prevalence_year") ? scenarioHifConfig.get("prevalence_year").asInt() : null;
	}

	/**
	 * 
	 * @param userProfile
	 * @return string representation of the scenario HIF configuration
	 */
	public String toString(Optional<UserProfile> userProfile, Integer count) {
		StringBuilder b = new StringBuilder();
		b.append("Scenario HIF Configuration:\n");
		b.append("HIF instance id: ").append(hifInstanceId).append("\n");
		b.append("Incidence year: ").append(incidenceYear).append("\n");
		b.append("Prevalance year: ").append(prevalenceYear).append("\n");
		return b.toString();
	}
}

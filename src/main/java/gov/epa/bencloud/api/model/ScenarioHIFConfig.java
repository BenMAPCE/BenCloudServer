package gov.epa.bencloud.api.model;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;
import org.pac4j.core.profile.UserProfile;

import gov.epa.bencloud.api.util.JSONUtil;
import io.kubernetes.client.openapi.JSON;

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
		hifInstanceId = JSONUtil.getInteger(scenarioHifConfig, "hif_instance_id");
		incidenceYear = JSONUtil.getInteger(scenarioHifConfig, "incidence_year");
		prevalenceYear = JSONUtil.getInteger(scenarioHifConfig, "prevalence_year");
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

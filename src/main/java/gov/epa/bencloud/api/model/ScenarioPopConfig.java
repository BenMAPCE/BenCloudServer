package gov.epa.bencloud.api.model;

import java.util.List;
import java.util.ArrayList;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;
import org.pac4j.core.profile.UserProfile;


public class ScenarioPopConfig {
    public Integer popYear = null;
    public List<ScenarioHIFConfig> scenarioHifConfigs = new ArrayList<ScenarioHIFConfig>();

	/**
	 * Creates an object from the scenario pop config parameter object
	 * @param scenarioPopConfig
	 */
	public ScenarioPopConfig(JsonNode scenarioPopConfig) {
		popYear = scenarioPopConfig.has("population_year") ? scenarioPopConfig.get("population_year").asInt() : null;
		JsonNode scenarioHifConfigsJson = scenarioPopConfig.has("hif_configs") ? scenarioPopConfig.get("hif_configs") : null;
		if(scenarioHifConfigsJson != null) {
			for(JsonNode scenarioHifConfig : scenarioHifConfigsJson) {
				scenarioHifConfigs.add(new ScenarioHIFConfig(scenarioHifConfig));
			}
		}
	}

	/**
	 * 
	 * @param userProfile
	 * @return string representation of the scenario population configuration
	 */
	public String toString(Optional<UserProfile> userProfile, Integer count) {
		StringBuilder b = new StringBuilder();
		b.append("Population Configuration (").append(count).append("):\n");
		b.append("Population year: ").append(popYear).append("\n");
		b.append("Scenario HIF Configurations:\n");
		for(int i = 0; i < scenarioHifConfigs.size(); i++) {
			b.append(scenarioHifConfigs.get(i).toString(userProfile, i+1)).append("\n");
		}
		return b.toString();
	}
}

package gov.epa.bencloud.api.model;

import java.util.List;
import java.util.ArrayList;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;
import org.pac4j.core.profile.UserProfile;

public class Scenario {
    public Integer id = 0;
    public String name;
    public List<ScenarioPopConfig> popConfigs = new ArrayList<ScenarioPopConfig>();

    /**
     * Creates an object from the task parameter object
     * @param scenario
     */
    public Scenario(JsonNode scenario) {
        id = scenario.has("post_policy_aq_id") ? scenario.get("post_policy_aq_id").asInt() : null;
        name = scenario.has("post_policy_aq") ? scenario.get("post_policy_aq").asText() : null;
        JsonNode scenarioPopConfigsJson = scenario.has("population_configs") ? scenario.get("population_configs") : null;
        if(scenarioPopConfigsJson != null) {
            for(JsonNode scenarioPopConfig : scenarioPopConfigsJson) {
                popConfigs.add(new ScenarioPopConfig(scenarioPopConfig));
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

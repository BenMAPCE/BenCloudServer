package gov.epa.bencloud.api.model;

import java.util.List;
import java.util.ArrayList;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;
import org.pac4j.core.profile.UserProfile;

public class BatchExposureGroup {
    public Integer id = null;
	public String name = null;
	public List<ExposureConfig> exposureConfigs = new ArrayList<ExposureConfig>();

	/**
	 * Default constructor
	 */
	public BatchExposureGroup() {

	}

	/**
	 * Creates an object from the exposure group parameter object
	 * @param batchExposureGroup
	 */
	public BatchExposureGroup(JsonNode batchExposureGroup) {
		id = batchExposureGroup.has("id") ? batchExposureGroup.get("id").asInt() : null;
		name = batchExposureGroup.has("name") ? batchExposureGroup.get("name").asText() : null;
		JsonNode exposureFunctions = batchExposureGroup.has("exposure_functions") ? batchExposureGroup.get("exposure_functions") : null;
		if(exposureFunctions != null) {
			for(JsonNode function : exposureFunctions) {
				exposureConfigs.add(new ExposureConfig(function));
			}
		}
	}

	/**
	 * 
	 * @param userProfile
	 * @return string representation of the batch Exposure group
	 */
	public String toString(Optional<UserProfile> userProfile) {
		StringBuilder b = new StringBuilder();
		b.append("Exposure group name: ").append(name).append("\n");
		b.append("Functions Selected: ").append(exposureConfigs.size()).append("\n\n");
		b.append("Exposure functions:\n");
		//exposure functions are sorted by population group by default
		for(int i=0; i < exposureConfigs.size(); i++) {
			ExposureConfig exposureConfig = exposureConfigs.get(i);
			b.append("Function ").append(i+1).append(":\n");
			b.append(exposureConfig.toString()).append("\n");
		}
		b.append("\n");
		return b.toString();
	}
}

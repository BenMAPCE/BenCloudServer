package gov.epa.bencloud.api.model;

import java.util.List;
import java.util.ArrayList;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;
import org.pac4j.core.profile.UserProfile;

public class BatchHIFGroup {
    public Integer id = null;
	public String name = null;
	public List<HIFConfig> hifs = new ArrayList<HIFConfig>();

	/**
	 * Creates an object from the HIF group parameter object
	 * @param batchHifGroup
	 */
	public BatchHIFGroup(JsonNode batchHifGroup) {
		id = batchHifGroup.has("id") ? batchHifGroup.get("id").asInt() : null;
		name = batchHifGroup.has("name") ? batchHifGroup.get("name").asText() : null;
		JsonNode hifFunctions = batchHifGroup.has("hif_functions") ? batchHifGroup.get("hif_functions") : null;
		if(hifFunctions != null) {
			for(JsonNode function : hifFunctions) {
				hifs.add(new HIFConfig(function));
			}
		}
	}

	/**
	 * 
	 * @param userProfile
	 * @return string representation of the batch HIF group
	 */
	public String toString(Optional<UserProfile> userProfile) {
		StringBuilder b = new StringBuilder();
		b.append("Health impact group name: ").append(name).append("\n");
		b.append("Functions Selected: ").append(hifs.size()).append("\n\n");
		b.append("Health impact functions:\n");
		//hifs are sorted by endpoint group and endpoint by default
		for(int i=0; i < hifs.size(); i++) {
			HIFConfig hif = hifs.get(i);
			b.append("Function ").append(i+1).append(":\n");
			b.append(hif.toString()).append("\n");
		}
		b.append("\n");
		return b.toString();
	}
}

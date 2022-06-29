package gov.epa.bencloud.server.routes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.jee.context.session.JEESessionStore;
import org.pac4j.sparkjava.SparkWebContext;

import gov.epa.bencloud.Constants;
import gov.epa.bencloud.server.util.ApplicationUtil;
import spark.Request;
import spark.Response;

public class RoutesBase {

	protected Optional<UserProfile> getUserProfile(Request request, Response response) {
			final SparkWebContext context = new SparkWebContext(request, response);
			final ProfileManager manager = new ProfileManager(context, JEESessionStore.INSTANCE);
			return manager.getProfile();

			// UserProfile userProfile = new CommonProfile();
			// userProfile.setId("TESTING");
			// return Optional.of(userProfile);

	}

	protected List<String> getPostParametersNames(Request req) {
	
		Map<String, String[]> params = req.raw().getParameterMap();
		
		List<String> names = new ArrayList<String>();
		
		for (Map.Entry<String, String[]> entry : params.entrySet()) {
			names.add(entry.getKey());
		}
		
		return names;
	}

	protected String getPostParameterValue(Request req, String name) {
		
		String value = null;
		
		Map<String, String[]> params = req.raw().getParameterMap();
		
		for (Map.Entry<String, String[]> entry : params.entrySet()) {
			if (entry.getKey().equals(name)) {
				value = entry.getValue()[0];
			}
		}
		
		return value;
	}

	protected String[] getPostParameterValues(Request req, String name) {
		
		String[] values = null;
		
		Map<String, String[]> params = req.raw().getParameterMap();
		
		for (Map.Entry<String, String[]> entry : params.entrySet()) {
			if (entry.getKey().equals(name)) {
				values = entry.getValue();
			}
		}
		
		return values;
	}

	protected Map<String, String[]> getPostParametersAsMap(Request req) {

		return req.raw().getParameterMap();
	}
}

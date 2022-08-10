package gov.epa.bencloud.server;

import org.apache.commons.lang3.StringUtils;
import org.pac4j.core.authorization.authorizer.ProfileAuthorizer;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.profile.UserProfile;

import gov.epa.bencloud.api.CoreApi;

import java.util.List;
import java.util.Optional;

public class BenCloudAuthorizer extends ProfileAuthorizer {

    @Override
    public boolean isAuthorized(final WebContext context, final SessionStore sessionStore, final List<UserProfile> profiles) {
    	if(profiles.isEmpty()) {
    		return false;
    	}
    	
    	UserProfile profile = profiles.get(0);
    	if(CoreApi.isUser(Optional.of(profile)) || CoreApi.isAdmin(Optional.of(profile))  ) {
    		return true;
    	}
    	
    	//If they are not in the BenMAP_Users or BenMAP_Admins group, we will only allow access to the /user endpoint.
    	String path = context.getPath();
    	if(path.endsWith("/user")) {
    		return true;
    	}
    	
        return false;
    }

    @Override
    public boolean isProfileAuthorized(final WebContext context, final SessionStore sessionStore, final UserProfile profile) {
    	return CoreApi.isAdmin(Optional.of(profile));

    }
}
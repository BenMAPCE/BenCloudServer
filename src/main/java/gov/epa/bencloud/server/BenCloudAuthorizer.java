package gov.epa.bencloud.server;

import org.apache.commons.lang3.StringUtils;
import org.pac4j.core.authorization.authorizer.ProfileAuthorizer;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.profile.UserProfile;

import java.util.List;

public class BenCloudAuthorizer extends ProfileAuthorizer {

    @Override
    public boolean isAuthorized(final WebContext context, final SessionStore sessionStore, final List<UserProfile> profiles) {
        return true; //isAnyAuthorized(context, sessionStore, profiles);
    }

    @Override
    public boolean isProfileAuthorized(final WebContext context, final SessionStore sessionStore, final UserProfile profile) {
        return true;
        /* 
        if (profile == null) {
            return false;
        }
        return StringUtils.startsWith(profile.getUsername(), "jle");
        */
    }
}
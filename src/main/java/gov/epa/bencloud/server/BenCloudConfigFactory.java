package gov.epa.bencloud.server;

import java.util.Optional;

import org.pac4j.core.authorization.authorizer.RequireAnyRoleAuthorizer;
import org.pac4j.core.authorization.generator.AuthorizationGenerator;
import org.pac4j.core.client.Clients;
import org.pac4j.core.config.Config;
import org.pac4j.core.config.ConfigFactory;
import org.pac4j.core.credentials.TokenCredentials;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.util.CommonHelper;
import org.pac4j.http.client.direct.HeaderClient;

import gov.epa.bencloud.Constants;

import org.pac4j.core.matching.checker.DefaultMatchingChecker;
import org.pac4j.core.matching.matcher.HttpMethodMatcher;

public class BenCloudConfigFactory implements ConfigFactory {

    public BenCloudConfigFactory() {

    }

    @Override
    public Config build(final Object... parameters) {
        //x-wam-id is a temporary header being added manually during implementation to stub out the auth code
        final HeaderClient headerClient = new HeaderClient(Constants.HEADER_USER_ID, (credentials, ctx, sessionStore) -> {           
            final String token = ((TokenCredentials) credentials).getToken();
            if (CommonHelper.isNotBlank(token)) {
                final CommonProfile profile = new CommonProfile();
                profile.setId(token);

                //This allows fake roles for now. Waiting on WAM implementation.
                Optional<String> roleHeader = ctx.getRequestHeader(Constants.HEADER_GROUPS);
                String[] roles = null;
                if(roleHeader.isPresent()) {
                    roles = roleHeader.get().split(";");
                    for (String role : roles) {
                        if(!role.equalsIgnoreCase("null")) {
                            profile.addRole(role);
                        }
                    }
                }
                String tmp = ctx.getRequestHeader(Constants.HEADER_DISPLAY_NAME).get();
                profile.addAttribute(Constants.HEADER_DISPLAY_NAME, tmp==null || tmp.equalsIgnoreCase("NOT_FOUND") ? "" : tmp);

                tmp = ctx.getRequestHeader(Constants.HEADER_MAIL).get();
                profile.addAttribute(Constants.HEADER_MAIL, tmp==null || tmp.equalsIgnoreCase("NOT_FOUND") ? "" : tmp);
                credentials.setUserProfile(profile);
            }
        });

        AuthorizationGenerator authGen = (ctx, session, profile) -> {
            // String roles = (String) profile.getAttribute("roles");
            // if(roles != null) {
            //     for (String role: roles.split(",")) {
            //     profile.addRole(role);
            //     }
            // }
            return Optional.of(profile);
          };
        
        headerClient.addAuthorizationGenerator(authGen);
        final Clients clients = new Clients(headerClient);
        final Config config = new Config(clients);
        //config.addAuthorizer("admin", new RequireAnyRoleAuthorizer("ROLE_ADMIN"));
        config.addAuthorizer("custom", new BenCloudAuthorizer());
        return config;
    }
}

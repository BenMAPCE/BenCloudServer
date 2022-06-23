package gov.epa.bencloud.server;

import java.io.File;
import java.io.IOException;

import org.pac4j.core.authorization.authorizer.RequireAnyRoleAuthorizer;
import org.pac4j.core.config.Config;
import org.pac4j.sparkjava.SecurityFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import freemarker.template.Configuration;
import gov.epa.bencloud.Constants;
import gov.epa.bencloud.api.util.ApiUtil;
import gov.epa.bencloud.server.jobs.JobsUtil;
import gov.epa.bencloud.server.routes.AdminRoutes;
import gov.epa.bencloud.server.routes.ApiRoutes;
import gov.epa.bencloud.server.routes.PublicRoutes;
import gov.epa.bencloud.server.tasks.TaskWorker;
import gov.epa.bencloud.server.util.ApplicationUtil;
import gov.epa.bencloud.server.util.FreeMarkerRenderUtil;
import spark.Service;
import spark.Spark;

public class BenCloudServer {
	
	private static final Logger log = LoggerFactory.getLogger(BenCloudServer.class);
	private static String applicationPath;
	
	public static void main(String[] args) {

		String javaVersion = System.getProperty("java.version");

		try {
			ApplicationUtil.loadProperties("bencloud-server.properties");
			ApplicationUtil.loadProperties("bencloud-local.properties", true);
		} catch (IOException e) {
			log.error("Unable to load application properties", e);
			System.exit(-1);
		}

		try {
			if (!ApplicationUtil.validateProperties()) {
				log.error("properties are not all valid, application exiting");
				System.exit(-1);
			}
		} catch (IOException e) {
			log.error("Unable to validate application properties", e);
			System.exit(-1);
		}

		log.debug("max Task Workers: " + TaskWorker.getMaxTaskWorkers());
		
		ApplicationUtil.configureLogging();
			
		try {
			applicationPath = new File(".").getCanonicalPath();
		} catch (IOException e1) {
			log.error("Unable to set application path", e1);
		}
		
		Configuration freeMarkerConfiguration = FreeMarkerRenderUtil.configureFreemarker(
				applicationPath + ApplicationUtil.getProperties().getProperty(
						"template.files.directory"));



		Service benCloudService = Service.ignite()
				.port(Integer.parseInt(ApplicationUtil.getProperty("server.port")))
				.threadPool(20);

		benCloudService.staticFiles.externalLocation(applicationPath + 
				ApplicationUtil.getProperties().getProperty("static.files.directory"));

		benCloudService.options("/*",
		        (request, response) -> {

		            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
		            if (accessControlRequestHeaders != null) {
		                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
		            }

		            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
		            if (accessControlRequestMethod != null) {
		                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
		            }

		            return "OK";
		        });

		benCloudService.before((request, response) -> {
			//Loosen up CORS; perhaps a bit too much?
			response.header("Access-Control-Allow-Origin", "*");
			log.debug("path: {} {}, uid: {}, ismemberof: {}", request.requestMethod(), request.pathInfo(), request.headers("uid"), request.headers("ismemberof"));

			// u = getUserProfile(request, response)
			// if (!authenticated) {
			// 	halt(401, "You are not welcome here");
			// }
		});

		Spark.exception(Exception.class, (exception, request, response) -> {
		    log.error("Spark exception thrown", exception);
		});

		// Handle authentication and authorization (Passed in via headers from EPA WAM)
		// if( ! ApplicationUtil.usingLocalProperties()) {
			final Config config = new BenCloudConfigFactory().build();

			benCloudService.before("/*", new SecurityFilter(config, "HeaderClient", "user"));
			//benCloudService.before("/api/admin-only", new SecurityFilter(config, "HeaderClient", "admin"));
		// }

		new PublicRoutes(benCloudService, freeMarkerConfiguration);
		new AdminRoutes(benCloudService, freeMarkerConfiguration);
		new ApiRoutes(benCloudService, freeMarkerConfiguration);
		
		JobsUtil.startJobScheduler();
		
		int dbVersion = ApiUtil.getDatabaseVersion();
		// if(ApiUtil.minimumDbVersion > dbVersion) {
			// log.error("STARTUP FAILED: Database version is " + dbVersion + " but must be at least " + ApiUtil.minimumDbVersion);
			// System.exit(-1);
		//}

		log.info("*** BenMAP API Server. Code version " + ApiUtil.appVersion + ", database version " + dbVersion + " ***");

	}

	public static String getApplicationPath() {
		return applicationPath;
	}
}

package gov.epa.bencloud.server;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.pac4j.core.config.Config;
import org.pac4j.core.context.HttpConstants;
import org.pac4j.sparkjava.SecurityFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.epa.bencloud.api.CrosswalksApi;
import gov.epa.bencloud.api.util.ApiUtil;
import gov.epa.bencloud.server.jobs.JobsUtil;
import gov.epa.bencloud.server.routes.ApiRoutes;
import gov.epa.bencloud.server.tasks.TaskWorker;
import gov.epa.bencloud.server.util.ApplicationUtil;
import spark.Request;
import spark.Service;
import spark.Spark;

public class BenCloudServer {
	
	private static final Logger log = LoggerFactory.getLogger(BenCloudServer.class);
	private static final Logger logAccess = LoggerFactory.getLogger("access");
	
	private static String applicationPath;
	
	public static void main(String[] args) {

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

		final Config config = new BenCloudConfigFactory().build();

		benCloudService.before((request, response) -> {
			response.header("Access-Control-Allow-Origin", "*");
			response.header("Content-Security-Policy", "default-src 'self';");
			
			logAccess.info("{} REQUEST {} {}, uid: {}, ismemberof: {}", request.ip(),  request.requestMethod(), request.pathInfo(), request.headers("uid"), request.headers("ismemberof"));

			//Exclude OPTIONS calls from security filter
			if(!request.requestMethod().equalsIgnoreCase(HttpConstants.HTTP_METHOD.OPTIONS.name())) {
				new SecurityFilter(config, "HeaderClient", "user").handle(request, response);
			}
		});
		
		benCloudService.after((request, response) -> {
			
			logAccess.info("{} RESPONSE {} {}, uid: {}, ismemberof: {}, status: {}", request.ip(),  request.requestMethod(), request.pathInfo(), request.headers("uid"), request.headers("ismemberof"), response.status());
		});

		Spark.exception(Exception.class, (exception, request, response) -> {
		    log.error("Spark exception thrown", exception);
		});
	
		int dbVersion = -999;
		try {
			dbVersion = ApiUtil.getDatabaseVersion();
		} catch (Exception e) {
			log.error("STARTUP FAILED: Unable to access database", e);
			System.exit(-1);
		}
		
		if(ApiUtil.minimumDbVersion > dbVersion) {
			log.error("STARTUP FAILED: Database version is " + dbVersion + " but must be at least " + ApiUtil.minimumDbVersion);
			System.exit(-1);
		}
		
		CrosswalksApi.calculateCrosswalks(18, 28);

		new ApiRoutes(benCloudService);
		
		JobsUtil.startJobScheduler();
	

		log.info("*** BenMAP API Server. Code version " + ApiUtil.appVersion + ", database version " + dbVersion + " ***");

	}
	
	public static String getPostParameterValue(Request req, String name) {
		
		String value = null;
		
		Map<String, String[]> params = req.raw().getParameterMap();
		
		for (Map.Entry<String, String[]> entry : params.entrySet()) {
			if (entry.getKey().equals(name)) {
				value = entry.getValue()[0];
			}
		}
		
		return value;
	}
	public static String getApplicationPath() {
		return applicationPath;
	}
}

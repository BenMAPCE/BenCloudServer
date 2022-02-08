package gov.epa.bencloud.server;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import gov.epa.bencloud.api.util.ApiUtil;
import gov.epa.bencloud.server.util.ApplicationUtil;

public class BenCloudTaskRunner {

	public static final String version = "0.1";
	
	private static final Logger log = LoggerFactory.getLogger(BenCloudTaskRunner.class);
    
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
		
		ApplicationUtil.configureLogging();
		LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
		Logger logger = loggerContext.getLogger("gov.epa.bencloud");
			
		try {
			applicationPath = new File(".").getCanonicalPath();
		} catch (IOException e1) {
			log.error("Unable to set application path", e1);
		}
		
		String taskUuid = System.getenv("TASK_UUID");
		
		log.debug("TASK UUID: " + taskUuid);
		
		//JobsUtil.startJobScheduler();
		
		// TODO: Add logic to check database version in settings table
		// and log it as info below. 
		// At some point, we might want to add a static final db version in here so we can throw an error if the db version is lower than exported.
		int dbVersion = ApiUtil.getDatabaseVersion();
		
		log.info("Starting BenCloud, Task Runner version " + version + ", database version " + dbVersion);
		log.info("Received arguments: " + String.join(", ",  args));

		System.exit(0);
	}

	public static String getApplicationPath() {
		return applicationPath;
	}
}

package gov.epa.bencloud.server;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import gov.epa.bencloud.api.util.ApiUtil;
import gov.epa.bencloud.server.tasks.TaskComplete;
import gov.epa.bencloud.server.tasks.TaskQueue;
import gov.epa.bencloud.server.tasks.local.HIFTaskRunnable;
import gov.epa.bencloud.server.tasks.local.ValuationTaskRunnable;
import gov.epa.bencloud.server.tasks.model.Task;
import gov.epa.bencloud.server.util.ApplicationUtil;

public class BenCloudTaskRunner {
	
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
		String taskRunnerUuid = System.getenv("TASK_RUNNER_UUID");
		
		log.debug("TASK UUID: " + taskUuid);
		
		int dbVersion = ApiUtil.getDatabaseVersion();
		if(ApiUtil.minimumDbVersion > dbVersion) {
			log.error("STARTUP FAILED: Database version is " + dbVersion + " but must be at least " + ApiUtil.minimumDbVersion);
			System.exit(-1);
		}

		log.info("*** BenMAP Task Runner. Code version " + ApiUtil.appVersion + ", database version " + dbVersion + " ***");
		log.info("Available processors (cores): " + Runtime.getRuntime().availableProcessors());
		log.info("Free memory (MB): " + Runtime.getRuntime().freeMemory()/1024/1024);
		
	    long maxMemory = Runtime.getRuntime().maxMemory();
	    log.info("Maximum memory (MB): " + (maxMemory == Long.MAX_VALUE ? "no limit" : maxMemory/1024/1024));
	    log.info("Total memory available to JVM (MB): " + Runtime.getRuntime().totalMemory()/1024/1024);

	    
	    try {
			Task task = TaskQueue.getTaskFromQueueRecord(taskUuid);
			if(task == null || task.getType() == null) {
				log.error("Task not found in queue");
				
			} else if(task.getType().equalsIgnoreCase("HIF")) {
				HIFTaskRunnable ht = new HIFTaskRunnable(taskUuid, taskRunnerUuid);
				ht.run();
			} else if(task.getType().equalsIgnoreCase("Valuation")) {
				ValuationTaskRunnable vt = new ValuationTaskRunnable(taskUuid, taskRunnerUuid);
				vt.run();
			} else {
				log.error("Unknown task type: " + task.getType());
				TaskComplete.addTaskToCompleteAndRemoveTaskFromQueue(taskUuid, taskRunnerUuid, false, "Task Failed");
			}
		} catch (Exception e) {
			log.error("Error running task", e);
		}
		
		System.exit(0);
	}

	public static String getApplicationPath() {
		return applicationPath;
	}
}

package gov.epa.bencloud.server.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.epa.bencloud.server.tasks.model.Task;

public class TaskManager {

	private static final Logger log = LoggerFactory.getLogger(TaskManager.class);

	public static void processTask(String uuid) {

		int maxTaskWorkers = TaskWorker.getMaxTaskWorkers();
		int currentTaskWorkers = TaskWorker.getTaskWorkersCount();
		
//		log.info("processTask: " + uuid);
//		log.info("max Task Workers: " + maxTaskWorkers);
//		log.info("current Task Workers: " + currentTaskWorkers);
		
		if (currentTaskWorkers + 1 > maxTaskWorkers) {
			log.info("Already have max TaskWorkers " + maxTaskWorkers +".");
			TaskQueue.returnTaskToQueue(uuid);
		} else {
			
			Task task = TaskQueue.getTaskFromQueueRecord(uuid);
			
			TaskWorker.startTaskWorker(task);
		}
	}
}

package gov.epa.bencloud.server.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.epa.bencloud.server.tasks.model.Task;

public class TaskManager {

	private static final Logger log = LoggerFactory.getLogger(TaskManager.class);

	public static void processTask(String uuid) {

		int maxTaskWorkers = TaskWorker.getMaxTaskWorkers();
		int currentTaskWorkers = TaskWorker.getTaskWorkersCount();
		
//		System.out.println("processTask: " + uuid);
//		System.out.println("max Task Workers: " + maxTaskWorkers);
//		System.out.println("current Task Workers: " + currentTaskWorkers);
		
		if (currentTaskWorkers + 1 > maxTaskWorkers) {
			System.out.println("Already have max TaskWorkers");
			TaskQueue.returnTaskToQueue(uuid);
		} else {
			
			Task task = TaskQueue.getTaskFromQueueRecord(uuid);
			
			TaskWorker.startTaskWorker(task);
		}
	}
}

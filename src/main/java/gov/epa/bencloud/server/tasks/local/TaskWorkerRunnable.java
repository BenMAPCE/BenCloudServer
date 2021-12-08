package gov.epa.bencloud.server.tasks.local;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.epa.bencloud.server.tasks.TaskComplete;
import gov.epa.bencloud.server.tasks.TaskQueue;
import gov.epa.bencloud.server.tasks.TaskWorker;
import gov.epa.bencloud.server.tasks.model.Task;

public class TaskWorkerRunnable implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(TaskWorkerRunnable.class);
	
	private String taskUuid;
	private String taskWorkerUuid;

	public TaskWorkerRunnable(String taskUuid, String taskWorkerUuid) {
		this.taskUuid = taskUuid;
		this.taskWorkerUuid = taskWorkerUuid;
	}

	private int minRandomNumber = 1;
	private int maxRandomNumber = 30;

	private boolean taskSuccessful = true;
	private String taskCompleteMessage = "Task Complete";
	
	public void run() {

		Task task = TaskQueue.getTaskFromQueueRecord(taskUuid);

		int loopTimes = (int) Math.floor(
				Math.random() * (maxRandomNumber - minRandomNumber + 1) + minRandomNumber);

		
		// Fake an unresponsive Task Worker
		if (loopTimes % 5 == 0) {
			LocalDateTime localDateTime = LocalDateTime.now().minusMinutes(30); 
			TaskWorker.updateTaskWorkerHeartbeat(taskWorkerUuid, localDateTime);
			return;
		}
		
		// Fake a failed Task
		if (loopTimes % 3 == 0) {
			taskSuccessful = false;
			taskCompleteMessage = "Failed because " + loopTimes + " is evenly divisible by 3";
		}

		try {

			for (int i = 0; i < loopTimes; i++) {

				TaskWorker.updateTaskWorkerHeartbeat(taskWorkerUuid);
				
				// Fake updating task percentage
				TaskQueue.updateTaskPercentage(taskUuid, ((100 / loopTimes) * (i + 1)));

				Thread.sleep( (long) (Math.random() * 5000) );

			}

			TaskComplete.addTaskToCompleteAndRemoveTaskFromQueue(
					taskUuid, taskWorkerUuid, taskSuccessful, taskCompleteMessage);

		} catch (InterruptedException e) {
			log.error("Task was interrupted", e);
		}
	}
}



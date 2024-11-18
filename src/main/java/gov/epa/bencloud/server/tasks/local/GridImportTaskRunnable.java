package gov.epa.bencloud.server.tasks.local;

import java.time.LocalDateTime;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.epa.bencloud.api.model.GridImportTaskConfig;
import gov.epa.bencloud.api.model.GridImportTaskLog;
import gov.epa.bencloud.server.tasks.TaskComplete;
import gov.epa.bencloud.server.tasks.TaskQueue;
import gov.epa.bencloud.server.tasks.model.Task;
import gov.epa.bencloud.server.tasks.model.TaskMessage;

/*
 * Import a user uploaded shapefile as a grid definition.
 */
public class GridImportTaskRunnable implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(GridImportTaskRunnable.class);
    protected static ObjectMapper objectMapper = new ObjectMapper();
    
	private String taskUuid;
	private String taskWorkerUuid;


	/**
	 * Creates a GridImportTaskRunnable object with the given taskUuid and taskWorkerUuid
	 * @param taskUuid
	 * @param taskWorkerUuid
	 */
	public GridImportTaskRunnable(String taskUuid, String taskWorkerUuid) {
		this.taskUuid = taskUuid;
		this.taskWorkerUuid = taskWorkerUuid;
	}

	private boolean taskSuccessful = true;

	public void run() {
		
		log.info("Grid Import Task Begin: " + taskUuid);
		ObjectMapper mapper = new ObjectMapper();
		Task task = TaskQueue.getTaskFromQueueRecord(taskUuid);

		ArrayList<TaskMessage> messages = new ArrayList<TaskMessage>();
		
		try {
			GridImportTaskConfig gridImportTaskConfig = null;
			gridImportTaskConfig = objectMapper.readValue(task.getParameters(), GridImportTaskConfig.class);

			
			GridImportTaskLog gridImportTaskLog = new GridImportTaskLog(gridImportTaskConfig, task.getUserIdentifier());
			gridImportTaskLog.setDtStart(LocalDateTime.now());
			
			gridImportTaskLog.addMessage("Starting grid import");
			messages.add(new TaskMessage("active", "Reading shapefile"));
			TaskQueue.updateTaskPercentage(taskUuid, 1, mapper.writeValueAsString(messages));
			
			//TODO:
			// Use gridImportTaskConfig to get file id and FilestoreUtil.getFilePath(id) to access file.
			// Extract and validate shapefile. Add any errors to the task log and abort if file cannot be imported
			
			// Generate unique name for shapefile. Maybe just a uuid?
			// Import shapefile into grids schema using the unique name.
			// Make sure and update the messages object with meaningful progress along the way and also increment the task percentage to show progress
			// Create record in grid_definition table with grid name, table name, and user info
			// Remove shapefile from file store using FileStoreUtil.deleteFile()
			// Note: Do not generate crosswalks at this point. 
			//  We will integrate that into our other processes to ensure the crosswalk exists before calling db functions that use it
			//  This will avoid creating unnecessary crosswalks
			
			messages.get(messages.size()-1).setStatus("complete");
			
			String completeMessage = String.format("Imported grid definition");
			gridImportTaskLog.addMessage(completeMessage);
			gridImportTaskLog.setSuccess(true);
			gridImportTaskLog.setDtEnd(LocalDateTime.now());
			
			//TODO
			//GridImportUtil.storeTaskLog(gridImportTaskLog);
			
			TaskComplete.addTaskToCompleteAndRemoveTaskFromQueue(taskUuid, taskWorkerUuid, taskSuccessful, completeMessage);

		} catch (Exception e) {
			TaskComplete.addTaskToCompleteAndRemoveTaskFromQueue(taskUuid, taskWorkerUuid, false, "Task Failed");
			log.error("Task failed", e);
		}
		log.info("Grid Import Task Complete: " + taskUuid);
	}


}

package gov.epa.bencloud.server.tasks.runnable;

import static gov.epa.bencloud.server.database.jooq.data.Tables.*;

import java.time.LocalDateTime;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.epa.bencloud.api.model.AirQualityImportTaskConfig;
import gov.epa.bencloud.api.model.AirQualityImportTaskLog;
import gov.epa.bencloud.api.model.ValidationMessage;
import gov.epa.bencloud.api.util.AirQualityUtil;
import gov.epa.bencloud.server.tasks.TaskComplete;
import gov.epa.bencloud.server.tasks.TaskQueue;
import gov.epa.bencloud.server.tasks.model.Task;
import gov.epa.bencloud.server.tasks.model.TaskMessage;

/*
 * Import a user uploaded shapefile as a grid definition.
 */
public class AirQualityImportTaskRunnable implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(AirQualityImportTaskRunnable.class);
    protected static ObjectMapper objectMapper = new ObjectMapper();
    
	private String taskUuid;
	private String taskWorkerUuid;
	private ValidationMessage validationMsg = new ValidationMessage();


	/**
	 * Creates a AirQualityImportTaskRunnable object with the given taskUuid and taskWorkerUuid
	 * @param taskUuid
	 * @param taskWorkerUuid
	 */
	public AirQualityImportTaskRunnable(String taskUuid, String taskWorkerUuid) {
		this.taskUuid = taskUuid;
		this.taskWorkerUuid = taskWorkerUuid;
	}

	private boolean taskSuccessful = true;
	
	public void run() {
		
		log.info("AirQuality Import Task Begin: " + taskUuid);
		ObjectMapper mapper = new ObjectMapper();
		Task task = TaskQueue.getTaskFromQueueRecord(taskUuid);

		ArrayList<TaskMessage> messages = new ArrayList<TaskMessage>();
		
		try {
			AirQualityImportTaskConfig AirQualityImportTaskConfig = null;
			AirQualityImportTaskConfig = objectMapper.readValue(task.getParameters(), AirQualityImportTaskConfig.class);
			
			AirQualityImportTaskLog AirQualityImportTaskLog = new AirQualityImportTaskLog(AirQualityImportTaskConfig, task.getUserIdentifier());
			AirQualityImportTaskLog.setDtStart(LocalDateTime.now());
			
			AirQualityImportTaskLog.addMessage("Starting grid import");
			messages.add(new TaskMessage("active", "Reading shapefile"));
			
			TaskQueue.updateTaskPercentage(taskUuid, 1, mapper.writeValueAsString(messages));
			
			// Use AirQualityImportTaskConfig to get file id and FilestoreUtil.getFilePath(id) to access file.

			// TODO: loop over files, AirQualityImportTaskConfig.csvFilestoreIds
			
			// Import csv files 
			
			// Create record in air_quality_layer table
			
			// Remove csv from file store using FileStoreUtil.deleteFile()

			messages.get(messages.size()-1).setStatus("complete");
			
			String completeMessage = String.format("Imported air quality layer(s)");
			AirQualityImportTaskLog.addMessage(completeMessage);
			AirQualityImportTaskLog.setSuccess(true);
			AirQualityImportTaskLog.setDtEnd(LocalDateTime.now());
			
			AirQualityUtil.storeTaskLog(AirQualityImportTaskLog);
			
			TaskComplete.addTaskToCompleteAndRemoveTaskFromQueue(taskUuid, taskWorkerUuid, taskSuccessful, completeMessage);

		} catch (Exception e) {
			TaskComplete.addTaskToCompleteAndRemoveTaskFromQueue(taskUuid, taskWorkerUuid, false, "Task failed");
			log.error("Task failed", e);
		}
		log.info("AirQuality Import Task Complete: " + taskUuid);
	}

}

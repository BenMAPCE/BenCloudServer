package gov.epa.bencloud.server.tasks;

import static gov.epa.bencloud.server.database.jooq.data.Tables.TASK_COMPLETE;
import static gov.epa.bencloud.server.database.jooq.data.Tables.TASK_QUEUE;
import static gov.epa.bencloud.server.database.jooq.data.Tables.TASK_WORKER;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;

import org.jooq.Record;
import org.jooq.Result;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.pac4j.core.profile.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import gov.epa.bencloud.api.CoreApi;
import gov.epa.bencloud.server.database.JooqUtil;
import gov.epa.bencloud.server.tasks.model.Task;
import gov.epa.bencloud.server.util.DataUtil;

/*
 * 
 */
public class TaskComplete {

	private static final Logger log = LoggerFactory.getLogger(TaskComplete.class);

	/**
	 * Removes a completed task from the task queue, removes its task worker, and adds the task to the task complete table.
	 * @param taskUuid
	 * @param taskWorkerUuid
	 * @param taskSuccessful
	 * @param taskCompleteMessage
	 */
	public static void addTaskToCompleteAndRemoveTaskFromQueue(
			String taskUuid, String taskWorkerUuid, 
			boolean taskSuccessful, String taskCompleteMessage) {

		if (null == taskUuid) {
			return;
		}

		Task task = TaskQueue.getTaskFromQueueRecord(taskUuid);

		try {

			DSL.using(JooqUtil.getJooqConfiguration()).transaction(ctx -> {

				//taskWorkerUuid might be null if a child task is failed because the parent task failed
				if(taskWorkerUuid != null) {
					DSL.using(ctx).delete(TASK_WORKER)
					.where(TASK_WORKER.TASK_WORKER_UUID.eq(taskWorkerUuid))
					.execute();
				}

				DSL.using(ctx).insertInto(TASK_COMPLETE,
						TASK_COMPLETE.USER_ID,
						TASK_COMPLETE.TASK_PRIORITY,
						TASK_COMPLETE.TASK_UUID,
						TASK_COMPLETE.TASK_PARENT_UUID,
						TASK_COMPLETE.TASK_NAME,
						TASK_COMPLETE.TASK_DESCRIPTION,
						TASK_COMPLETE.TASK_TYPE,
						TASK_COMPLETE.TASK_PARAMETERS,
						TASK_COMPLETE.TASK_RESULTS,
						TASK_COMPLETE.TASK_SUCCESSFUL,
						TASK_COMPLETE.TASK_COMPLETE_MESSAGE,
						TASK_COMPLETE.TASK_SUBMITTED_DATE,
						TASK_COMPLETE.TASK_STARTED_DATE,
						TASK_COMPLETE.TASK_COMPLETED_DATE)
				.values(
						task.getUserIdentifier(),
						task.getPriority(),
						task.getUuid(),
						task.getParentUuid(),
						task.getName(),
						task.getDescription(),
						task.getType(),
						task.getParameters(),
						"{}",
						taskSuccessful,
						taskCompleteMessage,
						task.getSubmittedDate(),
						task.getStartedDate(),
						LocalDateTime.now())
				.execute();

				DSL.using(ctx).delete(TASK_QUEUE)
				.where(TASK_QUEUE.TASK_UUID.eq(task.getUuid()))
				.execute();

			});

		} catch (DataAccessException e1) {
			log.error("Error moving task to completed queue", e1);
		}

	}

	/**
	 * 
	 * @param userProfile
	 * @param postParameters
	 * @return an ObjectNode representation of all of a user's completed tasks.
	 */
	public static ObjectNode getCompletedTasks(Optional<UserProfile> userProfile, Map<String, String[]> postParameters) {

//		System.out.println("getCompletedTasks");
//		System.out.println("userIdentifier: " + userIdentifier);
		
//		System.out.println("length: " + postParameters.get("length")[0]);
//		System.out.println("start: " + postParameters.get("start")[0]);
//		System.out.println("searchValue: " + postParameters.get("searchValue")[0]);
//		System.out.println("sortColumn: " + postParameters.get("sortColumn")[0]);
//		System.out.println("sortDirection: " + postParameters.get("sortDirection")[0]);

		String userId = userProfile.get().getId();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode data = mapper.createObjectNode();
        
        ArrayNode tasks = mapper.createArrayNode();
        ObjectNode task = mapper.createObjectNode();
        ObjectNode wrappedObject = mapper.createObjectNode();

        int records = 0;

		try {

			Result<Record> result = DSL.using(JooqUtil.getJooqConfiguration()).select().from(TASK_COMPLETE)
					.where(TASK_COMPLETE.USER_ID.eq(userId)
							.or(CoreApi.isAdmin(userProfile) ? DSL.trueCondition() : DSL.noCondition()) //Show all completed results to admins for now
						)
					.orderBy(TASK_COMPLETE.TASK_COMPLETED_DATE.asc())
					.fetch();

			for (Record record : result) {

				task = mapper.createObjectNode();

				task.put("task_name", record.getValue(TASK_COMPLETE.TASK_NAME));
				task.put("task_type", record.getValue(TASK_COMPLETE.TASK_TYPE));
				task.put("task_description", record.getValue(TASK_COMPLETE.TASK_DESCRIPTION));
				task.put("task_uuid", record.getValue(TASK_COMPLETE.TASK_UUID));
				task.put("task_submitted_date", record.getValue(TASK_COMPLETE.TASK_SUBMITTED_DATE).format(formatter));
				task.put("task_started_date", record.getValue(TASK_COMPLETE.TASK_STARTED_DATE).format(formatter));
				task.put("task_completed_date", record.getValue(TASK_COMPLETE.TASK_COMPLETED_DATE).format(formatter));
				task.put("task_user_id", record.getValue(TASK_COMPLETE.USER_ID));
				
				wrappedObject = mapper.createObjectNode();
				wrappedObject.put("task_wait_time_display", DataUtil.getHumanReadableTime(
						record.getValue(TASK_COMPLETE.TASK_SUBMITTED_DATE), 
						record.getValue(TASK_COMPLETE.TASK_STARTED_DATE)));
				wrappedObject.put("task_wait_time_seconds", 
						ChronoUnit.SECONDS.between(record.getValue(TASK_COMPLETE.TASK_SUBMITTED_DATE),
								record.getValue(TASK_COMPLETE.TASK_STARTED_DATE)));
				task.set("task_wait_time", wrappedObject);

				wrappedObject = mapper.createObjectNode();
				wrappedObject.put("task_execution_time_display", DataUtil.getHumanReadableTime(
						record.getValue(TASK_COMPLETE.TASK_STARTED_DATE), 
						record.getValue(TASK_COMPLETE.TASK_COMPLETED_DATE)));
				wrappedObject.put("task_execution_time_seconds", 
						ChronoUnit.SECONDS.between(record.getValue(TASK_COMPLETE.TASK_STARTED_DATE),
								record.getValue(TASK_COMPLETE.TASK_COMPLETED_DATE)));
				task.set("task_execution_time", wrappedObject);

				task.put("task_elapsed_time", DataUtil.getHumanReadableTime(
						record.getValue(TASK_COMPLETE.TASK_STARTED_DATE), 
						record.getValue(TASK_COMPLETE.TASK_COMPLETED_DATE)));
				
				task.put("task_successful", record.getValue(TASK_COMPLETE.TASK_SUCCESSFUL));
				task.put("task_message", record.getValue(TASK_COMPLETE.TASK_COMPLETE_MESSAGE));
			    					
				tasks.add(task);
				records++;
				
			}
			
			data.set("data", tasks);
			data.put("success", true);
			data.put("recordsFiltered", records);
			data.put("recordsTotal", records);

		} catch (DataAccessException e) {
			data.put("success", false);
			data.put("error_message", e.getMessage());
			log.error("Error getting completed tasks", e);
		} catch (IllegalArgumentException e) {
			data.put("success", false);
			data.put("error_message", e.getMessage());
			log.error("Error getting completed tasks", e);
		} catch (Exception e) {
			data.put("success", false);
			data.put("error_message", "Unknown error");
			log.error("Error getting completed tasks", e);			
		}
	
		return data;
	} 

	/**
	 * 
	 * @param uuid
	 * @return  If uuid is associated with a single task in task complete, returns that task.
	 * 			If uuid is not in the task complete table, or is associated with multiple record in the table, 
	 * 			returns an empty task object.
	 */
	public static Task getTaskFromCompleteRecord(String uuid) {

		Task task = new Task();

		try {
			Result<Record> result = DSL.using(JooqUtil.getJooqConfiguration()).select().from(TASK_COMPLETE)
					.where(TASK_COMPLETE.TASK_UUID.eq(uuid))
					.fetch();

			if (result.size() == 0) {
				System.out.println("no uuid in complete");
			} else if (result.size() > 1) {
				System.out.println("received more than 1 uuid record");
			} else {
				Record record = result.get(0);
				task.setName(record.getValue(TASK_COMPLETE.TASK_NAME));
				task.setDescription(record.getValue(TASK_COMPLETE.TASK_DESCRIPTION));
				task.setUserIdentifier(record.getValue(TASK_COMPLETE.USER_ID));
				task.setPriority(record.getValue(TASK_COMPLETE.TASK_PRIORITY));
				task.setUuid(record.getValue(TASK_COMPLETE.TASK_UUID));
				task.setParentUuid(record.getValue(TASK_COMPLETE.TASK_PARENT_UUID));
				task.setParameters(record.getValue(TASK_COMPLETE.TASK_PARAMETERS));
				task.setType(record.getValue(TASK_COMPLETE.TASK_TYPE));
				task.setSubmittedDate(record.getValue(TASK_COMPLETE.TASK_SUBMITTED_DATE));
				task.setStartedDate(record.getValue(TASK_COMPLETE.TASK_STARTED_DATE));
				task.setCompletedDate(record.getValue(TASK_COMPLETE.TASK_COMPLETED_DATE));
			}
		} catch (DataAccessException e1) {
			log.error("Error getting task", e1);
		}

		return task;
	}
}

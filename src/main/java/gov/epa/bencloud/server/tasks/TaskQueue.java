package gov.epa.bencloud.server.tasks;

import static gov.epa.bencloud.server.database.jooq.data.Tables.TASK_QUEUE;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
import com.fasterxml.jackson.databind.util.RawValue;

import gov.epa.bencloud.api.HIFApi;
import gov.epa.bencloud.server.database.JooqUtil;
import gov.epa.bencloud.server.tasks.model.Task;

public class TaskQueue {

	private static final Logger log = LoggerFactory.getLogger(TaskQueue.class);

	public static String getTaskFromQueue() {

		String uuid = null;;

		try {

			uuid = DSL.using(JooqUtil.getJooqConfiguration())
					.transactionResult(ctx -> {

						String taskUuid = null;
						boolean shouldStart = true;
						gov.epa.bencloud.server.database.jooq.data.tables.TaskQueue t1 = TASK_QUEUE.as("t1");
						gov.epa.bencloud.server.database.jooq.data.tables.TaskQueue t2 = TASK_QUEUE.as("t2");
						
						// Query the next pending task that does not have a parent present in the queue
						Result<Record> result = DSL.using(ctx).select(t1.asterisk())
								.from(t1)
								.leftJoin(t2).on(t1.TASK_PARENT_UUID.eq(t2.TASK_UUID))
								.where(t1.TASK_IN_PROCESS.isFalse()
										.and(t2.TASK_UUID.isNull()))
								.orderBy(t1.TASK_SUBMITTED_DATE.asc())
								.limit(1)
								.forUpdate().of(t1)
								.fetch();

						
						if (result.size() == 0) {
							// System.out.println("no tasks to process");
						} else if (result.size() > 1) {
							System.out.println("recieved more than 1 task record");
						} else {
							Record record = result.get(0);

							// If it's a child task, make sure the parent succeeded
							String parentUuid = record.getValue(TASK_QUEUE.TASK_PARENT_UUID);
							if(parentUuid != null && parentUuid.length() > 0) {
								String parentStatus = HIFApi.getHIFTaskStatus(parentUuid);
								
								if(parentStatus.equals("failed")) { //parent failed so fail this task
									TaskComplete.addTaskToCompleteAndRemoveTaskFromQueue(record.getValue(TASK_QUEUE.TASK_UUID), null, false, "Parent task failed");
									shouldStart = false;
								}	
							}
							
							if (shouldStart) {
								DSL.using(ctx).update(TASK_QUEUE)
								.set(TASK_QUEUE.TASK_IN_PROCESS, true)
								.where(TASK_QUEUE.TASK_ID.eq(record.getValue(TASK_QUEUE.TASK_ID)))
								.execute();
	
								taskUuid = record.getValue(TASK_QUEUE.TASK_UUID);
							} else {
								
							}
						}

						return taskUuid;

					});
		} catch (Exception e) {
			log.error("Error getting task", e);
		} finally {

		}

		return uuid;

	}

	public static void updateTaskPercentage(String taskUuid, int percentage) {

		try {

			DSL.using(JooqUtil.getJooqConfiguration())
			.transactionResult(ctx -> {

				Result<Record> result = DSL.using(ctx).select().from(TASK_QUEUE)
						.where(TASK_QUEUE.TASK_UUID.eq(taskUuid))
						.limit(1)
						.forUpdate()
						.fetch();

				if (result.size() == 0) {
					// System.out.println("no tasks to process");
				} else if (result.size() > 1) {
					System.out.println("recieved more than 1 task record");
				} else {
					Record record = result.get(0);

					DSL.using(ctx).update(TASK_QUEUE)
					.set(TASK_QUEUE.TASK_PERCENTAGE, percentage)
					.where(TASK_QUEUE.TASK_UUID.eq(taskUuid))
					.execute();
				}
				return taskUuid;
			});

		} catch (Exception e) {
			log.error("Error updating task", e);
		} finally {

		}
	}
	
	public static void updateTaskPercentage(String taskUuid, int percentage, String message) {

		try {

			DSL.using(JooqUtil.getJooqConfiguration())
			.transactionResult(ctx -> {

				Result<Record> result = DSL.using(ctx).select().from(TASK_QUEUE)
						.where(TASK_QUEUE.TASK_UUID.eq(taskUuid))
						.limit(1)
						.forUpdate()
						.fetch();

				if (result.size() == 0) {
					// System.out.println("no tasks to process");
				} else if (result.size() > 1) {
					System.out.println("recieved more than 1 task record");
				} else {
					Record record = result.get(0);

					DSL.using(ctx).update(TASK_QUEUE)
					.set(TASK_QUEUE.TASK_PERCENTAGE, percentage)
					.set(TASK_QUEUE.TASK_MESSAGE, message)
					.where(TASK_QUEUE.TASK_UUID.eq(taskUuid))
					.execute();
				}
				return taskUuid;
			});

		} catch (Exception e) {
			log.error("Error updating task", e);
		} finally {

		}
	}


	public static void returnTaskToQueue(String uuid) {

		try {

			DSL.using(JooqUtil.getJooqConfiguration())
			.transaction(ctx -> {
				Result<Record> result = DSL.using(ctx).select().from(TASK_QUEUE)
						.where(TASK_QUEUE.TASK_IN_PROCESS.isTrue().and(TASK_QUEUE.TASK_UUID.eq(uuid)))
						.orderBy(TASK_QUEUE.TASK_SUBMITTED_DATE.asc())
						.forUpdate()
						.fetch();

				if (result.size() == 0) {
					System.out.println("no task for uuid: " + uuid);
				} else if (result.size() > 1) {
					System.out.println("recieved more than 1 task record for uuid: " + uuid);
				} else {
					Record record = result.get(0);

					System.out.println("making job available again: " + 
							record.get(TASK_QUEUE.TASK_NAME));

					DSL.using(ctx).update(TASK_QUEUE)
					.set(TASK_QUEUE.TASK_IN_PROCESS, false)
					.where(TASK_QUEUE.TASK_UUID.eq(record.getValue(TASK_QUEUE.TASK_UUID)))
					.execute();
				}
				System.out.println("returning task to Queue: " + uuid);
			});
		} catch (Exception e) {
			log.error("Error returning task", e);
		} finally {

		}		
	}

	public static void writeTaskToQueue(Task task) {

		// System.out.println("writeTaskToQueue: " + task.getUuid());

		try {
			DSL.using(JooqUtil.getJooqConfiguration()).insertInto(TASK_QUEUE,
					TASK_QUEUE.USER_ID,
					TASK_QUEUE.TASK_PRIORITY,
					TASK_QUEUE.TASK_UUID,
					TASK_QUEUE.TASK_PARENT_UUID,
					TASK_QUEUE.TASK_NAME,
					TASK_QUEUE.TASK_DESCRIPTION,
					TASK_QUEUE.TASK_PARAMETERS,
					TASK_QUEUE.TASK_TYPE,
					TASK_QUEUE.TASK_IN_PROCESS,
					TASK_QUEUE.TASK_SUBMITTED_DATE)
			.values(
					task.getUserIdentifier(),
					Integer.valueOf(10),
					task.getUuid(),
					task.getParentUuid(),
					task.getName(),
					task.getDescription(),
					task.getParameters(),
					task.getType(),
					false,
					LocalDateTime.now())
			.execute();

		} catch (DataAccessException e1) {
			log.error("Error writing task", e1);
		}
	}

	public static ObjectNode getPendingTasks(Optional<UserProfile> userProfile, Map<String, String[]> postParameters) {

		//System.out.println("getPendingTasks");
//		System.out.println("userIdentifier: " + userIdentifier);
		String userId = userProfile.get().getId();

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

		ObjectMapper mapper = new ObjectMapper();
		ObjectNode data = mapper.createObjectNode();

		ArrayNode tasks = mapper.createArrayNode();
		ObjectNode task = mapper.createObjectNode();
		ObjectNode wrappedObject = mapper.createObjectNode();

		int records = 0;

//		if (null != userIdentifier) {

			try {
				LocalDateTime now = LocalDateTime.now();
				try {

					Result<Record> result = DSL.using(JooqUtil.getJooqConfiguration()).select().from(TASK_QUEUE)
							.where(TASK_QUEUE.USER_ID.eq(userId))
							.orderBy(TASK_QUEUE.TASK_SUBMITTED_DATE.asc())
							.fetch();

					for (Record record : result) {

						task = mapper.createObjectNode();

						task.put("task_name", record.getValue(TASK_QUEUE.TASK_NAME));
						//task.put("task_description", record.getValue(TASK_QUEUE.TASK_DESCRIPTION));
						task.put("task_uuid", record.getValue(TASK_QUEUE.TASK_UUID));
						task.put("task_submitted_date", record.getValue(TASK_QUEUE.TASK_SUBMITTED_DATE).format(formatter));
						task.put("task_type", record.getValue(TASK_QUEUE.TASK_TYPE));

						wrappedObject = mapper.createObjectNode();

						if (record.getValue(TASK_QUEUE.TASK_IN_PROCESS)) {

							task.put("task_status_message", "Started at " + record.getValue(TASK_QUEUE.TASK_SUBMITTED_DATE).format(formatter) );
							task.putRawValue("task_progress_message", new RawValue(record.getValue(TASK_QUEUE.TASK_MESSAGE)));

							
							//task.put("task_wait_time", DataUtil.getHumanReadableTime(
							//		record.getValue(TASK_QUEUE.TASK_SUBMITTED_DATE), 
							//		record.getValue(TASK_QUEUE.TASK_STARTED_DATE)));

							//task.put("task_active_time", DataUtil.getHumanReadableTime(
							//		record.getValue(TASK_QUEUE.TASK_SUBMITTED_DATE), 
							//		now));

							//task.put("task_started_date", record.getValue(TASK_QUEUE.TASK_SUBMITTED_DATE).format(formatter));
						} else {
							
							task.put("task_status_message", "Pending");
							task.putRawValue("task_progress_message", new RawValue(record.getValue(TASK_QUEUE.TASK_MESSAGE)));
							//							task.put("task_wait_time", DataUtil.getHumanReadableTime(
//									record.getValue(TASK_QUEUE.TASK_SUBMITTED_DATE), 
//									now));
//							
//							task.put("task_active_time", "");
//							task.put("task_started_date", "");
						}
						task.put("task_percentage", record.getValue(TASK_QUEUE.TASK_PERCENTAGE));
						task.put("task_status", record.getValue(TASK_QUEUE.TASK_IN_PROCESS));

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
					log.error("Error getting pending tasks", e);
				} catch (IllegalArgumentException e) {
					data.put("success", false);
					data.put("error_message", e.getMessage());
					log.error("Error getting pending tasks", e);
				}
			} catch (Exception e) {
				log.error("Error getting pending tasks", e);
			}

		return data;
	} 

	
	public static Task getTaskFromQueueRecord(String uuid) {

		// System.out.println("getTaskFromQueueRecord: " + uuid);

		Task task = new Task();

		try {
			Result<Record> result = DSL.using(JooqUtil.getJooqConfiguration()).select().from(TASK_QUEUE)
					.where(TASK_QUEUE.TASK_UUID.eq(uuid))
					.fetch();

			if (result.size() == 0) {
				System.out.println("no uuid in queue");
			} else if (result.size() > 1) {
				System.out.println("recieved more than 1 uuid record");
			} else {
				Record record = result.get(0);
				task.setName(record.getValue(TASK_QUEUE.TASK_NAME));
				task.setDescription(record.getValue(TASK_QUEUE.TASK_DESCRIPTION));
				task.setUserIdentifier(record.getValue(TASK_QUEUE.USER_ID));
				task.setPriority(record.getValue(TASK_QUEUE.TASK_PRIORITY));
				task.setUuid(record.getValue(TASK_QUEUE.TASK_UUID));
				task.setParentUuid(record.getValue(TASK_QUEUE.TASK_PARENT_UUID));
				task.setParameters(record.getValue(TASK_QUEUE.TASK_PARAMETERS));
				task.setType(record.getValue(TASK_QUEUE.TASK_TYPE));
				task.setSubmittedDate(record.getValue(TASK_QUEUE.TASK_SUBMITTED_DATE));
				task.setStartedDate(record.getValue(TASK_QUEUE.TASK_STARTED_DATE));
			}
		} catch (DataAccessException e1) {
			log.error("Error getting task", e1);
		}

		return task;
	}

	public static Task getChildValuationTaskFromQueueRecord(String parentUuid) {

		// System.out.println("getTaskFromQueueRecord: " + uuid);

		Task task = new Task();

		try {
			Result<Record> result = DSL.using(JooqUtil.getJooqConfiguration()).select().from(TASK_QUEUE)
					.where(TASK_QUEUE.TASK_PARENT_UUID.eq(parentUuid)
							.and(TASK_QUEUE.TASK_TYPE.equalIgnoreCase("Valuation")))
					.fetch();

			if (result.size() == 0) {
				System.out.println("no uuid in queue");
			} else if (result.size() > 1) {
				System.out.println("recieved more than 1 uuid record");
			} else {
				Record record = result.get(0);
				task.setName(record.getValue(TASK_QUEUE.TASK_NAME));
				task.setDescription(record.getValue(TASK_QUEUE.TASK_DESCRIPTION));
				task.setUserIdentifier(record.getValue(TASK_QUEUE.USER_ID));
				task.setPriority(record.getValue(TASK_QUEUE.TASK_PRIORITY));
				task.setUuid(record.getValue(TASK_QUEUE.TASK_UUID));
				task.setParentUuid(record.getValue(TASK_QUEUE.TASK_PARENT_UUID));
				task.setParameters(record.getValue(TASK_QUEUE.TASK_PARAMETERS));
				task.setType(record.getValue(TASK_QUEUE.TASK_TYPE));
				task.setSubmittedDate(record.getValue(TASK_QUEUE.TASK_SUBMITTED_DATE));
				task.setStartedDate(record.getValue(TASK_QUEUE.TASK_STARTED_DATE));
			}
		} catch (DataAccessException e1) {
			log.error("Error getting task", e1);
		}

		return task;
	}
}

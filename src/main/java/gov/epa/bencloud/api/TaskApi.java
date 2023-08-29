/**
 * 
 */
package gov.epa.bencloud.api;

import static gov.epa.bencloud.server.database.jooq.data.Tables.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import javax.servlet.MultipartConfigElement;

import spark.Request;
import spark.Response;

import org.jetbrains.annotations.Nullable;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.JSON;
import org.jooq.JSONFormat;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.exception.DataAccessException;
import org.jooq.JSONFormat.RecordFormat;
import org.jooq.impl.DSL;
import org.pac4j.core.profile.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.RawValue;

import gov.epa.bencloud.api.model.BatchTaskConfig;
import gov.epa.bencloud.api.model.ExposureConfig;
import gov.epa.bencloud.api.model.ExposureTaskConfig;
import gov.epa.bencloud.api.model.HIFConfig;
import gov.epa.bencloud.api.model.HIFTaskConfig;
import gov.epa.bencloud.api.util.AirQualityUtil;
import gov.epa.bencloud.api.util.ApiUtil;
import gov.epa.bencloud.api.util.ExposureUtil;
import gov.epa.bencloud.api.util.HIFUtil;
import gov.epa.bencloud.api.util.ValuationUtil;
import gov.epa.bencloud.server.database.JooqUtil;
import gov.epa.bencloud.server.database.jooq.data.tables.PopConfig;
import gov.epa.bencloud.server.database.jooq.data.tables.records.TaskBatchRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.TaskConfigRecord;
import gov.epa.bencloud.server.tasks.TaskQueue;
import gov.epa.bencloud.server.tasks.model.Task;
import gov.epa.bencloud.server.util.ApplicationUtil;
import gov.epa.bencloud.server.util.ParameterUtil;
import gov.epa.bencloud.api.model.Scenario;
import gov.epa.bencloud.api.model.ScenarioHIFConfig;
import gov.epa.bencloud.api.model.ScenarioPopConfig;
import gov.epa.bencloud.api.model.ValuationConfig;
import gov.epa.bencloud.api.model.ValuationTaskConfig;
import gov.epa.bencloud.Constants;
import gov.epa.bencloud.api.model.BatchExposureGroup;
import gov.epa.bencloud.api.model.BatchHIFGroup;


/**
 * @author jimanderton
 *
 */
public class TaskApi {
	private static final Logger log = LoggerFactory.getLogger(TaskApi.class);
    protected static ObjectMapper objectMapper = new ObjectMapper();
    
	/**
	 * 
	 * @param request
	 * @param response
	 * @param userProfile
	 * @return a JSON representation of the current user's task configurations.
	 */
	public static Object getTaskConfigs(Request request, Response response, Optional<UserProfile> userProfile) {
		//TODO: Add type filter to select HIF or Valuation

		Result<Record> res = DSL.using(JooqUtil.getJooqConfiguration())
				.select(TASK_CONFIG.asterisk())
				.from(TASK_CONFIG)
				.where(TASK_CONFIG.USER_ID.eq(userProfile.get().getId()))
				.orderBy(TASK_CONFIG.NAME)
				.fetch();
		
		response.type("application/json");
		return res.formatJSON(new JSONFormat().header(false).recordFormat(RecordFormat.OBJECT));
	}
	

	
	public static Object deleteTaskConfig(Request request, Response response, Optional<UserProfile> userProfile) {
		Integer id;
		try {
			id = Integer.valueOf(request.params("id"));
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return CoreApi.getErrorResponseInvalidId(request, response);
		}
		
		
		//Users can only delete templates created by themselves 		
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());	
		Result<Record1<Integer>> res = create.select(TASK_CONFIG.ID)
				.from(TASK_CONFIG)
				.where(TASK_CONFIG.USER_ID.eq(userProfile.get().getId()))
				.and(TASK_CONFIG.ID.eq(id))
				.fetch();
		
		if(res==null) {
			return CoreApi.getErrorResponseForbidden(request, response);
		}		
		
		int configRows = create.deleteFrom(TASK_CONFIG).where(TASK_CONFIG.ID.eq(id)).execute();
		
		if(configRows == 0) {
			return CoreApi.getErrorResponse(request, response, 400, "Unknown error");
		} else {
			response.status(204);
			return response;
		}
		
	}
	
	public static Object renameTaskConfig(Request request, Response response, Optional<UserProfile> userProfile) {
		request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));
		Integer id;
		String newName;

		try {
			id = Integer.valueOf(request.params("id"));
			newName = ApiUtil.getMultipartFormParameterAsString(request, "newName");
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return CoreApi.getErrorResponseInvalidId(request, response);
		}		
		
		//make sure the new template name is unique among this user's templates
			List<String>taskNames = ApiUtil.getAllTemplateNamesByUser(userProfile.get().getId());
			if (taskNames.contains(newName)) {
				response.status(409);
				String errorMsg = "A task named " + newName + " already exists. Please enter a different name.";
				//return "{\"message\": \"" + errorMsg + "\"}";
				return CoreApi.getSuccessResponse(request, response, 409, errorMsg);
			}			
		
		//Users can only edit templates created by themselves 		
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());	
		Result<Record1<Integer>> res = create.select(TASK_CONFIG.ID)
				.from(TASK_CONFIG)
				.where(TASK_CONFIG.USER_ID.eq(userProfile.get().getId()))
				.and(TASK_CONFIG.ID.eq(id))
				.fetch();
		
		if(res==null) {
			response.status(400);
			return CoreApi.getErrorResponse(request, response, 400, "You can only rename tasks created by yourself.");
		}		
		
		int configRows = create.update(TASK_CONFIG)
				.set(TASK_CONFIG.NAME, newName)
				.where(TASK_CONFIG.ID.eq(id))
				.execute();
		
		if(configRows == 0) {
			response.status(400);
			return CoreApi.getErrorResponse(request, response, 400, "Unknown error");
		} else {
			response.status(200);
			return CoreApi.getSuccessResponse(request, response, 200, "Successfully renamed.");
		}
		
	}

	/**
	 * 
	 * @param request HTTP request body contains task configuration parameters
	 * @param response
	 * @param userProfile
	 * @return add a task configuration to the database.
	 */
	public static String postTaskConfig(Request request, Response response, Optional<UserProfile> userProfile) {
		String body = request.body();
		
		JsonNode jsonPost = null;
		
		try {
			jsonPost = objectMapper.readTree(body);
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			response.status(400);
			return null;
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			response.status(400);
			return null;
		} 
		
		String name;
		String type;
		JSON params;
		try {
			name = jsonPost.get("name").asText();
			type = jsonPost.get("type").asText();
			params = JSON.json(jsonPost.get("parameters").toString());
		} catch (NullPointerException e) {
			e.printStackTrace();
			response.status(400);
			return null;
		}
		

		//make sure the new template name is unique among this user's templates
		List<String>taskNames = ApiUtil.getAllTemplateNamesByUser(userProfile.get().getId());
		if (taskNames.contains(name)) {
			response.status(200);
			String errorMsg = "A task named " + name + " already exists. Please enter a different name.";
			return "{\"message\": \"" + errorMsg + "\"}";
		}	
		
		TaskConfigRecord rec = DSL.using(JooqUtil.getJooqConfiguration())
		.insertInto(TASK_CONFIG, TASK_CONFIG.NAME, TASK_CONFIG.TYPE, TASK_CONFIG.PARAMETERS, TASK_CONFIG.USER_ID)
		.values(name, type, params, userProfile.get().getId())
		.returning(TASK_CONFIG.asterisk())
		.fetchOne();
		
		return rec.formatJSON(new JSONFormat().header(false).recordFormat(RecordFormat.OBJECT));
	}

	public static int getTotalTaskCountForUser(UserProfile profile) {
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());
		int pendingCount = create.fetchCount(TASK_QUEUE, TASK_QUEUE.USER_ID.eq(profile.getId()));
		int completedCount = create.fetchCount(TASK_COMPLETE, TASK_COMPLETE.USER_ID.eq(profile.getId()));
		return pendingCount + completedCount;
	}
	
	public static Object getTaskDeleteSuccessResponse(Request request, Response response) {
		return CoreApi.getSuccessResponse(request, response, 204, "Successfully deleted task");
	}	
	
	/**
	 * Create a partial batch task config to populate the Value of Effects page
	 * 
	 * @param request
	 * @param response
	 * @param userProfile
	 * @return a partially populated batch task config based on the provided parameters.
	 * 
	 */
	public static Object getBatchTaskConfig(Request request, Response response, Optional<UserProfile> userProfile) {
		
		/*
		 * /api/batch-task-config
		 * ?groupIds=5
		 * &pollutantId=5
		 * &baselineId=15
		 * &populationId=40
		 * &gridDefinitionId=18
		 * scenarios=24|2030~2035,22|2030~2035  //Note the elaborate delimiting
		 * &incidencePrevalenceDataset=1
		 * 
		 */
		// 
		// 
		BatchTaskConfig b = new BatchTaskConfig();
		b.name = null;

		int defaultIncidencePrevalenceDataset;
		int gridDefinitionId;
		int pollutantId;
		int populationId;
		int baselineId;
		String scenariosParam;
		List<Scenario> scenarios = new ArrayList<Scenario>();
		String hifGroupParam;
		List<Integer> hifGroupList;

		//boolean userPrefered; //If true, BenMAP will use the incidence/prevalence selected by the user even when there is another dataset which matches the demo groups better.
		Boolean preserveLegacyBehavior = true;

		try{
			// HIF group ids passed as a comma-separated list of integers
			hifGroupParam = ParameterUtil.getParameterValueAsString(request.raw().getParameter("groupIds"), "").replace(" ", "");
			hifGroupList = Stream.of(hifGroupParam.split(",")).mapToInt(Integer::parseInt).boxed().collect(Collectors.toList());

			defaultIncidencePrevalenceDataset = ParameterUtil.getParameterValueAsInteger(request.raw().getParameter("incidencePrevalenceDataset"), 0);
			pollutantId = ParameterUtil.getParameterValueAsInteger(request.raw().getParameter("pollutantId"), 0);
			baselineId = ParameterUtil.getParameterValueAsInteger(request.raw().getParameter("baselineId"), 0);
			populationId = ParameterUtil.getParameterValueAsInteger(request.raw().getParameter("populationId"), 0);
			gridDefinitionId = ParameterUtil.getParameterValueAsInteger(request.raw().getParameter("gridDefinitionId"), AirQualityApi.getAirQualityLayerGridId(baselineId));
			//userPrefered = ParameterUtil.getParameterValueAsBoolean(request.raw().getParameter("userPrefered"), false);

			// Scenario ids passed as a comma-separated list of strings. Each scenario represents an AQ surface ID and associated population years
			// They will be parsed further within the loop below
			scenariosParam = ParameterUtil.getParameterValueAsString(request.raw().getParameter("scenarios"), "");
			String[] scenariosSplit = scenariosParam.split(",");

			for(int i = 0; i < scenariosSplit.length; i++) {
				Scenario scenario = new Scenario();
				//Separate the AQ surface ID from the population years
				String[] scenarioComponents = scenariosSplit[i].split("\\|");
				
				scenario.id = Integer.parseInt(scenarioComponents[0]);
				scenario.name = AirQualityApi.getAirQualityLayerName(scenario.id);     
				List<Integer> years = Stream.of(scenarioComponents[1].split("~")).mapToInt(Integer::parseInt).boxed().collect(Collectors.toList());

				for(int j = 0; j < years.size(); j++) {
					ScenarioPopConfig tempPopConfig = new ScenarioPopConfig();
					tempPopConfig.popYear = years.get(j);
					//TODO: Will need to do this AFTER we have looked up the HIF groups and assigned the hifs instanceIds
//					for(int k = 0; k < hifIds.size(); k++) {
//						ScenarioHIFConfig tempHifConfig = new ScenarioHIFConfig();
//
//						tempPopConfig.scenarioHifConfigs.add(tempHifConfig);
//					}

					scenario.popConfigs.add(tempPopConfig);
				}
				scenarios.add(scenario);
			}

			for(int i = 0; i < hifGroupList.size(); i++) {
				BatchHIFGroup tempHifGroup = new BatchHIFGroup();
				tempHifGroup.id = hifGroupList.get(i);
				
			}


		} catch (NumberFormatException e) {
			e.printStackTrace();
			return CoreApi.getErrorResponseInvalidId(request, response);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return CoreApi.getErrorResponseInvalidId(request, response);
		} catch (Exception e) {
			e.printStackTrace();
			return CoreApi.getErrorResponse(request, response, 400, "Unable to process request");
			
		}
		
		
		Result<Record> hifGroupRecords = DSL.using(JooqUtil.getJooqConfiguration())
				.select(HEALTH_IMPACT_FUNCTION_GROUP.NAME
						, HEALTH_IMPACT_FUNCTION_GROUP.ID.as("groupId")
						, HEALTH_IMPACT_FUNCTION_GROUP.HELP_TEXT
						, HEALTH_IMPACT_FUNCTION.asterisk()
						, ENDPOINT_GROUP.NAME.as("endpoint_group_name")
						, ENDPOINT.NAME.as("endpoint_name")
						, RACE.NAME.as("race_name")
						, GENDER.NAME.as("gender_name")
						, ETHNICITY.NAME.as("ethnicity_name")
						)
				.from(HEALTH_IMPACT_FUNCTION_GROUP)
				.join(HEALTH_IMPACT_FUNCTION_GROUP_MEMBER).on(HEALTH_IMPACT_FUNCTION_GROUP.ID.eq(HEALTH_IMPACT_FUNCTION_GROUP_MEMBER.HEALTH_IMPACT_FUNCTION_GROUP_ID))
				.join(HEALTH_IMPACT_FUNCTION).on(HEALTH_IMPACT_FUNCTION_GROUP_MEMBER.HEALTH_IMPACT_FUNCTION_ID.eq(HEALTH_IMPACT_FUNCTION.ID))
				.join(ENDPOINT_GROUP).on(HEALTH_IMPACT_FUNCTION.ENDPOINT_GROUP_ID.eq(ENDPOINT_GROUP.ID))
				.join(ENDPOINT).on(HEALTH_IMPACT_FUNCTION.ENDPOINT_ID.eq(ENDPOINT.ID))
				.join(RACE).on(HEALTH_IMPACT_FUNCTION.RACE_ID.eq(RACE.ID))
				.join(GENDER).on(HEALTH_IMPACT_FUNCTION.GENDER_ID.eq(GENDER.ID))
				.join(ETHNICITY).on(HEALTH_IMPACT_FUNCTION.ETHNICITY_ID.eq(ETHNICITY.ID))
				.where(HEALTH_IMPACT_FUNCTION_GROUP.ID.in(hifGroupList)
						.and(HEALTH_IMPACT_FUNCTION.POLLUTANT_ID.eq(pollutantId))
						)
				.orderBy(HEALTH_IMPACT_FUNCTION_GROUP.NAME)
				.fetch();

		
		ObjectMapper mapper = new ObjectMapper();
		ArrayNode groups = mapper.createArrayNode();
		ObjectNode group = null;
		ArrayNode functions = null;
		Integer hifInstanceId = 1;
		
		BatchHIFGroup batchHifGroup = null;
		int currentGroupId = -1;
		
		try {
			for(Record r : hifGroupRecords) {
				if(currentGroupId != r.getValue(HEALTH_IMPACT_FUNCTION_GROUP.ID.as("groupId"))) {
					currentGroupId = r.getValue(HEALTH_IMPACT_FUNCTION_GROUP.ID.as("groupId"));
					
					batchHifGroup = new BatchHIFGroup();
					batchHifGroup.id = currentGroupId;
					batchHifGroup.name = r.getValue(HEALTH_IMPACT_FUNCTION_GROUP.NAME);
					//batchHifGroup.helpText = r.getValue(HEALTH_IMPACT_FUNCTION_GROUP.HELP_TEXT);
					b.batchHifGroups.add(batchHifGroup);
				}
				
				HIFConfig hifConfig = new HIFConfig();
				hifConfig.hifInstanceId = hifInstanceId++;
				hifConfig.hifId = r.getValue(HEALTH_IMPACT_FUNCTION.ID);		
				hifConfig.hifRecord = r.intoMap();
				
				//for each scenario and popyear, add this function instance with the appropriate incidence data
				for(Scenario scenario : scenarios) {
					for(ScenarioPopConfig popConfig : scenario.popConfigs) {
						ScenarioHIFConfig scenarioHIFConfig = new ScenarioHIFConfig();
						scenarioHIFConfig.hifInstanceId = hifConfig.hifInstanceId;

						HIFUtil.setIncidencePrevalence(hifConfig, scenario, popConfig, scenarioHIFConfig, defaultIncidencePrevalenceDataset, true);
						popConfig.scenarioHifConfigs.add(scenarioHIFConfig);
					}
				}
				batchHifGroup.hifs.add(hifConfig);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return CoreApi.getErrorResponse(request, response, 400, "Unable to build response"); 
		}
		b.gridDefinitionId = gridDefinitionId;
		b.pollutantId = pollutantId;
		b.pollutantName = PollutantApi.getPollutantName(pollutantId);
		b.popId = populationId;
		b.aqBaselineId = baselineId;
		b.aqScenarios = scenarios;
		b.preserveLegacyBehavior = preserveLegacyBehavior;
		return b;

	}
	
	/*
	 * Accepts a BatchTaskConfig in json format
	 * Uses the data to create tasks for one or more scenarios
	 * If successful, a header record will be added to task_batch and individual records for each scenario will be added to task_queue
	 */
	public static Object postBatchTask(Request request, Response response, Optional<UserProfile> userProfile) {
		String body = request.body();
		BatchTaskConfig batchTaskConfig = null;
		try {
			batchTaskConfig = objectMapper.readValue(body, BatchTaskConfig.class);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return CoreApi.getErrorResponseBadRequest(request, response);
		}
		
		// TODO: We have successfully parsed the object. Now, we need to validate the contents.
//		if(CoreApi.isValidTaskType(task.getType()) == false) {
//			return CoreApi.getErrorResponseBadRequest(request, response);
//		}

		// As an interim protection against overloading the system, users can only have a maximum of 10 pending or completed tasks total
//		int taskCount = TaskApi.getTotalTaskCountForUser(userProfile.get());			
//		int maxTasks = 20;
//		
//		String sMaxTaskPerUser = ApplicationUtil.getProperty("default.max.tasks.per.user"); 
//		
//		try {
//			maxTasks = Integer.parseInt(sMaxTaskPerUser);
//		} catch(NumberFormatException e) {
//			//If this is no set in the properties, we will use the default of 20.
//		}
//				
//		if(maxTasks != 0 && taskCount >= maxTasks) {
//			return CoreApi.getErrorResponse(request, response, 401, "You have reached the maximum of " + maxTasks + " tasks allowed per user. Please delete existing task results before submitting new tasks.");
//		}
//	
		
		String batchParameters;
		try {
			batchParameters = objectMapper.writeValueAsString(batchTaskConfig);
		} catch (JsonProcessingException e1) {
			e1.printStackTrace();
			return CoreApi.getErrorResponse(request, response, 400, "Unable to serialize batch task");
		} 

		//Insert the task_batch parent record
		TaskBatchRecord rec = DSL.using(JooqUtil.getJooqConfiguration())
		.insertInto(TASK_BATCH, TASK_BATCH.NAME, TASK_BATCH.PARAMETERS, TASK_BATCH.USER_ID, TASK_BATCH.SHARING_SCOPE)
		.values(
				batchTaskConfig.name
				, batchParameters
				, userProfile.get().getId()
				, Constants.SHARING_NONE
			)
		.returning(TASK_BATCH.ID)
		.fetchOne();
		Integer batchTaskId = rec.getId();
		
		if(batchTaskConfig.batchHifGroups.size() > 0) {
			//Create hif and valuation tasks
			//Create a HIFTaskConfig and ValuationTaskConfig templates for the task parameters
			HIFTaskConfig hifTaskConfig = new HIFTaskConfig();
			hifTaskConfig.aqBaselineId = batchTaskConfig.aqBaselineId;
			hifTaskConfig.popId = batchTaskConfig.popId;
			
			ValuationTaskConfig valuationTaskConfig = new ValuationTaskConfig();
			valuationTaskConfig.gridDefinitionId = batchTaskConfig.gridDefinitionId;
			valuationTaskConfig.useGrowthFactors = true;
			valuationTaskConfig.useInflationFactors = true;
			valuationTaskConfig.variableDatasetId = 1;
			
			//Combine all the selected HIFs into a big, flat list for processing
			for (BatchHIFGroup hifGroup : batchTaskConfig.batchHifGroups) {
				for (HIFConfig hifConfig : hifGroup.hifs) {
					hifTaskConfig.hifs.add(hifConfig);
					for (ValuationConfig valuationConfig : hifConfig.valuationFunctions) {
						valuationTaskConfig.valuationFunctions.add(valuationConfig);					
					}
				}
			}
			
			boolean hasValuation = valuationTaskConfig.valuationFunctions.size() > 0;
			
			// Finally, insert task_queue records (1 per AQ scenario and pop year combo)
			for (Scenario scenario : batchTaskConfig.aqScenarios) {
				hifTaskConfig.aqScenarioId = scenario.id;
				for (ScenarioPopConfig popScenario : scenario.popConfigs) {
					hifTaskConfig.popYear = popScenario.popYear;
					hifTaskConfig.name = batchTaskConfig.name + "-" + scenario.name + "-" + popScenario.popYear; // TODO:Figure out nicer name
					for (ScenarioHIFConfig hifScenario : popScenario.scenarioHifConfigs) {
						// TODO: Find a more efficient way of doing this?
						for (HIFConfig hifConfig : hifTaskConfig.hifs) {
							if (hifConfig.hifInstanceId.equals(hifScenario.hifInstanceId)) {
								hifConfig.incidenceYear = hifScenario.incidenceYear;
								hifConfig.prevalenceYear = hifScenario.prevalenceYear;
								
								//TODO: Only override here if needed
							    if(hifConfig.startAge == null) {
							    	hifConfig.startAge = (Integer) hifConfig.hifRecord.get("start_age"); 
							    }
							    if(hifConfig.endAge == null) {
							    	hifConfig.endAge = (Integer) hifConfig.hifRecord.get("end_age");  
							    }
							    if(hifConfig.race == null) {
							    	hifConfig.race = (Integer) hifConfig.hifRecord.get("race_id"); 
							    	hifConfig.incidenceRace = hifConfig.race;
							    	hifConfig.prevalenceRace = hifConfig.race;
							    }
							    if(hifConfig.ethnicity == null) {
							    	hifConfig.ethnicity = (Integer) hifConfig.hifRecord.get("ethnicity_id");
							    	hifConfig.incidenceEthnicity = hifConfig.ethnicity;
							    	hifConfig.prevalenceEthnicity = hifConfig.ethnicity;
							    }
							    if(hifConfig.gender == null) {
							    	hifConfig.gender = (Integer) hifConfig.hifRecord.get("gender_id");
							    	hifConfig.incidenceGender = hifConfig.gender;
							    	hifConfig.prevalenceGender = hifConfig.gender;
							    }
								
								break;
							}
						}
					}

					try {
						Task hifTask = new Task();
						hifTask.setUserIdentifier(userProfile.get().getId());
						hifTask.setType("HIF");
						hifTask.setBatchId(batchTaskId);
						hifTask.setName(hifTaskConfig.name);
						hifTask.setParameters(objectMapper.writeValueAsString(hifTaskConfig));
						String hifTaskUUID = UUID.randomUUID().toString(); 
						hifTask.setUuid(hifTaskUUID);
						TaskQueue.writeTaskToQueue(hifTask);
						
						//Only add the valuation task if there are valuation functions to run
						if(hasValuation) {
							valuationTaskConfig.hifTaskUuid = hifTaskUUID;
		
							Task valuationTask = new Task();
							valuationTask.setUserIdentifier(userProfile.get().getId());
							valuationTask.setType("Valuation");
							valuationTask.setBatchId(batchTaskId);
							valuationTask.setName(hifTaskConfig.name + "-Valuation");
							valuationTask.setParentUuid(valuationTaskConfig.hifTaskUuid);
							valuationTask.setParameters(objectMapper.writeValueAsString(valuationTaskConfig));
							String valuationTaskUUID = UUID.randomUUID().toString(); 
							valuationTask.setUuid(valuationTaskUUID);
							TaskQueue.writeTaskToQueue(valuationTask);	
						}
						
					} catch (JsonProcessingException e) {
						e.printStackTrace();
						return CoreApi.getErrorResponse(request, response, 400, "Unable to serialize task scenario");
					}
				}
			}			
		} else if(batchTaskConfig.batchExposureGroups.size() > 0) {
			//Create exposure tasks
			//Create a HIFTaskConfig and ValuationTaskConfig templates for the task parameters
			ExposureTaskConfig exposureTaskConfig = new ExposureTaskConfig();
			exposureTaskConfig.aqBaselineId = batchTaskConfig.aqBaselineId;
			exposureTaskConfig.popId = batchTaskConfig.popId;
			
			
			//Combine all the selected HIFs into a big, flat list for processing
			for (BatchExposureGroup exposureGroup : batchTaskConfig.batchExposureGroups) {
				for (ExposureConfig exposureConfig : exposureGroup.exposureConfigs) {
					exposureTaskConfig.exposureFunctions.add(exposureConfig);
				}
			}
			
			// Finally, insert task_queue records (1 per AQ scenario and pop year combo)
			for (Scenario scenario : batchTaskConfig.aqScenarios) {
				exposureTaskConfig.aqScenarioId = scenario.id;
				for (ScenarioPopConfig popScenario : scenario.popConfigs) {
					exposureTaskConfig.popYear = popScenario.popYear;
					exposureTaskConfig.name = batchTaskConfig.name + "-" + scenario.name + "-" + popScenario.popYear; // TODO:Figure out nicer name

					try {
						Task exposureTask = new Task();
						exposureTask.setUserIdentifier(userProfile.get().getId());
						exposureTask.setType("Exposure");
						exposureTask.setBatchId(batchTaskId);
						exposureTask.setName(exposureTaskConfig.name);
						exposureTask.setParameters(objectMapper.writeValueAsString(exposureTaskConfig));
						String hifTaskUUID = UUID.randomUUID().toString(); 
						exposureTask.setUuid(hifTaskUUID);
						TaskQueue.writeTaskToQueue(exposureTask);
						
					} catch (JsonProcessingException e) {
						e.printStackTrace();
						return CoreApi.getErrorResponse(request, response, 400, "Unable to serialize task scenario");
					}
				}
			}
		}

	
		return CoreApi.getSuccessResponse(request, response, 200, "Task was submitted");
	}


	public static ObjectNode getBatchTaskScenarios(Request request, Response response, Optional<UserProfile> userProfile) {	
		String userId = userProfile.get().getId();
		boolean showAll;
		int batchTaskId;

		ObjectMapper mapper = new ObjectMapper();
		ObjectNode data = mapper.createObjectNode();

		ArrayNode tasks = mapper.createArrayNode();
		ObjectNode task = mapper.createObjectNode();

		ArrayNode scenarios = mapper.createArrayNode();
		ObjectNode scenario = mapper.createObjectNode();
		List<Integer> scenarioIdList = new ArrayList<Integer>();


		showAll = ParameterUtil.getParameterValueAsBoolean(request.raw().getParameter("showAll"), false);
		batchTaskId = Integer.valueOf(request.params("id"));

			try {
				LocalDateTime now = LocalDateTime.now();
				try {
					Condition batchFilterCondition = DSL.trueCondition();
					
					batchFilterCondition = batchFilterCondition.and(TASK_BATCH.ID.eq(batchTaskId));

					// Skip the following for an admin user that wants to see all data
					if(!showAll || !CoreApi.isAdmin(userProfile)) {
						batchFilterCondition = batchFilterCondition.and(TASK_BATCH.USER_ID.eq(userId));
					}

					Result<Record> batchResult = DSL.using(JooqUtil.getJooqConfiguration()).select()
						.from(TASK_BATCH)
						.where(batchFilterCondition) 
						.orderBy(TASK_BATCH.ID.asc())
						.fetch();

					for(Record batchRecord : batchResult) {
						tasks = mapper.createArrayNode();
						data.put("batch_task_name", batchRecord.getValue(TASK_BATCH.NAME));
						data.put("batch_task_id", batchRecord.getValue(TASK_BATCH.ID));
						data.put("batch_task_user_id", batchRecord.getValue(TASK_BATCH.USER_ID));
						JSON batchParams = batchRecord.getValue(TASK_BATCH.PARAMETERS, JSON.class);
						ObjectMapper batchMapper = new ObjectMapper();
						JsonNode batchParamsNode = batchMapper.readTree(batchParams.data());
						data.put("aq_baseline_id", batchParamsNode.get("aqBaselineId").asText());
						String aqBaselineName = AirQualityApi.getAirQualityLayerName(Integer.valueOf(batchParamsNode.get("aqBaselineId").asText()));
						data.put("aq_baseline_name", aqBaselineName);
						data.put("pollutant_name", batchParamsNode.get("pollutantName").asText());


						Condition filterCondition = DSL.trueCondition();
						filterCondition = filterCondition.and(TASK_COMPLETE.TASK_BATCH_ID.equal(batchTaskId));
						Result<Record> result = DSL.using(JooqUtil.getJooqConfiguration()).select()
							.from(TASK_COMPLETE)
							.where(filterCondition) 
							.orderBy(TASK_COMPLETE.TASK_STARTED_DATE.asc())
							.fetch();

						for (Record record : result) {

							task = mapper.createObjectNode();

							task.put("task_name", record.getValue(TASK_COMPLETE.TASK_NAME));
							task.put("task_uuid", record.getValue(TASK_COMPLETE.TASK_UUID));
							task.put("task_type", record.getValue(TASK_COMPLETE.TASK_TYPE));
							task.put("task_user_id", record.getValue(TASK_COMPLETE.USER_ID));
							

							JsonNode json = null;
		
							try {
								json = objectMapper.readTree(record.getValue(TASK_COMPLETE.TASK_PARAMETERS));
							} catch (JsonMappingException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
								response.status(400);
								return null;
							} catch (JsonProcessingException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
								response.status(400);
								return null;
							} 
							
							if(record.getValue(TASK_COMPLETE.TASK_TYPE).equals("HIF")) {
								String aqScenarioId;
								String popYear;
								String aqScenarioName;
								try {
									aqScenarioId = json.get("aqScenarioId").asText();
									aqScenarioName = AirQualityApi.getAirQualityLayerName(Integer.valueOf(aqScenarioId));
									task.put("aq_scenario_name", aqScenarioName);
									popYear = json.get("popYear").asText();
									task.put("pop_year", popYear);
									scenarioIdList.add(json.get("aqScenarioId").asInt());
								} catch (NullPointerException e) {
									e.printStackTrace();
									response.status(400);
									return null;
								}
							} else if (record.getValue(TASK_COMPLETE.TASK_TYPE).equals("Valuation")) {
								task.put("task_parent_uuid", record.getValue(TASK_COMPLETE.TASK_PARENT_UUID));
							}
							
							tasks.add(task);
						}
					}

					Result<Record> scenarioRecords = DSL.using(JooqUtil.getJooqConfiguration()).select()
						.from(AIR_QUALITY_LAYER)
						.where(AIR_QUALITY_LAYER.ID.in(scenarioIdList)) 
						.orderBy(AIR_QUALITY_LAYER.ID.asc())
						.fetch();

					scenarios = mapper.createArrayNode();
					for(Record scenarioRecord : scenarioRecords) {
						scenario = mapper.createObjectNode();
						scenario.put("scenario_id", scenarioRecord.getValue(AIR_QUALITY_LAYER.ID));
						scenario.put("scenario_name", scenarioRecord.getValue(AIR_QUALITY_LAYER.NAME));
						scenarios.add(scenario);
					}

					data.set("tasks", tasks);
					data.set("scenarios", scenarios);
					data.put("success", true);

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

	/**
	 * 
	 * @param request
	 * @param response
	 * @param userProfile
	 * @return a fully populated batch task config that can be POSTed to /batch-tasks.
	 * 
	 */
	public static Object getBatchTaskConfigExampleHIF(Request request, Response response, Optional<UserProfile> userProfile) {		
		BatchTaskConfig b = new BatchTaskConfig();
		b.name = "Hello World";
		
		b.gridDefinitionId = 18; //County
		b.aqBaselineId = 15; //PM2.5_Annual_Baseline_12.35_Partial_Attain_PM NAAQS Proposal_2032
		b.popId = 40;
		b.pollutantName = "PM2.5";
		b.pollutantId = 6;
		b.preserveLegacyBehavior = true;
		//TODO: Add default incidence/prevalence group the user selected
		
		b.aqScenarios = new ArrayList<Scenario>();
		b.batchHifGroups = new ArrayList<BatchHIFGroup>();
		
		BatchHIFGroup batchHIFGroup = new BatchHIFGroup();
		batchHIFGroup.id = 5;
		batchHIFGroup.name = "Premature Death - Primary";
		//TODO: Add helpText to BatchHIFGroup
		
		// HIF #1 with 1 valuation function
		HIFConfig hifConfig = new HIFConfig();
		hifConfig.hifId = 1011;
		hifConfig.hifInstanceId = 1;
		hifConfig.hifRecord = HIFUtil.getFunctionDefinition(hifConfig.hifId).intoMap();
		hifConfig.incidence = 1;
		hifConfig.incidenceYear = 2030;
		
		ValuationConfig vfConfig = new ValuationConfig();
		vfConfig.hifId = hifConfig.hifId;
		vfConfig.hifInstanceId = hifConfig.hifInstanceId;
		vfConfig.vfId = 472;
		vfConfig.vfRecord = ValuationUtil.getFunctionDefinition(vfConfig.vfId).intoMap();

		hifConfig.valuationFunctions.add(vfConfig);
		batchHIFGroup.hifs.add(hifConfig);
		
		// HIF #2 with 1 valuation function
		hifConfig = new HIFConfig();
		hifConfig.hifId = 1012;
		hifConfig.hifInstanceId = 2;
		hifConfig.hifRecord = HIFUtil.getFunctionDefinition(hifConfig.hifId).intoMap();
		hifConfig.incidence = 1;
		hifConfig.incidenceYear = 2030;

		vfConfig = new ValuationConfig();
		vfConfig.hifId = hifConfig.hifId;
		vfConfig.hifInstanceId = hifConfig.hifInstanceId;
		vfConfig.vfId = 472;
		vfConfig.vfRecord = ValuationUtil.getFunctionDefinition(vfConfig.vfId).intoMap();

		hifConfig.valuationFunctions.add(vfConfig);
		batchHIFGroup.hifs.add(hifConfig);
		
		// HIF #3 with 1 valuation function
		hifConfig = new HIFConfig();
		hifConfig.hifId = 979;
		hifConfig.hifInstanceId = 3;
		hifConfig.hifRecord = HIFUtil.getFunctionDefinition(hifConfig.hifId).intoMap();
		hifConfig.incidence = 1;
		hifConfig.incidenceYear = 2030;

		vfConfig = new ValuationConfig();
		vfConfig.hifId = hifConfig.hifId;
		vfConfig.hifInstanceId = hifConfig.hifInstanceId;
		vfConfig.vfId = 472;
		vfConfig.vfRecord = ValuationUtil.getFunctionDefinition(vfConfig.vfId).intoMap();

		hifConfig.valuationFunctions.add(vfConfig);
		batchHIFGroup.hifs.add(hifConfig);
		
		b.batchHifGroups.add(batchHIFGroup);
		
		//CREATE THE SCENARIOS
		
		// *** AQ SCENARIO #1
		Scenario aqScenario = new Scenario();
		aqScenario.id = 24;
		aqScenario.name = "PM2.5_Annual_Policy_8.35_Partial_Attain_PM NAAQS Proposal_2032";

		ScenarioPopConfig popConfig = new ScenarioPopConfig();
		popConfig.popYear = 2030;
		
		ScenarioHIFConfig hifScenario = new ScenarioHIFConfig();
		hifScenario.hifInstanceId = 1;
		hifScenario.incidenceYear = popConfig.popYear;
		popConfig.scenarioHifConfigs.add(hifScenario);

		hifScenario = new ScenarioHIFConfig();
		hifScenario.hifInstanceId = 2;
		hifScenario.incidenceYear = popConfig.popYear;
		popConfig.scenarioHifConfigs.add(hifScenario);
		
		hifScenario = new ScenarioHIFConfig();
		hifScenario.hifInstanceId = 3;
		hifScenario.incidenceYear = popConfig.popYear;
		popConfig.scenarioHifConfigs.add(hifScenario);
		
		aqScenario.popConfigs.add(popConfig);

		popConfig = new ScenarioPopConfig();
		popConfig.popYear = 2035;
		
		hifScenario = new ScenarioHIFConfig();
		hifScenario.hifInstanceId = 1;
		hifScenario.incidenceYear = popConfig.popYear;
		popConfig.scenarioHifConfigs.add(hifScenario);

		hifScenario = new ScenarioHIFConfig();
		hifScenario.hifInstanceId = 2;
		hifScenario.incidenceYear = popConfig.popYear;
		popConfig.scenarioHifConfigs.add(hifScenario);
		
		hifScenario = new ScenarioHIFConfig();
		hifScenario.hifInstanceId = 3;
		hifScenario.incidenceYear = popConfig.popYear;
		popConfig.scenarioHifConfigs.add(hifScenario);
		
		aqScenario.popConfigs.add(popConfig);
		b.aqScenarios.add(aqScenario);
		
		// *** AQ SCENARIO #2
		aqScenario = new Scenario();
		aqScenario.id = 22;
		aqScenario.name = "PM2.5_Annual_Policy_9.35_Partial_Attain_PM NAAQS Proposal_2032";

		popConfig = new ScenarioPopConfig();
		popConfig.popYear = 2030;
		
		hifScenario = new ScenarioHIFConfig();
		hifScenario.hifInstanceId = 1;
		hifScenario.incidenceYear = popConfig.popYear;
		popConfig.scenarioHifConfigs.add(hifScenario);

		hifScenario = new ScenarioHIFConfig();
		hifScenario.hifInstanceId = 2;
		hifScenario.incidenceYear = popConfig.popYear;
		popConfig.scenarioHifConfigs.add(hifScenario);
		
		hifScenario = new ScenarioHIFConfig();
		hifScenario.hifInstanceId = 3;
		hifScenario.incidenceYear = popConfig.popYear;
		popConfig.scenarioHifConfigs.add(hifScenario);
		
		aqScenario.popConfigs.add(popConfig);

		popConfig = new ScenarioPopConfig();
		popConfig.popYear = 2035;
		
		hifScenario = new ScenarioHIFConfig();
		hifScenario.hifInstanceId = 1;
		hifScenario.incidenceYear = popConfig.popYear;
		popConfig.scenarioHifConfigs.add(hifScenario);

		hifScenario = new ScenarioHIFConfig();
		hifScenario.hifInstanceId = 2;
		hifScenario.incidenceYear = popConfig.popYear;
		popConfig.scenarioHifConfigs.add(hifScenario);
		
		hifScenario = new ScenarioHIFConfig();
		hifScenario.hifInstanceId = 3;
		hifScenario.incidenceYear = popConfig.popYear;
		popConfig.scenarioHifConfigs.add(hifScenario);
		
		aqScenario.popConfigs.add(popConfig);
		b.aqScenarios.add(aqScenario);
		
		return b;
	
	}
	
	/**
	 * 
	 * @param request
	 * @param response
	 * @param userProfile
	 * @return a fully populated batch task config that can be POSTed to /batch-tasks.
	 * 
	 */
	public static Object getBatchTaskConfigExampleExposure(Request request, Response response, Optional<UserProfile> userProfile) {		
		BatchTaskConfig b = new BatchTaskConfig();
		b.name = "Hello World";
		
		b.gridDefinitionId = 18; //County
		b.aqBaselineId = 15; //PM2.5_Annual_Baseline_12.35_Partial_Attain_PM NAAQS Proposal_2032
		b.popId = 40;
		b.pollutantName = "PM2.5";
		b.pollutantId = 6;
		
		b.aqScenarios = new ArrayList<Scenario>();
		b.batchExposureGroups = new ArrayList<BatchExposureGroup>();
		
		BatchExposureGroup batchExposureGroup = new BatchExposureGroup();
		batchExposureGroup.id = 5;
		batchExposureGroup.name = "PM NAAQS RIA 2022";
		//TODO: Add helpText to BatchHIFGroup
		
		// Exposure function #1
		ExposureConfig exposureConfig = new ExposureConfig();
		exposureConfig.efId = 1;
		exposureConfig.efInstanceId = 1;
		exposureConfig.efRecord = ExposureUtil.getFunctionDefinition(exposureConfig.efId).intoMap();
		
		batchExposureGroup.exposureConfigs.add(exposureConfig);
		
		// Exposure function #2
		exposureConfig = new ExposureConfig();
		exposureConfig.efId = 2;
		exposureConfig.efInstanceId = 2;
		exposureConfig.efRecord = ExposureUtil.getFunctionDefinition(exposureConfig.efId).intoMap();

		batchExposureGroup.exposureConfigs.add(exposureConfig);
		
		b.batchExposureGroups.add(batchExposureGroup);
		
		//CREATE THE SCENARIOS
		
		// *** AQ SCENARIO #1
		Scenario aqScenario = new Scenario();
		aqScenario.id = 24;
		aqScenario.name = "PM2.5_Annual_Policy_8.35_Partial_Attain_PM NAAQS Proposal_2032";

		ScenarioPopConfig popConfig = new ScenarioPopConfig();
		popConfig.popYear = 2030;		
		aqScenario.popConfigs.add(popConfig);

		popConfig = new ScenarioPopConfig();
		popConfig.popYear = 2035;		
		aqScenario.popConfigs.add(popConfig);
		b.aqScenarios.add(aqScenario);
		
		// *** AQ SCENARIO #2
		aqScenario = new Scenario();
		aqScenario.id = 22;
		aqScenario.name = "PM2.5_Annual_Policy_9.35_Partial_Attain_PM NAAQS Proposal_2032";

		popConfig = new ScenarioPopConfig();
		popConfig.popYear = 2030;		
		aqScenario.popConfigs.add(popConfig);

		popConfig = new ScenarioPopConfig();
		popConfig.popYear = 2035;
				
		aqScenario.popConfigs.add(popConfig);
		b.aqScenarios.add(aqScenario);
		
		return b;
	
	}
}

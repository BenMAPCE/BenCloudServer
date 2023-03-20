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
import java.util.ArrayList;

import javax.servlet.MultipartConfigElement;

import spark.Request;
import spark.Response;

import org.jetbrains.annotations.Nullable;
import org.jooq.DSLContext;
import org.jooq.JSON;
import org.jooq.JSONFormat;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.JSONFormat.RecordFormat;
import org.jooq.impl.DSL;
import org.pac4j.core.profile.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import gov.epa.bencloud.api.model.BatchTaskConfig;
import gov.epa.bencloud.api.model.HIFConfig;
import gov.epa.bencloud.api.model.HIFTaskConfig;
import gov.epa.bencloud.api.util.AirQualityUtil;
import gov.epa.bencloud.api.util.ApiUtil;
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
	 * 
	 * @param request
	 * @param response
	 * @param userProfile
	 * @return a partially populated batch task config based on the provided parameters.
	 * 
	 */
	public static Object getBatchTaskConfig(Request request, Response response, Optional<UserProfile> userProfile) {
		
		//TODO: Update this to create a partial batch task config to populate the Value of Effects page
		BatchTaskConfig b = new BatchTaskConfig();
		b.name = "Hello World";

		int defaultIncidencePrevalenceDataset;
		int gridDefinitionId;
		int pollutantId;
		String pollutantName;
		int populationId;
		int baselineId;
		String hifIdsParam;
		List<Integer> hifIds;
		String scenarioIdsParam;
		List<Integer> scenarioIdsList;
		String scenarioNamesParam;
		List<String> scenarioNamesList;
		String popYearsParam;
		List<String> popYearsList;
		List<Scenario> scenarios = new ArrayList<Scenario>();
		List<BatchHIFGroup> hifGroups = new ArrayList<BatchHIFGroup>();	
		String hifGroupParam;
		List<Integer> hifGroupList;
		boolean userPrefered; //If true, BenMAP will use the incidence/prevalence selected by the user even when there is another dataset which matches the demo groups better.
		Boolean preserveLegacyBehavior = true;

		try{
			hifIdsParam = ParameterUtil.getParameterValueAsString(request.raw().getParameter("hifIds"), "");
			hifIds = Stream.of(hifIdsParam.split(",")).mapToInt(Integer::parseInt).boxed().collect(Collectors.toList());
			defaultIncidencePrevalenceDataset = ParameterUtil.getParameterValueAsInteger(request.raw().getParameter("incidencePrevalenceDataset"), 0);
			pollutantId = ParameterUtil.getParameterValueAsInteger(request.raw().getParameter("pollutantId"), 0);
			baselineId = ParameterUtil.getParameterValueAsInteger(request.raw().getParameter("baselineId"), 0);
			userPrefered = ParameterUtil.getParameterValueAsBoolean(request.raw().getParameter("userPrefered"), false);

			// Need to figure out how to structure all of this into a multi-dimensional object in the front end,
			// and break it into component parts to use here


			// Scenario-specific data (ids, names, years) are passed in order (nth index of each contains the data for one scenario)
			// In front-end, ids are stored as an array, names are stored as a map (key = id, value = name),
			//		popYears are stored as a map (key = id, value = array of years)

			// Scenario ids passed as a comma-separated list of integers
			scenarioIdsParam = String.valueOf(request.params("scenarioIds").replace(" ", ""));
			scenarioIdsList = Stream.of(scenarioIdsParam.split(",")).mapToInt(Integer::parseInt).boxed().collect(Collectors.toList());

			// Scenario ids passed as a tilde-separated list of names
			scenarioNamesParam = String.valueOf(request.params("scenarioNames").replace(" ", ""));
			scenarioNamesList = Stream.of(scenarioIdsParam.split("~")).collect(Collectors.toList());

			// Population years passed as a tilde-separated list of comma-separated years
			popYearsParam = String.valueOf(request.params("popYears").replace(" ", ""));
			popYearsList = Stream.of(scenarioIdsParam.split("~")).collect(Collectors.toList());
			
			// HIF group ids passed as a comma-separated list of integers
			hifGroupParam = String.valueOf(request.params("scenarioIds").replace(" ", ""));
			hifGroupList = Stream.of(hifGroupParam.split(",")).mapToInt(Integer::parseInt).boxed().collect(Collectors.toList());

			for(int i = 0; i < scenarioIdsList.size(); i++) {
				Scenario tempScenario = new Scenario();
				tempScenario.id = scenarioIdsList.get(i);
				tempScenario.name = scenarioNamesList.get(i);
				List<Integer> years = Stream.of(popYearsList.get(i).split(",")).mapToInt(Integer::parseInt).boxed().collect(Collectors.toList());

				for(int j = 0; j < years.size(); j++) {
					ScenarioPopConfig tempPopConfig = new ScenarioPopConfig();
					tempPopConfig.popYear = years.get(j);
					for(int k = 0; k < hifIds.size(); k++) {
						ScenarioHIFConfig tempHifConfig = new ScenarioHIFConfig();

						tempPopConfig.scenarioHifConfigs.add(tempHifConfig);
					}

					tempScenario.popConfigs.add(tempPopConfig);
				}
				scenarios.add(tempScenario);
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
		}
		
		
		Result<Record> hifGroupRecords = DSL.using(JooqUtil.getJooqConfiguration())
				.select(HEALTH_IMPACT_FUNCTION_GROUP.NAME
						, HEALTH_IMPACT_FUNCTION_GROUP.ID
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
		int currentGroupId = -1;
		
		for(Record r : hifGroupRecords) {
			if(currentGroupId != r.getValue(HEALTH_IMPACT_FUNCTION_GROUP.ID)) {
				currentGroupId = r.getValue(HEALTH_IMPACT_FUNCTION_GROUP.ID);
				group = mapper.createObjectNode();
				group.put("id", currentGroupId);
				group.put("name", r.getValue(HEALTH_IMPACT_FUNCTION_GROUP.NAME));
				group.put("help_text", r.getValue(HEALTH_IMPACT_FUNCTION_GROUP.HELP_TEXT));
				functions = group.putArray("functions");
				groups.add(group);
			}
			
			ObjectNode function = mapper.createObjectNode();
			function.put("id", r.getValue(HEALTH_IMPACT_FUNCTION.ID));
			function.put("health_impact_function_dataset_id",r.getValue(HEALTH_IMPACT_FUNCTION.HEALTH_IMPACT_FUNCTION_DATASET_ID));
			function.put("endpoint_group_id",r.getValue(HEALTH_IMPACT_FUNCTION.ENDPOINT_GROUP_ID));
			function.put("endpoint_id",r.getValue(HEALTH_IMPACT_FUNCTION.ENDPOINT_ID));
			function.put("pollutant_id",r.getValue(HEALTH_IMPACT_FUNCTION.POLLUTANT_ID));
			function.put("metric_id",r.getValue(HEALTH_IMPACT_FUNCTION.METRIC_ID));
			function.put("seasonal_metric_id",r.getValue(HEALTH_IMPACT_FUNCTION.SEASONAL_METRIC_ID));
			function.put("metric_statistic",r.getValue(HEALTH_IMPACT_FUNCTION.METRIC_STATISTIC));
			function.put("author",r.getValue(HEALTH_IMPACT_FUNCTION.AUTHOR));
			function.put("function_year",r.getValue(HEALTH_IMPACT_FUNCTION.FUNCTION_YEAR));
			function.put("location",r.getValue(HEALTH_IMPACT_FUNCTION.LOCATION));
			function.put("other_pollutants",r.getValue(HEALTH_IMPACT_FUNCTION.OTHER_POLLUTANTS));
			function.put("qualifier",r.getValue(HEALTH_IMPACT_FUNCTION.QUALIFIER));
			function.put("reference",r.getValue(HEALTH_IMPACT_FUNCTION.REFERENCE));
			function.put("start_age",r.getValue(HEALTH_IMPACT_FUNCTION.START_AGE));
			function.put("end_age",r.getValue(HEALTH_IMPACT_FUNCTION.END_AGE));
			function.put("function_text",r.getValue(HEALTH_IMPACT_FUNCTION.FUNCTION_TEXT));
			function.put("variable_dataset_id",r.getValue(HEALTH_IMPACT_FUNCTION.VARIABLE_DATASET_ID));
			function.put("beta",r.getValue(HEALTH_IMPACT_FUNCTION.BETA));
			function.put("dist_beta",r.getValue(HEALTH_IMPACT_FUNCTION.DIST_BETA));
			function.put("p1_beta",r.getValue(HEALTH_IMPACT_FUNCTION.P1_BETA));
			function.put("p2_beta",r.getValue(HEALTH_IMPACT_FUNCTION.P2_BETA));
			function.put("val_a",r.getValue(HEALTH_IMPACT_FUNCTION.VAL_A));
			function.put("name_a",r.getValue(HEALTH_IMPACT_FUNCTION.NAME_A));
			function.put("val_b",r.getValue(HEALTH_IMPACT_FUNCTION.VAL_B));
			function.put("name_b",r.getValue(HEALTH_IMPACT_FUNCTION.NAME_B));
			function.put("val_c",r.getValue(HEALTH_IMPACT_FUNCTION.VAL_C));
			function.put("name_c",r.getValue(HEALTH_IMPACT_FUNCTION.NAME_C));
			function.put("baseline_function_text",r.getValue(HEALTH_IMPACT_FUNCTION.BASELINE_FUNCTION_TEXT));
			function.put("race_id",r.getValue(HEALTH_IMPACT_FUNCTION.RACE_ID));
			function.put("gender_id",r.getValue(HEALTH_IMPACT_FUNCTION.GENDER_ID));
			function.put("ethnicity_id",r.getValue(HEALTH_IMPACT_FUNCTION.ETHNICITY_ID));
			function.put("start_day",r.getValue(HEALTH_IMPACT_FUNCTION.START_DAY));
			function.put("end_day",r.getValue(HEALTH_IMPACT_FUNCTION.END_DAY));
			function.put("endpoint_group_name",r.getValue("endpoint_group_name",String.class));
			function.put("endpoint_name",r.getValue("endpoint_name", String.class));
			function.put("race_name",r.getValue("race_name", String.class));
			function.put("gender_name",r.getValue("gender_name", String.class));
			function.put("ethnicity_name",r.getValue("ethnicity_name", String.class));
			
			//This will select the most appropriate incidence/prevalence dataset and year based on user selection and function definition
			//HIFUtil.setIncidencePrevalence(function, popYear, defaultIncidencePrevalenceDataset,r.getValue(HEALTH_IMPACT_FUNCTION.INCIDENCE_DATASET_ID), r.getValue(HEALTH_IMPACT_FUNCTION.PREVALENCE_DATASET_ID), userPrefered);
			
			functions.add(function);
			
		}
		//b.gridDefinitionId = gridDefinitionId;
		b.pollutantId = pollutantId;
		//b.pollutantName = pollutantName;
		//b.popId = populationId;
		b.aqBaselineId = baselineId;
		b.aqScenarios = scenarios;
		b.preserveLegacyBehavior = preserveLegacyBehavior;
		b.batchHifGroups = hifGroups;
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
					
				} catch (JsonProcessingException e) {
					e.printStackTrace();
					return CoreApi.getErrorResponse(request, response, 400, "Unable to serialize task scenario");
				}
			}
		}
	
		return CoreApi.getSuccessResponse(request, response, 200, "Task was submitted");
	}



	/**
	 * 
	 * @param request
	 * @param response
	 * @param userProfile
	 * @return a fully populated batch task config that can be POSTed to /batch-tasks.
	 * 
	 */
	public static Object getBatchTaskConfigExample(Request request, Response response, Optional<UserProfile> userProfile) {		
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
}

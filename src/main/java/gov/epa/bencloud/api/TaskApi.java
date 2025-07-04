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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import javax.servlet.MultipartConfigElement;

import spark.Request;
import spark.Response;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.JSON;
import org.jooq.JSONFormat;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Record17;
import org.jooq.Record21;
import org.jooq.Result;
import org.jooq.Table;
import org.jooq.exception.DataAccessException;
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
import gov.epa.bencloud.api.model.ExposureConfig;
import gov.epa.bencloud.api.model.ExposureTaskConfig;
import gov.epa.bencloud.api.model.ExposureTaskLog;
import gov.epa.bencloud.api.model.HIFConfig;
import gov.epa.bencloud.api.model.HIFTaskConfig;
import gov.epa.bencloud.api.model.HIFTaskLog;
import gov.epa.bencloud.api.util.ApiUtil;
import gov.epa.bencloud.api.util.ExposureUtil;
import gov.epa.bencloud.api.util.HIFUtil;
import gov.epa.bencloud.api.util.ValuationUtil;
import gov.epa.bencloud.server.database.JooqUtil;
import gov.epa.bencloud.server.database.jooq.data.tables.records.GetExposureResultsRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.GetHifResultsRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.GetValuationResultsRecord;
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
import gov.epa.bencloud.api.model.ValuationTaskLog;
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
			response.status(409);
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
		int pendingCount = create.fetchCount(TASK_QUEUE
		, TASK_QUEUE.USER_ID.eq(profile.getId()).and(TASK_QUEUE.TASK_TYPE.in("HIF", "Valuation","Exposure")));
		int completedCount = create.fetchCount(TASK_COMPLETE
		, TASK_COMPLETE.USER_ID.eq(profile.getId()).and(TASK_COMPLETE.TASK_TYPE.in("HIF", "Valuation","Exposure")));
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
		 * &valuationSelection=Use EPA's current default values
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
		List<Integer> hifGroupList = new ArrayList<Integer>();
		String efGroupParam;
		List<Integer> efGroupList = new ArrayList<Integer>();
		String valuationSelection;
		
		//boolean userPrefered; //If true, BenMAP will use the incidence/prevalence selected by the user even when there is another dataset which matches the demo groups better.
		Boolean preserveLegacyBehavior = true;

		try{
			// HIF group ids passed as a comma-separated list of integers
			hifGroupParam = ParameterUtil.getParameterValueAsString(request.raw().getParameter("hifGroupIds"), "").replace(" ", "");
			if(!hifGroupParam.equals("")) {
				hifGroupList = Stream.of(hifGroupParam.split(",")).mapToInt(Integer::parseInt).boxed().collect(Collectors.toList());
			}

			// EF group ids passed as a comma-separated list of integers
			efGroupParam = ParameterUtil.getParameterValueAsString(request.raw().getParameter("efGroupIds"), "").replace(" ", "");
			if(!efGroupParam.equals("")) {
				efGroupList = Stream.of(efGroupParam.split(",")).mapToInt(Integer::parseInt).boxed().collect(Collectors.toList());
			}

			defaultIncidencePrevalenceDataset = ParameterUtil.getParameterValueAsInteger(request.raw().getParameter("incidencePrevalenceDataset"), 0);
			valuationSelection = ParameterUtil.getParameterValueAsString(request.raw().getParameter("valuationSelection"), "");
			pollutantId = ParameterUtil.getParameterValueAsInteger(request.raw().getParameter("pollutantId"), 0);
			baselineId = ParameterUtil.getParameterValueAsInteger(request.raw().getParameter("baselineId"), 0);
			populationId = ParameterUtil.getParameterValueAsInteger(request.raw().getParameter("populationId"), 0);
			gridDefinitionId = ParameterUtil.getParameterValueAsInteger(request.raw().getParameter("gridDefinitionId"), AirQualityApi.getAirQualityLayerGridId(baselineId));
			//userPrefered = ParameterUtil.getParameterValueAsBoolean(request.raw().getParameter("userPrefered"), false);

			// TODO: Add logic to determine if epa_standard=true should be handled
			
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

			for(int i = 0; i < efGroupList.size(); i++) {
				BatchExposureGroup tempExposureGroup = new BatchExposureGroup();
				tempExposureGroup.id = efGroupList.get(i);
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

		Record1<Integer> baselineMetricRecord = DSL.using(JooqUtil.getJooqConfiguration())
				.select(AIR_QUALITY_LAYER_METRICS.METRIC_ID)
				.from(AIR_QUALITY_LAYER_METRICS)
				.where(AIR_QUALITY_LAYER_METRICS.AIR_QUALITY_LAYER_ID.eq(baselineId))
				.fetchOne();

		Integer baselineMetricId = baselineMetricRecord.value1();
		
		
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
						, POLLUTANT_METRIC.NAME.as("metric_name")
						, SEASONAL_METRIC.NAME.as("seasonal_metric_name")
						, STATISTIC_TYPE.NAME.as("metric_statistic_name")
						)
				.from(HEALTH_IMPACT_FUNCTION_GROUP)
				.join(HEALTH_IMPACT_FUNCTION_GROUP_MEMBER).on(HEALTH_IMPACT_FUNCTION_GROUP.ID.eq(HEALTH_IMPACT_FUNCTION_GROUP_MEMBER.HEALTH_IMPACT_FUNCTION_GROUP_ID))
				.join(HEALTH_IMPACT_FUNCTION).on(HEALTH_IMPACT_FUNCTION_GROUP_MEMBER.HEALTH_IMPACT_FUNCTION_ID.eq(HEALTH_IMPACT_FUNCTION.ID))
				.join(ENDPOINT_GROUP).on(HEALTH_IMPACT_FUNCTION.ENDPOINT_GROUP_ID.eq(ENDPOINT_GROUP.ID))
				.join(ENDPOINT).on(HEALTH_IMPACT_FUNCTION.ENDPOINT_ID.eq(ENDPOINT.ID))
				.join(RACE).on(HEALTH_IMPACT_FUNCTION.RACE_ID.eq(RACE.ID))
				.join(GENDER).on(HEALTH_IMPACT_FUNCTION.GENDER_ID.eq(GENDER.ID))
				.join(ETHNICITY).on(HEALTH_IMPACT_FUNCTION.ETHNICITY_ID.eq(ETHNICITY.ID))
				.leftJoin(POLLUTANT_METRIC).on(HEALTH_IMPACT_FUNCTION.METRIC_ID.eq(POLLUTANT_METRIC.ID))
				.leftJoin(SEASONAL_METRIC).on(HEALTH_IMPACT_FUNCTION.SEASONAL_METRIC_ID.eq(SEASONAL_METRIC.ID))
				.leftJoin(STATISTIC_TYPE).on(HEALTH_IMPACT_FUNCTION.METRIC_STATISTIC.eq(STATISTIC_TYPE.ID))
				.where(HEALTH_IMPACT_FUNCTION_GROUP.ID.in(hifGroupList)
						.and(HEALTH_IMPACT_FUNCTION.POLLUTANT_ID.eq(pollutantId))
						.and(HEALTH_IMPACT_FUNCTION.METRIC_ID.contains(baselineMetricId))
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
				hifConfig.startAge = r.getValue(HEALTH_IMPACT_FUNCTION.START_AGE);
				hifConfig.endAge = r.getValue(HEALTH_IMPACT_FUNCTION.END_AGE);
				
				//If the user has specified a default valuation list, add it for this HIF here
				if(valuationSelection != null && !valuationSelection.isBlank()) {
					ValuationUtil.populateValuationFunctions(hifConfig, valuationSelection);					
				}

				//for each scenario and popyear, add this function instance with the appropriate incidence data
				for(Scenario scenario : scenarios) {
					for(ScenarioPopConfig popConfig : scenario.popConfigs) {
						ScenarioHIFConfig scenarioHIFConfig = new ScenarioHIFConfig();
						scenarioHIFConfig.hifInstanceId = hifConfig.hifInstanceId;

						HIFUtil.setIncidencePrevalence(hifConfig, scenario, popConfig, scenarioHIFConfig, defaultIncidencePrevalenceDataset, true, populationId, userProfile);
						popConfig.scenarioHifConfigs.add(scenarioHIFConfig);
					}
				}
				batchHifGroup.hifs.add(hifConfig);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return CoreApi.getErrorResponse(request, response, 400, "Unable to build response"); 
		}

		Result<Record> exposureGroupRecords = DSL.using(JooqUtil.getJooqConfiguration())
				.select(EXPOSURE_FUNCTION_GROUP.NAME
						, EXPOSURE_FUNCTION_GROUP.ID.as("groupId")
						, EXPOSURE_FUNCTION_GROUP.HELP_TEXT
						, EXPOSURE_FUNCTION.asterisk()
						, RACE.NAME.as("race_name")
						, GENDER.NAME.as("gender_name")
						, ETHNICITY.NAME.as("ethnicity_name")
						)
				.from(EXPOSURE_FUNCTION_GROUP)
				.join(EXPOSURE_FUNCTION_GROUP_MEMBER).on(EXPOSURE_FUNCTION_GROUP.ID.eq(EXPOSURE_FUNCTION_GROUP_MEMBER.EXPOSURE_FUNCTION_GROUP_ID))
				.join(EXPOSURE_FUNCTION).on(EXPOSURE_FUNCTION_GROUP_MEMBER.EXPOSURE_FUNCTION_ID.eq(EXPOSURE_FUNCTION.ID))
				.join(RACE).on(EXPOSURE_FUNCTION.RACE_ID.eq(RACE.ID))
				.join(GENDER).on(EXPOSURE_FUNCTION.GENDER_ID.eq(GENDER.ID))
				.join(ETHNICITY).on(EXPOSURE_FUNCTION.ETHNICITY_ID.eq(ETHNICITY.ID))
				.where(EXPOSURE_FUNCTION_GROUP.ID.in(efGroupList))
				.orderBy(EXPOSURE_FUNCTION_GROUP.NAME)
				.fetch();

		Integer efInstanceId = 1;
		
		BatchExposureGroup batchExposureGroup = null;
		currentGroupId = -1;

		try {
			for(Record r : exposureGroupRecords) {
				if(currentGroupId != r.getValue(EXPOSURE_FUNCTION_GROUP.ID.as("groupId"))) {
					currentGroupId = r.getValue(EXPOSURE_FUNCTION_GROUP.ID.as("groupId"));
					
					batchExposureGroup = new BatchExposureGroup();
					batchExposureGroup.id = currentGroupId;
					batchExposureGroup.name = r.getValue(EXPOSURE_FUNCTION_GROUP.NAME);
					//batchExposureGroup.helpText = r.getValue(EXPOSURE_FUNCTION_GROUP.HELP_TEXT);
					b.batchExposureGroups.add(batchExposureGroup);
				}
				
				ExposureConfig efConfig = new ExposureConfig();
				efConfig.efInstanceId = efInstanceId++;
				efConfig.efId = r.getValue(EXPOSURE_FUNCTION.ID);		
				efConfig.efRecord = r.intoMap();
				
				//for each scenario and popyear, add this function instance with the appropriate incidence data
				for(Scenario scenario : scenarios) {
					for(ScenarioPopConfig popConfig : scenario.popConfigs) {
						ScenarioHIFConfig scenarioHIFConfig = new ScenarioHIFConfig();
						scenarioHIFConfig.hifInstanceId = efConfig.efInstanceId;
						popConfig.scenarioHifConfigs.add(scenarioHIFConfig);
					}
				}
				batchExposureGroup.exposureConfigs.add(efConfig);
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

		// As an interim protection against overloading the system, users can only have a maximum of 40 pending or completed tasks total
		int taskCount = TaskApi.getTotalTaskCountForUser(userProfile.get());			
		int maxTasks = 40;
		int maxEpaTasks = 1000;
		
		String sMaxTaskPerUser = ApplicationUtil.getProperty("default.max.tasks.per.user"); 
		String sMaxTaskPerEpaUser = ApplicationUtil.getProperty("default.max.tasks.per.epa.user"); 
		
		try {
			maxTasks = Integer.parseInt(sMaxTaskPerUser);
		} catch(NumberFormatException e) {
			//If this is not set in the properties, we will use the default.
		}
				
		try {
			maxEpaTasks = Integer.parseInt(sMaxTaskPerEpaUser);
		} catch(NumberFormatException e) {
			//If this is not set in the properties, we will use the default.
		}

		// If user is part of epa.gov, use theEpaUser max count instead
		if(CoreApi.getUserEmail(request, response, userProfile).toLowerCase().endsWith("epa.gov")) {
			maxTasks = maxEpaTasks;
		}
		
		if(maxTasks != 0 && taskCount >= maxTasks) {
			return CoreApi.getErrorResponse(request, response, 405, "You have reached the maximum of " + maxTasks + " task scenarios allowed per user. Please delete existing task results before submitting new tasks. You can save the current configuration as a template and return to it later.");
		}
	
		
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

		//Some functions are present across multiple endpoint groups
		//Track all health impact and valuation functions to 
		// avoid running the same functions multiple times
		Set<Integer> allHifs = new HashSet<Integer>();
		
		
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
			//TODO: This is temporarily hard coded to test with both 2010 and 2020 datasets in the database. In the future, we want to allow users to select variable dataset
			if (hifTaskConfig.popId == 50 || hifTaskConfig.popId == 51 || hifTaskConfig.popId == 52 || hifTaskConfig.popId == 53) {
				valuationTaskConfig.variableDatasetId = 2;
			}
			
			
			//Combine all the selected HIFs into a big, flat list for processing
			for (BatchHIFGroup hifGroup : batchTaskConfig.batchHifGroups) {
				for (HIFConfig hifConfig : hifGroup.hifs) {
					Set<Integer> allValuationFunctions = new HashSet<Integer>();
					if(allHifs.add(hifConfig.hifId)) {
						hifTaskConfig.hifs.add(hifConfig);
						//Update 09/24 - prevent valuation function duplication if the hif config is already processed
						for (ValuationConfig valuationConfig : hifConfig.valuationFunctions) {
							if(allValuationFunctions.add(valuationConfig.vfId)) {
								valuationTaskConfig.valuationFunctions.add(valuationConfig);
							}
						}
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

						Record1<String> metricName = 
						DSL.using(JooqUtil.getJooqConfiguration())
							.select(
									POLLUTANT_METRIC.NAME
									)
							.from(POLLUTANT_METRIC)
							.join(AIR_QUALITY_LAYER_METRICS).on(AIR_QUALITY_LAYER_METRICS.METRIC_ID.eq(POLLUTANT_METRIC.ID))
							.join(AIR_QUALITY_LAYER).on(AIR_QUALITY_LAYER.ID.eq(AIR_QUALITY_LAYER_METRICS.AIR_QUALITY_LAYER_ID))				
							.where(AIR_QUALITY_LAYER.ID.eq(batchParamsNode.get("aqBaselineId").asInt()))
							.fetchOne();

						data.put("task_metric_name", metricName.value1());
						data.put("pollutant_name", batchParamsNode.get("pollutantName").asText());
						int valuationGridId = batchParamsNode.get("gridDefinitionId").asInt();
						data.put("valuation_grid_id", valuationGridId);
						data.put("valuation_grid_name", GridDefinitionApi.getGridDefinitionName(valuationGridId));

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
							
							if(record.getValue(TASK_COMPLETE.TASK_TYPE).equals("HIF") || record.getValue(TASK_COMPLETE.TASK_TYPE).equals("Exposure")) {
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


//
	public static void getResultExport(Request request, Response response, Optional<UserProfile> userProfile) {
//		 GET results as a zip file from an analysis
//		 PARAMETERS:
//		   :id (batch task id)
//		   includeHealthImpact (boolean, default to false)
//		   includeValuation (boolean, default to false)
//		   includeExposure (boolean, default to false)
//		   gridId= (comma delimited list. aggregate the results to one or more grid definition)
		// taskUuid=(optional. since there is not a defined class for "scenario", we use parent-child relationship to pull all relevant results)
		// uuidType = (optional. examples: "H", "V", "E")
	   
		
		
		Integer batchId;
		int[] gridIds;
		Boolean includeHealthImpact = false;
		Boolean includeValuation = false;
		Boolean includeExposure = false;
		String taskUuid="";
		String uuidType = "";
		try {
			String idParam = String.valueOf(request.params("id"));
			batchId = Integer.valueOf(idParam);
			includeHealthImpact = ParameterUtil.getParameterValueAsBoolean(request.raw().getParameter("includeHealthImpact").equals("1")?"true":request.raw().getParameter("includeHealthImpact"), false);
			includeValuation = ParameterUtil.getParameterValueAsBoolean(request.raw().getParameter("includeValuation").equals("1")?"true":request.raw().getParameter("includeValuation"), false);
			includeExposure = ParameterUtil.getParameterValueAsBoolean(request.raw().getParameter("includeExposure").equals("1")?"true":request.raw().getParameter("includeExposure"), false);
			taskUuid = ParameterUtil.getParameterValueAsString(request.raw().getParameter("taskUuid"),"");
			uuidType = ParameterUtil.getParameterValueAsString(request.raw().getParameter("uuidType"),"");			
			
			String gridIdParam = ParameterUtil.getParameterValueAsString(request.raw().getParameter("gridId"), "");

			if(gridIdParam==null || gridIdParam.equals("")){
				gridIds=null;
			}else {
				String[] gridIdParamArr = gridIdParam.split(",");
				gridIds = new int[gridIdParamArr.length];
				for (int i = 0; i < gridIdParamArr.length; i++) {
					gridIds[i] = Integer.parseInt(gridIdParamArr[i]);
		        }
			}				


		} catch (NumberFormatException e) {
			e.printStackTrace();
			CoreApi.getErrorResponseInvalidId(request, response);
			return;
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			CoreApi.getErrorResponseInvalidId(request, response);
			return;
		}
		
		BatchTaskConfig batchTaskConfig = getTaskBatchConfigFromDb(batchId);
		if(batchTaskConfig==null) {
			response.status(400);
			return;
		}
		
		// If a gridId wasn't provided, look up the baseline AQ grid grid for this resultset
		try {
			if(gridIds == null) {
				gridIds = new int[] {batchTaskConfig.gridDefinitionId.intValue()};
			}
		} catch (NullPointerException e) {
			e.printStackTrace();
			response.status(400);
			return;
		}
					
		//set zip file name
		String taskBatchFileName = ApplicationUtil.replaceNonValidCharacters(batchTaskConfig.name);
		StringBuilder batchTaskLog = new StringBuilder(); 
		
		response.header("Content-Disposition", "attachment; filename=" + taskBatchFileName + ".zip");
		response.header("Access-Control-Expose-Headers", "Content-Disposition");
		response.type("application/zip");
		
		// Get response output stream
		OutputStream responseOutputStream;
		ZipOutputStream zipStream;
		
		try {
			responseOutputStream = response.raw().getOutputStream();
			
			// Stream .ZIP file to response
			zipStream = new ZipOutputStream(responseOutputStream);
		} catch (java.io.IOException e1) {
			log.error("Error getting output stream", e1);
			return;
		}
		
//		For now we we export EITHER exposure OR HIF/Valuation results. We may want to change the logic in the future.
		if(includeExposure) {
			includeHealthImpact=false;
			includeValuation=false;
		}
		
		if(includeExposure) {
			//Exposure results
			DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());
			//get valuation task ids 
			List<Integer> exposureResultDatasetIds;
			if(uuidType.equals("E")) {
				//export current scenario
				exposureResultDatasetIds = create.select()
						.from(EXPOSURE_RESULT_DATASET)
						.join(TASK_COMPLETE).on(EXPOSURE_RESULT_DATASET.TASK_UUID.eq(TASK_COMPLETE.TASK_UUID))
						.where(TASK_COMPLETE.TASK_UUID.eq(taskUuid))
						.fetch(EXPOSURE_RESULT_DATASET.ID);
			}
			else {
				//export all scenarios in this batch task
				exposureResultDatasetIds = create.select()
						.from(EXPOSURE_RESULT_DATASET)
						.join(TASK_COMPLETE).on(EXPOSURE_RESULT_DATASET.TASK_UUID.eq(TASK_COMPLETE.TASK_UUID))
						.where(TASK_COMPLETE.TASK_BATCH_ID.eq(batchId))
						.fetch(EXPOSURE_RESULT_DATASET.ID);
			}
			
			
			//Loop through each function and each grid definition
			for(int exposureResultDatasetId : exposureResultDatasetIds) {
				//csv file name
				String taskFileName = ApplicationUtil.replaceNonValidCharacters(ExposureApi.getExposureTaskConfigFromDb(exposureResultDatasetId).name);
				for(int i=0; i < gridIds.length; i++) {
					Result<?> efRecordsClean = null;
					//Move the following to ValuationApi.java? 
					//valuationRecordsClean = ValuationApi.getValuationResultRecordsClean(gridIds[i], valuationResultDatasetId) //use this instead?
					try {
						//If the crosswalk isn't there, create it now
						CrosswalksApi.ensureCrosswalkExists(batchTaskConfig.gridDefinitionId, gridIds[i]);
						
						Table<GetExposureResultsRecord> efResultRecords = create.selectFrom(
								GET_EXPOSURE_RESULTS(
									exposureResultDatasetId, 
									null,
									gridIds[i]))
							.asTable("ef_result_records");
						Result<Record17<Integer, Integer, String, Integer, Integer, String, String, String, String, String, Double, Double, Double, Double, Double, Double, Double>> efRecords = create.select(
								efResultRecords.field(GET_EXPOSURE_RESULTS.GRID_COL).as("column"),
								efResultRecords.field(GET_EXPOSURE_RESULTS.GRID_ROW).as("row"),
								EXPOSURE_RESULT_FUNCTION_CONFIG.POPULATION_GROUP,
								EXPOSURE_RESULT_FUNCTION_CONFIG.START_AGE,
								EXPOSURE_RESULT_FUNCTION_CONFIG.END_AGE,
								EXPOSURE_RESULT_FUNCTION_CONFIG.FUNCTION_TYPE,
								RACE.NAME.as("race"),
								ETHNICITY.NAME.as("ethnicity"),
								GENDER.NAME.as("gender"),
								VARIABLE_ENTRY.NAME.as("variable"),
								efResultRecords.field(GET_EXPOSURE_RESULTS.DELTA_AQ),
								efResultRecords.field(GET_EXPOSURE_RESULTS.BASELINE_AQ),
								efResultRecords.field(GET_EXPOSURE_RESULTS.SCENARIO_AQ),
								DSL.when(efResultRecords.field(GET_EXPOSURE_RESULTS.BASELINE_AQ).eq(0.0), 0.0)
								.otherwise(efResultRecords.field(GET_EXPOSURE_RESULTS.DELTA_AQ).div(efResultRecords.field(GET_EXPOSURE_RESULTS.BASELINE_AQ)).times(100.0)).as("delta_aq_percent"),
								efResultRecords.field(GET_EXPOSURE_RESULTS.SUBGROUP_POPULATION),
								efResultRecords.field(GET_EXPOSURE_RESULTS.ALL_POPULATION),
								DSL.when(efResultRecords.field(GET_EXPOSURE_RESULTS.ALL_POPULATION).eq(0.0), 0.0)
								.otherwise(efResultRecords.field(GET_EXPOSURE_RESULTS.SUBGROUP_POPULATION).div(efResultRecords.field(GET_EXPOSURE_RESULTS.ALL_POPULATION)).times(100.0)).as("percent_of_population")
								)
								.from(efResultRecords)
								.leftJoin(EXPOSURE_FUNCTION).on(efResultRecords.field(GET_EXPOSURE_RESULTS.EXPOSURE_FUNCTION_ID).eq(EXPOSURE_FUNCTION.ID))
								.join(EXPOSURE_RESULT_FUNCTION_CONFIG)
									.on(EXPOSURE_RESULT_FUNCTION_CONFIG.EXPOSURE_RESULT_DATASET_ID.eq(exposureResultDatasetId)
											.and(EXPOSURE_RESULT_FUNCTION_CONFIG.EXPOSURE_FUNCTION_INSTANCE_ID.eq(efResultRecords.field(GET_EXPOSURE_RESULTS.EXPOSURE_FUNCTION_INSTANCE_ID))))
								.leftJoin(RACE).on(EXPOSURE_RESULT_FUNCTION_CONFIG.RACE_ID.eq(RACE.ID))
								.join(ETHNICITY).on(EXPOSURE_RESULT_FUNCTION_CONFIG.ETHNICITY_ID.eq(ETHNICITY.ID))
								.join(GENDER).on(EXPOSURE_RESULT_FUNCTION_CONFIG.GENDER_ID.eq(GENDER.ID))
								.leftJoin(VARIABLE_ENTRY).on(EXPOSURE_RESULT_FUNCTION_CONFIG.VARIABLE_ID.eq(VARIABLE_ENTRY.ID))
                                .orderBy(efResultRecords.field(GET_EXPOSURE_RESULTS.GRID_COL).asc(), efResultRecords.field(GET_EXPOSURE_RESULTS.GRID_ROW).asc(), EXPOSURE_RESULT_FUNCTION_CONFIG.HIDDEN_SORT_ORDER.asc())
								.fetch();
						
						efRecordsClean = efRecords;
					} catch(DataAccessException e) {
						e.printStackTrace();
						response.status(400);
						return;
					}	
					try {						
						zipStream.putNextEntry(new ZipEntry(taskFileName + "_" + ApplicationUtil.replaceNonValidCharacters(GridDefinitionApi.getGridDefinitionName(gridIds[i])) + ".csv"));
						efRecordsClean.formatCSV(zipStream);
						zipStream.closeEntry();
						log.info(taskFileName + " added.");
						} 
					catch (Exception e) {
							log.error("Error creating export file", e);
						} 
					finally {
		
						}
					ExposureTaskLog efTaskLog = ExposureUtil.getTaskLog(exposureResultDatasetId);
					batchTaskLog.append(System.getProperty("line.separator"));
					batchTaskLog.append(efTaskLog.toString(userProfile));
				}				
			}					
		}
		if(includeHealthImpact) {
			DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());
			//get hif task ids
			List<Integer> hifResultDatasetIds;
			if(uuidType.equals("H")) {
				//export all hif results from the same senario as taskUuid.
				hifResultDatasetIds = create.select()
						.from(HIF_RESULT_DATASET)
						.join(TASK_COMPLETE).on(HIF_RESULT_DATASET.TASK_UUID.eq(TASK_COMPLETE.TASK_UUID))
						.and(HIF_RESULT_DATASET.TASK_UUID.eq(taskUuid))
						.fetch(HIF_RESULT_DATASET.ID);
			}
			else if(uuidType.equals("V")) {
				hifResultDatasetIds = create.select()
						.from(HIF_RESULT_DATASET)
						.join(TASK_COMPLETE).on(HIF_RESULT_DATASET.TASK_UUID.eq(TASK_COMPLETE.TASK_UUID))
						.join(VALUATION_RESULT_DATASET).on(HIF_RESULT_DATASET.ID.eq(VALUATION_RESULT_DATASET.HIF_RESULT_DATASET_ID))
						.where(VALUATION_RESULT_DATASET.TASK_UUID.eq(taskUuid))
						.fetch(HIF_RESULT_DATASET.ID);
			}
			else {
				hifResultDatasetIds = create.select()
						.from(HIF_RESULT_DATASET)
						.join(TASK_COMPLETE).on(HIF_RESULT_DATASET.TASK_UUID.eq(TASK_COMPLETE.TASK_UUID))
						.where(TASK_COMPLETE.TASK_BATCH_ID.eq(batchId))
						.fetch(HIF_RESULT_DATASET.ID);
			}
			
			
			//Loop through each function and each grid definition
			for(int hifResultDatasetId : hifResultDatasetIds) {
				//csv file name
				String taskFileName = ApplicationUtil.replaceNonValidCharacters(HIFApi.getHifTaskConfigFromDb(hifResultDatasetId).name);
				Integer baselineGridId = HIFApi.getBaselineGridForHifResults(hifResultDatasetId);
				for(int i=0; i < gridIds.length; i++) {
					Result<?> hifRecordsClean = null;
					//Move the following to HIFApi.java? 
					//hifRecordsClean = HIFApi.getHifResultRecordsClean(gridIds[i], hifResultDatasetId) //use this instead?
					
					//If the crosswalk isn't there, create it now
					CrosswalksApi.ensureCrosswalkExists(baselineGridId, gridIds[i]);
					try {
						Table<GetHifResultsRecord> hifResultRecords = create.selectFrom(
							GET_HIF_RESULTS(
									hifResultDatasetId, 
									null, 
									gridIds[i]))
							.asTable("hif_result_records");
						Result<Record> hifRecords = create.select(
								hifResultRecords.field(GET_HIF_RESULTS.GRID_COL).as("column"),
								hifResultRecords.field(GET_HIF_RESULTS.GRID_ROW).as("row"),
								ENDPOINT.NAME.as("endpoint"),
								HEALTH_IMPACT_FUNCTION.AUTHOR,
								HEALTH_IMPACT_FUNCTION.FUNCTION_YEAR.as("year"),
								HEALTH_IMPACT_FUNCTION.LOCATION,
								HEALTH_IMPACT_FUNCTION.QUALIFIER,
								HIF_RESULT_FUNCTION_CONFIG.START_AGE,
								HIF_RESULT_FUNCTION_CONFIG.END_AGE,
								HEALTH_IMPACT_FUNCTION.BETA,
								RACE.NAME.as("race"),
								ETHNICITY.NAME.as("ethnicity"),
								GENDER.NAME.as("gender"),
								POLLUTANT_METRIC.NAME.as("metric"),
								SEASONAL_METRIC.NAME.as("seasonal_metric"),
								STATISTIC_TYPE.NAME.as("metric_statistic"),
								hifResultRecords.field(GET_HIF_RESULTS.POINT_ESTIMATE),
								hifResultRecords.field(GET_HIF_RESULTS.POPULATION),
								hifResultRecords.field(GET_HIF_RESULTS.DELTA_AQ),
								hifResultRecords.field(GET_HIF_RESULTS.BASELINE_AQ),
								hifResultRecords.field(GET_HIF_RESULTS.SCENARIO_AQ),
								//hifResultRecords.field(GET_HIF_RESULTS.INCIDENCE),
								hifResultRecords.field(GET_HIF_RESULTS.MEAN),
								hifResultRecords.field(GET_HIF_RESULTS.BASELINE),
								DSL.when(hifResultRecords.field(GET_HIF_RESULTS.BASELINE).eq(0.0), 0.0)
									.otherwise(hifResultRecords.field(GET_HIF_RESULTS.MEAN).div(hifResultRecords.field(GET_HIF_RESULTS.BASELINE)).times(100.0)).as("percent_of_baseline"),
								hifResultRecords.field(GET_HIF_RESULTS.STANDARD_DEV).as("standard_deviation"),
								hifResultRecords.field(GET_HIF_RESULTS.VARIANCE).as("variance"),
								hifResultRecords.field(GET_HIF_RESULTS.PCT_2_5),
								hifResultRecords.field(GET_HIF_RESULTS.PCT_97_5),
								HIFApi.getBaselineGridForHifResults(hifResultDatasetId) == gridIds[i] ? null : hifResultRecords.field(GET_HIF_RESULTS.PERCENTILES) //Only include percentiles if we're aggregating
								)
								.from(hifResultRecords)
								.leftJoin(HEALTH_IMPACT_FUNCTION).on(hifResultRecords.field(GET_HIF_RESULTS.HIF_ID).eq(HEALTH_IMPACT_FUNCTION.ID))
								.join(HIF_RESULT_FUNCTION_CONFIG)
									.on(HIF_RESULT_FUNCTION_CONFIG.HIF_RESULT_DATASET_ID.eq(hifResultDatasetId)
											.and(HIF_RESULT_FUNCTION_CONFIG.HIF_INSTANCE_ID.eq(hifResultRecords.field(GET_HIF_RESULTS.HIF_INSTANCE_ID))))
								.join(ENDPOINT).on(ENDPOINT.ID.eq(HEALTH_IMPACT_FUNCTION.ENDPOINT_ID))
								.join(RACE).on(HIF_RESULT_FUNCTION_CONFIG.RACE_ID.eq(RACE.ID))
								.join(ETHNICITY).on(HIF_RESULT_FUNCTION_CONFIG.ETHNICITY_ID.eq(ETHNICITY.ID))
								.join(GENDER).on(HIF_RESULT_FUNCTION_CONFIG.GENDER_ID.eq(GENDER.ID))
								.join(POLLUTANT_METRIC).on(HIF_RESULT_FUNCTION_CONFIG.METRIC_ID.eq(POLLUTANT_METRIC.ID))
								.leftJoin(SEASONAL_METRIC).on(HIF_RESULT_FUNCTION_CONFIG.SEASONAL_METRIC_ID.eq(SEASONAL_METRIC.ID))
								.join(STATISTIC_TYPE).on(HIF_RESULT_FUNCTION_CONFIG.METRIC_STATISTIC.eq(STATISTIC_TYPE.ID))
								.fetch();
						
						//If results are being aggregated, recalculate mean, variance, std deviation, and percent of baseline
						if(HIFApi.getBaselineGridForHifResults(hifResultDatasetId) != gridIds[i]) {
							for(Record res : hifRecords) {
								DescriptiveStatistics stats = new DescriptiveStatistics();
								Double[] pct = res.getValue(GET_HIF_RESULTS.PERCENTILES);
								for (int j = 0; j < pct.length; j++) {
									stats.addValue(pct[j]);
								}
								
								res.setValue(GET_HIF_RESULTS.MEAN, stats.getMean());
								
								//Add point estimate to the list before calculating variance and standard deviation to match approach of desktop
								stats.addValue(res.getValue(GET_HIF_RESULTS.POINT_ESTIMATE));
								res.setValue(GET_HIF_RESULTS.VARIANCE, stats.getVariance());
								res.setValue(DSL.field("standard_deviation", Double.class), stats.getStandardDeviation());
								
								res.setValue(DSL.field("percent_of_baseline", Double.class), stats.getMean() / res.getValue(GET_HIF_RESULTS.BASELINE) * 100.0);
							}
						}
						//Remove percentiles by keeping all other fields
						hifRecordsClean = hifRecords.into(hifRecords.fields(0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27));
					} catch(DataAccessException e) {
						e.printStackTrace();
						response.status(400);
						return;
					}	
					try {						
						zipStream.putNextEntry(new ZipEntry(taskFileName + "_" + ApplicationUtil.replaceNonValidCharacters(GridDefinitionApi.getGridDefinitionName(gridIds[i])) + ".csv"));
						hifRecordsClean.formatCSV(zipStream);
						zipStream.closeEntry();
						log.info(taskFileName + " added.");
						} 
					catch (Exception e) {
							log.error("Error creating export file", e);
						} 
					finally {
		
						}
					HIFTaskLog hifTaskLog = HIFUtil.getTaskLog(hifResultDatasetId);
					batchTaskLog.append(System.getProperty("line.separator"));
					batchTaskLog.append(hifTaskLog.toString(userProfile));
				}				
			}					
		}
		if(includeValuation) {
			//Valuation results
			DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());
			//get valuation task ids
			List<Integer> valuationResultDatasetIds;
			if(uuidType.equals("H")) {
				//export all val results from the same scenario as hif taskUuid.
				valuationResultDatasetIds = create.select()
						.from(VALUATION_RESULT_DATASET)
						.join(TASK_COMPLETE).on(VALUATION_RESULT_DATASET.TASK_UUID.eq(TASK_COMPLETE.TASK_UUID))
						.join(HIF_RESULT_DATASET).on(VALUATION_RESULT_DATASET.HIF_RESULT_DATASET_ID.eq(HIF_RESULT_DATASET.ID))
						.where(HIF_RESULT_DATASET.TASK_UUID.eq(taskUuid))
						.fetch(VALUATION_RESULT_DATASET.ID);
			}
			else if(uuidType.equals("V")) {
				valuationResultDatasetIds = create.select()
						.from(VALUATION_RESULT_DATASET)
						.join(TASK_COMPLETE).on(VALUATION_RESULT_DATASET.TASK_UUID.eq(TASK_COMPLETE.TASK_UUID))
						.and(VALUATION_RESULT_DATASET.TASK_UUID.eq(taskUuid))
						.fetch(VALUATION_RESULT_DATASET.ID);
			}
			else {
				valuationResultDatasetIds = create.select()
						.from(VALUATION_RESULT_DATASET)
						.join(TASK_COMPLETE).on(VALUATION_RESULT_DATASET.TASK_UUID.eq(TASK_COMPLETE.TASK_UUID))
						.where(TASK_COMPLETE.TASK_BATCH_ID.eq(batchId))
						.fetch(VALUATION_RESULT_DATASET.ID);
			}
			
			//Loop through each function and each grid definition
			for(int valuationResultDatasetId : valuationResultDatasetIds) {
				//csv file name
				String taskFileName = ApplicationUtil.replaceNonValidCharacters(ValuationApi.getValuationTaskConfigFromDb(valuationResultDatasetId).name);
				Integer baselineGridId = ValuationApi.getBaselineGridForValuationResults(valuationResultDatasetId);				
				for(int i=0; i < gridIds.length; i++) {
					Result<?> vfRecordsClean = null;
					//Move the following to ValuationApi.java? 
					//valuationRecordsClean = ValuationApi.getValuationResultRecordsClean(gridIds[i], valuationResultDatasetId) //use this instead?
					
					//If the crosswalk isn't there, create it now
					CrosswalksApi.ensureCrosswalkExists(baselineGridId, gridIds[i]);
					try {
						Table<GetValuationResultsRecord> vfResultRecords = create.selectFrom(
								GET_VALUATION_RESULTS(
									valuationResultDatasetId, 
									null, 
									null,
									gridIds[i]))
							.asTable("valuation_result_records");
						Result<Record> vfRecords;
						vfRecords = create.select(
								vfResultRecords.field(GET_VALUATION_RESULTS.GRID_COL).as("column"),
								vfResultRecords.field(GET_VALUATION_RESULTS.GRID_ROW).as("row"),
								DSL.val(null, String.class).as("endpoint"),
								DSL.val(null, String.class).as("name"),
								HEALTH_IMPACT_FUNCTION.AUTHOR,
								HEALTH_IMPACT_FUNCTION.FUNCTION_YEAR.as("year"),
								HEALTH_IMPACT_FUNCTION.QUALIFIER,
								RACE.NAME.as("race"),
								ETHNICITY.NAME.as("ethnicity"),
								GENDER.NAME.as("gender"),
								POLLUTANT_METRIC.NAME.as("metric"),
								SEASONAL_METRIC.NAME.as("seasonal_metric"),
								STATISTIC_TYPE.NAME.as("metric_statistic"),
								HEALTH_IMPACT_FUNCTION.START_AGE,
								HEALTH_IMPACT_FUNCTION.END_AGE,
								VALUATION_FUNCTION.START_AGE.as("valuation_start_age"),
								VALUATION_FUNCTION.END_AGE.as("valuation_end_age"),
								vfResultRecords.field(GET_VALUATION_RESULTS.POINT_ESTIMATE),
								vfResultRecords.field(GET_VALUATION_RESULTS.MEAN),
								vfResultRecords.field(GET_VALUATION_RESULTS.STANDARD_DEV).as("standard_deviation"),
								vfResultRecords.field(GET_VALUATION_RESULTS.VARIANCE).as("variance"),
								vfResultRecords.field(GET_VALUATION_RESULTS.PCT_2_5),
								vfResultRecords.field(GET_VALUATION_RESULTS.PCT_97_5),
								vfResultRecords.field(GET_VALUATION_RESULTS.POINT_ESTIMATE).as("point_estimate (2020$)"),
								ValuationApi.getBaselineGridForValuationResults(valuationResultDatasetId) == gridIds[i] ? null : vfResultRecords.field(GET_VALUATION_RESULTS.PERCENTILES), //Only include percentiles if we're aggregating
								vfResultRecords.field(GET_VALUATION_RESULTS.VF_ID)
								)
								.from(vfResultRecords)
								.join(VALUATION_RESULT_FUNCTION_CONFIG)
									.on(VALUATION_RESULT_FUNCTION_CONFIG.VALUATION_RESULT_DATASET_ID.eq(valuationResultDatasetId)
										.and(VALUATION_RESULT_FUNCTION_CONFIG.HIF_INSTANCE_ID.eq(vfResultRecords.field(GET_VALUATION_RESULTS.HIF_INSTANCE_ID)))
										.and(VALUATION_RESULT_FUNCTION_CONFIG.VF_ID.eq(vfResultRecords.field(GET_VALUATION_RESULTS.VF_ID))))
								.join(VALUATION_RESULT_DATASET)
									.on(VALUATION_RESULT_FUNCTION_CONFIG.VALUATION_RESULT_DATASET_ID.eq(VALUATION_RESULT_DATASET.ID))
								.join(HIF_RESULT_FUNCTION_CONFIG)
									.on(VALUATION_RESULT_DATASET.HIF_RESULT_DATASET_ID.eq(HIF_RESULT_FUNCTION_CONFIG.HIF_RESULT_DATASET_ID)
										.and(VALUATION_RESULT_FUNCTION_CONFIG.HIF_ID.eq(HIF_RESULT_FUNCTION_CONFIG.HIF_ID)))
								.join(VALUATION_FUNCTION).on((VALUATION_FUNCTION.ID.eq(vfResultRecords.field(GET_VALUATION_RESULTS.VF_ID))))
								.join(HEALTH_IMPACT_FUNCTION).on(vfResultRecords.field(GET_VALUATION_RESULTS.HIF_ID).eq(HEALTH_IMPACT_FUNCTION.ID))
								.join(RACE).on(HIF_RESULT_FUNCTION_CONFIG.RACE_ID.eq(RACE.ID))
								.join(ETHNICITY).on(HIF_RESULT_FUNCTION_CONFIG.ETHNICITY_ID.eq(ETHNICITY.ID))
								.join(GENDER).on(HIF_RESULT_FUNCTION_CONFIG.GENDER_ID.eq(GENDER.ID))
								.join(POLLUTANT_METRIC).on(HIF_RESULT_FUNCTION_CONFIG.METRIC_ID.eq(POLLUTANT_METRIC.ID))
								.leftJoin(SEASONAL_METRIC).on(HIF_RESULT_FUNCTION_CONFIG.SEASONAL_METRIC_ID.eq(SEASONAL_METRIC.ID))
								.join(STATISTIC_TYPE).on(HIF_RESULT_FUNCTION_CONFIG.METRIC_STATISTIC.eq(STATISTIC_TYPE.ID))
								.fetch();
						
						// Add in valuation function information
						ValuationTaskLog vfTaskLog = ValuationUtil.getTaskLog(valuationResultDatasetId);
						HashMap<Integer, HashMap<String, String>> vfConfigs = new HashMap<Integer, HashMap<String, String>>();
						
						for (ValuationConfig vf : vfTaskLog.getVfTaskConfig().valuationFunctions) {
							if(! vfConfigs.containsKey(vf.vfId)) {
								HashMap<String, String> vfInfo = new HashMap<String, String>();
								vfInfo.put("name", vf.vfRecord.get("qualifier").toString());
								vfInfo.put("endpoint", vf.vfRecord.get("endpoint_name").toString());
								vfConfigs.put(vf.vfId, vfInfo);
							}
						}

						for(Record res : vfRecords) {
							HashMap<String, String> vfConfig = vfConfigs.get(res.getValue(GET_VALUATION_RESULTS.VF_ID));
							res.setValue(DSL.field("name"), vfConfig.get("name"));
							res.setValue(DSL.field("endpoint"), vfConfig.get("endpoint"));	
						}
						
						//If results are being aggregated, recalc mean, variance, std deviation, and percent of baseline
						if(ValuationApi.getBaselineGridForValuationResults(valuationResultDatasetId) != gridIds[i]) {
							for(Record res : vfRecords) {
								DescriptiveStatistics stats = new DescriptiveStatistics();
								Double[] pct = res.getValue(GET_VALUATION_RESULTS.PERCENTILES);
								for (int j = 0; j < pct.length; j++) {
									stats.addValue(pct[j]);
								}
								
								res.setValue(GET_VALUATION_RESULTS.MEAN, stats.getMean());
								
								//Add point estimate to the list before calculating variance and standard deviation to match approach of desktop
								stats.addValue(res.getValue(GET_VALUATION_RESULTS.POINT_ESTIMATE));
								res.setValue(GET_VALUATION_RESULTS.VARIANCE, stats.getVariance());
								res.setValue(DSL.field("standard_deviation", Double.class), stats.getStandardDeviation());
							}
						}
						//Remove percentiles by keeping all other fields
						vfRecordsClean = vfRecords.into(vfRecords.fields(0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,21,16,17,18,19,20,21,22));
					} catch(DataAccessException e) {
						e.printStackTrace();
						response.status(400);
						return;
					}	
					try {						
						zipStream.putNextEntry(new ZipEntry(taskFileName + "_" + ApplicationUtil.replaceNonValidCharacters(GridDefinitionApi.getGridDefinitionName(gridIds[i])) + ".csv"));
						vfRecordsClean.formatCSV(zipStream);
						zipStream.closeEntry();
						log.info(taskFileName + " added.");
						} 
					catch (Exception e) {
							log.error("Error creating export file", e);
						} 
					finally {
		
						}
					ValuationTaskLog vfTaskLog = ValuationUtil.getTaskLog(valuationResultDatasetId);
					batchTaskLog.append(System.getProperty("line.separator"));
					batchTaskLog.append(vfTaskLog.toString());
				}				
			}					
		
		}
		
		
		// Add log file
		try {
			zipStream.putNextEntry(new ZipEntry(taskBatchFileName + "_TaskLog.txt"));
			
			zipStream.write(batchTaskLog.toString().getBytes());
			zipStream.closeEntry();			
			zipStream.close();
			responseOutputStream.flush();
		} catch (Exception e) {
			log.error("Error writing task log, closing and flushing export", e);
		}
		
		
	}
	
	/**
	 * @param task batch id
	 * @return a batch task configuration from a given task batch id.
	 */
	public static BatchTaskConfig getTaskBatchConfigFromDb(Integer batchId) {
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());

		BatchTaskConfig batchTaskConfig = new BatchTaskConfig();		

		TaskBatchRecord batchTaskRecord = create
				.selectFrom(TASK_BATCH)
				.where(TASK_BATCH.ID.eq(batchId))
				.fetchOne();		
			
		try {
			batchTaskConfig = objectMapper.readValue(batchTaskRecord.getParameters(), BatchTaskConfig.class);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return null;
		}
		return batchTaskConfig;
	}


		public static Object postResultExportTask(Request request, Response response, Optional<UserProfile> userProfile) {
	//		 PARAMETERS:
	//		   :id (batch task id)
	//		   includeHealthImpact (boolean, default to false)
	//		   includeValuation (boolean, default to false)
	//		   includeExposure (boolean, default to false)
	//		   gridId= (comma delimited list. aggregate the results to one or more grid definition)
	// 		   taskUuid=(optional. since there is not a defined class for "scenario", we use parent-child relationship to pull all relevant results)
	// 		   uuidType = (optional. examples: "H", "V", "E")		
			
			Integer batchId;
			int[] gridIds;
			Boolean includeHealthImpact = false;
			Boolean includeValuation = false;
			Boolean includeExposure = false;
			String taskUuid="";
			String uuidType = "";
			String visibleColumns = "";
			try {
				String idParam = String.valueOf(request.params("id"));
				batchId = Integer.valueOf(idParam);
				includeHealthImpact = ParameterUtil.getParameterValueAsBoolean(request.raw().getParameter("includeHealthImpact").equals("1")?"true":request.raw().getParameter("includeHealthImpact"), false);
				includeValuation = ParameterUtil.getParameterValueAsBoolean(request.raw().getParameter("includeValuation").equals("1")?"true":request.raw().getParameter("includeValuation"), false);
				includeExposure = ParameterUtil.getParameterValueAsBoolean(request.raw().getParameter("includeExposure").equals("1")?"true":request.raw().getParameter("includeExposure"), false);
				taskUuid = ParameterUtil.getParameterValueAsString(request.raw().getParameter("taskUuid"),"");
				uuidType = ParameterUtil.getParameterValueAsString(request.raw().getParameter("uuidType"),"");	
				visibleColumns = ParameterUtil.getParameterValueAsString(request.raw().getParameter("visibleColumns"),"");			
				
				String gridIdParam = ParameterUtil.getParameterValueAsString(request.raw().getParameter("gridId"), "");
	
				if(gridIdParam==null || gridIdParam.equals("")){
					gridIds=null;
				}else {
					String[] gridIdParamArr = gridIdParam.split(",");
					gridIds = new int[gridIdParamArr.length];
					for (int i = 0; i < gridIdParamArr.length; i++) {
						gridIds[i] = Integer.parseInt(gridIdParamArr[i]);
			        }
				}				
	
			} catch (NumberFormatException e) {
				e.printStackTrace();
				return CoreApi.getErrorResponseInvalidId(request, response);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				return CoreApi.getErrorResponseInvalidId(request, response);
			}
			
			BatchTaskConfig batchTaskConfig = getTaskBatchConfigFromDb(batchId);
			if(batchTaskConfig==null) {
				return CoreApi.getErrorResponseNotFound(request, response);
			}
			
			// If a gridId wasn't provided, look up the baseline AQ grid grid for this resultset
			try {
				if(gridIds == null) {
					gridIds = new int[] {batchTaskConfig.gridDefinitionId.intValue()};
				}
			} catch (NullPointerException e) {
				e.printStackTrace();
				return CoreApi.getErrorResponse(request, response, 404, "Grid definition id not found");
			}
				
			ObjectMapper mapper = new ObjectMapper();
			ObjectNode taskParamsNode = mapper.createObjectNode();
			ObjectNode batchParamsNode = mapper.createObjectNode();

			batchParamsNode.put("name", batchTaskConfig.name);
			batchParamsNode.put("gridDefinitionId", batchTaskConfig.gridDefinitionId);
			
			// Add params so task processor can create the export file
			taskParamsNode.put("batchId", batchId);	
			taskParamsNode.put("name", batchTaskConfig.name);
			taskParamsNode.put("includeHealthImpact", includeHealthImpact);
			taskParamsNode.put("includeValuation", includeValuation);
			taskParamsNode.put("includeExposure", includeExposure);
			taskParamsNode.put("taskUuid", taskUuid);
			taskParamsNode.put("uuidType", uuidType);			
			taskParamsNode.put("isAdmin", CoreApi.isAdmin(userProfile));	
			taskParamsNode.put("visibleColumns", visibleColumns);		

			ArrayNode gridsNode = taskParamsNode.putArray("gridIds");
			for (int g : gridIds) {
				gridsNode.add(g);
			}
			
			// Add records to task_batch and task_queue to export the results
			TaskBatchRecord rec = DSL.using(JooqUtil.getJooqConfiguration())
					.insertInto(TASK_BATCH, TASK_BATCH.NAME, TASK_BATCH.PARAMETERS, TASK_BATCH.USER_ID, TASK_BATCH.SHARING_SCOPE)
					.values("Result export: " + batchTaskConfig.name, batchParamsNode.toString(), userProfile.get().getId(), Constants.SHARING_NONE)
					.returning(TASK_BATCH.ID).fetchOne();
			Integer batchTaskId = rec.getId();

			Task resultExportTask = new Task();
			resultExportTask.setUserIdentifier(userProfile.get().getId());
			resultExportTask.setType(Constants.TASK_TYPE_RESULT_EXPORT);
			resultExportTask.setBatchId(batchTaskId);
			resultExportTask.setName("Result export: " + batchTaskConfig.name);
			resultExportTask.setParameters(taskParamsNode.toString());
			String resultExportTaskUUID = UUID.randomUUID().toString();
			resultExportTask.setUuid(resultExportTaskUUID);
			TaskQueue.writeTaskToQueue(resultExportTask);
			
			// Return success
			return CoreApi.getSuccessResponse(request, response, 200, "Export request submitted for processing.");
		}

		public static Object getExportFileID(Request request, Response response, Optional<UserProfile> userProfile) {
			Integer batchTaskId;
			try {
				batchTaskId = Integer.valueOf(request.params("id"));
			} catch (NumberFormatException e) {
				e.printStackTrace();
				return CoreApi.getErrorResponseInvalidId(request, response);
			}

			DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());
			Record record = create
					.select(TASK_COMPLETE.TASK_PARAMETERS)
					.from(TASK_COMPLETE)
					.where(TASK_COMPLETE.TASK_BATCH_ID.eq(batchTaskId))
					.fetchOne();

			if (record == null) {
				return CoreApi.getErrorResponseNotFound(request, response);
			}

			String parameters = record.get(TASK_COMPLETE.TASK_PARAMETERS);
			try {
				ObjectMapper objectMapper = new ObjectMapper();
				JsonNode jsonNode = objectMapper.readTree(parameters);
				int filestoreId = jsonNode.get("filestoreId").asInt();
				ObjectNode result = objectMapper.createObjectNode();
				result.put("filestoreId", filestoreId);
				response.type("application/json");
				return result;
			} catch (JsonProcessingException e) {
				e.printStackTrace();
				return CoreApi.getErrorResponse(request, response, 400, "Unable to parse task parameters");
			}
		}
}

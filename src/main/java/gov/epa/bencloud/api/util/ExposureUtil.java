package gov.epa.bencloud.api.util;

import static gov.epa.bencloud.server.database.jooq.data.Tables.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.JSON;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.mariuszgromada.math.mxparser.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import gov.epa.bencloud.api.model.ExposureTaskConfig;
import gov.epa.bencloud.api.model.ExposureConfig;
import gov.epa.bencloud.api.model.ExposureTaskLog;
import gov.epa.bencloud.api.function.EFArguments;
import gov.epa.bencloud.api.function.EFNativeFactory;
import gov.epa.bencloud.api.function.EFunction;
import gov.epa.bencloud.server.database.JooqUtil;
import gov.epa.bencloud.server.database.jooq.data.Routines;
import gov.epa.bencloud.server.database.jooq.data.tables.records.ExposureFunctionRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.ExposureResultDatasetRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.ExposureResultRecord;
import gov.epa.bencloud.server.tasks.model.Task;

/*
 * Methods for updating and accessing resources related to health impact functions.
 */
public class ExposureUtil {

	/**
	 * 
	 * @param id
	 * @return an exposure function.
	 */
	public static EFunction getFunctionForEF(Integer id) {

		// Load the function by id
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());

		ExposureFunctionRecord record = create
				.selectFrom(EXPOSURE_FUNCTION)
				.where(EXPOSURE_FUNCTION.ID.eq(id))
				.fetchOne();

		EFunction function = new EFunction();

		//TODO: Think about trimming spaces here and with EXPOSUREs and VFs as well
		function.nativeFunction = EFNativeFactory.create(record.getFunctionText());

		function.efArguments = new EFArguments();


		//If we don't have a native function, we'll use the interpreted one instead
		if(function.nativeFunction == null) {
			// Populate/create the necessary arguments and constants
			Argument deltaQ = new Argument("DELTA", 0.0);
			Argument population = new Argument("POPULATION", 0.0);
			Argument variable = new Argument("VARIABLE", 0.0);

			function.interpretedFunction = new Expression(record.getFunctionText(), deltaQ, population, variable);
		}

		return function;
	}

	/**
	 * 
	 * @param id
	 * @return the function definition for a given exposure function.
	 */
	public static Record getFunctionDefinition(Integer id) {

		// Load the function by id
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());

		Record record = create
				.select(EXPOSURE_FUNCTION.asterisk()
						,RACE.NAME.as("race_name")
						,GENDER.NAME.as("gender_name")
						,ETHNICITY.NAME.as("ethnicity_name")
						)
				.from(EXPOSURE_FUNCTION)
				.leftJoin(RACE).on(EXPOSURE_FUNCTION.RACE_ID.eq(RACE.ID))
				.leftJoin(GENDER).on(EXPOSURE_FUNCTION.GENDER_ID.eq(GENDER.ID))
				.leftJoin(ETHNICITY).on(EXPOSURE_FUNCTION.ETHNICITY_ID.eq(ETHNICITY.ID))
				.where(EXPOSURE_FUNCTION.ID.eq(id))
				.fetchOne();
				
		return record;
	}
	
	/**
	 * This method supports saving partial datasets to avoid a situation where 
	 * the complete result set gets so large it cannot be kept in memory.
	 * It will only create the dataset and function_config records on the first call.
	 * Subsequent calls will only write the results themselves.
	 * 
	 * @param task
	 * @param hifTaskConfig
	 * @param hifResults
	 */
	public static void storeResults(Task task, ExposureTaskConfig exposureTaskConfig, Vector<ExposureResultRecord> exposureResults) {
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration(task.getUuid()));
		Integer exposureResultDatasetId = create
				.selectFrom(EXPOSURE_RESULT_DATASET)
				.where(EXPOSURE_RESULT_DATASET.TASK_UUID.eq(task.getUuid()))
				.fetchOne(EXPOSURE_RESULT_DATASET.ID);
		
		// If this is the first call, we need to store the dataset header record and the function_config records
		if(exposureResultDatasetId == null) {	
			// Exposure result dataset record links the result dataset id to the task uuid
			ExposureResultDatasetRecord exposureResultDatasetRecord = create.insertInto(
					EXPOSURE_RESULT_DATASET
					, EXPOSURE_RESULT_DATASET.TASK_UUID
					, EXPOSURE_RESULT_DATASET.NAME
					, EXPOSURE_RESULT_DATASET.POPULATION_DATASET_ID
					, EXPOSURE_RESULT_DATASET.POPULATION_YEAR
					, EXPOSURE_RESULT_DATASET.BASELINE_AQ_LAYER_ID
					, EXPOSURE_RESULT_DATASET.SCENARIO_AQ_LAYER_ID
					, EXPOSURE_RESULT_DATASET.GRID_DEFINITION_ID
					, EXPOSURE_RESULT_DATASET.USER_ID
					)
			.values(
					task.getUuid()
					, exposureTaskConfig.name
					, exposureTaskConfig.popId
					, exposureTaskConfig.popYear
					, exposureTaskConfig.aqBaselineId
					, exposureTaskConfig.aqScenarioId
					, exposureTaskConfig.gridDefinitionId
					, task.getUserIdentifier())
			.returning(EXPOSURE_RESULT_DATASET.ID)
			.fetchOne();
			
			exposureResultDatasetId = exposureResultDatasetRecord.getId();
			exposureTaskConfig.resultDatasetId = exposureResultDatasetId;
			
			// Each exposure result function config contains the details of how the function was configured
			for(ExposureConfig exposureFunction : exposureTaskConfig.exposureFunctions) {
				int gender = exposureFunction.efRecord.get("complement_gender") != null ? (int)exposureFunction.efRecord.get("complement_gender") : exposureFunction.gender;
				int ethnicity = exposureFunction.efRecord.get("complement_ethnicity") != null ? (int)exposureFunction.efRecord.get("complement_ethnicity") : exposureFunction.ethnicity;
				Integer race = exposureFunction.efRecord.get("complement_race") != null ? null : exposureFunction.race;

				create.insertInto(EXPOSURE_RESULT_FUNCTION_CONFIG
						, EXPOSURE_RESULT_FUNCTION_CONFIG.EXPOSURE_RESULT_DATASET_ID
						, EXPOSURE_RESULT_FUNCTION_CONFIG.EXPOSURE_FUNCTION_ID
						, EXPOSURE_RESULT_FUNCTION_CONFIG.EXPOSURE_FUNCTION_INSTANCE_ID
						, EXPOSURE_RESULT_FUNCTION_CONFIG.START_AGE
						, EXPOSURE_RESULT_FUNCTION_CONFIG.END_AGE
						, EXPOSURE_RESULT_FUNCTION_CONFIG.RACE_ID
						, EXPOSURE_RESULT_FUNCTION_CONFIG.GENDER_ID
						, EXPOSURE_RESULT_FUNCTION_CONFIG.ETHNICITY_ID
						, EXPOSURE_RESULT_FUNCTION_CONFIG.VARIABLE_ID
						, EXPOSURE_RESULT_FUNCTION_CONFIG.POPULATION_GROUP
						, EXPOSURE_RESULT_FUNCTION_CONFIG.HIDDEN_SORT_ORDER
						, EXPOSURE_RESULT_FUNCTION_CONFIG.FUNCTION_TYPE)
				.values(exposureResultDatasetId
						, exposureFunction.efId
						, exposureFunction.efInstanceId						
						, exposureFunction.startAge
						, exposureFunction.endAge
						, race
						, gender
						, ethnicity
						, exposureFunction.variable
						, (String) exposureFunction.efRecord.get("population_group")
						, (String) exposureFunction.efRecord.get("hidden_sort_order")
						, (String) exposureFunction.efRecord.get("function_type"))
				.execute();
				
			}
		}

		// Finally, store the actual estimates
		for(ExposureResultRecord exposureResult : exposureResults) {
			exposureResult.setExposureResultDatasetId(exposureResultDatasetId);
		}
		
		create
		.batchInsert(exposureResults)
		.execute();	
	}

	/**
	 * Stores aggregated exposure results to result_agg table.
	 * @param task
	 * @param grid_id
	 */
	public static void storeAggResults(Task task, Integer grid_id) {
        DSLContext create = DSL.using(JooqUtil.getJooqConfiguration(task.getUuid()));
		Integer exposureResultDatasetId = create
				.selectFrom(EXPOSURE_RESULT_DATASET)
				.where(EXPOSURE_RESULT_DATASET.TASK_UUID.eq(task.getUuid()))
				.fetchOne(EXPOSURE_RESULT_DATASET.ID);
		Routines.addExposureResultsAgg(create.configuration(), exposureResultDatasetId, grid_id);
    }

	/**
	 * Stores the given exposureTaskLog in the database.
	 * @param exposureTaskLog
	 */
	public static void storeTaskLog(ExposureTaskLog exposureTaskLog) {
		
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());
		JSON taskLogJson = JSON.json(exposureTaskLog.toJsonString());
		
		create.update(EXPOSURE_RESULT_DATASET)
			.set(EXPOSURE_RESULT_DATASET.TASK_LOG, taskLogJson)
			.where(EXPOSURE_RESULT_DATASET.ID.eq(exposureTaskLog.getExposureTaskConfig().resultDatasetId))
			.execute();
		
	}
	
	/**
	 * Gets a mapping from variable ids to their corresponding names.
	 * @param exposureFunctions
	 * @return
	 */
    public static Map<Integer, String> getVarIdNameMapping(List<ExposureConfig> exposureFunctions) {
		HashSet<Integer> requiredVars = new HashSet<Integer>();

		for (ExposureConfig eConfig : exposureFunctions) {
			requiredVars.add(eConfig.variable);			
		}

		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());

		Result<Record2<Integer, String>> results = create.select(VARIABLE_ENTRY.ID, VARIABLE_ENTRY.NAME)
			.where(VARIABLE_ENTRY.ID.in(requiredVars))
			.fetch();

		Map<Integer, String> mapping = new HashMap<Integer, String>();
		for (Record2<Integer, String> result : results) {
			mapping.put(result.value1(), result.value2());
		}	

		return mapping;
    }


	/**
	 * 
	 * @param datasetId
	 * @return and ExposureTaskLog object, based on the given exposure result dataset id.
	 */
	public static ExposureTaskLog getTaskLog(Integer datasetId) {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());
		
		//JSON taskLogJson = JSON.json(hifTaskLog.toJsonString());
		
		Record1<JSON> taskLogJson = create.select(EXPOSURE_RESULT_DATASET.TASK_LOG)
			.from(EXPOSURE_RESULT_DATASET)
			.where(EXPOSURE_RESULT_DATASET.ID.eq(datasetId))
			.fetchOne();
		
		try {
			ExposureTaskLog exposureTaskLog = mapper.readValue(taskLogJson.value1().toString(), ExposureTaskLog.class);
			return exposureTaskLog;
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
		
	}

    

	
	
}

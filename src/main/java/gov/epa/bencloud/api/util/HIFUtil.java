package gov.epa.bencloud.api.util;

import static gov.epa.bencloud.server.database.jooq.data.Tables.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;

import org.jooq.DSLContext;
import org.jooq.JSON;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.exception.DataAccessException;
import org.jooq.exception.TooManyRowsException;
import org.jooq.impl.DSL;
import org.mariuszgromada.math.mxparser.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import gov.epa.bencloud.api.IncidenceApi;
import gov.epa.bencloud.api.model.HIFConfig;
import gov.epa.bencloud.api.model.HIFTaskConfig;
import gov.epa.bencloud.api.model.HIFTaskLog;
import gov.epa.bencloud.api.model.Scenario;
import gov.epa.bencloud.api.model.ScenarioHIFConfig;
import gov.epa.bencloud.api.model.ScenarioPopConfig;
import gov.epa.bencloud.api.function.HIFArguments;
import gov.epa.bencloud.api.function.HIFNativeFactory;
import gov.epa.bencloud.api.function.HIFunction;
import gov.epa.bencloud.server.database.JooqUtil;
import gov.epa.bencloud.server.database.jooq.data.tables.records.HealthImpactFunctionRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.HifResultDatasetRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.HifResultRecord;
import gov.epa.bencloud.server.tasks.model.Task;

/*
 * Methods for updating and accessing resources related to health impact functions.
 */
public class HIFUtil {

	/**
	 * 
	 * @param id
	 * @return an array of functions (function, baseline function) for a given health impact function.
	 */
	public static HIFunction[] getFunctionsForHIF(Integer id) {

		// Load the function by id
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());

		HealthImpactFunctionRecord record = create
				.selectFrom(HEALTH_IMPACT_FUNCTION)
				.where(HEALTH_IMPACT_FUNCTION.ID.eq(id))
				.fetchOne();

		HIFunction[] functions = new HIFunction[2];

		functions[0] = new HIFunction();
		functions[1] = new HIFunction();

		functions[0].nativeFunction = HIFNativeFactory.create(record.getFunctionText());
		functions[1].nativeFunction = HIFNativeFactory.create(record.getBaselineFunctionText());

		functions[0].hifArguments = new HIFArguments();
		functions[1].hifArguments = new HIFArguments();

		functions[0].hifArguments.a = record.getValA().doubleValue();
		functions[0].hifArguments.b = record.getValB().doubleValue();
		functions[0].hifArguments.c = record.getValC().doubleValue();
		functions[0].hifArguments.beta = record.getBeta().doubleValue();

		functions[1].hifArguments.a = record.getValA().doubleValue();
		functions[1].hifArguments.b = record.getValB().doubleValue();
		functions[1].hifArguments.c = record.getValC().doubleValue();
		functions[1].hifArguments.beta = record.getBeta().doubleValue();

		//If we don't have a native function, we'll use the interpreted one instead
		if(functions[0].nativeFunction == null) {
			// Populate/create the necessary arguments and constants
			//{ a, b, c, beta, deltaq, q0, q1, incidence, pop, prevalence };
			Constant a = new Constant("A", functions[0].hifArguments.a);
			Constant b = new Constant("B", functions[0].hifArguments.b);
			Constant c = new Constant("C", functions[0].hifArguments.c);
			
			//The following will be set while iterating cells
			Argument beta = new Argument("BETA", functions[0].hifArguments.beta);
			Argument deltaQ = new Argument("DELTAQ", 0.0);
			Argument q0 = new Argument("Q0", 0.0);
			Argument q1 = new Argument("Q1", 0.0);
			Argument incidence = new Argument("INCIDENCE", 0.0);
			Argument prevalence = new Argument("PREVALENCE", 0.0);
			Argument population = new Argument("POPULATION", 0.0);

			functions[0].interpretedFunction = new Expression(record.getFunctionText(), a, b, c, beta, deltaQ, q0, q1, incidence, prevalence, population);
			functions[0].interpretedFunction.disableImpliedMultiplicationMode();
			functions[0].requiredExpressionVariables = Arrays.asList(functions[0].interpretedFunction.getMissingUserDefinedArguments());
			functions[0].interpretedFunction.defineArguments(functions[0].interpretedFunction.getMissingUserDefinedArguments());
		}

		//If we don't have a native baseline function, we'll use the interpreted one instead
		if(functions[1].nativeFunction == null) {
			// Populate/create the necessary arguments and constants
			//{ a, b, c, beta, deltaq, q0, q1, incidence, pop, prevalence };
			Constant a = new Constant("A", functions[1].hifArguments.a);
			Constant b = new Constant("B", functions[1].hifArguments.b);
			Constant c = new Constant("C", functions[1].hifArguments.c);
			
			//The following will be set while iterating cells
			Argument beta = new Argument("BETA", functions[1].hifArguments.beta);
			Argument deltaQ = new Argument("DELTAQ", 0.0);
			Argument q0 = new Argument("Q0", 0.0);
			Argument q1 = new Argument("Q1", 0.0);
			Argument incidence = new Argument("INCIDENCE", 0.0);
			Argument prevalence = new Argument("PREVALENCE", 0.0);
			Argument population = new Argument("POPULATION", 0.0);

			functions[1].interpretedFunction = new Expression(record.getBaselineFunctionText(), a, b, c, beta, deltaQ, q0, q1, incidence, prevalence, population);
			functions[1].interpretedFunction.disableImpliedMultiplicationMode();
			functions[1].requiredExpressionVariables = Arrays.asList(functions[1].interpretedFunction.getMissingUserDefinedArguments());
			functions[1].interpretedFunction.defineArguments(functions[1].interpretedFunction.getMissingUserDefinedArguments());
		}

		return functions;
	}

	/**
	 * 
	 * @param id
	 * @return the function definition for a given hif.
	 */
	public static Record getFunctionDefinition(Integer id) {

		// Load the function by id
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());

		Record record = create
				.select(HEALTH_IMPACT_FUNCTION.asterisk()
						,ENDPOINT_GROUP.NAME.as("endpoint_group_name")
						,ENDPOINT.NAME.as("endpoint_name")
						,POLLUTANT.NAME.as("pollutant_name")
						,POLLUTANT.FRIENDLY_NAME.as("pollutant_friendly_name")
						,POLLUTANT_METRIC.NAME.as("metric_name")
						,SEASONAL_METRIC.NAME.as("seasonal_metric_name")
						,STATISTIC_TYPE.NAME.as("metric_statistic_name")
						,RACE.NAME.as("race_name")
						,GENDER.NAME.as("gender_name")
						,ETHNICITY.NAME.as("ethnicity_name")
						)
				.from(HEALTH_IMPACT_FUNCTION)
				.leftJoin(ENDPOINT_GROUP).on(ENDPOINT_GROUP.ID.eq(HEALTH_IMPACT_FUNCTION.ENDPOINT_GROUP_ID))
				.leftJoin(ENDPOINT).on(ENDPOINT.ID.eq(HEALTH_IMPACT_FUNCTION.ENDPOINT_ID))
				.leftJoin(RACE).on(HEALTH_IMPACT_FUNCTION.RACE_ID.eq(RACE.ID))
				.leftJoin(GENDER).on(HEALTH_IMPACT_FUNCTION.GENDER_ID.eq(GENDER.ID))
				.leftJoin(ETHNICITY).on(HEALTH_IMPACT_FUNCTION.ETHNICITY_ID.eq(ETHNICITY.ID))
				.leftJoin(POLLUTANT).on(POLLUTANT.ID.eq(HEALTH_IMPACT_FUNCTION.POLLUTANT_ID))
				.leftJoin(POLLUTANT_METRIC).on(POLLUTANT_METRIC.ID.eq(HEALTH_IMPACT_FUNCTION.METRIC_ID))
				.leftJoin(SEASONAL_METRIC).on(SEASONAL_METRIC.ID.eq(HEALTH_IMPACT_FUNCTION.SEASONAL_METRIC_ID))
				.leftJoin(STATISTIC_TYPE).on(STATISTIC_TYPE.ID.eq(HEALTH_IMPACT_FUNCTION.METRIC_STATISTIC))			
				.where(HEALTH_IMPACT_FUNCTION.ID.eq(id))
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
	public static void storeResults(Task task, HIFTaskConfig hifTaskConfig, Vector<HifResultRecord> hifResults) {
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());
		
		Integer hifResultDatasetId = create
				.selectFrom(HIF_RESULT_DATASET)
				.where(HIF_RESULT_DATASET.TASK_UUID.eq(task.getUuid()))
				.fetchOne(HIF_RESULT_DATASET.ID);
		
		// If this is the first call, we need to store the dataset header record and the function_config records
		if(hifResultDatasetId == null) {	
			// HIF result dataset record links the result dataset id to the task uuid
			HifResultDatasetRecord hifResultDatasetRecord = create.insertInto(
					HIF_RESULT_DATASET
					, HIF_RESULT_DATASET.TASK_UUID
					, HIF_RESULT_DATASET.NAME
					, HIF_RESULT_DATASET.POPULATION_DATASET_ID
					, HIF_RESULT_DATASET.POPULATION_YEAR
					, HIF_RESULT_DATASET.BASELINE_AQ_LAYER_ID
					, HIF_RESULT_DATASET.SCENARIO_AQ_LAYER_ID
					, HIF_RESULT_DATASET.GRID_DEFINITION_ID
					, HIF_RESULT_DATASET.USER_ID
					)
			.values(
					task.getUuid()
					, hifTaskConfig.name
					, hifTaskConfig.popId
					, hifTaskConfig.popYear
					, hifTaskConfig.aqBaselineId
					, hifTaskConfig.aqScenarioId
					, hifTaskConfig.gridDefinitionId
					, task.getUserIdentifier())
			.returning(HIF_RESULT_DATASET.ID)
			.fetchOne();
			
			hifResultDatasetId = hifResultDatasetRecord.getId();
			hifTaskConfig.resultDatasetId = hifResultDatasetId;
			
			// Each HIF result function config contains the details of how the function was configured
			for(HIFConfig hif : hifTaskConfig.hifs) {
				create.insertInto(HIF_RESULT_FUNCTION_CONFIG
						, HIF_RESULT_FUNCTION_CONFIG.HIF_RESULT_DATASET_ID
						, HIF_RESULT_FUNCTION_CONFIG.HIF_ID
						, HIF_RESULT_FUNCTION_CONFIG.START_AGE
						, HIF_RESULT_FUNCTION_CONFIG.END_AGE
						, HIF_RESULT_FUNCTION_CONFIG.RACE_ID
						, HIF_RESULT_FUNCTION_CONFIG.GENDER_ID
						, HIF_RESULT_FUNCTION_CONFIG.ETHNICITY_ID
						, HIF_RESULT_FUNCTION_CONFIG.METRIC_ID
						, HIF_RESULT_FUNCTION_CONFIG.SEASONAL_METRIC_ID
						, HIF_RESULT_FUNCTION_CONFIG.METRIC_STATISTIC
						, HIF_RESULT_FUNCTION_CONFIG.INCIDENCE_DATASET_ID
						, HIF_RESULT_FUNCTION_CONFIG.PREVALENCE_DATASET_ID
						, HIF_RESULT_FUNCTION_CONFIG.VARIABLE_DATASET_ID)
				.values(hifResultDatasetId
						, hif.hifId
						, hif.startAge
						, hif.endAge
						, hif.race
						, hif.gender
						, hif.ethnicity
						, hif.metric
						, hif.seasonalMetric
						, hif.metricStatistic
						, hif.incidence
						, hif.prevalence
						, hif.variable)
				.execute();
				
			}
		}

		// Finally, store the actual estimates
		for(HifResultRecord hifResult : hifResults) {
			hifResult.setHifResultDatasetId(hifResultDatasetId);
		}
		
		create
		.batchInsert(hifResults)
		.execute();	
	}

	/**
	 * Selects the most appropriate incidence and prevalence dataset and year for a given function.
	 * First, tries the user's default incidence/prevalence selection.
	 * If that doesn't work, tries to use the selection in the health impact function config.
	 * Failing that, it resorts to default datasets that contain data for the endpoint group.
	 * In all cases, the selected year is the closest available to the population year.
	 * @param function
	 * @param popYear
	 * @param defaultIncidencePrevalenceDataset
	 * @param functionIncidenceDataset
	 * @param functionPrevalenceDataset
	 */
	public static void setIncidencePrevalence(HIFConfig hifConfig, Scenario scenario, ScenarioPopConfig scenarioPopConfig, ScenarioHIFConfig scenarioHIFConfig, int defaultIncidencePrevalenceDataset, Boolean userPrefered) {

		
		try {
			//int endpointGroupId = function.get("endpoint_group_id").asInt();
			Map<String, Object> function = hifConfig.hifRecord;
			
			int endpointId = (Integer) function.get("endpoint_id");
			int raceId = (Integer) function.get("race_id");
			int genderId = (Integer) function.get("gender_id");
			int ethnicityId = (Integer) function.get("ethnicity_id");
			String functionText = function.get("function_text").toString().toLowerCase();
			boolean isPrevalenceFunction = functionText.contains("prevalence");
			boolean isIncidenceFunction = functionText.contains("incidence");
			boolean defaultDatasetSupportsIncidenceForEndpoint = false;
			boolean defaultDatasetSupportsPrevalenceForEndpoint = false;
			
			int incidenceRace = 0;
			int incidenceEthnicity = 0;
			int incidenceGender = 0;
			int prevalenceRace = 0;
			int prevalenceEthnicity = 0;
			int prevalenceGender = 0;

			if (isIncidenceFunction) {
				
				//step 1: check if the default dataset a perfect match	
				if(defaultIncidencePrevalenceDataset != 0) {
					incidenceRace = raceId;
					incidenceEthnicity = ethnicityId;
					incidenceGender = genderId;
					Record1<Integer> countIncidence = DSL.using(JooqUtil.getJooqConfiguration())
							.select(DSL.count())
							.from(INCIDENCE_ENTRY)
							.where(INCIDENCE_ENTRY.INCIDENCE_DATASET_ID.eq(defaultIncidencePrevalenceDataset)
									.and(INCIDENCE_ENTRY.PREVALENCE.ne(true))
									.and(INCIDENCE_ENTRY.ENDPOINT_ID.eq(endpointId))
									.and(DSL.when(INCIDENCE_ENTRY.RACE_ID.eq(6), 5).otherwise(INCIDENCE_ENTRY.RACE_ID).eq(raceId))
									.and(DSL.when(INCIDENCE_ENTRY.GENDER_ID.eq(4), 3).otherwise(INCIDENCE_ENTRY.GENDER_ID).eq(genderId))
									.and(DSL.when(INCIDENCE_ENTRY.ETHNICITY_ID.eq(4), 3).otherwise(INCIDENCE_ENTRY.ETHNICITY_ID).eq(ethnicityId)))
							.fetchOne();
					
					//step 2: if the default dataset is not a perfect match, check if it's a partial match (dataset's group (e.g. ALL) includes hif group (e.g. White))
//					if(countIncidence.value1() <= 0 && userPrefered) {
//						countIncidence = DSL.using(JooqUtil.getJooqConfiguration())
//								.select(DSL.count())
//								.from(INCIDENCE_ENTRY)
//								.where(INCIDENCE_ENTRY.INCIDENCE_DATASET_ID.eq(defaultIncidencePrevalenceDataset)
//										.and(INCIDENCE_ENTRY.PREVALENCE.ne(true))
//										.and(INCIDENCE_ENTRY.ENDPOINT_ID.eq(endpointId))
//										.and((DSL.when(INCIDENCE_ENTRY.RACE_ID.eq(6), 5).otherwise(INCIDENCE_ENTRY.RACE_ID).eq(5))
//												.or(DSL.when(INCIDENCE_ENTRY.RACE_ID.eq(6), 5).otherwise(INCIDENCE_ENTRY.RACE_ID).eq(raceId)))
//										.and((DSL.when(INCIDENCE_ENTRY.GENDER_ID.eq(4), 3).otherwise(INCIDENCE_ENTRY.GENDER_ID).eq(3))
//												.or(DSL.when(INCIDENCE_ENTRY.GENDER_ID.eq(4), 3).otherwise(INCIDENCE_ENTRY.GENDER_ID).eq(genderId)))
//										.and((DSL.when(INCIDENCE_ENTRY.ETHNICITY_ID.eq(4), 3).otherwise(INCIDENCE_ENTRY.ETHNICITY_ID).eq(3)))
//										.or(DSL.when(INCIDENCE_ENTRY.ETHNICITY_ID.eq(4), 3).otherwise(INCIDENCE_ENTRY.ETHNICITY_ID).eq(ethnicityId)))
//								.fetchAny();				
//					}
					defaultDatasetSupportsIncidenceForEndpoint = countIncidence.value1() > 0;
				}
				
				if (defaultDatasetSupportsIncidenceForEndpoint) {
					hifConfig.incidence = defaultIncidencePrevalenceDataset;
					scenarioHIFConfig.incidenceYear = HIFUtil.getClosestIncidenceYear(defaultIncidencePrevalenceDataset, false, endpointId, scenarioPopConfig.popYear);
					hifConfig.incidenceName = IncidenceApi.getIncidenceDatasetName(defaultIncidencePrevalenceDataset);
					hifConfig.incidenceRace = incidenceRace;
					hifConfig.incidenceEthnicity = incidenceEthnicity;
					hifConfig.incidenceGender = incidenceGender;

				} else if (hifConfig.incidence != null && hifConfig.incidence != 0 && 1==2) {
					// As of August 2022 we want to ignore Incidence/Prevalence dataset assigned in HIF dataset thus using 1==2 here.
					
					//function.put("incidence_dataset_id", functionIncidenceDataset);
					//function.put("incidence_year", HIFUtil.getClosestIncidenceYear(functionIncidenceDataset, false, endpointGroupId, popYear));
					//function.put("incidence_dataset_name", IncidenceApi.getIncidenceDatasetName(functionIncidenceDataset));

				} else {
					// Find perfect match. If doesn't exist, use 'ALL, ALL, ALL'
					int dsId = 0;
					
					//TODO: I think this logic will work with existing datasets, but we will need to expand this to pull multiple records and then allow the user to choose 
					// since there could be more than one "best" id.

					Record1<Integer> bestId = DSL.using(JooqUtil.getJooqConfiguration())
							.selectDistinct(INCIDENCE_ENTRY.INCIDENCE_DATASET_ID)
							.from(INCIDENCE_ENTRY)
							.where(INCIDENCE_ENTRY.PREVALENCE.ne(true)
									.and(INCIDENCE_ENTRY.ENDPOINT_ID.eq(endpointId))
									.and(DSL.when(INCIDENCE_ENTRY.RACE_ID.eq(6), 5).otherwise(INCIDENCE_ENTRY.RACE_ID).eq(raceId))
									.and(DSL.when(INCIDENCE_ENTRY.GENDER_ID.eq(4), 3).otherwise(INCIDENCE_ENTRY.GENDER_ID).eq(genderId))
									.and(DSL.when(INCIDENCE_ENTRY.ETHNICITY_ID.eq(4), 3).otherwise(INCIDENCE_ENTRY.ETHNICITY_ID).eq(ethnicityId)))
							.fetchAny();
					if(bestId != null && bestId.value1() !=null) {
						dsId = bestId.value1();
						incidenceRace = raceId;
						incidenceEthnicity = ethnicityId;
						incidenceGender = genderId;
					}
					else {
						bestId = DSL.using(JooqUtil.getJooqConfiguration())
								.selectDistinct(INCIDENCE_ENTRY.INCIDENCE_DATASET_ID)
								.from(INCIDENCE_ENTRY)
								.where(INCIDENCE_ENTRY.PREVALENCE.ne(true)
										.and(INCIDENCE_ENTRY.ENDPOINT_ID.eq(endpointId))
										.and(DSL.when(INCIDENCE_ENTRY.RACE_ID.eq(6), 5).otherwise(INCIDENCE_ENTRY.RACE_ID).eq(5))
										.and(DSL.when(INCIDENCE_ENTRY.GENDER_ID.eq(4), 3).otherwise(INCIDENCE_ENTRY.GENDER_ID).eq(3))
										.and(DSL.when(INCIDENCE_ENTRY.ETHNICITY_ID.eq(4), 3).otherwise(INCIDENCE_ENTRY.ETHNICITY_ID).eq(3)))
								.fetchAny();
						if(bestId != null && bestId.value1() !=null) {
							dsId = bestId.value1();
							incidenceRace = 5;
							incidenceEthnicity = 3;
							incidenceGender = 3;
						}
					}
					
					if(dsId!=0) {
						hifConfig.incidence = dsId; 
						scenarioHIFConfig.incidenceYear = HIFUtil.getClosestIncidenceYear(dsId, false, endpointId, scenarioPopConfig.popYear);
						hifConfig.incidenceName = IncidenceApi.getIncidenceDatasetName(dsId);
						hifConfig.incidenceRace = incidenceRace;
						hifConfig.incidenceEthnicity = incidenceEthnicity;
						hifConfig.incidenceGender = incidenceGender;
					}
					else {
						//No incidence dataset can be found to work with the HIF
						hifConfig.incidence = null; 
						scenarioHIFConfig.incidenceYear = null;
						hifConfig.incidenceName = null;
						hifConfig.incidenceRace = null;
						hifConfig.incidenceEthnicity = null;
						hifConfig.incidenceGender = null;
					}			 
				}
			} else {
				hifConfig.incidence = null; 
				scenarioHIFConfig.incidenceYear = null;
				hifConfig.incidenceName = null;
				hifConfig.incidenceRace = null;
				hifConfig.incidenceEthnicity = null;
				hifConfig.incidenceGender = null;
			}
					
			if(isPrevalenceFunction) {
				if(defaultIncidencePrevalenceDataset != 0) {		
					//Determine if this dataset supports incidence and/or prevalence for this function's endpoint group
					prevalenceRace = raceId;
					prevalenceEthnicity = ethnicityId;
					prevalenceGender = genderId;
					Record1<Integer> countPrevalence = DSL.using(JooqUtil.getJooqConfiguration())
							.select(DSL.count())
							.from(INCIDENCE_ENTRY)
							.where(INCIDENCE_ENTRY.INCIDENCE_DATASET_ID.eq(defaultIncidencePrevalenceDataset)
									.and(INCIDENCE_ENTRY.PREVALENCE.eq(true))
									.and(INCIDENCE_ENTRY.ENDPOINT_ID.eq(endpointId))
									.and(DSL.when(INCIDENCE_ENTRY.RACE_ID.eq(6), 5).otherwise(INCIDENCE_ENTRY.RACE_ID).eq(raceId))
									.and(DSL.when(INCIDENCE_ENTRY.GENDER_ID.eq(4), 3).otherwise(INCIDENCE_ENTRY.GENDER_ID).eq(genderId))
									.and(DSL.when(INCIDENCE_ENTRY.ETHNICITY_ID.eq(4), 3).otherwise(INCIDENCE_ENTRY.ETHNICITY_ID).eq(ethnicityId)))
							.fetchOne();
					
//					if(countPrevalence.value1() <= 0 && userPrefered) {
//						countPrevalence = DSL.using(JooqUtil.getJooqConfiguration())
//								.select(DSL.count())
//								.from(INCIDENCE_ENTRY)
//								.where(INCIDENCE_ENTRY.INCIDENCE_DATASET_ID.eq(defaultIncidencePrevalenceDataset)
//										.and(INCIDENCE_ENTRY.PREVALENCE.eq(true))
//										.and(INCIDENCE_ENTRY.ENDPOINT_ID.eq(endpointId))
//										.and((DSL.when(INCIDENCE_ENTRY.RACE_ID.eq(6), 5).otherwise(INCIDENCE_ENTRY.RACE_ID).eq(5))
//												.or(DSL.when(INCIDENCE_ENTRY.RACE_ID.eq(6), 5).otherwise(INCIDENCE_ENTRY.RACE_ID).eq(raceId)))
//										.and((DSL.when(INCIDENCE_ENTRY.GENDER_ID.eq(4), 3).otherwise(INCIDENCE_ENTRY.GENDER_ID).eq(3))
//												.or(DSL.when(INCIDENCE_ENTRY.GENDER_ID.eq(4), 3).otherwise(INCIDENCE_ENTRY.GENDER_ID).eq(genderId)))
//										.and((DSL.when(INCIDENCE_ENTRY.ETHNICITY_ID.eq(4), 3).otherwise(INCIDENCE_ENTRY.ETHNICITY_ID).eq(3)))
//										.or(DSL.when(INCIDENCE_ENTRY.ETHNICITY_ID.eq(4), 3).otherwise(INCIDENCE_ENTRY.ETHNICITY_ID).eq(ethnicityId)))
//								.fetchOne();
//					}
					defaultDatasetSupportsPrevalenceForEndpoint = countPrevalence.value1() > 0;
				}
				
				
				if (defaultDatasetSupportsPrevalenceForEndpoint) {
					hifConfig.prevalence = defaultIncidencePrevalenceDataset;
					scenarioHIFConfig.prevalenceYear = HIFUtil.getClosestIncidenceYear(defaultIncidencePrevalenceDataset, true, endpointId, scenarioPopConfig.popYear);
					hifConfig.prevalenceName = IncidenceApi.getIncidenceDatasetName(defaultIncidencePrevalenceDataset);
					hifConfig.prevalenceRace = prevalenceRace;
					hifConfig.prevalenceEthnicity = prevalenceEthnicity;
					hifConfig.prevalenceGender = prevalenceGender;
				} else if (hifConfig.prevalence != null && hifConfig.prevalence != 0 && 1==2) {
					// As of August 2022 we want to ignore Incidence/Prevalence dataset assigned in HIF dataset thus using 1==2 here.
					
					// Default selection won't work so use what's defined in the function
					//function.put("prevalence_dataset_id", functionPrevalenceDataset);
					//function.put("prevalence_year", HIFUtil.getClosestIncidenceYear(functionPrevalenceDataset, true, endpointGroupId, popYear));
					//function.put("prevalence_dataset_name", IncidenceApi.getIncidenceDatasetName(functionPrevalenceDataset));
				} else {
					// Find perfect match. If doesn't exist, use 'ALL, ALL, ALL'
					//int dsId = (endpointGroupId == 30 || endpointGroupId == 28) ? 3 : 1; //National Incidence & Prevalence OR Prevalence
					int dsId = 0;
					
					Record1<Integer> bestId = DSL.using(JooqUtil.getJooqConfiguration())
							.selectDistinct(INCIDENCE_ENTRY.INCIDENCE_DATASET_ID)
							.from(INCIDENCE_ENTRY)
							.where(INCIDENCE_ENTRY.PREVALENCE.eq(true)
									.and(INCIDENCE_ENTRY.ENDPOINT_ID.eq(endpointId))
									.and(DSL.when(INCIDENCE_ENTRY.RACE_ID.eq(6), 5).otherwise(INCIDENCE_ENTRY.RACE_ID).eq(raceId))
									.and(DSL.when(INCIDENCE_ENTRY.GENDER_ID.eq(4), 3).otherwise(INCIDENCE_ENTRY.GENDER_ID).eq(genderId))
									.and(DSL.when(INCIDENCE_ENTRY.ETHNICITY_ID.eq(4), 3).otherwise(INCIDENCE_ENTRY.ETHNICITY_ID).eq(ethnicityId)))
							.fetchAny();
					if(bestId != null && bestId.value1() !=null) {
						dsId = bestId.value1();
						prevalenceRace = raceId;
						prevalenceEthnicity = ethnicityId;
						prevalenceGender = genderId;
					}
					else {
						bestId = DSL.using(JooqUtil.getJooqConfiguration())
								.selectDistinct(INCIDENCE_ENTRY.INCIDENCE_DATASET_ID)
								.from(INCIDENCE_ENTRY)
								.where(INCIDENCE_ENTRY.PREVALENCE.eq(true)
										.and(INCIDENCE_ENTRY.ENDPOINT_ID.eq(endpointId))
										.and(DSL.when(INCIDENCE_ENTRY.RACE_ID.eq(6), 5).otherwise(INCIDENCE_ENTRY.RACE_ID).eq(5))
										.and(DSL.when(INCIDENCE_ENTRY.GENDER_ID.eq(4), 3).otherwise(INCIDENCE_ENTRY.GENDER_ID).eq(3))
										.and(DSL.when(INCIDENCE_ENTRY.ETHNICITY_ID.eq(4), 3).otherwise(INCIDENCE_ENTRY.ETHNICITY_ID).eq(3)))
								.fetchAny();
						if(bestId != null && bestId.value1() !=null) {
							dsId = bestId.value1();
							prevalenceRace = 5;
							prevalenceEthnicity = 3;
							prevalenceGender = 3;
						}
					}						
					
					if(dsId!=0) {
						hifConfig.prevalence = dsId; 
						scenarioHIFConfig.prevalenceYear = HIFUtil.getClosestIncidenceYear(dsId, true, endpointId, scenarioPopConfig.popYear);
						hifConfig.prevalenceName = IncidenceApi.getIncidenceDatasetName(dsId);
						hifConfig.prevalenceRace = prevalenceRace;
						hifConfig.prevalenceEthnicity = prevalenceEthnicity;
						hifConfig.prevalenceGender = prevalenceGender;
					}
					else {
						//No prevalence dataset can be found to work with the HIF
						hifConfig.prevalence = null; 
						scenarioHIFConfig.prevalenceYear = null;
						hifConfig.prevalenceName = null;
						hifConfig.prevalenceRace = null;
						hifConfig.prevalenceEthnicity = null;
						hifConfig.prevalenceGender = null;
					}					
				}
			} else {
				hifConfig.prevalence = null; 
				scenarioHIFConfig.prevalenceYear = null;
				hifConfig.prevalenceName = null;
				hifConfig.prevalenceRace = null;
				hifConfig.prevalenceEthnicity = null;
				hifConfig.prevalenceGender = null;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}

	/**
	 * TEMPORARILY RETAINED FOR BACKWARD COMPATABILITY WHILE BATCH TASKS ARE BEING IMPLEMENTED
	 * 
	 * Selects the most appropriate incidence and prevalence dataset and year for a given function.
	 * First, tries the user's default incidence/prevalence selection.
	 * If that doesn't work, tries to use the selection in the health impact function config.
	 * Failing that, it resorts to default datasets that contain data for the endpoint group.
	 * In all cases, the selected year is the closest available to the population year.
	 * @param function
	 * @param popYear
	 * @param defaultIncidencePrevalenceDataset
	 * @param functionIncidenceDataset
	 * @param functionPrevalenceDataset
	 */
	public static void setIncidencePrevalenceV1(ObjectNode function, int popYear, int defaultIncidencePrevalenceDataset, Integer functionIncidenceDataset, Integer functionPrevalenceDataset, Boolean userPrefered) {

		
		try {
			//int endpointGroupId = function.get("endpoint_group_id").asInt();
			int endpointId = function.get("endpoint_id").asInt();
			int raceId = function.get("race_id").asInt();
			int genderId = function.get("gender_id").asInt();
			int ethnicityId = function.get("ethnicity_id").asInt();
			String functionText = function.get("function_text").asText().toLowerCase();
			boolean isPrevalenceFunction = functionText.contains("prevalence");
			boolean isIncidenceFunction = functionText.contains("incidence");
			boolean defaultDatasetSupportsIncidenceForEndpoint = false;
			boolean defaultDatasetSupportsPrevalenceForEndpoint = false;
			
			int incidenceRace = 0;
			int incidenceEthnicity = 0;
			int incidenceGender = 0;
			int prevalenceRace = 0;
			int prevalenceEthnicity = 0;
			int prevalenceGender = 0;

			if (isIncidenceFunction) {
				
				//step 1: check if the default dataset a perfect match	
				if(defaultIncidencePrevalenceDataset != 0) {
					incidenceRace = raceId;
					incidenceEthnicity = ethnicityId;
					incidenceGender = genderId;
					Record1<Integer> countIncidence = DSL.using(JooqUtil.getJooqConfiguration())
							.select(DSL.count())
							.from(INCIDENCE_ENTRY)
							.where(INCIDENCE_ENTRY.INCIDENCE_DATASET_ID.eq(defaultIncidencePrevalenceDataset)
									.and(INCIDENCE_ENTRY.PREVALENCE.ne(true))
									.and(INCIDENCE_ENTRY.ENDPOINT_ID.eq(endpointId))
									.and(DSL.when(INCIDENCE_ENTRY.RACE_ID.eq(6), 5).otherwise(INCIDENCE_ENTRY.RACE_ID).eq(raceId))
									.and(DSL.when(INCIDENCE_ENTRY.GENDER_ID.eq(4), 3).otherwise(INCIDENCE_ENTRY.GENDER_ID).eq(genderId))
									.and(DSL.when(INCIDENCE_ENTRY.ETHNICITY_ID.eq(4), 3).otherwise(INCIDENCE_ENTRY.ETHNICITY_ID).eq(ethnicityId)))
							.fetchOne();
					
					//step 2: if the default dataset is not a perfect match, check if it's a partial match (dataset's group (e.g. ALL) includes hif group (e.g. White))
//					if(countIncidence.value1() <= 0 && userPrefered) {
//						countIncidence = DSL.using(JooqUtil.getJooqConfiguration())
//								.select(DSL.count())
//								.from(INCIDENCE_ENTRY)
//								.where(INCIDENCE_ENTRY.INCIDENCE_DATASET_ID.eq(defaultIncidencePrevalenceDataset)
//										.and(INCIDENCE_ENTRY.PREVALENCE.ne(true))
//										.and(INCIDENCE_ENTRY.ENDPOINT_ID.eq(endpointId))
//										.and((DSL.when(INCIDENCE_ENTRY.RACE_ID.eq(6), 5).otherwise(INCIDENCE_ENTRY.RACE_ID).eq(5))
//												.or(DSL.when(INCIDENCE_ENTRY.RACE_ID.eq(6), 5).otherwise(INCIDENCE_ENTRY.RACE_ID).eq(raceId)))
//										.and((DSL.when(INCIDENCE_ENTRY.GENDER_ID.eq(4), 3).otherwise(INCIDENCE_ENTRY.GENDER_ID).eq(3))
//												.or(DSL.when(INCIDENCE_ENTRY.GENDER_ID.eq(4), 3).otherwise(INCIDENCE_ENTRY.GENDER_ID).eq(genderId)))
//										.and((DSL.when(INCIDENCE_ENTRY.ETHNICITY_ID.eq(4), 3).otherwise(INCIDENCE_ENTRY.ETHNICITY_ID).eq(3)))
//										.or(DSL.when(INCIDENCE_ENTRY.ETHNICITY_ID.eq(4), 3).otherwise(INCIDENCE_ENTRY.ETHNICITY_ID).eq(ethnicityId)))
//								.fetchAny();				
//					}
					defaultDatasetSupportsIncidenceForEndpoint = countIncidence.value1() > 0;
				}
				
				if (defaultDatasetSupportsIncidenceForEndpoint) {
					function.put("incidence_dataset_id", defaultIncidencePrevalenceDataset);
					function.put("incidence_year", HIFUtil.getClosestIncidenceYear(defaultIncidencePrevalenceDataset, false, endpointId, popYear));
					function.put("incidence_dataset_name", IncidenceApi.getIncidenceDatasetName(defaultIncidencePrevalenceDataset));
					function.put("incidence_race", incidenceRace);
					function.put("incidence_ethnicity", incidenceEthnicity);
					function.put("incidence_gender", incidenceGender);

				} else if (functionIncidenceDataset != null && functionIncidenceDataset != 0 && 1==2) {
					// As of August 2022 we want to ignore Incidence/Prevalence dataset assigned in HIF dataset thus using 1==2 here.
					
					//function.put("incidence_dataset_id", functionIncidenceDataset);
					//function.put("incidence_year", HIFUtil.getClosestIncidenceYear(functionIncidenceDataset, false, endpointGroupId, popYear));
					//function.put("incidence_dataset_name", IncidenceApi.getIncidenceDatasetName(functionIncidenceDataset));

				} else {
					// Find perfect match. If doesn't exist, use 'ALL, ALL, ALL'
					int dsId = 0;
					
					//TODO: I think this logic will work with existing datasets, but we will need to expand this to pull multiple records and then allow the user to choose 
					// since there could be more than one "best" id.

					Record1<Integer> bestId = DSL.using(JooqUtil.getJooqConfiguration())
							.selectDistinct(INCIDENCE_ENTRY.INCIDENCE_DATASET_ID)
							.from(INCIDENCE_ENTRY)
							.where(INCIDENCE_ENTRY.PREVALENCE.ne(true)
									.and(INCIDENCE_ENTRY.ENDPOINT_ID.eq(endpointId))
									.and(DSL.when(INCIDENCE_ENTRY.RACE_ID.eq(6), 5).otherwise(INCIDENCE_ENTRY.RACE_ID).eq(raceId))
									.and(DSL.when(INCIDENCE_ENTRY.GENDER_ID.eq(4), 3).otherwise(INCIDENCE_ENTRY.GENDER_ID).eq(genderId))
									.and(DSL.when(INCIDENCE_ENTRY.ETHNICITY_ID.eq(4), 3).otherwise(INCIDENCE_ENTRY.ETHNICITY_ID).eq(ethnicityId)))
							.fetchAny();
					if(bestId != null && bestId.value1() !=null) {
						dsId = bestId.value1();
						incidenceRace = raceId;
						incidenceEthnicity = ethnicityId;
						incidenceGender = genderId;
					}
					else {
						bestId = DSL.using(JooqUtil.getJooqConfiguration())
								.selectDistinct(INCIDENCE_ENTRY.INCIDENCE_DATASET_ID)
								.from(INCIDENCE_ENTRY)
								.where(INCIDENCE_ENTRY.PREVALENCE.ne(true)
										.and(INCIDENCE_ENTRY.ENDPOINT_ID.eq(endpointId))
										.and(DSL.when(INCIDENCE_ENTRY.RACE_ID.eq(6), 5).otherwise(INCIDENCE_ENTRY.RACE_ID).eq(5))
										.and(DSL.when(INCIDENCE_ENTRY.GENDER_ID.eq(4), 3).otherwise(INCIDENCE_ENTRY.GENDER_ID).eq(3))
										.and(DSL.when(INCIDENCE_ENTRY.ETHNICITY_ID.eq(4), 3).otherwise(INCIDENCE_ENTRY.ETHNICITY_ID).eq(3)))
								.fetchAny();
						if(bestId != null && bestId.value1() !=null) {
							dsId = bestId.value1();
							incidenceRace = 5;
							incidenceEthnicity = 3;
							incidenceGender = 3;
						}
					}
					
					if(dsId!=0) {
						function.put("incidence_dataset_id", dsId); 
						function.put("incidence_year", HIFUtil.getClosestIncidenceYear(dsId, false, endpointId, popYear));
						function.put("incidence_dataset_name", IncidenceApi.getIncidenceDatasetName(dsId));
						function.put("incidence_race", incidenceRace);
						function.put("incidence_ethnicity", incidenceEthnicity);
						function.put("incidence_gender", incidenceGender);
					}
					else {
						//No incidence dataset can be found to work with the HIF
						function.putNull("incidence_dataset_id");
						function.putNull("incidence_year");
						function.putNull("incidence_dataset_name");
						function.putNull("incidence_race");
						function.putNull("incidence_ethnicity");
						function.putNull("incidence_gender");
					}			 
				}
			} else {
				function.putNull("incidence_dataset_id");
				function.putNull("incidence_year");
				function.putNull("incidence_dataset_name");
				function.putNull("incidence_race");
				function.putNull("incidence_ethnicity");
				function.putNull("incidence_gender");
			}
					
			if(isPrevalenceFunction) {
				if(defaultIncidencePrevalenceDataset != 0) {		
					//Determine if this dataset supports incidence and/or prevalence for this function's endpoint group
					prevalenceRace = raceId;
					prevalenceEthnicity = ethnicityId;
					prevalenceGender = genderId;
					Record1<Integer> countPrevalence = DSL.using(JooqUtil.getJooqConfiguration())
							.select(DSL.count())
							.from(INCIDENCE_ENTRY)
							.where(INCIDENCE_ENTRY.INCIDENCE_DATASET_ID.eq(defaultIncidencePrevalenceDataset)
									.and(INCIDENCE_ENTRY.PREVALENCE.eq(true))
									.and(INCIDENCE_ENTRY.ENDPOINT_ID.eq(endpointId))
									.and(DSL.when(INCIDENCE_ENTRY.RACE_ID.eq(6), 5).otherwise(INCIDENCE_ENTRY.RACE_ID).eq(raceId))
									.and(DSL.when(INCIDENCE_ENTRY.GENDER_ID.eq(4), 3).otherwise(INCIDENCE_ENTRY.GENDER_ID).eq(genderId))
									.and(DSL.when(INCIDENCE_ENTRY.ETHNICITY_ID.eq(4), 3).otherwise(INCIDENCE_ENTRY.ETHNICITY_ID).eq(ethnicityId)))
							.fetchOne();
					
//					if(countPrevalence.value1() <= 0 && userPrefered) {
//						countPrevalence = DSL.using(JooqUtil.getJooqConfiguration())
//								.select(DSL.count())
//								.from(INCIDENCE_ENTRY)
//								.where(INCIDENCE_ENTRY.INCIDENCE_DATASET_ID.eq(defaultIncidencePrevalenceDataset)
//										.and(INCIDENCE_ENTRY.PREVALENCE.eq(true))
//										.and(INCIDENCE_ENTRY.ENDPOINT_ID.eq(endpointId))
//										.and((DSL.when(INCIDENCE_ENTRY.RACE_ID.eq(6), 5).otherwise(INCIDENCE_ENTRY.RACE_ID).eq(5))
//												.or(DSL.when(INCIDENCE_ENTRY.RACE_ID.eq(6), 5).otherwise(INCIDENCE_ENTRY.RACE_ID).eq(raceId)))
//										.and((DSL.when(INCIDENCE_ENTRY.GENDER_ID.eq(4), 3).otherwise(INCIDENCE_ENTRY.GENDER_ID).eq(3))
//												.or(DSL.when(INCIDENCE_ENTRY.GENDER_ID.eq(4), 3).otherwise(INCIDENCE_ENTRY.GENDER_ID).eq(genderId)))
//										.and((DSL.when(INCIDENCE_ENTRY.ETHNICITY_ID.eq(4), 3).otherwise(INCIDENCE_ENTRY.ETHNICITY_ID).eq(3)))
//										.or(DSL.when(INCIDENCE_ENTRY.ETHNICITY_ID.eq(4), 3).otherwise(INCIDENCE_ENTRY.ETHNICITY_ID).eq(ethnicityId)))
//								.fetchOne();
//					}
					defaultDatasetSupportsPrevalenceForEndpoint = countPrevalence.value1() > 0;
				}
				
				
				if (defaultDatasetSupportsPrevalenceForEndpoint) {
					function.put("prevalence_dataset_id", defaultIncidencePrevalenceDataset);
					function.put("prevalence_year", HIFUtil.getClosestIncidenceYear(defaultIncidencePrevalenceDataset, true, endpointId, popYear));
					function.put("prevalence_dataset_name", IncidenceApi.getIncidenceDatasetName(defaultIncidencePrevalenceDataset));
					function.put("prevalence_race", prevalenceRace);
					function.put("prevalence_ethnicity", prevalenceEthnicity);
					function.put("prevalence_gender", prevalenceGender);
				} else if (functionPrevalenceDataset != null && functionPrevalenceDataset != 0 && 1==2) {
					// As of August 2022 we want to ignore Incidence/Prevalence dataset assigned in HIF dataset thus using 1==2 here.
					
					// Default selection won't work so use what's defined in the function
					//function.put("prevalence_dataset_id", functionPrevalenceDataset);
					//function.put("prevalence_year", HIFUtil.getClosestIncidenceYear(functionPrevalenceDataset, true, endpointGroupId, popYear));
					//function.put("prevalence_dataset_name", IncidenceApi.getIncidenceDatasetName(functionPrevalenceDataset));
				} else {
					// Find perfect match. If doesn't exist, use 'ALL, ALL, ALL'
					//int dsId = (endpointGroupId == 30 || endpointGroupId == 28) ? 3 : 1; //National Incidence & Prevalence OR Prevalence
					int dsId = 0;
					
					Record1<Integer> bestId = DSL.using(JooqUtil.getJooqConfiguration())
							.selectDistinct(INCIDENCE_ENTRY.INCIDENCE_DATASET_ID)
							.from(INCIDENCE_ENTRY)
							.where(INCIDENCE_ENTRY.PREVALENCE.eq(true)
									.and(INCIDENCE_ENTRY.ENDPOINT_ID.eq(endpointId))
									.and(DSL.when(INCIDENCE_ENTRY.RACE_ID.eq(6), 5).otherwise(INCIDENCE_ENTRY.RACE_ID).eq(raceId))
									.and(DSL.when(INCIDENCE_ENTRY.GENDER_ID.eq(4), 3).otherwise(INCIDENCE_ENTRY.GENDER_ID).eq(genderId))
									.and(DSL.when(INCIDENCE_ENTRY.ETHNICITY_ID.eq(4), 3).otherwise(INCIDENCE_ENTRY.ETHNICITY_ID).eq(ethnicityId)))
							.fetchAny();
					if(bestId != null && bestId.value1() !=null) {
						dsId = bestId.value1();
						incidenceRace = raceId;
						incidenceEthnicity = ethnicityId;
						incidenceGender = genderId;
					}
					else {
						bestId = DSL.using(JooqUtil.getJooqConfiguration())
								.selectDistinct(INCIDENCE_ENTRY.INCIDENCE_DATASET_ID)
								.from(INCIDENCE_ENTRY)
								.where(INCIDENCE_ENTRY.PREVALENCE.eq(true)
										.and(INCIDENCE_ENTRY.ENDPOINT_ID.eq(endpointId))
										.and(DSL.when(INCIDENCE_ENTRY.RACE_ID.eq(6), 5).otherwise(INCIDENCE_ENTRY.RACE_ID).eq(5))
										.and(DSL.when(INCIDENCE_ENTRY.GENDER_ID.eq(4), 3).otherwise(INCIDENCE_ENTRY.GENDER_ID).eq(3))
										.and(DSL.when(INCIDENCE_ENTRY.ETHNICITY_ID.eq(4), 3).otherwise(INCIDENCE_ENTRY.ETHNICITY_ID).eq(3)))
								.fetchAny();
						if(bestId != null && bestId.value1() !=null) {
							dsId = bestId.value1();
							incidenceRace = 5;
							incidenceEthnicity = 3;
							incidenceGender = 3;
						}
					}						
					
					if(dsId!=0) {
						function.put("prevalence_dataset_id", dsId); 
						function.put("prevalence_year", HIFUtil.getClosestIncidenceYear(dsId, true, endpointId, popYear));
						function.put("prevalence_dataset_name", IncidenceApi.getIncidenceDatasetName(dsId));
						function.put("prevalence_race", prevalenceRace);
						function.put("prevalence_ethnicity", prevalenceEthnicity);
						function.put("prevalence_gender", prevalenceGender);
					}
					else {
						//No prevalence dataset can be found to work with the HIF
						function.putNull("prevalence_dataset_id");
						function.putNull("prevalence_year");
						function.putNull("prevalence_dataset_name");
						function.putNull("prevalence_race");
						function.putNull("prevalence_ethnicity");
						function.putNull("prevalence_gender");
					}
				}
			} else {
				function.putNull("prevalence_dataset_id");
				function.putNull("prevalence_year");
				function.putNull("prevalence_dataset_name");
				function.putNull("prevalence_race");
				function.putNull("prevalence_ethnicity");
				function.putNull("prevalence_gender");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}

	
	
	
	/**
	 * 
	 * @param defaultIncidencePrevalenceDataset
	 * @param isPrevalence
	 * @param endpointId
	 * @param popYear
	 * @return the closest available incidence year compared to the popYear.
	 */
	private static int getClosestIncidenceYear(int defaultIncidencePrevalenceDataset, boolean isPrevalence, int endpointId, int popYear) {

		Record1<Integer>[] incidenceYears = DSL.using(JooqUtil.getJooqConfiguration())
				.selectDistinct(INCIDENCE_ENTRY.YEAR)
				.from(INCIDENCE_ENTRY)
				.where(INCIDENCE_ENTRY.INCIDENCE_DATASET_ID.eq(defaultIncidencePrevalenceDataset)
						.and(INCIDENCE_ENTRY.ENDPOINT_ID.eq(endpointId))
						.and(DSL.coalesce(INCIDENCE_ENTRY.PREVALENCE, false).eq(isPrevalence))
						)
				.orderBy(INCIDENCE_ENTRY.YEAR)
				.fetchArray();
		
		int[] years = new int[incidenceYears.length];
		int i = 0;
		for(Record1<Integer> year : incidenceYears) {
			years[i++] = year.value1();
		}
		
		if(years.length == 1) {
			return years[0];
		}
		// binarySearch returns the index of the popYear in the dataset's list of supported years
		// If the exact value is not found, a negative is returned. The absolute value of which
		// is the insertion point in the sorted array. Using this information, we can find the 
		// closest match
		int n = Arrays.binarySearch(years, popYear);
		if (n >= 0) {
			return years[n];
		} else {
			int l = Math.abs(n) - 2;
			int u = Math.abs(n) - 1;
			if(u > years.length-1) {
				return years[l];
			} else if (l < 0) {
				return years[0];
			} else if(popYear - years[l] < years[u] - popYear) {
				return years[l];
			} else {
				return years[u];
			}
		}
		 
	}


	/**
	 * Stores the given hifTaskLog in the database.
	 * @param hifTaskLog
	 */
	public static void storeTaskLog(HIFTaskLog hifTaskLog) {
		
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());
		JSON taskLogJson = JSON.json(hifTaskLog.toJsonString());
		
		create.update(HIF_RESULT_DATASET)
			.set(HIF_RESULT_DATASET.TASK_LOG, taskLogJson)
			.where(HIF_RESULT_DATASET.ID.eq(hifTaskLog.getHifTaskConfig().resultDatasetId))
			.execute();
		
	}
	
	/**
	 * 
	 * @param datasetId
	 * @return and HIFTaskLog object, based on the given hif result dataset id.
	 */
	public static HIFTaskLog getTaskLog(Integer datasetId) {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());
		
		//JSON taskLogJson = JSON.json(hifTaskLog.toJsonString());
		
		Record1<JSON> taskLogJson = create.select(HIF_RESULT_DATASET.TASK_LOG)
			.from(HIF_RESULT_DATASET)
			.where(HIF_RESULT_DATASET.ID.eq(datasetId))
			.fetchOne();
		
		try {
			HIFTaskLog hifTaskLog = mapper.readValue(taskLogJson.value1().toString(), HIFTaskLog.class);
			return hifTaskLog;
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
		
	}

	/**
	 * 
	 * @param hifs
	 * @return a unique, sorted list of health effects included in a list of hifs along with a count.
	 */
	public static String getHealthEffectsListFromHifs(List<HIFConfig> hifs) {
		Map<String, Integer> epMap = new TreeMap<String, Integer>();

		for (HIFConfig hif : hifs) {
			Object ep = hif.hifRecord.get("endpoint_name");

			if (ep != null) {
				epMap.put(ep.toString(), epMap.getOrDefault(ep.toString(), 0) + 1);
			}
		}
		
		StringBuilder s = new StringBuilder();
		for(Entry<String, Integer> ep : epMap.entrySet()) {
			s.append("- ").append(ep.getKey()).append(" (").append(ep.getValue()).append(")\n");
		}
		return s.toString();
	}
	
	/**
	 * 
	 * @param hifs
	 * @return a unique, sorted list of health effect groups included in a list of hifs along with a count.
	 */
	public static String getHealthEffectGroupsListFromHifs(List<HIFConfig> hifs) {
		Map<String, Integer> epMap = new TreeMap<String, Integer>();

		for (HIFConfig hif : hifs) {
			Object ep = hif.hifRecord.get("endpoint_group_name");

			if (ep != null) {
				epMap.put(ep.toString(), epMap.getOrDefault(ep.toString(), 0) + 1);
			}
		}
		
		StringBuilder s = new StringBuilder();
		for(Entry<String, Integer> ep : epMap.entrySet()) {
			s.append("- ").append(ep.getKey()).append(" (").append(ep.getValue()).append(")\n");
		}
		return s.toString();
	}

	/**
	 * 
	 * @param hifId
	 * @return an hif heading (containing hif author and id) for the valuation function task log.
	 */
	public static String getHifHeadingForVFTaskLog(Integer hifId) {
		// Load the function by id
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());

		Record record = create
				.select(HEALTH_IMPACT_FUNCTION.asterisk()
						,ENDPOINT_GROUP.NAME.as("endpoint_group_name")
						,ENDPOINT.NAME.as("endpoint_name")
						,POLLUTANT.NAME.as("pollutant_name")
						,POLLUTANT.FRIENDLY_NAME.as("pollutant_friendly_name")
						,POLLUTANT_METRIC.NAME.as("metric_name")
						,SEASONAL_METRIC.NAME.as("seasonal_metric_name")
						,STATISTIC_TYPE.NAME.as("metric_statistic_name")
						)
				.from(HEALTH_IMPACT_FUNCTION)
				.leftJoin(ENDPOINT_GROUP).on(ENDPOINT_GROUP.ID.eq(HEALTH_IMPACT_FUNCTION.ENDPOINT_GROUP_ID))
				.leftJoin(ENDPOINT).on(ENDPOINT.ID.eq(HEALTH_IMPACT_FUNCTION.ENDPOINT_ID))
				.leftJoin(POLLUTANT).on(POLLUTANT.ID.eq(HEALTH_IMPACT_FUNCTION.POLLUTANT_ID))
				.leftJoin(POLLUTANT_METRIC).on(POLLUTANT_METRIC.ID.eq(HEALTH_IMPACT_FUNCTION.METRIC_ID))
				.leftJoin(SEASONAL_METRIC).on(SEASONAL_METRIC.ID.eq(HEALTH_IMPACT_FUNCTION.SEASONAL_METRIC_ID))
				.leftJoin(STATISTIC_TYPE).on(STATISTIC_TYPE.ID.eq(HEALTH_IMPACT_FUNCTION.METRIC_STATISTIC))			
				.where(HEALTH_IMPACT_FUNCTION.ID.eq(hifId))
				.fetchOne();
				
		return record.get(HEALTH_IMPACT_FUNCTION.AUTHOR) + " (Unique ID: " + record.get(HEALTH_IMPACT_FUNCTION.ID) + ")";
	}
	
	
}

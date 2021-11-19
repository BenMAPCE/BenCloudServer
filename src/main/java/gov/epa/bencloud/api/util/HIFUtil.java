package gov.epa.bencloud.api.util;

import static gov.epa.bencloud.server.database.jooq.data.Tables.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.impl.DSL;
import org.mariuszgromada.math.mxparser.*;

import com.fasterxml.jackson.databind.node.ObjectNode;

import gov.epa.bencloud.api.IncidenceApi;
import gov.epa.bencloud.api.model.HIFConfig;
import gov.epa.bencloud.api.model.HIFTaskConfig;
import gov.epa.bencloud.server.database.JooqUtil;
import gov.epa.bencloud.server.database.jooq.data.tables.records.HealthImpactFunctionRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.HifResultDatasetRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.HifResultRecord;
import gov.epa.bencloud.server.tasks.model.Task;

public class HIFUtil {

	public static Expression[] getFunctionAndBaselineExpression(Integer id) {

		Expression[] functionAndBaselineExpressions = new Expression[2];
		// Load the function by id
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());

		HealthImpactFunctionRecord record = create
				.selectFrom(HEALTH_IMPACT_FUNCTION)
				.where(HEALTH_IMPACT_FUNCTION.ID.eq(id))
				.fetchOne();
		
		// Populate/create the necessary arguments and constants
		//{ a, b, c, beta, deltaq, q0, q1, incidence, pop, prevalence };
		Constant a = new Constant("A", record.getValA().doubleValue());
		Constant b = new Constant("B", record.getValB().doubleValue());
		Constant c = new Constant("C", record.getValC().doubleValue());
		
		//The following will be set while iterating cells
		Argument beta = new Argument("BETA", record.getBeta().doubleValue());
		Argument deltaQ = new Argument("DELTAQ", 0.0);
		Argument q1 = new Argument("Q0", 0.0);
		Argument q2 = new Argument("Q1", 0.0);
		Argument incidence = new Argument("INCIDENCE", 0.0);
		Argument prevalence = new Argument("PREVALENCE", 0.0);
		Argument population = new Argument("POPULATION", 0.0);
		
		// return the expression
		functionAndBaselineExpressions[0] = new Expression(record.getFunctionText(), a, b, c, beta, deltaQ, q1, q2, incidence, prevalence, population);		
		functionAndBaselineExpressions[1] = new Expression(record.getBaselineFunctionText(), a, b, c, beta, deltaQ, q1, q2, incidence, prevalence, population);		

		return functionAndBaselineExpressions;
	}

	public static HealthImpactFunctionRecord getFunctionDefinition(Integer id) {

		// Load the function by id
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());

		HealthImpactFunctionRecord record = create
				.selectFrom(HEALTH_IMPACT_FUNCTION)
				.where(HEALTH_IMPACT_FUNCTION.ID.eq(id))
				.fetchOne();
				
		return record;
	}
	
	/**
	 * This method supports saving partial datasets to avoid a situation where 
	 * the complete result set get so large it cannot be kept in memory
	 * It will only create the dataset and function_config records on the first call
	 * Subsequent calls will only write the results themselves
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
					)
			.values(
					task.getUuid()
					, hifTaskConfig.name
					, hifTaskConfig.popId
					, hifTaskConfig.popYear
					, hifTaskConfig.aqBaselineId
					, hifTaskConfig.aqScenarioId)
			.returning(HIF_RESULT_DATASET.ID)
			.fetchOne();
			
			hifResultDatasetId = hifResultDatasetRecord.getId();
			
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
	 * Selects the most appropriate incidence and prevalence dataset and year for a given function
	 * First, tries the user's default incidence/prevalence selection
	 * If that doesn't work, tries to use the selection in the health impact function config
	 * Failing that, it resorts to default datasets that contain data for the endpoint group
	 * In all cases, the selected year the closest available to the population year
	 * @param function
	 * @param popYear
	 * @param defaultIncidencePrevalenceDataset
	 * @param functionIncidenceDataset
	 * @param functionPrevalenceDataset
	 */
	public static void setIncidencePrevalence(ObjectNode function, int popYear, int defaultIncidencePrevalenceDataset, Integer functionIncidenceDataset, Integer functionPrevalenceDataset) {

		int endpointGroupId = function.get("endpoint_group_id").asInt();
		String functionText = function.get("function_text").asText().toLowerCase();
		boolean isPrevalenceFunction = functionText.contains("prevalence");
		boolean isIncidenceFunction = functionText.contains("incidence");
		boolean defaultDatasetSupportsIncidenceForEndpointGroup = false;
		boolean defaultDatasetSupportsPrevalenceForEndpointGroup = false;


		if(defaultIncidencePrevalenceDataset != 0) {		
			//Determine if this dataset supports incidence and/or prevalence for this function's endpoint group
			Record1<Integer> countIncidence = DSL.using(JooqUtil.getJooqConfiguration())
					.select(DSL.count())
					.from(INCIDENCE_ENTRY)
					.where(INCIDENCE_ENTRY.INCIDENCE_DATASET_ID.eq(defaultIncidencePrevalenceDataset)
							.and(INCIDENCE_ENTRY.PREVALENCE.ne(true))
							.and(INCIDENCE_ENTRY.ENDPOINT_GROUP_ID.eq(endpointGroupId)))
					.fetchOne();
			defaultDatasetSupportsIncidenceForEndpointGroup = countIncidence.value1() > 0;
			
			Record1<Integer> countPrevalence = DSL.using(JooqUtil.getJooqConfiguration())
					.select(DSL.count())
					.from(INCIDENCE_ENTRY)
					.where(INCIDENCE_ENTRY.INCIDENCE_DATASET_ID.eq(defaultIncidencePrevalenceDataset)
							.and(INCIDENCE_ENTRY.PREVALENCE.eq(true))
							.and(INCIDENCE_ENTRY.ENDPOINT_GROUP_ID.eq(endpointGroupId)))
					.fetchOne();
			defaultDatasetSupportsPrevalenceForEndpointGroup = countPrevalence.value1() > 0;
		}
		
		if (isIncidenceFunction) {
			if (defaultDatasetSupportsIncidenceForEndpointGroup) {
				function.put("incidence_dataset_id", defaultIncidencePrevalenceDataset);
				function.put("incidence_year", HIFUtil.getClosestIncidenceYear(defaultIncidencePrevalenceDataset, false, endpointGroupId, popYear));
				function.put("incidence_dataset_name", IncidenceApi.getIncidenceDatasetName(defaultIncidencePrevalenceDataset));

			} else if (functionIncidenceDataset != null && functionIncidenceDataset != 0) {
				// Default selection won't work so use what's defined in the function
				function.put("incidence_dataset_id", functionIncidenceDataset);
				function.put("incidence_year", HIFUtil.getClosestIncidenceYear(functionIncidenceDataset, false, endpointGroupId, popYear));
				function.put("incidence_dataset_name", IncidenceApi.getIncidenceDatasetName(functionIncidenceDataset));
	
			} else {
				// Choose the default incidence based on endpoint group
				// TODO: Improve this logic
				int dsId = 0;
				switch (endpointGroupId) {
				case 12:
					dsId = 1; //County
					break;
				case 28:
				case 29:
				case 30:
					dsId = 3; //National
					break;
				default:
					dsId = 1; //County
				}
				
				function.put("incidence_dataset_id", dsId); 
				function.put("incidence_year", HIFUtil.getClosestIncidenceYear(dsId, false, endpointGroupId, popYear));
				function.put("incidence_dataset_name", IncidenceApi.getIncidenceDatasetName(dsId));

			}
		} else {
			function.putNull("incidence_dataset_id");
			function.putNull("incidence_year");
			function.putNull("incidence_dataset_name");
		}
				
		if(isPrevalenceFunction) {
			if (defaultDatasetSupportsPrevalenceForEndpointGroup) {
				function.put("prevalence_dataset_id", defaultIncidencePrevalenceDataset);
				function.put("prevalence_year", HIFUtil.getClosestIncidenceYear(defaultIncidencePrevalenceDataset, true, endpointGroupId, popYear));
				function.put("prevalence_dataset_name", IncidenceApi.getIncidenceDatasetName(defaultIncidencePrevalenceDataset));
			} else if (functionPrevalenceDataset != null && functionPrevalenceDataset != 0) {
				// Default selection won't work so use what's defined in the function
				function.put("prevalence_dataset_id", functionPrevalenceDataset);
				function.put("prevalence_year", HIFUtil.getClosestIncidenceYear(functionPrevalenceDataset, true, endpointGroupId, popYear));
				function.put("prevalence_dataset_name", IncidenceApi.getIncidenceDatasetName(functionPrevalenceDataset));
			} else {
				int dsId = (endpointGroupId == 30 || endpointGroupId == 28) ? 3 : 1; //National Incidence & Prevalence OR Prevalence
				function.put("prevalence_dataset_id", dsId);
				function.put("prevalence_year", HIFUtil.getClosestIncidenceYear(dsId, true, endpointGroupId, popYear));
				function.put("prevalence_dataset_name", IncidenceApi.getIncidenceDatasetName(dsId));
			}
		} else {
			function.putNull("prevalence_dataset_id");
			function.putNull("prevalence_year");
			function.putNull("prevalence_dataset_name");
		}	
	}

	private static int getClosestIncidenceYear(int defaultIncidencePrevalenceDataset, boolean isPrevalence, int endpointGroupId, int popYear) {

		Record1<Integer>[] incidenceYears = DSL.using(JooqUtil.getJooqConfiguration())
				.selectDistinct(INCIDENCE_ENTRY.YEAR)
				.from(INCIDENCE_ENTRY)
				.where(INCIDENCE_ENTRY.INCIDENCE_DATASET_ID.eq(defaultIncidencePrevalenceDataset)
						.and(INCIDENCE_ENTRY.ENDPOINT_GROUP_ID.eq(endpointGroupId))
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
	
	
}

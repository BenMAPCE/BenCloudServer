package gov.epa.bencloud.api.util;

import static gov.epa.bencloud.server.database.jooq.data.Tables.*;

import java.util.ArrayList;

import org.jooq.DSLContext;
import org.jooq.JSON;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.impl.DSL;
import org.mariuszgromada.math.mxparser.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import gov.epa.bencloud.api.function.VFArguments;
import gov.epa.bencloud.api.function.VFNativeFactory;
import gov.epa.bencloud.api.function.VFunction;
import gov.epa.bencloud.api.model.ValuationConfig;
import gov.epa.bencloud.api.model.ValuationTaskConfig;
import gov.epa.bencloud.api.model.ValuationTaskLog;
import gov.epa.bencloud.server.database.JooqUtil;
import gov.epa.bencloud.server.database.jooq.data.tables.records.ValuationFunctionRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.ValuationResultDatasetRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.ValuationResultRecord;
import gov.epa.bencloud.server.tasks.model.Task;

/*
 * Methods for updating and accessing resources related to valuation functions
 */
public class ValuationUtil {


	/**
	 * 
	 * @param id
	 * @return the valuation function expression for a given valuation function id.
	 */
	public static VFunction getFunctionForVF(Integer id) {

		// Load the function by id
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());

		ValuationFunctionRecord record = create
				.selectFrom(VALUATION_FUNCTION)
				.where(VALUATION_FUNCTION.ID.eq(id))
				.fetchOne();
		
		VFunction function = new VFunction();

		function.nativeFunction = VFNativeFactory.create(record.getFunctionText());
		function.vfArguments = new VFArguments();

		function.vfArguments.a = record.getValA().doubleValue();
		function.vfArguments.b = record.getValB().doubleValue();
		function.vfArguments.c = record.getValC().doubleValue();
		function.vfArguments.d = record.getValD().doubleValue();
		function.vfArguments.allGoodsIndex = 0.0;
		function.vfArguments.medicalCostIndex = 0.0;
		function.vfArguments.wageIndex = 0.0;
		function.vfArguments.medianIncome = 0.0;
		
		if(function.nativeFunction == null) {
			Constant a = new Constant("A", function.vfArguments.a);
			Constant b = new Constant("B", function.vfArguments.b);
			Constant c = new Constant("C", function.vfArguments.c);
			Constant d = new Constant("D", function.vfArguments.d);
			Argument allGoodsIndex = new Argument("AllGoodsIndex", function.vfArguments.allGoodsIndex);
			Argument medicalCostIndex = new Argument("MedicalCostIndex", function.vfArguments.medicalCostIndex);
			Argument wageIndex = new Argument("WageIndex", function.vfArguments.wageIndex);
			
			//TODO: Inspect function for variables and create arguments to match
			//Hardcoding median_income for now
			Argument medianIncome = new Argument("median_income", function.vfArguments.medianIncome);
			function.interpretedFunction = new Expression(record.getFunctionText(), a, b, c, d, allGoodsIndex, medicalCostIndex, wageIndex, medianIncome);
		}

		return function;
	}

	/**
	 * 
	 * @param id
	 * @return the function definition for a given valuation function id.
	 */
	public static Record getFunctionDefinition(Integer id) {

		// Load the function by id
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());		
		
		Record record = create
				.select(VALUATION_FUNCTION.asterisk()
						,ENDPOINT_GROUP.NAME.as("endpoint_group_name")
						,ENDPOINT.NAME.as("endpoint_name"))
				.from(VALUATION_FUNCTION)
				.leftJoin(ENDPOINT_GROUP).on(ENDPOINT_GROUP.ID.eq(VALUATION_FUNCTION.ENDPOINT_GROUP_ID))
				.leftJoin(ENDPOINT).on(ENDPOINT.ID.eq(VALUATION_FUNCTION.ENDPOINT_ID))
				.where(VALUATION_FUNCTION.ID.eq(id))
				.fetchOne();
				
		return record;
	}
	

	/**
	 * 
	 * @param valuationResultDatasetId
	 * @return the hif result dataset id related to the given valuation result dataset id.
	 */
	public static Integer getHifResultDatasetIdForValuationResultDataset(Integer valuationResultDatasetId) {

		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());

		ValuationResultDatasetRecord record = create
				.selectFrom(VALUATION_RESULT_DATASET)
				.where(VALUATION_RESULT_DATASET.ID.eq(valuationResultDatasetId))
				.fetchOne();
				
		return record.getHifResultDatasetId();
	}
	
	/**
	 * Stores the valuation results from a given valuation task in the database.
	 * @param task
	 * @param valuationTaskConfig
	 * @param valuationResults
	 */
	public static void storeResults(Task task, ValuationTaskConfig valuationTaskConfig, ArrayList<ValuationResultRecord> valuationResults) {
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());
		
		Integer vfResultDatasetId = create
				.selectFrom(VALUATION_RESULT_DATASET)
				.where(VALUATION_RESULT_DATASET.TASK_UUID.eq(task.getUuid()))
				.fetchOne(VALUATION_RESULT_DATASET.ID);
		
		if(vfResultDatasetId == null) {
		// Valuation result dataset record links the result dataset id to the task uuid
		ValuationResultDatasetRecord valuationResultDatasetRecord = create.insertInto(VALUATION_RESULT_DATASET
				, VALUATION_RESULT_DATASET.TASK_UUID
				, VALUATION_RESULT_DATASET.NAME
				, VALUATION_RESULT_DATASET.HIF_RESULT_DATASET_ID
				, VALUATION_RESULT_DATASET.VARIABLE_DATASET_ID)
		.values(task.getUuid()
				,task.getName()
				,valuationTaskConfig.hifResultDatasetId
				,valuationTaskConfig.variableDatasetId)
		.returning(VALUATION_RESULT_DATASET.ID)
		.fetchOne();

		vfResultDatasetId = valuationResultDatasetRecord.getId();
		valuationTaskConfig.resultDatasetId = vfResultDatasetId;
		
		// Each HIF result function config contains the details of how the function was configured
		for(ValuationConfig vf : valuationTaskConfig.valuationFunctions) {
			create.insertInto(VALUATION_RESULT_FUNCTION_CONFIG
					, VALUATION_RESULT_FUNCTION_CONFIG.VALUATION_RESULT_DATASET_ID
					, VALUATION_RESULT_FUNCTION_CONFIG.VF_ID
					, VALUATION_RESULT_FUNCTION_CONFIG.HIF_ID)
			.values(vfResultDatasetId
					, vf.vfId
					, vf.hifId)
			.execute();
			
		}
		}
		// Finally, store the actual estimates
		for(ValuationResultRecord valuationResult : valuationResults) {
			valuationResult.setValuationResultDatasetId(vfResultDatasetId);
		}
		
		create
		.batchInsert(valuationResults)
		.execute();	
	}
	
	/**
	 * Stores a given valuation function task log in the database.
	 * @param vfTaskLog
	 */
	public static void storeTaskLog(ValuationTaskLog vfTaskLog) {
		
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());
		JSON taskLogJson = JSON.json(vfTaskLog.toJsonString());
		
		create.update(VALUATION_RESULT_DATASET)
			.set(VALUATION_RESULT_DATASET.TASK_LOG, taskLogJson)
			.where(VALUATION_RESULT_DATASET.ID.eq(vfTaskLog.getVfTaskConfig().resultDatasetId))
			.execute();	
	}
	
	/**
	 * 
	 * @param datasetId
	 * @return a valuation task log associated with the given valuation result dataset id.
	 */
	public static ValuationTaskLog getTaskLog(Integer datasetId) {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());
		
		Record1<JSON> taskLogJson = create.select(VALUATION_RESULT_DATASET.TASK_LOG)
			.from(VALUATION_RESULT_DATASET)
			.where(VALUATION_RESULT_DATASET.ID.eq(datasetId))
			.fetchOne();
		
		try {
			ValuationTaskLog vfTaskLog = mapper.readValue(taskLogJson.value1().toString(), ValuationTaskLog.class);
			return vfTaskLog;
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

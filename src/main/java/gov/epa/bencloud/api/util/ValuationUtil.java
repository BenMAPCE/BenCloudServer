package gov.epa.bencloud.api.util;

import static gov.epa.bencloud.server.database.jooq.data.Tables.*;

import java.util.ArrayList;

import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.mariuszgromada.math.mxparser.*;

import gov.epa.bencloud.api.model.ValuationConfig;
import gov.epa.bencloud.api.model.ValuationTaskConfig;
import gov.epa.bencloud.server.database.JooqUtil;
import gov.epa.bencloud.server.database.jooq.data.tables.records.ValuationFunctionRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.ValuationResultDatasetRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.ValuationResultRecord;
import gov.epa.bencloud.server.tasks.model.Task;

public class ValuationUtil {

	public static Expression getFunctionExpression(Integer id) {

		// Load the function by id
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());

		ValuationFunctionRecord record = create
				.selectFrom(VALUATION_FUNCTION)
				.where(VALUATION_FUNCTION.ID.eq(id))
				.fetchOne();
		
		// Populate/create the necessary arguments
		//{ A, B, C, D, AllGoodsIndex, MedicalCostIndex, WageIndex, LagAdjustment, dicSetupVariables };
		Constant a = new Constant("A", record.getValA().doubleValue());
		Constant b = new Constant("B", record.getValB().doubleValue());
		Constant c = new Constant("C", record.getValC().doubleValue());
		Constant d = new Constant("D", record.getValD().doubleValue());
		Argument allGoodsIndex = new Argument("AllGoodsIndex", 0.0);
		Argument medicalCostIndex = new Argument("MedicalCostIndex", 0.0);
		Argument wageIndex = new Argument("WageIndex", 0.0);
		
		//TODO: Inspect function for variables and create arguments to match
		//Hardcoding median_income for now
		Argument medianIncome = new Argument("median_income", 0.0);

		return new Expression(record.getFunctionText(), a, b, c, d, allGoodsIndex, medicalCostIndex, wageIndex, medianIncome);
	}

	public static ValuationFunctionRecord getFunctionDefinition(Integer id) {

		// Load the function by id
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());

		ValuationFunctionRecord record = create
				.selectFrom(VALUATION_FUNCTION)
				.where(VALUATION_FUNCTION.ID.eq(id))
				.fetchOne();
				
		return record;
	}
	
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
	
}

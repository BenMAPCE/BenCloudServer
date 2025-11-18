package gov.epa.bencloud.api.util;

import static gov.epa.bencloud.server.database.jooq.data.Tables.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
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

import gov.epa.bencloud.Constants;
import gov.epa.bencloud.api.function.VFArguments;
import gov.epa.bencloud.api.function.VFNativeFactory;
import gov.epa.bencloud.api.function.VFunction;
import gov.epa.bencloud.api.model.HIFConfig;
import gov.epa.bencloud.api.model.ValuationConfig;
import gov.epa.bencloud.api.model.ValuationTaskConfig;
import gov.epa.bencloud.api.model.ValuationTaskLog;
import gov.epa.bencloud.server.database.JooqUtil;
import gov.epa.bencloud.server.database.jooq.data.Routines;
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
		
		if(function.nativeFunction == null) {
			Constant a = new Constant("A", function.vfArguments.a);
			Constant b = new Constant("B", function.vfArguments.b);
			Constant c = new Constant("C", function.vfArguments.c);
			Constant d = new Constant("D", function.vfArguments.d);
			Argument allGoodsIndex = new Argument("AllGoodsIndex", function.vfArguments.allGoodsIndex);
			Argument medicalCostIndex = new Argument("MedicalCostIndex", function.vfArguments.medicalCostIndex);
			Argument wageIndex = new Argument("WageIndex", function.vfArguments.wageIndex);
			
			Expression e = new Expression(record.getFunctionText(), a, b, c, d, allGoodsIndex, medicalCostIndex, wageIndex);
			function.createInterpretedFunctionFromExpression(e);
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
						,ENDPOINT.DISPLAY_NAME.as("endpoint_name"))
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
					, VALUATION_RESULT_DATASET.VARIABLE_DATASET_ID
					, VALUATION_RESULT_DATASET.GRID_DEFINITION_ID
					, VALUATION_RESULT_DATASET.USER_ID)
			.values(task.getUuid()
					,task.getName()
					,valuationTaskConfig.hifResultDatasetId
					,valuationTaskConfig.variableDatasetId
					,valuationTaskConfig.gridDefinitionId
					,task.getUserIdentifier())
			.returning(VALUATION_RESULT_DATASET.ID)
			.fetchOne();
	
			vfResultDatasetId = valuationResultDatasetRecord.getId();
			valuationTaskConfig.resultDatasetId = vfResultDatasetId;
			
			// Each HIF result function config contains the details of how the function was configured
			for(ValuationConfig vf : valuationTaskConfig.valuationFunctions) {
				create.insertInto(VALUATION_RESULT_FUNCTION_CONFIG
						, VALUATION_RESULT_FUNCTION_CONFIG.VALUATION_RESULT_DATASET_ID
						, VALUATION_RESULT_FUNCTION_CONFIG.VF_ID
						, VALUATION_RESULT_FUNCTION_CONFIG.HIF_ID
						, VALUATION_RESULT_FUNCTION_CONFIG.HIF_INSTANCE_ID)
				.values(vfResultDatasetId
						, vf.vfId
						, vf.hifId
						, vf.hifInstanceId)
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
	 * Stores aggregated valuation results to result_agg table.
	 * @param task
	 * @param grid_id
	 */
	public static void storeAggResults(Task task, int grid_id) {
        // aggregate task's results to a grid and store in result_agg table
        DSLContext create = DSL.using(JooqUtil.getJooqConfiguration(task.getUuid()));
		Integer vfResultDatasetId = create
				.selectFrom(VALUATION_RESULT_DATASET)
				.where(VALUATION_RESULT_DATASET.TASK_UUID.eq(task.getUuid()))
				.fetchOne(VALUATION_RESULT_DATASET.ID);
		Routines.addValuationResultsAgg(create.configuration(), vfResultDatasetId, grid_id);
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

	/**
	 * 
	 * @param id
	 * @return the function definition for a given valuation function id.
	 */
	public static Integer[] getFunctionsForEndpoint(Integer endpointId) {
	
		// Load the function by id
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());		
		
		Integer[] ret = create
				.select(VALUATION_FUNCTION.ID)
				.from(VALUATION_FUNCTION)
				.where(VALUATION_FUNCTION.ENDPOINT_ID.eq(endpointId))
				.fetchArray(VALUATION_FUNCTION.ID);
				
		return ret;
	}

	/**
	 * @param hifConfig
	 * @param valuationSelection
	 * Add all the valuation functions to this HIF that match valuationSelection's values
	 * NOTE: For now, we only support EPA's default functions. In the future, we'll add
	 *  a new structure to allow users to create their own lists.
	 */
	public static void populateValuationFunctions(HIFConfig hifConfig, String valuationSelection) {
		
		Integer[] vfIds = ValuationUtil.getFunctionsForEndpoint((Integer) hifConfig.hifRecord.get("endpoint_id"));
		
		// Tier 1 - Look for valuation function age ranges that completely contain the HIF age range
		for (Integer vfId : vfIds) {
			Record vfRecord = ValuationUtil.getFunctionDefinition(vfId);
			if(valuationSelection.equalsIgnoreCase(Constants.EPA_STANDARD_VALUATION) && vfRecord.get(VALUATION_FUNCTION.EPA_STANDARD)
					&& hifConfig.startAge >= vfRecord.get(VALUATION_FUNCTION.START_AGE)
					&& hifConfig.endAge <= vfRecord.get(VALUATION_FUNCTION.END_AGE)) {
				ValuationConfig vf = new ValuationConfig();
				vf.hifId = hifConfig.hifId;
				vf.hifInstanceId = hifConfig.hifInstanceId;
				vf.vfId = vfRecord.get(VALUATION_FUNCTION.ID);
				vf.vfRecord = vfRecord.intoMap();
				hifConfig.valuationFunctions.add(vf);							
			}
		}
		
		// Tier 2 - If no matches, restrict the HIF age range by 1 year on both sides and check again
		if(hifConfig.valuationFunctions.size() == 0) {
			for (Integer vfId : vfIds) {
				Record vfRecord = ValuationUtil.getFunctionDefinition(vfId);
				if(valuationSelection.equalsIgnoreCase(Constants.EPA_STANDARD_VALUATION) && vfRecord.get(VALUATION_FUNCTION.EPA_STANDARD)
						&& hifConfig.startAge+1 >= vfRecord.get(VALUATION_FUNCTION.START_AGE)
						&& hifConfig.endAge-1 <= vfRecord.get(VALUATION_FUNCTION.END_AGE)) {
					ValuationConfig vf = new ValuationConfig();
					vf.hifId = hifConfig.hifId;
					vf.hifInstanceId = hifConfig.hifInstanceId;
					vf.vfId = vfRecord.get(VALUATION_FUNCTION.ID);
					vf.vfRecord = vfRecord.intoMap();
					hifConfig.valuationFunctions.add(vf);							
				}
			}			
		}
		
		// Tier 3 - If still no matches, just include all EPA default functions for this endpoint
		if(hifConfig.valuationFunctions.size() == 0) {
			for (Integer vfId : vfIds) {
				Record vfRecord = ValuationUtil.getFunctionDefinition(vfId);
				if(valuationSelection.equalsIgnoreCase(Constants.EPA_STANDARD_VALUATION) && vfRecord.get(VALUATION_FUNCTION.EPA_STANDARD)) {
					ValuationConfig vf = new ValuationConfig();
					vf.hifId = hifConfig.hifId;
					vf.hifInstanceId = hifConfig.hifInstanceId;
					vf.vfId = vfRecord.get(VALUATION_FUNCTION.ID);
					vf.vfRecord = vfRecord.intoMap();
					hifConfig.valuationFunctions.add(vf);							
				}
			}			
		}
		
	}

	public static String validateModelColumnHeadings(int endpointIdx, int qualifierIdx, int referenceIdx, int startAgeIdx, int endAgeIdx, int functionIdx, int param1Idx, int param2Idx, int paramAIdx, int paramANameIdx, int paramBIdx, int paramBNameIdx, int distributionIdx) {
		StringBuilder b = new StringBuilder();
		if(endpointIdx == -999) {
			b.append((b.length()==0 ? "" : ", ") + "Health Effect Category");
		}
		if(qualifierIdx == -999) {
			b.append((b.length()==0 ? "" : ", ") + "Risk Model Details");
		}
		if(referenceIdx == -999) {
			b.append((b.length()==0 ? "" : ", ") + "Reference");
		}
        if(startAgeIdx == -999) {
			b.append((b.length()==0 ? "" : ", ") + "Start Age");
		}
		if(endAgeIdx == -999) {
			b.append((b.length()==0 ? "" : ", ") + "End Age");
		}
        if(functionIdx == -999) {
			b.append((b.length()==0 ? "" : ", ") + "Function");
		}
        if(param1Idx == -999) {
			b.append((b.length()==0 ? "" : ", ") + "Standard Error");
		}
        if(param2Idx == -999) {
			b.append((b.length()==0 ? "" : ", ") + "Param 2 A");
		}
        if(paramAIdx == -999) {
			b.append((b.length()==0 ? "" : ", ") + "A");
		}
        if(paramANameIdx == -999) {
			b.append((b.length()==0 ? "" : ", ") + "Name A");
		}
		if(paramBIdx == -999) {
			b.append((b.length()==0 ? "" : ", ") + "B");
		}
        if(paramBNameIdx == -999) {
			b.append((b.length()==0 ? "" : ", ") + "Name B");
		}
		if(distributionIdx == -999) {
			b.append((b.length()==0 ? "" : ", ") + "Distribution");
		}

		return b.toString();
	}

	/**
     * 
     * @param endpointGroupId
     * @return a mapping of endpoint names to endpoint ids for a given endpoint group Id
     */
    public static Map<String, Integer> getEndpointIdLookup(short endpointGroupId) {
        Map<String, Integer> endpointMap = DSL.using(JooqUtil.getJooqConfiguration())
            .select(DSL.lower(ENDPOINT.DISPLAY_NAME), ENDPOINT.ID)
            .from(ENDPOINT)
            .where(ENDPOINT.ENDPOINT_GROUP_ID.eq(endpointGroupId))
            .fetchMap(DSL.lower(ENDPOINT.DISPLAY_NAME), ENDPOINT.ID);
        return endpointMap;}
    
}

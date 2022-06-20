package gov.epa.bencloud.api.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.pac4j.core.profile.UserProfile;

import static gov.epa.bencloud.server.database.jooq.data.Tables.*;

import gov.epa.bencloud.api.HIFApi;
import gov.epa.bencloud.api.ValuationApi;
import gov.epa.bencloud.api.model.ValuationTaskConfig;
import gov.epa.bencloud.server.database.JooqUtil;
import gov.epa.bencloud.server.database.jooq.data.Routines;
import gov.epa.bencloud.server.database.jooq.data.tables.records.GetVariableRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.InflationEntryRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.ValuationFunctionRecord;
import gov.epa.bencloud.server.tasks.TaskComplete;
import gov.epa.bencloud.server.tasks.TaskUtil;
import gov.epa.bencloud.server.tasks.model.Task;
import spark.Request;
import spark.Response;

/**
 * @author jimanderton
 *
 */
public class ApiUtil {

	public static final String appVersion = "0.3.0";
	public static final int minimumDbVersion = 3;
	
	/**
	 * @param columnIdx
	 * @param rowIdx
	 * @return unique integer using the Cantor pairing function to combine column and row indices
	 * https://en.wikipedia.org/wiki/Pairing_function
	 */
	public static long getCellId(int columnIdx, int rowIdx) {
		return (long)(((columnIdx+rowIdx)*(columnIdx+rowIdx+1)*0.5)+rowIdx);
	}


	public static Map<String, Double> getInflationIndices(int id, Integer inflationYear) {

		InflationEntryRecord inflationRecord = DSL.using(JooqUtil.getJooqConfiguration())
				.selectFrom(INFLATION_ENTRY)
				.where(INFLATION_ENTRY.INFLATION_DATASET_ID.eq(id).and(INFLATION_ENTRY.ENTRY_YEAR.eq(inflationYear)))
				.fetchOne();
		Map<String, Double> inflationIndices = new HashMap<String, Double>();
		inflationIndices.put("AllGoodsIndex", inflationRecord.getAllGoodsIndex().doubleValue());
		inflationIndices.put("MedicalCostIndex", inflationRecord.getMedicalCostIndex().doubleValue());		
		inflationIndices.put("WageIndex", inflationRecord.getWageIndex().doubleValue());
		
		return inflationIndices;
	}

	public static Map<Short, Record2<Short, Double>> getIncomeGrowthFactors(int id, Integer popYear) {

		Map<Short, Record2<Short, Double>> incomeGrowthFactorRecords = DSL.using(JooqUtil.getJooqConfiguration())
				.select(INCOME_GROWTH_ADJ_FACTOR.ENDPOINT_GROUP_ID,
						INCOME_GROWTH_ADJ_FACTOR.MEAN_VALUE)
				.from(INCOME_GROWTH_ADJ_FACTOR)
				.where(INCOME_GROWTH_ADJ_FACTOR.INCOME_GROWTH_ADJ_DATASET_ID.eq((short) id).and(INCOME_GROWTH_ADJ_FACTOR.GROWTH_YEAR.eq(popYear.shortValue())))
				.fetchMap(INCOME_GROWTH_ADJ_FACTOR.ENDPOINT_GROUP_ID);
				
		return incomeGrowthFactorRecords;
	}

	public static Map<String, Integer> getStatisticIdLookup() {
		Map<String, Integer> statistics = DSL.using(JooqUtil.getJooqConfiguration())
				.select(DSL.lower(STATISTIC_TYPE.NAME), STATISTIC_TYPE.ID)
				.from(STATISTIC_TYPE)
				.fetchMap(DSL.lower(STATISTIC_TYPE.NAME), STATISTIC_TYPE.ID);		
		return statistics;	
	}
	
	public static Map<Integer, String> getMetricNameLookup() {
		Map<Integer, String> map = DSL.using(JooqUtil.getJooqConfiguration())
				.select(POLLUTANT_METRIC.ID, POLLUTANT_METRIC.NAME)
				.from(POLLUTANT_METRIC)
				.fetchMap(POLLUTANT_METRIC.ID, POLLUTANT_METRIC.NAME);		
		return map;	
	}

	public static Map<Integer, String> getSeasonalMetricNameLookup() {
		Map<Integer, String> map = DSL.using(JooqUtil.getJooqConfiguration())
				.select(SEASONAL_METRIC.ID, SEASONAL_METRIC.NAME)
				.from(SEASONAL_METRIC)
				.fetchMap(SEASONAL_METRIC.ID, SEASONAL_METRIC.NAME);		
		return map;	
	}
	
	public static Map<Integer, String> getRaceNameLookup() {
		Map<Integer, String> map = DSL.using(JooqUtil.getJooqConfiguration())
				.select(RACE.ID, RACE.NAME)
				.from(RACE)
				.fetchMap(RACE.ID, RACE.NAME);		
		return map;	
	}
	
	public static Map<Integer, String> getEthnicityNameLookup() {
		Map<Integer, String> map = DSL.using(JooqUtil.getJooqConfiguration())
				.select(ETHNICITY.ID, ETHNICITY.NAME)
				.from(ETHNICITY)
				.fetchMap(ETHNICITY.ID, ETHNICITY.NAME);		
		return map;	
	}
	
	public static Map<Integer, String> getGenderNameLookup() {
		Map<Integer, String> map = DSL.using(JooqUtil.getJooqConfiguration())
				.select(GENDER.ID, GENDER.NAME)
				.from(GENDER)
				.fetchMap(GENDER.ID, GENDER.NAME);		
		return map;	
	}
	
	public static Object deleteTaskResults(Request req, Response res, Optional<UserProfile> userProfile) {
		String uuid = req.params("uuid");
		
		Result<Record> completedTasks = 
				DSL.using(JooqUtil.getJooqConfiguration()).select().from(TASK_COMPLETE)
				.where(TASK_COMPLETE.TASK_UUID.eq(uuid))
				.fetch();

		if (completedTasks.size() > 0) {
			Record taskCompleteRecord = completedTasks.get(0);
			
			if (taskCompleteRecord.get(TASK_COMPLETE.TASK_TYPE).equals("HIF")) {
				TaskUtil.deleteHifResults(uuid);
			} else if (taskCompleteRecord.get(TASK_COMPLETE.TASK_TYPE).equals("Valuation")) {
				TaskUtil.deleteValuationResults(uuid);
			}
		}
		
		return null;
	}

	// Note that this implementation is currently incomplete. 
	// It will currently ONLY return data for the median_income variable. 
	// It needs to be extended so it will look up each variable that is required.
	public static Map<String, Map<Long, Double>> getVariableValues(ValuationTaskConfig valuationTaskConfig, List<Record> vfDefinitionList) {
		 // Load list of functions from the database
		
		//TODO: Change this to only load what we need
		List<String> allVariableNames = ApiUtil.getAllVariableNames(valuationTaskConfig.variableDatasetId);
		
		//TODO: Temp override until we can improve variable selection
		allVariableNames.removeIf(n -> (!n.equals("median_income")));
		
		HashMap<String, Map<Long, Double>> variableMap = new HashMap<String, Map<Long, Double>>();
		
		Result<GetVariableRecord> variableRecords = Routines.getVariable(JooqUtil.getJooqConfiguration(), 
				1, 
				allVariableNames.get(0), 
				28);
		//Look at all valuation functions to determine which variables are needed
		for(String variableName: allVariableNames) {
			for(Record function : vfDefinitionList) {
				if(function.get("function_text", String.class).toLowerCase().contains(variableName)) {
					if(!variableMap.containsKey(variableName)) {
						variableMap.put(variableName, new HashMap<Long, Double>());
					}
				}
			}
		}
		// Finally load the cell values for each needed variable
		for (GetVariableRecord variableRecord : variableRecords) {
			if(variableMap.containsKey(variableRecord.getVariableName())) {
				variableMap.get(variableRecord.getVariableName()).put(variableRecord.getGridCellId(), variableRecord.getValue().doubleValue());
			}
		}
		
		return variableMap;
	}

	private static List<String> getAllVariableNames(Integer variableDatasetId) {
		if(variableDatasetId == null) {
			return null;
		}

		List<String> allVariableNames = DSL.using(JooqUtil.getJooqConfiguration())
				.select(VARIABLE_ENTRY.NAME)
				.from(VARIABLE_ENTRY)
				.where(VARIABLE_ENTRY.VARIABLE_DATASET_ID.eq(variableDatasetId))
				.orderBy(VARIABLE_ENTRY.NAME)
				.fetch(VARIABLE_ENTRY.NAME);
		return allVariableNames;
	}


	public static int getDatabaseVersion() {
		Record1<Integer> versionRecord = DSL.using(JooqUtil.getJooqConfiguration())
		.select(SETTINGS.VALUE_INT)
		.from(SETTINGS)
		.where(SETTINGS.KEY.equalIgnoreCase("version"))
		.fetchOne();
		
		if(versionRecord == null) {
			return -999;
		}
		return versionRecord.value1().intValue();
	}
}

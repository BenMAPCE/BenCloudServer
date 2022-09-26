package gov.epa.bencloud.api.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.ServletException;
import javax.servlet.http.Part;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.pac4j.core.profile.UserProfile;

import static gov.epa.bencloud.server.database.jooq.data.Tables.*;

import gov.epa.bencloud.api.CoreApi;
import gov.epa.bencloud.api.model.ValuationTaskConfig;
import gov.epa.bencloud.server.database.JooqUtil;
import gov.epa.bencloud.server.database.jooq.data.Routines;
import gov.epa.bencloud.server.database.jooq.data.tables.records.GetVariableRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.InflationEntryRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.TaskCompleteRecord;
import gov.epa.bencloud.server.tasks.TaskUtil;
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
		long columnIdxLong = (long)columnIdx;
		long rowIdxLong = (long)rowIdx;
		return (long)(((columnIdxLong+rowIdxLong)*(columnIdxLong+rowIdxLong+1)*0.5)+rowIdxLong);
	}

	/**
	 * 
	 * @param id
	 * @param inflationYear
	 * @param useInflationFactors 
	 * @return  a map of inflation indices with key = index name, value = index value
	 */
	public static Map<String, Double> getInflationIndices(int id, Integer inflationYear, Boolean useInflationFactors) {

		Map<String, Double> inflationIndices = new HashMap<String, Double>();
		
		if(! useInflationFactors) {
			inflationIndices.put("AllGoodsIndex", 1.0);
			inflationIndices.put("MedicalCostIndex", 1.0);		
			inflationIndices.put("WageIndex", 1.0);
		} else {
			InflationEntryRecord inflationRecord = DSL.using(JooqUtil.getJooqConfiguration())
					.selectFrom(INFLATION_ENTRY)
					.where(INFLATION_ENTRY.INFLATION_DATASET_ID.eq(id).and(INFLATION_ENTRY.ENTRY_YEAR.eq(inflationYear)))
					.fetchOne();
	
			inflationIndices.put("AllGoodsIndex", inflationRecord.getAllGoodsIndex().doubleValue());
			inflationIndices.put("MedicalCostIndex", inflationRecord.getMedicalCostIndex().doubleValue());		
			inflationIndices.put("WageIndex", inflationRecord.getWageIndex().doubleValue());
		}		
		return inflationIndices;
	}

	/**
	 * 
	 * @param id Income growth adjustment dataset id
	 * @param popYear
	 * @param useGrowthFactors 
	 * @return a map of income growth factors, with key = endpoint group id, value = income growth adjustment dataset id and growth year.
	 */
	public static Map<Short, Record2<Short, Double>> getIncomeGrowthFactors(int id, Integer popYear, Boolean useGrowthFactors) {

		if(! useGrowthFactors) {
			return null;
		}
		
		Map<Short, Record2<Short, Double>> incomeGrowthFactorRecords = DSL.using(JooqUtil.getJooqConfiguration())
				.select(INCOME_GROWTH_ADJ_FACTOR.ENDPOINT_GROUP_ID,
						INCOME_GROWTH_ADJ_FACTOR.MEAN_VALUE)
				.from(INCOME_GROWTH_ADJ_FACTOR)
				.where(INCOME_GROWTH_ADJ_FACTOR.INCOME_GROWTH_ADJ_DATASET_ID.eq((short) id).and(INCOME_GROWTH_ADJ_FACTOR.GROWTH_YEAR.eq(popYear.shortValue())))
				.fetchMap(INCOME_GROWTH_ADJ_FACTOR.ENDPOINT_GROUP_ID);
				
		return incomeGrowthFactorRecords;
	}

	/**
	 * 
	 * @return a map of statistic ids with key = statistic name, value = statistic id.
	 */
	public static Map<String, Integer> getStatisticIdLookup() {
		Map<String, Integer> statistics = DSL.using(JooqUtil.getJooqConfiguration())
				.select(DSL.lower(STATISTIC_TYPE.NAME), STATISTIC_TYPE.ID)
				.from(STATISTIC_TYPE)
				.fetchMap(DSL.lower(STATISTIC_TYPE.NAME), STATISTIC_TYPE.ID);		
		return statistics;	
	}
	
	/**
	 * 
	 * @return a map of pollutant metric names with key = pollutant metric id, value = pollutant metric name.
	 */
	public static Map<Integer, String> getMetricNameLookup() {
		Map<Integer, String> map = DSL.using(JooqUtil.getJooqConfiguration())
				.select(POLLUTANT_METRIC.ID, POLLUTANT_METRIC.NAME)
				.from(POLLUTANT_METRIC)
				.fetchMap(POLLUTANT_METRIC.ID, POLLUTANT_METRIC.NAME);		
		return map;	
	}

	/**
	 * 
	 * @return a map of seasonal metric names with key = seasonal metric id, value = seasonal metric name.
	 */
	public static Map<Integer, String> getSeasonalMetricNameLookup() {
		Map<Integer, String> map = DSL.using(JooqUtil.getJooqConfiguration())
				.select(SEASONAL_METRIC.ID, SEASONAL_METRIC.NAME)
				.from(SEASONAL_METRIC)
				.fetchMap(SEASONAL_METRIC.ID, SEASONAL_METRIC.NAME);		
		return map;	
	}
	
	/**
	 * 
	 * @return a map of race names with key = race id, value = race name.
	 */
	public static Map<Integer, String> getRaceNameLookup() {
		Map<Integer, String> map = DSL.using(JooqUtil.getJooqConfiguration())
				.select(RACE.ID, RACE.NAME)
				.from(RACE)
				.fetchMap(RACE.ID, RACE.NAME);		
		return map;	
	}
	
	/**
	 * 
	 * @return a map of ethnicity names with key = ethnicity id, value = ethnicity name.
	 */
	public static Map<Integer, String> getEthnicityNameLookup() {
		Map<Integer, String> map = DSL.using(JooqUtil.getJooqConfiguration())
				.select(ETHNICITY.ID, ETHNICITY.NAME)
				.from(ETHNICITY)
				.fetchMap(ETHNICITY.ID, ETHNICITY.NAME);		
		return map;	
	}
	
	/**
	 * 
	 * @return a map of gender names with key = gender id, value = gender name.
	 */
	public static Map<Integer, String> getGenderNameLookup() {
		Map<Integer, String> map = DSL.using(JooqUtil.getJooqConfiguration())
				.select(GENDER.ID, GENDER.NAME)
				.from(GENDER)
				.fetchMap(GENDER.ID, GENDER.NAME);		
		return map;	
	}
	
	/**
	 * Deletes the results of a task, task UUID given in the req parameters.
	 * @param req
	 * @param res
	 * @param userProfile
	 * @return null
	 */
	public static Object deleteTaskResults(Request req, Response res, Optional<UserProfile> userProfile) {
		String uuid = req.params("uuid");

		TaskCompleteRecord completedTask = DSL.using(JooqUtil.getJooqConfiguration()).selectFrom(TASK_COMPLETE)
				.where(TASK_COMPLETE.TASK_UUID.eq(uuid))
				.fetchAny();

		if(completedTask == null) {
			return CoreApi.getErrorResponseNotFound(req, res);
		}
		
		if(CoreApi.isAdmin(userProfile) == false && completedTask.getUserId().equalsIgnoreCase(userProfile.get().getId()) == false) {
			return CoreApi.getErrorResponseForbidden(req, res);
		}

		if (completedTask.get(TASK_COMPLETE.TASK_TYPE).equals("HIF")) {
			TaskUtil.deleteHifResults(uuid);
		} else if (completedTask.get(TASK_COMPLETE.TASK_TYPE).equals("Valuation")) {
			TaskUtil.deleteValuationResults(uuid);
		}
		res.status(204);
		return res;
	}

	// Note that this implementation is currently incomplete. 
	// It will currently ONLY return data for the median_income variable. 
	// It needs to be extended so it will look up each variable that is required.
	/**
	 * 
	 * @param valuationTaskConfig
	 * @param vfDefinitionList
	 * @return
	 */
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

	/**
	 * 
	 * @param variableDatasetId
	 * @return a list of all variable names for a given variable dataset.
	 */
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

	/**
	 * 
	 * @return the database version. If null, returns -999.
	 */
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

	/**
	 * 
	 * @param request
	 * @param paramName
	 * @return a string representation of a given multipart parameter.
	 */
    public static String getMultipartFormParameterAsString(Request request, String paramName) {
        try {
			Part formPart = request.raw().getPart(paramName);
			if(formPart == null || formPart.getSize() == 0) {
				return null;
			}
			InputStream partInputStream = formPart.getInputStream();
			return IOUtils.toString(partInputStream, StandardCharsets.UTF_8);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} catch (ServletException e) {
			e.printStackTrace();
			return null;
		}
    }


	/**
	 * 
	 * @param request
	 * @param paramName
	 * @return an integer representation of a given multipart parameter.
	 */
	public static Integer getMultipartFormParameterAsInteger(Request request, String paramName) {
		return Integer.valueOf(getMultipartFormParameterAsString(request, paramName));
	}
}

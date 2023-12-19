package gov.epa.bencloud.api.util;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.InvalidParameterException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.ServletException;
import javax.servlet.http.Part;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Record2;
import org.jooq.Record3;
import org.jooq.Result;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.pac4j.core.profile.UserProfile;

import static gov.epa.bencloud.server.database.jooq.data.Tables.*;

import gov.epa.bencloud.api.CoreApi;
import gov.epa.bencloud.api.HIFApi;
import gov.epa.bencloud.api.model.ValuationTaskConfig;
import gov.epa.bencloud.server.database.JooqUtil;
import gov.epa.bencloud.server.database.jooq.data.Data;
import gov.epa.bencloud.server.database.jooq.data.Routines;
import gov.epa.bencloud.server.database.jooq.data.tables.records.GetVariableRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.InflationEntryRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.TaskCompleteRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.TaskQueueRecord;
import gov.epa.bencloud.server.tasks.TaskComplete;
import gov.epa.bencloud.server.tasks.TaskQueue;
import gov.epa.bencloud.server.tasks.TaskUtil;
import gov.epa.bencloud.server.tasks.model.Task;
import spark.Request;
import spark.Response;

/**
 * @author jimanderton
 *
 */
public class ApiUtil {

	public static final String appVersion = "0.4.2";
	public static final int minimumDbVersion = 21;
	
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
	 * Cancel a queued task and remove generated results. Task UUID given in the req parameters.
	 * @param req
	 * @param res
	 * @param userProfile
	 */
	
	public static Object cancelTaskAndResults(Request req, Response res, Optional<UserProfile> userProfile) {
		String uuid=req.params("uuid");
		TaskQueueRecord queueTask = DSL.using(JooqUtil.getJooqConfiguration()).selectFrom(TASK_QUEUE)
				.where(TASK_QUEUE.TASK_UUID.eq(uuid))
				.fetchAny();
		
		if(queueTask == null) {
			return CoreApi.getErrorResponseNotFound(req, res);
		}
		if(CoreApi.isAdmin(userProfile) == false && queueTask.getUserId().equalsIgnoreCase(userProfile.get().getId()) == false) {
			return CoreApi.getErrorResponseForbidden(req, res);
		}
		
		// If it's a parent task, cancel its children tasks as well
		Result<Record> resultChild = DSL.using(JooqUtil.getJooqConfiguration()).select().from(TASK_QUEUE)
				.where(TASK_QUEUE.TASK_PARENT_UUID.eq(uuid))
				.fetch();
		for (Record record : resultChild) {			

			String childUuid = record.getValue(TASK_QUEUE.TASK_UUID);
			
			//remove from worker and update in_process = false
			TaskComplete.addTaskToCompleteAndRemoveTaskFromQueue(childUuid, null, false, "Parent task canceled.");
			
			//remove hif and valuation results
			if (record.get(TASK_QUEUE.TASK_TYPE).equals("HIF")) {
				TaskUtil.deleteHifResults(childUuid, false);
			} else if (record.get(TASK_QUEUE.TASK_TYPE).equals("Valuation")) {
				TaskUtil.deleteValuationResults(childUuid, false);
			}			
		}						
		
		//remove (parent) task from worker and update in_process = false
		TaskComplete.addTaskToCompleteAndRemoveTaskFromQueue(uuid, null, false, "Task canceled.");

		//remove hif and valuation results
		if (queueTask.get(TASK_COMPLETE.TASK_TYPE).equals("HIF")) {
			TaskUtil.deleteHifResults(uuid, false);
		} else if (queueTask.get(TASK_COMPLETE.TASK_TYPE).equals("Valuation")) {
			TaskUtil.deleteValuationResults(uuid, false);
		}
		res.status(204);
		return res;
		
	}
	
	/**
	 * Cancel a running batch task and remove generated results. batch task id given in the req parameters.
	 * @param req
	 * @param res
	 * @param userProfile
	 */
	public static Object cancelBatchTaskAndResults(Request req, Response res, Optional<UserProfile> userProfile) {
		int batchId = Integer.valueOf(req.params("id"));
		
		//check batch task existence and ownership
		Result<Record> result = DSL.using(JooqUtil.getJooqConfiguration()).select().from(TASK_BATCH)
				.where(TASK_BATCH.ID.eq(batchId))
				.fetch();
		
		if(result == null) {
			return CoreApi.getErrorResponseNotFound(req, res);
		}
		else if(result.size()==0) {
			return CoreApi.getErrorResponseNotFound(req, res);
		}
		
		if(CoreApi.isAdmin(userProfile) == false && result.get(0).getValue(TASK_BATCH.USER_ID).equalsIgnoreCase(userProfile.get().getId()) == false) {
			return CoreApi.getErrorResponseForbidden(req, res);
		}
		
		// cancel batch task
		result = DSL.using(JooqUtil.getJooqConfiguration()).select().from(TASK_QUEUE)
				.where(TASK_QUEUE.TASK_BATCH_ID.eq(batchId))
				.fetch();
		
		if(result == null) {
			return CoreApi.getErrorResponseNotFound(req, res);
		}
		
		for (Record record : result) {			

			String uuid = record.getValue(TASK_QUEUE.TASK_UUID);
			
			//remove from worker and update in_process = false
			TaskComplete.addTaskToCompleteAndRemoveTaskFromQueue(uuid, null, false, "Task canceled.");
			//remove hif and valuation results
			if (record.get(TASK_QUEUE.TASK_TYPE).equals("HIF")) {
				TaskUtil.deleteHifResults(uuid, false);
			} else if (record.get(TASK_QUEUE.TASK_TYPE).equals("Valuation")) {
				TaskUtil.deleteValuationResults(uuid, false);
			}			
		}		
				
		res.status(204);
		return res;
		
	}
	
	

	/**
	 * @param requiredVariableNames
	 * @param variableDatasetId
	 * @param gridId
	 * @return a map from variable names and grid cell ids to variable values
	 * @throws InvalidParameterException
	 */
	public static Map<String, Map<Long, Double>> getVariableValues(List<String> requiredVariableNames, Integer variableDatasetId, Integer gridId) 
		throws DataAccessException	
	{
		Map<String, Map<Long, Double>> variableMap = new HashMap<String, Map<Long, Double>>();

		if (requiredVariableNames == null || requiredVariableNames.size() == 0 || variableDatasetId == null || gridId == null) {
			return variableMap;
		}

		// FOR EACH VARIABLE
		for (String variableName : requiredVariableNames) {
			Result<GetVariableRecord> variableRecords = Routines.getVariable(JooqUtil.getJooqConfiguration(), 
					variableDatasetId, 
					variableName, 
					gridId);


			// Map from grid cell id to variable value
			Map<Long, Double> values = new HashMap<Long, Double>();

			// FOR EACH GRID CELL
			for (GetVariableRecord variableRecord : variableRecords) {
				values.put(variableRecord.getGridCellId(), variableRecord.getValue());
			}

			variableMap.put(variableName, values);
		}

		return variableMap;
	}

	/**
	 * Like getVariableValues, but instead returning a map from variable id and grid cell id to its value, rather than it's name + grid cell id
	 * This is used for the exposure tasks
	 * @param requiredVariableNames
	 * @param variableDatasetId
	 * @param gridId
	 * @return A map from variable ids and grid cell ids to variable values
	 */
	public static Map<Integer, Map<Long, Double>> getVariableValuesFromIds(List<Integer> requiredVariableIds, Integer gridId) {
		Map<Integer, Map<Long, Double>> variableMap = new HashMap<Integer, Map<Long, Double>>();

		if (requiredVariableIds == null || requiredVariableIds.size() == 0 || gridId == null) {
			return variableMap;
		}

		// Get the corresponding names and dataset IDs for each variable ID, so we can use Routines.getVariable
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());
		Result<Record3<Integer, Integer, String>> results = create
			.select(VARIABLE_ENTRY.ID, VARIABLE_ENTRY.VARIABLE_DATASET_ID, VARIABLE_ENTRY.NAME)
			.from(VARIABLE_ENTRY)
			.where(VARIABLE_ENTRY.ID.in(requiredVariableIds))
			.fetch();
		

		// FOR EACH VARIABLE
		for (Record3<Integer, Integer, String> result : results ) {
			Result<GetVariableRecord> variableRecords = Routines.getVariable(JooqUtil.getJooqConfiguration(), 
					result.value2(), 
					result.value3(), 
					gridId);


			// Map from grid cell id to variable value
			Map<Long, Double> values = new HashMap<Long, Double>();

			// FOR EACH GRID CELL
			for (GetVariableRecord variableRecord : variableRecords) {
				values.put(variableRecord.getGridCellId(), variableRecord.getValue());
			}

			variableMap.put(result.value1(), values);
		}

		return variableMap;
	}
	
	
	/**
	 * 
	 * @param variableDatasetId
	 * @return a list of all variable names for a given variable dataset.
	 */
	public static String getVariableName(Integer variableId) {
		if(variableId == null) {
			return null;
		}

		Record1<String> variableName = DSL.using(JooqUtil.getJooqConfiguration())
				.select(VARIABLE_ENTRY.NAME)
				.from(VARIABLE_ENTRY)
				.where(VARIABLE_ENTRY.ID.eq(variableId))
				.fetchOne();
		return variableName.value1();
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
    
    public static LocalDateTime getMultipartFormParameterAsLocalDateTime(Request request, String paramName, String strFormatter) {
        String strParaValue = getMultipartFormParameterAsString(request, paramName);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(strFormatter);
        LocalDateTime dateTime = null;
        try {
            dateTime = LocalDateTime.parse(strParaValue, formatter);
        } catch (DateTimeParseException e) {
        	try{
        		dateTime = LocalDateTime.parse(strParaValue);
        	}
        	catch(DateTimeParseException e2) {
        		return null;
        	}        	
        	
        }    	
    	return dateTime;
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
	    
    /**
     * 
     * @param userId
     * @return a list of all template names for a given user id.
     */
    public static List<String> getAllTemplateNamesByUser(String userId) {
        if(userId == null) {
            return null;
        }

        List<String> allTemplateNames = DSL.using(JooqUtil.getJooqConfiguration())
                .select(TASK_CONFIG.NAME)
                .from(TASK_CONFIG)
                .where(TASK_CONFIG.USER_ID.equal(userId))
                .orderBy(TASK_CONFIG.USER_ID)
                .fetch(TASK_CONFIG.NAME);
        return allTemplateNames;
    }

    
    /**
	 * Deletes the results of a completed batch_task, task task_batch_id given in the req parameters.
	 * @param req
	 * @param res
	 * @param userProfile
	 * @return null
	 */
	public static Object deleteBatchTaskResults(Request req, Response res, Optional<UserProfile> userProfile) {
		String idParam;
		Integer batchId;
		try {
			idParam = String.valueOf(req.params("id"));

			batchId = Integer.valueOf(idParam);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return CoreApi.getErrorResponseInvalidId(req, res);	
		} catch (Exception e) {
			e.printStackTrace();
			return CoreApi.getErrorResponseInvalidId(req, res);
		}
		

		
		Result<Record> result = DSL.using(JooqUtil.getJooqConfiguration()).select().from(TASK_BATCH)
				.where(TASK_BATCH.ID.eq(batchId))
				.fetch();
		
		if(result == null) {
			return CoreApi.getErrorResponseNotFound(req, res);
		}
		else if(result.size()==0) {
			return CoreApi.getErrorResponseNotFound(req, res);
		}
		
		if(CoreApi.isAdmin(userProfile) == false && result.get(0).getValue(TASK_BATCH.USER_ID).equalsIgnoreCase(userProfile.get().getId()) == false) {
			return CoreApi.getErrorResponseForbidden(req, res);
		}
		
		result = DSL.using(JooqUtil.getJooqConfiguration()).select().from(TASK_COMPLETE)
				.where(TASK_COMPLETE.TASK_BATCH_ID.eq(batchId))
				.fetch();
		
		if(result == null) {
			return CoreApi.getErrorResponseNotFound(req, res);
		}
		else if(result.size()==0) {
			return CoreApi.getErrorResponseNotFound(req, res);
		}		
		
		for (Record record : result) {			
			//TODO: Do we need to remove children task results before removing parent tasks? 
			// Valuation results have hif tasks as their parents but both hif and valuation results have the same task_batch_id.
			String uuid = record.getValue(TASK_COMPLETE.TASK_UUID);
			if (record.get(TASK_COMPLETE.TASK_TYPE).equals("HIF")) {
				TaskUtil.deleteHifResults(uuid, true);
			} else if (record.get(TASK_COMPLETE.TASK_TYPE).equals("Valuation")) {
				TaskUtil.deleteValuationResults(uuid, true);
			}
			 else if (record.get(TASK_COMPLETE.TASK_TYPE).equals("Exposure")) {
					TaskUtil.deleteExposureResults(uuid, true);
				}
		}
		
		//delete from batch_task table	
		DSL.using(JooqUtil.getJooqConfiguration()).deleteFrom(TASK_BATCH)
		.where(TASK_BATCH.ID.eq(batchId))
		.execute();
		
		res.status(204);
		return res;
	}

	/*
	 * Code from https://stackoverflow.com/questions/7548841/round-a-double-to-3-significant-figures
	 */
	public static String getValueSigFigs(double value, int significantDigits) {
		BigDecimal bd = new BigDecimal(value, MathContext.DECIMAL64);
		bd = bd.round(new MathContext(significantDigits));
		final int precision = bd.precision();
		if (precision < significantDigits) {
			bd = bd.setScale(bd.scale() + (significantDigits-precision));
		}
 
		String format = "#,###.";
		for (int i = 0; i < significantDigits; i++){
			format+="#";
		}
		DecimalFormat df = new DecimalFormat(format);
		return df.format(bd);
	}

	/**
	 * Formats results for display
	 * @param pointEstimate
	 * @param p2_5
	 * @param p97_5
	 * @param sigFigs
	 * @return string containing formatted point estimate and 95% confidence interval
	 */
	public static String createFormattedResultsString(Double pointEstimate, Double p2_5, Double p97_5, int sigFigs) {
		StringBuilder s = new StringBuilder();
		s.append(getValueSigFigs(pointEstimate, sigFigs));
		s.append(" (");
		s.append(getValueSigFigs(p2_5, sigFigs));
		s.append(" to ");
		s.append(getValueSigFigs(p97_5, sigFigs));
		s.append(")");

		return s.toString();
	}
}

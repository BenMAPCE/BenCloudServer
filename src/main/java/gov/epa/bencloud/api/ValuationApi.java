package gov.epa.bencloud.api;

import static gov.epa.bencloud.server.database.jooq.data.Tables.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.MultipartConfigElement;

import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jetbrains.annotations.NotNull;
import org.jooq.Cursor;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.JSONFormat;
import org.jooq.Result;
import org.jooq.SortOrder;
import org.jooq.Table;
import org.jooq.exception.DataAccessException;
import org.jooq.JSONFormat.RecordFormat;
import org.jooq.OrderField;
import org.jooq.Condition;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Record2;
import org.jooq.Record21;
import org.jooq.Record22;
import org.jooq.Record4;
import org.jooq.impl.DSL;
import org.pac4j.core.profile.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jooq.JSON;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opencsv.CSVReader;

import org.mariuszgromada.math.mxparser.Argument;
import org.mariuszgromada.math.mxparser.Expression;
import org.mariuszgromada.math.mxparser.mXparser;
import org.mariuszgromada.math.mxparser.Constant;

import gov.epa.bencloud.Constants;
import gov.epa.bencloud.api.model.BatchTaskConfig;
import gov.epa.bencloud.api.model.HIFTaskLog;
import gov.epa.bencloud.api.model.ValidationMessage;
import gov.epa.bencloud.api.model.ValuationConfig;
import gov.epa.bencloud.api.model.ValuationTaskConfig;
import gov.epa.bencloud.api.model.ValuationTaskLog;
import gov.epa.bencloud.api.util.ApiUtil;
import gov.epa.bencloud.api.util.HIFUtil;
import gov.epa.bencloud.api.util.IncidenceUtil;
import gov.epa.bencloud.api.util.ValuationUtil;
import gov.epa.bencloud.server.database.JooqUtil;
import gov.epa.bencloud.server.database.jooq.data.tables.records.EndpointGroupRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.EndpointRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.GetValuationResultsRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.HealthImpactFunctionGroupRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.HealthImpactFunctionRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.ValuationFunctionRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.ValuationResultDatasetRecord;
import gov.epa.bencloud.server.util.ApplicationUtil;
import gov.epa.bencloud.server.util.DataConversionUtil;
import gov.epa.bencloud.server.util.ParameterUtil;
import spark.Request;
import spark.Response;

/*
 * Methods related to valuation data.
 */
public class ValuationApi {
	private static final Logger log = LoggerFactory.getLogger(ValuationApi.class);
	
	/**
	 * Gets the contents of a valuation result dataset and stores it in the response.
	 * @param request
	 * @param response
	 * @param userProfile
	 */
	public static void getValuationResultContents(Request request, Response response, Optional<UserProfile> userProfile) {
		
		 //*  :id (valuation results dataset id)
		 //*  gridId= (aggregate the results to another grid definition)
		 //*  hifId= (filter results to those from one or more functions via comma delimited list)
		 //*  vfId= (filter results to those from one or more functions via comma delimited list)
		 //*  page=
		 //*  rowsPerPage=
		 //*  sortBy=
		 //*  descending=
		 //*  filter=

		// TODO: Add user security enforcement 
		// TODO: Implement sortBy, descending, and filter

		String idParam;
		Integer id;	

		String hifIdsParam;
		String vfIdsParam;
		int gridId;
		int page;
		int rowsPerPage;
		String sortBy;
		boolean descending;
		String filter;

		try {

			idParam = String.valueOf(request.params("id"));
			id = idParam.length() == 36 ? ValuationApi.getValuationResultDatasetId(idParam) : Integer.valueOf(idParam);


			hifIdsParam = request.raw().getParameter("hifId");
			vfIdsParam = request.raw().getParameter("vfId");
			
			gridId = ParameterUtil.getParameterValueAsInteger(request.raw().getParameter("gridId"), 0);			
			page = ParameterUtil.getParameterValueAsInteger(request.raw().getParameter("page"), 1);
			rowsPerPage = ParameterUtil.getParameterValueAsInteger(request.raw().getParameter("rowsPerPage"), 1000);
			sortBy = ParameterUtil.getParameterValueAsString(request.raw().getParameter("sortBy"), "");
			descending = ParameterUtil.getParameterValueAsBoolean(request.raw().getParameter("descending"), false);
			filter = ParameterUtil.getParameterValueAsString(request.raw().getParameter("filter"), "");
		} catch (NumberFormatException e) {
			e.printStackTrace();
			CoreApi.getErrorResponseInvalidId(request, response);
			return;
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			CoreApi.getErrorResponseInvalidId(request, response);
			return;
		}
		
		
		List<Integer> hifIds = (hifIdsParam == null || hifIdsParam.equals("")) ? null : Stream.of(hifIdsParam.split(",")).mapToInt(Integer::parseInt).boxed().collect(Collectors.toList());
		List<Integer> vfIds = (vfIdsParam == null || vfIdsParam.equals(""))? null : Stream.of(hifIdsParam.split(",")).mapToInt(Integer::parseInt).boxed().collect(Collectors.toList());
		
//		try {
//			if(gridId == 0) {
//				gridId = ValuationApi.getBaselineGridForValuationResults(id).intValue();
//			}
//		} catch (NullPointerException e) {
//			e.printStackTrace();
//			response.status(400);
//			return;
//		}
		
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());

		//If the crosswalk isn't there, create it now
		CrosswalksApi.ensureCrosswalkExists(ValuationApi.getBaselineGridForValuationResults(id), gridId);
		Integer limitToGridId = ValuationApi.getValuationTaskConfigFromDb(id).limitToGridId;
		Table<GetValuationResultsRecord> vfResultRecords = create.selectFrom(
				GET_VALUATION_RESULTS(
						id, 
						hifIds == null ? null : hifIds.toArray(new Integer[0]),
						vfIds == null ? null : vfIds.toArray(new Integer[0]),
								gridId,
						limitToGridId))
				.asTable("vf_result_records");
		
		Result<?> vfRecordsClean = null;
		Result<Record> vfRecords;
		try {
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
				TIMING_TYPE.NAME.as("timing"),
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
				vfResultRecords.field(GET_VALUATION_RESULTS.PERCENTILES),
				DSL.val(null, String.class).as("formatted_results_2sf"),
				DSL.val(null, String.class).as("formatted_results_3sf"),
				vfResultRecords.field(GET_VALUATION_RESULTS.VF_ID)
				)
				.from(vfResultRecords)
				.join(VALUATION_RESULT_FUNCTION_CONFIG)
				.on(VALUATION_RESULT_FUNCTION_CONFIG.VALUATION_RESULT_DATASET_ID.eq(id)
						.and(VALUATION_RESULT_FUNCTION_CONFIG.HIF_ID.eq(vfResultRecords.field(GET_VALUATION_RESULTS.HIF_ID)))
						.and(VALUATION_RESULT_FUNCTION_CONFIG.HIF_INSTANCE_ID.eq(vfResultRecords.field(GET_VALUATION_RESULTS.HIF_INSTANCE_ID)))
						.and(VALUATION_RESULT_FUNCTION_CONFIG.VF_ID.eq(vfResultRecords.field(GET_VALUATION_RESULTS.VF_ID))))
				.join(VALUATION_RESULT_DATASET)
				.on(VALUATION_RESULT_FUNCTION_CONFIG.VALUATION_RESULT_DATASET_ID.eq(VALUATION_RESULT_DATASET.ID))
				.join(HIF_RESULT_FUNCTION_CONFIG)
				.on(VALUATION_RESULT_DATASET.HIF_RESULT_DATASET_ID.eq(HIF_RESULT_FUNCTION_CONFIG.HIF_RESULT_DATASET_ID)
						.and(VALUATION_RESULT_FUNCTION_CONFIG.HIF_ID.eq(HIF_RESULT_FUNCTION_CONFIG.HIF_ID))
						.and(VALUATION_RESULT_FUNCTION_CONFIG.HIF_INSTANCE_ID.eq(HIF_RESULT_FUNCTION_CONFIG.HIF_INSTANCE_ID)))
				.join(VALUATION_FUNCTION).on((VALUATION_FUNCTION.ID.eq(vfResultRecords.field(GET_VALUATION_RESULTS.VF_ID))))
				.leftJoin(HEALTH_IMPACT_FUNCTION).on(vfResultRecords.field(GET_VALUATION_RESULTS.HIF_ID).eq(HEALTH_IMPACT_FUNCTION.ID))
				.leftJoin(RACE).on(HIF_RESULT_FUNCTION_CONFIG.RACE_ID.eq(RACE.ID))
				.leftJoin(ETHNICITY).on(HIF_RESULT_FUNCTION_CONFIG.ETHNICITY_ID.eq(ETHNICITY.ID))
				.leftJoin(GENDER).on(HIF_RESULT_FUNCTION_CONFIG.GENDER_ID.eq(GENDER.ID))
				.leftJoin(POLLUTANT_METRIC).on(HIF_RESULT_FUNCTION_CONFIG.METRIC_ID.eq(POLLUTANT_METRIC.ID))
				.leftJoin(SEASONAL_METRIC).on(HIF_RESULT_FUNCTION_CONFIG.SEASONAL_METRIC_ID.eq(SEASONAL_METRIC.ID))
				.leftJoin(STATISTIC_TYPE).on(HIF_RESULT_FUNCTION_CONFIG.METRIC_STATISTIC.eq(STATISTIC_TYPE.ID))
				.leftJoin(TIMING_TYPE).on(HIF_RESULT_FUNCTION_CONFIG.TIMING_ID.eq(TIMING_TYPE.ID))
				//.offset((page * rowsPerPage) - rowsPerPage)
				//.limit(rowsPerPage)
				.fetch();
			
				//If results are being aggregated, recalc mean, variance, std deviation, and percent of baseline
				if(ValuationApi.getBaselineGridForValuationResults(id) != gridId) {
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

				// Add in valuation function information
				ValuationTaskLog vfTaskLog = ValuationUtil.getTaskLog(id);
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
				
				// Add in the formatted results
				for (Record res : vfRecords) {
					res.setValue(DSL.field("formatted_results_2sf", String.class), 
									ApiUtil.createFormattedResultsString(res.get("point_estimate", Double.class), res.get("pct_2_5", Double.class), res.get("pct_97_5", Double.class), 2, true));
					res.setValue(DSL.field("formatted_results_3sf", String.class), 
									ApiUtil.createFormattedResultsString(res.get("point_estimate", Double.class), res.get("pct_2_5", Double.class), res.get("pct_97_5", Double.class), 3, true));
				}
				
				//Remove percentiles by keeping all other fields
				vfRecordsClean = vfRecords.into(vfRecords.fields(0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,22,23,25,26));
				//21 variance, 24percentiles, 25 formatted 2sf, 26 formatted 3sf, 27 vfid
		} catch (DataAccessException e) {
			e.printStackTrace();
			response.status(400);
			return;
		}
		
		try {
			response.type("application/json");
			vfRecordsClean.formatJSON(response.raw().getWriter(), new JSONFormat().header(false).recordFormat(RecordFormat.OBJECT));
		} catch (Exception e) {
			log.error("Error in Valuation export", e);
		} finally {

		}
		
	}
	
	
	/**
	 * Exports all valuation results to a zip file.
	 * @param request
	 * @param response
	 * @param userProfile
	 */
	
	/* OBSOLETE CODE
	public static void getValuationResultExport(Request request, Response response, Optional<UserProfile> userProfile) {
		
		 //*  :id (valuation results dataset id)
		 //*  gridId= (aggregate the results to another grid definition)
		// TODO: Add user security enforcement 		 
		
		String idParam;
		Integer id;
		String gridIdParam;
		try {
			idParam = String.valueOf(request.params("id"));

			//If the id is 36 characters long, we'll assume it's a task uuid
			id = idParam.length() == 36 ? ValuationApi.getValuationResultDatasetId(idParam) : Integer.valueOf(idParam);
			gridIdParam = ParameterUtil.getParameterValueAsString(request.raw().getParameter("gridId"), "");
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			CoreApi.getErrorResponseInvalidId(request, response);
			return;
		} catch (Exception e) {
			e.printStackTrace();
			CoreApi.getErrorResponseInvalidId(request, response);
			return;
		}
		

		int[] gridIds = (gridIdParam==null || gridIdParam.equals("")) ? null : Arrays.stream(gridIdParam.split(","))
			    .mapToInt(Integer::parseInt)
			    .toArray();
		
		// If a gridId wasn't provided, look up the baseline AQ grid grid for this resultset
		try{
			if(gridIds == null) {
				gridIds = new int[] {ValuationApi.getBaselineGridForValuationResults(id).intValue()};
			}	
		} catch (NullPointerException e) {
			e.printStackTrace();
			response.status(400);
			return;
		}

		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());
		response.type("application/zip");
		String taskFileName = ApplicationUtil.replaceNonValidCharacters(ValuationApi.getValuationTaskConfigFromDb(id).name);
		response.header("Content-Disposition", "attachment; filename=" + taskFileName + ".zip");
		response.header("Access-Control-Expose-Headers", "Content-Disposition");

		OutputStream responseOutputStream;
		ZipOutputStream zipStream;
		try {
			// Get response output stream
			responseOutputStream = response.raw().getOutputStream();
			// Stream .ZIP file to response
			zipStream = new ZipOutputStream(responseOutputStream);
		} catch (java.io.IOException e1) {
			log.error("Error getting output stream", e1);
			return;
		}
		
		for(int i=0; i < gridIds.length; i++) {
			Result<?> vfRecordsClean = null;
			Result<Record> vfRecords;
			try {
				Table<GetValuationResultsRecord> vfResultRecords = create.selectFrom(
						GET_VALUATION_RESULTS(
								id, 
								null,
								null ,
								gridIds[i]))
						.asTable("vf_result_records");

				vfRecords = create.select(
						vfResultRecords.field(GET_VALUATION_RESULTS.GRID_COL).as("column"),
						vfResultRecords.field(GET_VALUATION_RESULTS.GRID_ROW).as("row"),
						ENDPOINT.NAME.as("endpoint"),
						VALUATION_FUNCTION.QUALIFIER.as("name"),
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
						vfResultRecords.field(GET_VALUATION_RESULTS.POINT_ESTIMATE),
						vfResultRecords.field(GET_VALUATION_RESULTS.MEAN),
						vfResultRecords.field(GET_VALUATION_RESULTS.STANDARD_DEV).as("standard_deviation"),
						vfResultRecords.field(GET_VALUATION_RESULTS.VARIANCE).as("variance"),
						vfResultRecords.field(GET_VALUATION_RESULTS.PCT_2_5),
						vfResultRecords.field(GET_VALUATION_RESULTS.PCT_97_5),
						ValuationApi.getBaselineGridForValuationResults(id) == gridIds[i] ? null : vfResultRecords.field(GET_VALUATION_RESULTS.PERCENTILES), //Only include percentiles if we're aggregating
						DSL.val(null, String.class).as("formatted_results_2sf"),
						DSL.val(null, String.class).as("formatted_results_3sf")
						)

						.from(vfResultRecords)
						.join(VALUATION_RESULT_FUNCTION_CONFIG)
						.on(VALUATION_RESULT_FUNCTION_CONFIG.VALUATION_RESULT_DATASET_ID.eq(id)
								.and(VALUATION_RESULT_FUNCTION_CONFIG.HIF_ID.eq(vfResultRecords.field(GET_VALUATION_RESULTS.HIF_ID)))
								.and(VALUATION_RESULT_FUNCTION_CONFIG.HIF_INSTANCE_ID.eq(vfResultRecords.field(GET_VALUATION_RESULTS.HIF_INSTANCE_ID)))
								.and(VALUATION_RESULT_FUNCTION_CONFIG.VF_ID.eq(vfResultRecords.field(GET_VALUATION_RESULTS.VF_ID))))
						.join(VALUATION_RESULT_DATASET)
						.on(VALUATION_RESULT_FUNCTION_CONFIG.VALUATION_RESULT_DATASET_ID.eq(VALUATION_RESULT_DATASET.ID))
						.join(HIF_RESULT_FUNCTION_CONFIG)
						.on(VALUATION_RESULT_DATASET.HIF_RESULT_DATASET_ID.eq(HIF_RESULT_FUNCTION_CONFIG.HIF_RESULT_DATASET_ID)
								.and(VALUATION_RESULT_FUNCTION_CONFIG.HIF_ID.eq(HIF_RESULT_FUNCTION_CONFIG.HIF_ID))
								.and(VALUATION_RESULT_FUNCTION_CONFIG.HIF_INSTANCE_ID.eq(HIF_RESULT_FUNCTION_CONFIG.HIF_INSTANCE_ID)))
						.join(VALUATION_FUNCTION).on(VALUATION_FUNCTION.ID.eq(vfResultRecords.field(GET_VALUATION_RESULTS.VF_ID)))
						.join(HEALTH_IMPACT_FUNCTION).on(vfResultRecords.field(GET_VALUATION_RESULTS.HIF_ID).eq(HEALTH_IMPACT_FUNCTION.ID))
						.join(ENDPOINT).on(ENDPOINT.ID.eq(VALUATION_FUNCTION.ENDPOINT_ID))
						.join(RACE).on(HIF_RESULT_FUNCTION_CONFIG.RACE_ID.eq(RACE.ID))
						.join(ETHNICITY).on(HIF_RESULT_FUNCTION_CONFIG.ETHNICITY_ID.eq(ETHNICITY.ID))
						.join(GENDER).on(HIF_RESULT_FUNCTION_CONFIG.GENDER_ID.eq(GENDER.ID))
						.join(POLLUTANT_METRIC).on(HIF_RESULT_FUNCTION_CONFIG.METRIC_ID.eq(POLLUTANT_METRIC.ID))
						.leftJoin(SEASONAL_METRIC).on(HIF_RESULT_FUNCTION_CONFIG.SEASONAL_METRIC_ID.eq(SEASONAL_METRIC.ID))
						.join(STATISTIC_TYPE).on(HIF_RESULT_FUNCTION_CONFIG.METRIC_STATISTIC.eq(STATISTIC_TYPE.ID))
						.fetch();
				
				
						//If results are being aggregated, recalc mean, variance, std deviation, and percent of baseline
						if(ValuationApi.getBaselineGridForValuationResults(id) != gridIds[i]) {
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

						for (Record res : vfRecords) {
							res.setValue(DSL.field("formatted_results_2sf", String.class), 
										 ApiUtil.createFormattedResultsString(res.get("point_estimate", Double.class), res.get("pct_2_5", Double.class), res.get("pct_97_5", Double.class), 2));
							res.setValue(DSL.field("formatted_results_3sf", String.class), 
										 ApiUtil.createFormattedResultsString(res.get("point_estimate", Double.class), res.get("pct_2_5", Double.class), res.get("pct_97_5", Double.class), 3));
						}

						//Remove percentiles by keeping all other fields
						vfRecordsClean = vfRecords.into(vfRecords.fields(0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,22,23));
			} catch (DataAccessException e) {
				e.printStackTrace();
				response.status(400);
				return;
			}
			try {
				zipStream.putNextEntry(new ZipEntry(taskFileName + "_" + ApplicationUtil.replaceNonValidCharacters(GridDefinitionApi.getGridDefinitionName(gridIds[i])) + ".csv"));
				vfRecordsClean.formatCSV(zipStream);
			} catch (Exception e) {
				log.error("Error in Valuation export", e);
			} finally {

			}
			
		}
		
		try {
			zipStream.putNextEntry(new ZipEntry(taskFileName + "_TaskLog.txt"));
			
			ValuationTaskLog vfTaskLog = ValuationUtil.getTaskLog(id);
			zipStream.write(vfTaskLog.toString().getBytes());
			
			//Get HifTaskLog and render below valuation log
			zipStream.write("\n".getBytes());
			HIFTaskLog hifTaskLog = HIFUtil.getTaskLog(ValuationUtil.getHifResultDatasetIdForValuationResultDataset(id));
			zipStream.write(hifTaskLog.toString(userProfile).getBytes());
			
			zipStream.closeEntry();
			
			zipStream.close();
			responseOutputStream.flush();
		} catch (Exception e) {
			log.error("Error writing task log, closing and flushing export", e);
		}
		
	}
	
	*/
	
	/**
	 * 
	 * @param valuationResultDatasetId
	 * @return a valuation task configuration from a given valuation result dataset id.
	 */
	public static ValuationTaskConfig getValuationTaskConfigFromDb(Integer valuationResultDatasetId) {
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());

		ValuationTaskConfig valuationTaskConfig = new ValuationTaskConfig();
		
		ValuationResultDatasetRecord valuationTaskConfigRecord = create
				.selectFrom(VALUATION_RESULT_DATASET)
				.where(VALUATION_RESULT_DATASET.ID.eq(valuationResultDatasetId))
				.fetchOne();
		
		valuationTaskConfig.name = valuationTaskConfigRecord.getName();
		//TODO: Add code to load the other details
		BatchTaskConfig batchTaskConfig = TaskApi.getTaskBatchConfigFromDbByResultID(valuationResultDatasetId,"valuation");

		valuationTaskConfig.limitToGridId = batchTaskConfig.limitToGridId;
		
		return valuationTaskConfig;
	}
	
	/**
	 * 
	 * @param uuid
	 * @return a valuation result dataset id from a given valuation task uuid.
	 */
	public static Integer getValuationResultDatasetId(String uuid) {

		ValuationResultDatasetRecord valuationResultDataset = DSL.using(JooqUtil.getJooqConfiguration())
		.select(VALUATION_RESULT_DATASET.asterisk())
		.from(VALUATION_RESULT_DATASET)
		.where(VALUATION_RESULT_DATASET.TASK_UUID.eq(uuid))
		.fetchOneInto(VALUATION_RESULT_DATASET);
		
		if(valuationResultDataset == null) {
			return null;
		}
		return valuationResultDataset.getId();
	}

	/**
	 * 
	 * @param request
	 * @param response
	 * @param userProfile
	 * @return a JSON representation of all valuation functions.
	 */
	public static Object getAllValuationFunctions(Request request, Response response, Optional<UserProfile> userProfile) {

		Result<Record> valuationRecords = null;
		try {
			valuationRecords = DSL.using(JooqUtil.getJooqConfiguration())
					.select(VALUATION_FUNCTION.asterisk()
							, ENDPOINT_GROUP.NAME.as("endpoint_group_name")
							, ENDPOINT.NAME.as("endpoint_name")
							)
					.from(VALUATION_FUNCTION)
					.join(ENDPOINT_GROUP).on(VALUATION_FUNCTION.ENDPOINT_GROUP_ID.eq(ENDPOINT_GROUP.ID))
					.join(ENDPOINT).on(VALUATION_FUNCTION.ENDPOINT_ID.eq(ENDPOINT.ID))
					.where(VALUATION_FUNCTION.ARCHIVED.eq((short) 0))
					.orderBy(ENDPOINT_GROUP.NAME, ENDPOINT.NAME, VALUATION_FUNCTION.QUALIFIER)
					.fetch();
		} catch (DataAccessException e) {
			log.error("Error getAllValuationFunctions", e);
		}
		
		response.type("application/json");
		return valuationRecords.formatJSON(new JSONFormat().header(false).recordFormat(RecordFormat.OBJECT));
	}

	/**
	 * 
	 * @param request
	 * @param response
	 * @param userProfile
	 * @return a JSON representation of all valuation functions.
	 */
	public static Object getAllValuationFunctionsByHealthEffect(Request request, Response response, Optional<UserProfile> userProfile) {

		String userId = userProfile.get().getId();	
		int healthEffectGroupId;
		int page;
		int rowsPerPage;
		String sortBy;
		boolean descending;
		String filter;
		boolean showAll;
		try {
			healthEffectGroupId = ParameterUtil.getParameterValueAsInteger(request.raw().getParameter("healthEffectGroupId"), 0);
			page = ParameterUtil.getParameterValueAsInteger(request.raw().getParameter("page"), 1);
			
			if(ParameterUtil.getParameterValueAsString(request.raw().getParameter("rowsPerPage"), "").equalsIgnoreCase("0")) {
				rowsPerPage = 1000000; //Let's use one million as a reasonable approximation of "all"
			} else {
				rowsPerPage = ParameterUtil.getParameterValueAsInteger(request.raw().getParameter("rowsPerPage"), 10);	
			}
			sortBy = ParameterUtil.getParameterValueAsString(request.raw().getParameter("sortBy"), "");
			descending = ParameterUtil.getParameterValueAsBoolean(request.raw().getParameter("descending"), false);
			filter = ParameterUtil.getParameterValueAsString(request.raw().getParameter("filter"), "");
			showAll = ParameterUtil.getParameterValueAsBoolean(request.raw().getParameter("showAll"), false);
			
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return CoreApi.getErrorResponseInvalidId(request, response);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return CoreApi.getErrorResponseInvalidId(request, response);
		}

		List<OrderField<?>> orderFields = new ArrayList<>();
		
		setValuationFunctionSortOrder(sortBy, descending, orderFields);

		Condition filterCondition = DSL.trueCondition();

		Condition healthEffectGroupCondition = DSL.trueCondition();

		if (healthEffectGroupId != 0) {
			healthEffectGroupCondition = DSL.field(VALUATION_FUNCTION.ENDPOINT_GROUP_ID).eq(healthEffectGroupId);
			filterCondition = filterCondition.and(healthEffectGroupCondition);
		}

		if (!"".equals(filter)) {
			filterCondition = filterCondition.and(buildValuationFunctionFilterCondition(filter));
		}

		if(!showAll || !CoreApi.isAdmin(userProfile)) {
			filterCondition = filterCondition.and(VALUATION_FUNCTION.SHARE_SCOPE.eq(Constants.SHARING_ALL).or(VALUATION_FUNCTION.USER_ID.eq(userId)));
		}

		filterCondition = filterCondition.and(VALUATION_FUNCTION.ARCHIVED.eq((short) 0));

		Integer filteredRecordsCount = 
				DSL.using(JooqUtil.getJooqConfiguration()).select(DSL.count())
				.from(VALUATION_FUNCTION)
					.join(ENDPOINT_GROUP).on(VALUATION_FUNCTION.ENDPOINT_GROUP_ID.eq(ENDPOINT_GROUP.ID))
					.join(ENDPOINT).on(VALUATION_FUNCTION.ENDPOINT_ID.eq(ENDPOINT.ID))
					.where(filterCondition)
				.fetchOne(DSL.count());

		Result<Record> valuationRecords = null;
		try {
			valuationRecords = DSL.using(JooqUtil.getJooqConfiguration())
					.select(VALUATION_FUNCTION.asterisk()
							, ENDPOINT_GROUP.NAME.as("endpoint_group_name")
							, ENDPOINT.NAME.as("endpoint_name")
							)
					.from(VALUATION_FUNCTION)
					.join(ENDPOINT_GROUP).on(VALUATION_FUNCTION.ENDPOINT_GROUP_ID.eq(ENDPOINT_GROUP.ID))
					.join(ENDPOINT).on(VALUATION_FUNCTION.ENDPOINT_ID.eq(ENDPOINT.ID))
					.where(filterCondition)
					.orderBy(orderFields)
					.offset((page * rowsPerPage) - rowsPerPage)
					.limit(rowsPerPage)
					.fetch();
		} catch (DataAccessException e) {
			log.error("Error getAllValuationFunctions", e);
		}
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode data = mapper.createObjectNode();

		data.put("filteredRecordsCount", filteredRecordsCount);

		try {
			JsonFactory factory = mapper.getFactory();
			JsonParser jp = factory.createParser(
					valuationRecords.formatJSON(new JSONFormat().header(false).recordFormat(RecordFormat.OBJECT)));
			JsonNode actualObj = mapper.readTree(jp);
			data.set("records", actualObj);
		} catch (JsonParseException e) {
			log.error("Error parsing JSON",e);
		} catch (JsonProcessingException e) {
			log.error("Error processing JSON",e);
		} catch (IOException e) {
			log.error("IO Exception", e);
		}

		response.type("application/json");
		return data;
	}


	/*
	 * @param userProfile
	 * @return JSON representation of all health effect groups for a given user.
	 */
	public static Map<String, Integer> getAllHealthEffectGroupsByUser(String userId) {
			
		if(userId == null) {
            return null;
        }

		Map<String, Integer> heGroupNameMap = DSL.using(JooqUtil.getJooqConfiguration())
				.select(DSL.lower(ENDPOINT_GROUP.NAME), ENDPOINT_GROUP.ID)
				.from(ENDPOINT_GROUP)
				.where(ENDPOINT_GROUP.USER_ID.equal(userId).or(ENDPOINT_GROUP.SHARE_SCOPE.equal((short) 1)))
				.fetchMap(DSL.lower(ENDPOINT_GROUP.NAME), ENDPOINT_GROUP.ID);

		return heGroupNameMap;
	}

	
	public static Object postValuationFunctionData(Request request, Response response, Optional<UserProfile> userProfile) {
		request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));
		String healthEffectGroupName;
		boolean newCategory;
		String description;
		
		try{
			healthEffectGroupName = ApiUtil.getMultipartFormParameterAsString(request, "healthEffectGroupName");
			description = ApiUtil.getMultipartFormParameterAsString(request, "description");
			newCategory = ParameterUtil.getParameterValueAsBoolean(request.raw().getParameter("newCategory"), false);
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return CoreApi.getErrorResponseInvalidId(request, response);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return CoreApi.getErrorResponseInvalidId(request, response);
		}
		
		//Validate csv file
		String errorMsg= ""; //stores more detailed info. Not used in report for now but may need in the future?
		ValidationMessage validationMsg = new ValidationMessage();

				
		if(healthEffectGroupName == null) {
			response.type("application/json");
			//response.status(400);
			validationMsg.success=false;
			validationMsg.messages.add(new ValidationMessage.Message("error","Missing required parameter: name."));
			return CoreApi.transformValMsgToJSON(validationMsg);
		}

		String userId = userProfile.get().getId();
		
		EndpointGroupRecord heGroupRecord=null;
		ValuationFunctionRecord vfRecord=null;
		int endpointIdx=-999;
		int qualifierIdx=-999;
		int referenceIdx=-999;
		int startAgeIdx=-999;
		int endAgeIdx=-999;
		int functionIdx=-999;
		int distributionIdx=-999;
		int param1Idx=-999;
		int param2Idx=-999;
		int paramAIdx=-999;
		int paramANameIdx=-999;
		int paramBIdx=-999;
		int paramBNameIdx=-999;
		int paramCIdx=-999;
		int paramCNameIdx=-999;
		int paramDIdx=-999;
		int paramDNameIdx=-999;
		int epaStandardIdx=-999;
		int valuationTypeIdx=-999;
		int multiyearIdx=-999;
		int multiyearDrIdx=-999;
		int multiyearCostsIdx=-999;
		int accessUrlIdx=-999;

		List<Integer> newHealthEffectGroups = new ArrayList<Integer>();
		List<Integer> newHealthEffects = new ArrayList<Integer>();

		Map<String,Integer> endpointIdLookup = new HashMap<String,Integer>();

		int heGroupId = 0;
		Map<String, Integer> heGroupNameMap = getAllHealthEffectGroupsByUser(userId);

		if(heGroupNameMap.containsKey(healthEffectGroupName.toLowerCase())) {
			if(newCategory) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "A Health Effect Category called '" + healthEffectGroupName + "' already exists. "
					+ "Please enter a different name, or select the 'Append to an existing health effect category' option.";
				msg.message = strRecord + "";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}
			heGroupId = heGroupNameMap.get(healthEffectGroupName.toLowerCase());
		} else {
			heGroupRecord = DSL.using(JooqUtil.getJooqConfiguration())
				.insertInto(ENDPOINT_GROUP
						, ENDPOINT_GROUP.NAME
						, ENDPOINT_GROUP.USER_ID
						, ENDPOINT_GROUP.SHARE_SCOPE
						)
				.values(healthEffectGroupName, userId, Constants.SHARING_NONE)
				.returning(ENDPOINT_GROUP.ID)
				.fetchOne();

			heGroupId = heGroupRecord.value1();

			newHealthEffectGroups.add(heGroupId);
		}

		
		
		//remove built in tokens (e, beta)
		//these were causing function arguments to get parsed incorrectly
		mXparser.removeBuiltinTokens("e");
		mXparser.removeBuiltinTokens("Beta");

		try (InputStream is = request.raw().getPart("file").getInputStream()) {
			BOMInputStream bis = new BOMInputStream(is, false);
			CSVReader csvReader = new CSVReader (new InputStreamReader(bis));				
			NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
			String[] record;
			
			//step 1: verify column names 
			// Read the header
			// allow either "column" or "col"; "values" or "value"
			// todo: warn or abort when both "column" and "col" exist.
			record = csvReader.readNext();
			for(int i=0; i < record.length; i++) {
				switch(record[i].toLowerCase().replace(" ", "")) {
				case "healtheffect":
					endpointIdx=i;
					break;
				case "riskmodeldetails":
					qualifierIdx=i;
					break;
				case "reference":
					referenceIdx=i;
					break;
				case "startage":
					startAgeIdx=i;
					break;
				case "endage":
					endAgeIdx=i;
					break;
				case "function":
					functionIdx=i;
					break;		
				case "distribution":
					distributionIdx=i;
					break;	
				case "standarderror":
					param1Idx=i;
					break;
				case "param2a":
					param2Idx=i;
					break;
				case "a":
					paramAIdx=i;
					break;
				case "namea":
					paramANameIdx=i;
					break;
				case "b":
					paramBIdx=i;
					break;
				case "nameb":
					paramBNameIdx=i;
					break;
				case "c":
					paramCIdx=i;
					break;
				case "namec":
					paramCNameIdx=i;
					break;	
				case "d":
					paramDIdx=i;
					break;
				case "named":
					paramDNameIdx=i;
					break;
				case "valuationtype":
					valuationTypeIdx=i;
					break;	
				case "epastandard":
					epaStandardIdx=i;
					break;	
				case "multiyear":
					multiyearIdx=i;
					break;	
				case "multiyeardr":
					multiyearDrIdx=i;
					break;	
				case "multiyearcosts":
					multiyearCostsIdx=i;
					break;	
				case "accessurl":
					accessUrlIdx=i;
					break;			
				default:
					System.out.println(record[i].toLowerCase().replace(" ", ""));
				}
			}

			String tmp = ValuationUtil.validateModelColumnHeadings(endpointIdx, qualifierIdx, referenceIdx, startAgeIdx, endAgeIdx, functionIdx, param1Idx, param2Idx, paramAIdx, paramANameIdx, paramBIdx, paramBNameIdx, distributionIdx);

			if(tmp.length() > 0) {
				log.debug("end age index is :" + endAgeIdx);

				log.debug("valuation function dataset posted - columns are missing: " + tmp);
				DSL.using(JooqUtil.getJooqConfiguration()).deleteFrom(ENDPOINT_GROUP)
					.where(ENDPOINT_GROUP.ID.in(newHealthEffectGroups))
					.execute();
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				msg.message = "The following columns are missing: " + tmp;
				msg.type = "error";
				validationMsg.messages.add(msg);
				response.type("application/json");
				return CoreApi.transformValMsgToJSON(validationMsg);
			}
			
			endpointIdLookup = ValuationUtil.getEndpointIdLookup((short) heGroupId);
			
		// 	//We might also need to clean up the header. Or, maybe we should make this a transaction?
			
		// 	//step 2: make sure file has > 0 rows. Check rowCount after while loop.
			int rowCount = 0;
			int countStartAgeTypeError = 0;
			int countEndAgeTypeError = 0;
			int countAgeRangeError = 0;

			int countEpaStandardTypeError = 0;
			int countMultiyearTypeError = 0;

			int countMissingEndpoint = 0;
			int countMissingEndpointGroup = 0;

			int countDistributionError = 0;
			int countParam1Error = 0;
			int countParam2Error = 0;
			int countParamAError = 0;
			int countParamBError = 0;
			int countMultiyearCostsError = 0;
			int countMultiyearCostsTypeError = 0;

			int countFunctionParamError = 0;
			int countFunctionError = 0;

			List<String> lstUndefinedEndpoints = new ArrayList<String>();
			List<String> lstUndefinedEndpointGroups = new ArrayList<String>();

			Map<String, Integer> dicUniqueRecord = new HashMap<String,Integer>();	

			List<String> distTypes = new ArrayList<String>();
			distTypes.add("None");
			distTypes.add("Triangular");
			distTypes.add("Normal");
			distTypes.add("Weibull");
			distTypes.add("LogNormal");
			distTypes.add("Custom");
			distTypes.add("Uniform");
			distTypes.add("Gamma");

			List<String> functionParameters = new ArrayList<String>();
			functionParameters.add("a");
			functionParameters.add("b");
			functionParameters.add("c");
			functionParameters.add("d");
			functionParameters.add("allgoodsindex");
			functionParameters.add("medicalcostindex");
			functionParameters.add("wageindex");

			while ((record = csvReader.readNext()) != null) {				
				rowCount ++;
				//endpoint id hashmap is a nested dictionary with the outer key being endpoint groups and values being hashmaps of endpoint names to ids
				if (!endpointIdLookup.containsKey(record[endpointIdx].strip().toLowerCase())){
					EndpointRecord heRecord = DSL.using(JooqUtil.getJooqConfiguration())
							.insertInto(ENDPOINT
									, ENDPOINT.NAME
									, ENDPOINT.ENDPOINT_GROUP_ID
									)
							.values(record[endpointIdx].strip(), (short) heGroupId)
							.returning(ENDPOINT.ID)
							.fetchOne();

					int heId = heRecord.value1();
					
					endpointIdLookup.put(record[endpointIdx].strip().toLowerCase(), heId);

					newHealthEffects.add(heId);
					
				}

				//TODO: Update this validation code when we add lookup tables for timeframe, units, and/or distribution
				// Make sure this metric exists in the db. If not, update the corresponding error array to return useful error message
				String str = "";

				//start age is required and should be an integer
				str = record[startAgeIdx].strip();
				//question: or use Integer.parseInt(str)??
				if(str=="" || !str.matches("-?\\d+")) {
					countStartAgeTypeError++;
				}	

				//end age is required and should be an integer
				str = record[endAgeIdx].strip();
				//question: or use Integer.parseInt(str)??
				if(str=="" || !str.matches("-?\\d+")) {
					countEndAgeTypeError++;
				}	

				if(Integer.parseInt(record[startAgeIdx].strip()) > Integer.parseInt(record[endAgeIdx].strip())) {
					countAgeRangeError++;
				}

				//EPA standard is optional, should be true or false
				if(epaStandardIdx != -999) {
					str = record[epaStandardIdx].strip();
					if(str != null && !(str.toLowerCase().equals("true") || str.toLowerCase().equals("false"))) {
						countEpaStandardTypeError++;
					}
				}

				//Multiyear is optional, should be true or false
				if(multiyearIdx != -999) {
					str = record[multiyearIdx].strip();
					if(str != null && !(str.toLowerCase().equals("true") || str.toLowerCase().equals("false"))) {
						countMultiyearTypeError++;
					}	
				}

				//distribution should be a value in the distTypes list
				str = record[distributionIdx].strip();
				if(!distTypes.contains(str)) {
					countDistributionError++;
				}

				//param 1 beta should be a double
				str = record[param1Idx].strip();
				try {
					if(!str.equals("")){
						Number number = format.parse(str);
						double value = number.doubleValue();
					}
				} catch(NumberFormatException e){
					countParam1Error ++;
				}

				//param 2 beta should be a double
				str = record[param2Idx].strip();
				try {
					if(!str.equals("")){
						Number number = format.parse(str);
						double value = number.doubleValue();
					}
				} catch(NumberFormatException e){
					countParam2Error ++;
				}

				//param a should be a double
				str = record[paramAIdx].strip();
				try {
					if(!str.equals("")){
						Number number = format.parse(str);
						double value = number.doubleValue();
					}
				} catch(NumberFormatException e){
					countParamAError ++;
				}

				//param b should be a double
				str = record[paramBIdx].strip();
				try {
					if(!str.equals("")){
						Number number = format.parse(str);
						double value = number.doubleValue();
					}
				} catch(NumberFormatException e){
					countParamBError ++;
				}

				//multiyear costs is optional, should be a double and >= 0
				if(multiyearCostsIdx != -999) {
					str = record[multiyearCostsIdx].strip();
					try {
						if(str != null && !str.equals("")) {
							Number number = format.parse(str);
							float value = number.floatValue();
							if(value < 0) {
								countMultiyearCostsError++;
							}
						}
					} catch(NumberFormatException e){
						countMultiyearCostsTypeError ++;
					}
				}

				//function should be a valid formula
				str = record[functionIdx].strip().toLowerCase();
				Expression e = new Expression(str);

				String[] missingVars = e.getMissingUserDefinedArguments();

				for (String varName : missingVars) {
					if(!functionParameters.contains(varName)) {
						countFunctionParamError ++;
					}
					e.addArguments(new Argument(varName + " = 1"));
				}

				if(!e.checkSyntax()) {
					countFunctionError++;
				}
		
			}

			//summarize validation message

			if(countStartAgeTypeError>0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countStartAgeTypeError == 1) {
					strRecord = String.valueOf(countStartAgeTypeError) + " record has a Start Age value that is not a valid integer.";
				}
				else {
					strRecord = String.valueOf(countStartAgeTypeError) + " records have Start Age values that are not valid integers.";
				}
				msg.message = strRecord + "";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}

			if(countEndAgeTypeError>0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countEndAgeTypeError == 1) {
					strRecord = String.valueOf(countEndAgeTypeError) + " record has a End Age value that is not a valid integer.";
				}
				else {
					strRecord = String.valueOf(countEndAgeTypeError) + " records have End Age values that are not valid integers.";
				}
				msg.message = strRecord + "";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}

			if(countAgeRangeError>0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countAgeRangeError == 1) {
					strRecord = String.valueOf(countAgeRangeError) + " record has an invalid age range.";
				}
				else {
					strRecord = String.valueOf(countAgeRangeError) + " records have an invalid age range.";
				}
				msg.message = strRecord + "";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}
			

			if(countDistributionError > 0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countDistributionError == 1) {
					strRecord = String.valueOf(countDistributionError) + " record has an invalid Distribution value.";
				}
				else {
					strRecord = String.valueOf(countDistributionError) + " records have invalid Distribution values.";
				}
				msg.message = strRecord + "";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}

			if(countParam1Error > 0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countParam1Error == 1) {
					strRecord = String.valueOf(countParam1Error) + " record has a Standard Error value that is not a valid number.";
				}
				else {
					strRecord = String.valueOf(countParam1Error) + " records have Standard Error values that are not valid numbers.";
				}
				msg.message = strRecord + "";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}

			if(countParam2Error > 0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countParam2Error == 1) {
					strRecord = String.valueOf(countParam2Error) + " record has a Param 2 A value that is not a valid number.";
				}
				else {
					strRecord = String.valueOf(countParam2Error) + " records have Param 2 A values that are not valid numbers.";
				}
				msg.message = strRecord + "";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}

			if(countParamAError > 0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countParamAError == 1) {
					strRecord = String.valueOf(countParamAError) + " record has an A value that is not a valid number.";
				}
				else {
					strRecord = String.valueOf(countParamAError) + " records have A values that are not valid numbers.";
				}
				msg.message = strRecord + "";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}

			if(countParamBError > 0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countParamBError == 1) {
					strRecord = String.valueOf(countParamBError) + " record has a B value that is not a valid number.";
				}
				else {
					strRecord = String.valueOf(countParamBError) + " records have B values that are not valid numbers.";
				}
				msg.message = strRecord + "";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}

			if(countMissingEndpoint>0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countMissingEndpoint == 1) {
					strRecord = String.valueOf(countMissingEndpoint) + " record is missing a Health Effect value.";
				}
				else {
					strRecord = String.valueOf(countMissingEndpoint) + " records are missing Health Effect values.";
				}
				msg.message = strRecord + "";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}

			if(countMultiyearCostsTypeError > 0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countMultiyearCostsTypeError == 1) {
					strRecord = String.valueOf(countMultiyearCostsTypeError) + " record has an Multiyear Costs value that is not a valid number.";
				}
				else {
					strRecord = String.valueOf(countMultiyearCostsTypeError) + " records have Multiyear Costs values that are not valid numbers.";
				}
				msg.message = strRecord + "";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}

			if(countMultiyearCostsError > 0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countMultiyearCostsError == 1) {
					strRecord = String.valueOf(countMultiyearCostsError) + " record has an Multiyear Costs value that is less than 0.";
				}
				else {
					strRecord = String.valueOf(countMultiyearCostsError) + " records have Multiyear Costs values that are less than 0.";
				}
				msg.message = strRecord + "";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}

			if(countEpaStandardTypeError > 0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countEpaStandardTypeError == 1) {
					strRecord = String.valueOf(countEpaStandardTypeError) + " record has an invalid EPA Standard value.";
				}
				else {
					strRecord = String.valueOf(countEpaStandardTypeError) + " records have invalid EPA Standard values.";
				}
				msg.message = strRecord + "";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}

			if(countMultiyearTypeError > 0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countMultiyearTypeError == 1) {
					strRecord = String.valueOf(countMultiyearTypeError) + " record has an invalid Multiyear value.";
				}
				else {
					strRecord = String.valueOf(countMultiyearTypeError) + " records have invalid Multiyear values.";
				}
				msg.message = strRecord + "";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}

			if(lstUndefinedEndpoints.size()>0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				msg.message = "The following Health Effect values are not defined: " + String.join(",", lstUndefinedEndpoints) + ".";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}

			if(countMissingEndpointGroup>0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countMissingEndpointGroup == 1) {
					strRecord = String.valueOf(countMissingEndpointGroup) + " record is missing a Health Effect Category value.";
				}
				else {
					strRecord = String.valueOf(countMissingEndpointGroup) + " records are missing Health Effect Category values.";
				}
				msg.message = strRecord + "";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}

			if(lstUndefinedEndpointGroups.size()>0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				msg.message = "The following Health Effect Category values are not defined: " + String.join(",", lstUndefinedEndpointGroups) + ".";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}

			if(countFunctionParamError > 0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countFunctionParamError == 1) {
					strRecord = String.valueOf(countFunctionParamError) + " invalid function parameter detected. Valid parameters include: " + String.join(", ", functionParameters).toUpperCase() + ".";
				}
				else {
					strRecord = String.valueOf(countFunctionParamError) + " invalid function parameters detected. Valid parameters include: " + String.join(", ", functionParameters).toUpperCase() + ".";
				}
				msg.message = strRecord + "";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}

			if(countFunctionError > 0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countFunctionError == 1) {
					strRecord = String.valueOf(countFunctionError) + " record has a function value that is not a valid formula.";
				}
				else {
					strRecord = String.valueOf(countFunctionError) + " records have function values that are not valid formulas.";
				}
				msg.message = strRecord + "";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}
			
			//---End of csv validation
			
		} catch (Exception e) {
			log.error("Error validating valuation function upload", e);
			DSL.using(JooqUtil.getJooqConfiguration()).deleteFrom(ENDPOINT_GROUP)
					.where(ENDPOINT_GROUP.ID.in(newHealthEffectGroups))
					.execute();
			DSL.using(JooqUtil.getJooqConfiguration()).deleteFrom(ENDPOINT)
					.where(ENDPOINT.ID.in(newHealthEffects))
					.execute();
			response.type("application/json");
			//response.status(400);
			validationMsg.success=false;
			validationMsg.messages.add(new ValidationMessage.Message("error","Error occurred during validation of valuation functions."));
			return CoreApi.transformValMsgToJSON(validationMsg);
		}

		if(validationMsg.messages.size() > 0) {
			log.error("Error validating valuation function upload");
			DSL.using(JooqUtil.getJooqConfiguration()).deleteFrom(ENDPOINT_GROUP)
					.where(ENDPOINT_GROUP.ID.in(newHealthEffectGroups))
					.execute();
			DSL.using(JooqUtil.getJooqConfiguration()).deleteFrom(ENDPOINT)
					.where(ENDPOINT.ID.in(newHealthEffects))
					.execute();
			response.type("application/json");
			//response.status(400);
			validationMsg.success=false;
			validationMsg.messages.add(new ValidationMessage.Message("error","Error occurred during validation of valuation functions."));
			return CoreApi.transformValMsgToJSON(validationMsg);
		}
		
		Integer hifDatasetId = null;
		
		// //import data
		try (InputStream is = request.raw().getPart("file").getInputStream()){
			CSVReader csvReader = new CSVReader (new InputStreamReader(is));
			String[] record;
			NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
			record = csvReader.readNext();
			while ((record = csvReader.readNext()) != null) {		

				String endpointName = record[endpointIdx].strip().toLowerCase();
				
				int endpointId = endpointIdLookup.get(endpointName);

				short startAge = Short.valueOf(record[startAgeIdx].strip());
				short endAge = Short.valueOf(record[endAgeIdx].strip());

				boolean multiyearValue = false;
				if(multiyearIdx != -999) {
					String multiyear = record[multiyearIdx].strip();
					if(multiyear != null && !multiyear.equals("")) {
						multiyearValue = Boolean.valueOf(multiyear);
					}
				}

				boolean epaStandardValue = false;
				if(epaStandardIdx != -999) {
					String epaStandard = record[epaStandardIdx].strip();
					if(epaStandard != null && !epaStandard.equals("")) {
						epaStandardValue = Boolean.valueOf(epaStandard);
					}
				}	

				String accessUrl = null;
				if(accessUrlIdx != -999) {
					accessUrl = record[accessUrlIdx];
				}

				String valuationType = null;
				if(valuationTypeIdx != -999) {
					valuationType = record[valuationTypeIdx];
				}

				Double p1beta = 0.0;
				if(!record[param1Idx].strip().equals("")){
					Number number = format.parse(record[param1Idx].strip());
					p1beta = number.doubleValue();
				}

				Double p2beta = 0.0;
				if(!record[param2Idx].strip().equals("")){
					Number number = format.parse(record[param2Idx].strip());
					p2beta = number.doubleValue();
				}

				Double valA = 0.0;
				if(!record[paramAIdx].strip().equals("")){
					Number number = format.parse(record[paramAIdx].strip());
					valA = number.doubleValue();
				}

				Double valB = 0.0;
				if(!record[paramBIdx].strip().equals("")){
					Number number = format.parse(record[paramBIdx].strip());
					valB = number.doubleValue();
				}

				Double valC = 0.0;
				if(paramCIdx != -999 && !record[paramCIdx].strip().equals("")){
					Number number = format.parse(record[paramCIdx].strip());
					valC = number.doubleValue();
				}

				Double valD = 0.0;
				if(paramDIdx != -999 && !record[paramDIdx].strip().equals("")){
					Number number = format.parse(record[paramDIdx].strip());
					valD = number.doubleValue();
				}

				// Double multiyearCosts = 0.0;
				// if(record[multiyearCostsIdx] != null && !record[multiyearCostsIdx].equals("")){
				// 	multiyearCosts = Double.valueOf(record[multiyearCostsIdx]);
				// }

				Double multiyearDr = 0.0;
				if(multiyearDrIdx != -999 && !record[multiyearDrIdx].strip().equals("")){
					Number number = format.parse(record[multiyearDrIdx].strip());
					multiyearDr = number.doubleValue();
				}

				String functionText = record[functionIdx].strip();
				// Normalize known expressions
				functionText = functionText.replaceAll("(?i)\\bEXP\\s*\\(", "exp(");
				functionText = functionText.replaceAll("(?i)\\bMIN\\s*\\(", "min(");
				functionText = functionText.replaceAll("(?i)\\bMAX\\s*\\(", "max(");
				functionText = functionText.replaceAll("(?i)\\bLOG10\\s*\\(", "log10(");
				functionText = functionText.replaceAll("(?i)\\bLOG\\s*\\(", "log10(");


				//Create the hif record
				vfRecord = DSL.using(JooqUtil.getJooqConfiguration())
				.insertInto(VALUATION_FUNCTION
						, VALUATION_FUNCTION.VALUATION_DATASET_ID
						, VALUATION_FUNCTION.ENDPOINT_GROUP_ID
						, VALUATION_FUNCTION.ENDPOINT_ID
						, VALUATION_FUNCTION.QUALIFIER
						, VALUATION_FUNCTION.REFERENCE
						, VALUATION_FUNCTION.START_AGE
						, VALUATION_FUNCTION.END_AGE
						, VALUATION_FUNCTION.FUNCTION_TEXT
						, VALUATION_FUNCTION.DIST_A
						, VALUATION_FUNCTION.P1A
						, VALUATION_FUNCTION.P2A
						, VALUATION_FUNCTION.VAL_A
						, VALUATION_FUNCTION.NAME_A
						, VALUATION_FUNCTION.VAL_B
						, VALUATION_FUNCTION.NAME_B
						, VALUATION_FUNCTION.VAL_C
						, VALUATION_FUNCTION.NAME_C
						, VALUATION_FUNCTION.VAL_D
						, VALUATION_FUNCTION.NAME_D
						, VALUATION_FUNCTION.EPA_STANDARD
						, VALUATION_FUNCTION.ACCESS_URL
						, VALUATION_FUNCTION.VALUATION_TYPE
						, VALUATION_FUNCTION.MULTIYEAR
						, VALUATION_FUNCTION.MULTIYEAR_DR
						, VALUATION_FUNCTION.USER_ID
						, VALUATION_FUNCTION.SHARE_SCOPE
						)
				.values(1, heGroupId, endpointId, record[qualifierIdx], record[referenceIdx], startAge, endAge, 
				functionText, record[distributionIdx].strip(), p1beta, p2beta, valA, record[paramANameIdx], valB, 
				record[paramBNameIdx], valC, record[paramCNameIdx], valD, record[paramDNameIdx], epaStandardValue, accessUrl,
				valuationType, multiyearValue, multiyearDr, userId, Constants.SHARING_NONE)
				.returning(VALUATION_FUNCTION.ID)
				.fetchOne();

				int vfRecordId = vfRecord.getId();

			}
		
		} catch (Exception e) {
			log.error("Error importing valuation functions", e);
			DSL.using(JooqUtil.getJooqConfiguration()).deleteFrom(ENDPOINT_GROUP)
					.where(ENDPOINT_GROUP.ID.in(newHealthEffectGroups))
					.execute();
			DSL.using(JooqUtil.getJooqConfiguration()).deleteFrom(ENDPOINT)
					.where(ENDPOINT.ID.in(newHealthEffects))
					.execute();
			response.type("application/json");
			validationMsg.success=false;
			validationMsg.messages.add(new ValidationMessage.Message("error","Error occurred during import of valuation functions."));
			// deleteIncidenceDataset(incidenceDatasetId, userProfile);
			return CoreApi.transformValMsgToJSON(validationMsg);
		}
		
		response.type("application/json");
		validationMsg.success = true;
		return CoreApi.transformValMsgToJSON(validationMsg); 
	}



	/**
	 * 
	 * @param request
	 * @param response
	 * @param userProfile
	 * @return a JSON representation of all valuation result datasets.
	 */
	public static Object getValuationResultDatasets(Request request, Response response, Optional<UserProfile> userProfile) {
		Result<Record> valuationDatasetRecords = DSL.using(JooqUtil.getJooqConfiguration())
				.select(VALUATION_RESULT_DATASET.asterisk())
				.from(VALUATION_RESULT_DATASET)
				.orderBy(VALUATION_RESULT_DATASET.NAME)
				.fetch();
		
		response.type("application/json");
		return valuationDatasetRecords.formatJSON(new JSONFormat().header(false).recordFormat(RecordFormat.OBJECT));
	}

	public static Object archiveValuationFunction(Request request, Response response, Optional<UserProfile> userProfile) {
	
		ValidationMessage validationMsg = new ValidationMessage();
		Integer id;

		try {
			id = Integer.valueOf(request.params("id"));
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return CoreApi.getErrorResponseInvalidId(request, response);
		} 
		DSLContext create = DSL.using(JooqUtil.getJooqConfigurationUnquoted());
		
		ValuationFunctionRecord vfResult = create.selectFrom(VALUATION_FUNCTION).where(VALUATION_FUNCTION.ID.eq(id)).fetchAny();
		if(vfResult == null) {
			return CoreApi.getErrorResponseNotFound(request, response);
		}

		//Nobody can archive shared VFs
		//All users can archive their own VFs
		//Admins can archive any non-shared VFs
		if(vfResult.getShareScope() == Constants.SHARING_ALL || !(vfResult.getUserId().equalsIgnoreCase(userProfile.get().getId()) || CoreApi.isAdmin(userProfile)) )  {
			return CoreApi.getErrorResponseForbidden(request, response);
		}

		vfResult.setArchived((short) 1);

		vfResult.store();

		response.type("application/json");
		validationMsg.success = true;
		return CoreApi.transformValMsgToJSON(validationMsg); 
	}
	
	/**
	 * 
	 * @param request
	 * @param response
	 * @param userProfile
	 * @return a JSON representation of valuation functions from a given valuation result dataset.
	 */
	public static Object getValuationResultDatasetFunctions(Request request, Response response, Optional<UserProfile> userProfile) {
		
		String idParam;
		Integer id;
		try {
			idParam = String.valueOf(request.params("id"));

			//If the id is 36 characters long, we'll assume it's a task uuid
			id = idParam.length() == 36 ? ValuationApi.getValuationResultDatasetId(idParam) : Integer.valueOf(idParam);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return CoreApi.getErrorResponseInvalidId(request, response);	
		} catch (Exception e) {
			e.printStackTrace();
			return CoreApi.getErrorResponseInvalidId(request, response);
		}
		
		Result<Record> hifRecords = DSL.using(JooqUtil.getJooqConfiguration())
				.select(VALUATION_FUNCTION.asterisk()
						, DSL.jsonObject(
								HEALTH_IMPACT_FUNCTION.ID
								, HEALTH_IMPACT_FUNCTION.AUTHOR
								, HEALTH_IMPACT_FUNCTION.FUNCTION_YEAR
								, HEALTH_IMPACT_FUNCTION.START_AGE
								, HEALTH_IMPACT_FUNCTION.END_AGE
								, HEALTH_IMPACT_FUNCTION.QUALIFIER
								, HEALTH_IMPACT_FUNCTION.LOCATION
								, HEALTH_IMPACT_FUNCTION.REFERENCE
								)
						.as("hif")
						, ENDPOINT_GROUP.NAME.as("endpoint_group_name")
						, ENDPOINT.NAME.as("endpoint_name")
						)
				.from(VALUATION_FUNCTION)
				.join(ENDPOINT_GROUP).on(VALUATION_FUNCTION.ENDPOINT_GROUP_ID.eq(ENDPOINT_GROUP.ID))
				.join(ENDPOINT).on(VALUATION_FUNCTION.ENDPOINT_ID.eq(ENDPOINT.ID))
				.join(VALUATION_RESULT_FUNCTION_CONFIG).on(VALUATION_RESULT_FUNCTION_CONFIG.VF_ID.eq(VALUATION_FUNCTION.ID))
				.join(HEALTH_IMPACT_FUNCTION).on(HEALTH_IMPACT_FUNCTION.ID.eq(VALUATION_RESULT_FUNCTION_CONFIG.HIF_ID))
				.where(VALUATION_RESULT_FUNCTION_CONFIG.VALUATION_RESULT_DATASET_ID.eq(id))
				.orderBy(ENDPOINT_GROUP.NAME, ENDPOINT.NAME, VALUATION_FUNCTION.REFERENCE)
				.fetch();
		
		response.type("application/json");
		return hifRecords.formatJSON(new JSONFormat().header(false).recordFormat(RecordFormat.OBJECT));
	}

	/**
	 * 
	 * @param valuationResultDatasetId
	 * @return a baseline grid definition id from a given valuation result dataset.
	 */
	public static Integer getBaselineGridForValuationResults(int valuationResultDatasetId) {

		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());
		
		Record1<Integer> gridId = create
				.select(VALUATION_RESULT_DATASET.GRID_DEFINITION_ID)
				.from(VALUATION_RESULT_DATASET)
				.where(VALUATION_RESULT_DATASET.ID.eq(valuationResultDatasetId))
				.fetchOne();

		if(gridId == null) {
			return null;
		}
		return gridId.value1();
	}

	/**
	 * 
	 * @param request
	 * @param response
	 * @param userProfile
	 * @return JSON representation of all health effect groups
	 */
	public static Object getAllHealthEffectGroups(Request request, Response response, Optional<UserProfile> userProfile) {

		String userId = userProfile.get().getId();

		Boolean getAll = ParameterUtil.getParameterValueAsBoolean(request.raw().getParameter("getAll"), false);

		Condition filterCondition = DSL.trueCondition();

		filterCondition = filterCondition.and(ENDPOINT_GROUP.SHARE_SCOPE.eq(Constants.SHARING_ALL).or(ENDPOINT_GROUP.USER_ID.eq(userId)));		

		Result<Record> healthEffectGroupRecords;
		
		if(getAll) {
			healthEffectGroupRecords = DSL.using(JooqUtil.getJooqConfiguration())
				.selectDistinct(ENDPOINT_GROUP.fields())
				.from(ENDPOINT_GROUP)
				.where(filterCondition)
				.orderBy(ENDPOINT_GROUP.NAME.asc())
				.fetch();
		} else {
			//limit to endpoint groups that are associated with and existing valuation function
			healthEffectGroupRecords = DSL.using(JooqUtil.getJooqConfiguration())
				.selectDistinct(ENDPOINT_GROUP.fields())
				.from(ENDPOINT_GROUP)
				.join(VALUATION_FUNCTION).on(ENDPOINT_GROUP.ID.eq(VALUATION_FUNCTION.ENDPOINT_GROUP_ID))
				.where(filterCondition)
				.orderBy(ENDPOINT_GROUP.NAME.asc())
				.fetch();
		}
		
		response.type("application/json");
		return healthEffectGroupRecords.formatJSON(new JSONFormat().header(false).recordFormat(RecordFormat.OBJECT));
	}

	/**
	 * Sets the sort order of the valuation functions.
	 * @param sortBy
	 * @param descending
	 * @param orderFields
	 */
	private static void setValuationFunctionSortOrder(
			String sortBy, Boolean descending, List<OrderField<?>> orderFields) {
		
		if (!"".equals(sortBy)) {
			
			SortOrder sortDirection = SortOrder.ASC;
			Field<?> sortField = null;
			
			sortDirection = descending ? SortOrder.DESC : SortOrder.ASC;
			
			switch (sortBy) {
			case "name":
				sortField = DSL.field(sortBy, String.class.getName());
				break;

			case "grid_definition_name":
				sortField = DSL.field(sortBy, Integer.class.getName());
				break;

			case "cell_count":
				sortField = DSL.field(sortBy, Integer.class.getName());
				break;

			case "mean_value":
				sortField = DSL.field(sortBy, Double.class.getName());
				break;

			default:
				sortField = DSL.field(sortBy, String.class.getName());
				break;
			}
			
			orderFields.add(sortField.sort(sortDirection));
			
		} else {
			orderFields.add(DSL.field("name", String.class.getName()).sort(SortOrder.ASC));	
		}
	}

	/**
	 * 
	 * @param filterValue
	 * @return a condition object representing a valuation function filter condition.
	 */
	private static Condition buildValuationFunctionFilterCondition(String filterValue) {

		Condition filterCondition = DSL.trueCondition();
		Condition searchCondition = DSL.falseCondition();

		Integer filterValueAsInteger = DataConversionUtil.getFilterValueAsInteger(filterValue);
		Long filterValueAsLong = DataConversionUtil.getFilterValueAsLong(filterValue);
		Double filterValueAsDouble = DataConversionUtil.getFilterValueAsDouble(filterValue);
		Date filterValueAsDate = DataConversionUtil.getFilterValueAsDate(filterValue, "MM/dd/yyyy");
		
		searchCondition = 
				searchCondition.or(ENDPOINT_GROUP.NAME
						.containsIgnoreCase(filterValue));

		searchCondition = 
				searchCondition.or(ENDPOINT.NAME
						.containsIgnoreCase(filterValue));
		
		searchCondition = 
				searchCondition.or(VALUATION_FUNCTION.QUALIFIER
						.containsIgnoreCase(filterValue));

		searchCondition = 
				searchCondition.or(VALUATION_FUNCTION.REFERENCE
						.containsIgnoreCase(filterValue));
		
		searchCondition = 
				searchCondition.or(VALUATION_FUNCTION.NAME_A
						.containsIgnoreCase(filterValue));

		searchCondition = 
				searchCondition.or(VALUATION_FUNCTION.NAME_B
						.containsIgnoreCase(filterValue));				

		searchCondition = 
				searchCondition.or(VALUATION_FUNCTION.DIST_A
						.containsIgnoreCase(filterValue));		


		if (null != filterValueAsInteger) {
			searchCondition = 
					searchCondition.or(VALUATION_FUNCTION.ID
							.eq(filterValueAsInteger));
		}

		if (null != filterValueAsInteger) {
			searchCondition = 
					searchCondition.or(VALUATION_FUNCTION.START_AGE
							.eq(filterValueAsInteger));
		}

		if (null != filterValueAsInteger) {
			searchCondition = 
					searchCondition.or(VALUATION_FUNCTION.END_AGE
							.eq(filterValueAsInteger));		
		}
		
		filterCondition = filterCondition.and(searchCondition);

		return filterCondition;
	}

}
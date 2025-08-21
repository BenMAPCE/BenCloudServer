package gov.epa.bencloud.api;

import static gov.epa.bencloud.server.database.jooq.data.Tables.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.MultipartConfigElement;

import java.util.stream.Collectors;

import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.InsertValuesStep5;
import org.jooq.JSONFormat;
import org.jooq.Result;
import org.jooq.SortOrder;
import org.jooq.Table;
import org.jooq.JSONFormat.RecordFormat;
import org.jooq.OrderField;
import org.jooq.exception.DataAccessException;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Record2;
import org.jooq.Record4;
import org.jooq.Record7;
import org.jooq.impl.DSL;
import org.pac4j.core.profile.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

import gov.epa.bencloud.Constants;
import gov.epa.bencloud.api.model.HIFTaskConfig;
import gov.epa.bencloud.api.model.HIFTaskLog;
import gov.epa.bencloud.api.model.ValidationMessage;
import gov.epa.bencloud.api.util.AirQualityUtil;
import gov.epa.bencloud.api.util.ApiUtil;
import gov.epa.bencloud.api.util.HIFUtil;
import gov.epa.bencloud.api.util.IncidenceUtil;
import gov.epa.bencloud.server.database.JooqUtil;
import gov.epa.bencloud.server.database.jooq.data.tables.IncidenceDataset;
import gov.epa.bencloud.server.database.jooq.data.tables.records.GetHifResultsRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.GridDefinitionRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.HifResultDatasetRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.HealthImpactFunctionGroupRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.HealthImpactFunctionRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.IncidenceValueRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.TaskCompleteRecord;
import gov.epa.bencloud.server.util.ApplicationUtil;
import gov.epa.bencloud.server.util.DataConversionUtil;
import gov.epa.bencloud.server.util.ParameterUtil;
import spark.Request;
import spark.Response;

/*
 * 
 */
public class HIFApi {
	private static final Logger log = LoggerFactory.getLogger(HIFApi.class);

	/**
	 * Gets the hif results and stores them in the response parameters.
	 * Request contains the hif result dataset id.
	 * @param request
	 * @param response
	 * @param userProfile
	 */
	public static void getHifResultContents(Request request, Response response, Optional<UserProfile> userProfile) {
		
		 //*  :id (health impact function results dataset id (can also support task id))
		 //*  gridId= (aggregate the results to another grid definition)
		 //*  hifId= (filter results to those from one or more functions via comma delimited list)
		 //*  page=
		 //*  rowsPerPage=
		 //*  sortBy=
		 //*  descending=
		 //*  filter=
		
		// TODO: Add user security enforcement
		//TODO: Implement sortBy, descending, and filter

		String idParam;
		Integer id;
		try {
			idParam = String.valueOf(request.params("id"));

			//If the id is 36 characters long, we'll assume it's a task uuid
			id = idParam.length() == 36 ? HIFApi.getHIFResultDatasetId(idParam) : Integer.valueOf(idParam);
		} catch (NumberFormatException e) {
			e.printStackTrace();
			CoreApi.getErrorResponseInvalidId(request, response);
			return;
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			CoreApi.getErrorResponseInvalidId(request, response);
			return;
		}
			
		String hifIdsParam;
		int gridId;
		int page;
		int rowsPerPage;
		String sortBy;
		boolean descending;
		String filter;

		try {
			hifIdsParam = ParameterUtil.getParameterValueAsString(request.raw().getParameter("hifId"), "");
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
		
		
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());

		//If the crosswalk isn't there, create it now
		CrosswalksApi.ensureCrosswalkExists(HIFApi.getBaselineGridForHifResults(id), gridId);

		Table<GetHifResultsRecord> hifResultRecords = create.selectFrom(
				GET_HIF_RESULTS(
						id, 
						hifIds == null ? null : hifIds.toArray(new Integer[0]), 
						gridId))
				.asTable("hif_result_records");

		try{
			var inc = IncidenceDataset.INCIDENCE_DATASET.as("inc");
			var pre = IncidenceDataset.INCIDENCE_DATASET.as("pre");

			Result<Record> hifRecords = create.select(
				hifResultRecords.field(GET_HIF_RESULTS.GRID_COL).as("column"),
				hifResultRecords.field(GET_HIF_RESULTS.GRID_ROW).as("row"),
				ENDPOINT.NAME.as("endpoint"),
				HEALTH_IMPACT_FUNCTION.AUTHOR,
				HEALTH_IMPACT_FUNCTION.FUNCTION_YEAR.as("year"),
				HEALTH_IMPACT_FUNCTION.LOCATION,
				HEALTH_IMPACT_FUNCTION.QUALIFIER,
				HEALTH_IMPACT_FUNCTION.BETA,
				HIF_RESULT_FUNCTION_CONFIG.START_AGE,
				HIF_RESULT_FUNCTION_CONFIG.END_AGE,
				DSL.coalesce(inc.NAME, pre.NAME, DSL.val("")).as("incidence_prevalence"),
				RACE.NAME.as("race"),
				ETHNICITY.NAME.as("ethnicity"),
				GENDER.NAME.as("gender"),
				POLLUTANT_METRIC.NAME.as("metric"),
				SEASONAL_METRIC.NAME.as("seasonal_metric"),
				STATISTIC_TYPE.NAME.as("metric_statistic"),
				hifResultRecords.field(GET_HIF_RESULTS.POINT_ESTIMATE),
				hifResultRecords.field(GET_HIF_RESULTS.POPULATION),
				hifResultRecords.field(GET_HIF_RESULTS.DELTA_AQ),
				hifResultRecords.field(GET_HIF_RESULTS.BASELINE_AQ),
				hifResultRecords.field(GET_HIF_RESULTS.SCENARIO_AQ),
				hifResultRecords.field(GET_HIF_RESULTS.INCIDENCE),
				hifResultRecords.field(GET_HIF_RESULTS.MEAN),
				hifResultRecords.field(GET_HIF_RESULTS.BASELINE),
				DSL.when(hifResultRecords.field(GET_HIF_RESULTS.BASELINE).eq(0.0), 0.0)
					.otherwise(hifResultRecords.field(GET_HIF_RESULTS.MEAN).div(hifResultRecords.field(GET_HIF_RESULTS.BASELINE)).times(100.0)).as("percent_of_baseline"),
				hifResultRecords.field(GET_HIF_RESULTS.STANDARD_DEV).as("standard_deviation"),
				hifResultRecords.field(GET_HIF_RESULTS.VARIANCE).as("variance"),
				hifResultRecords.field(GET_HIF_RESULTS.PCT_2_5),
				hifResultRecords.field(GET_HIF_RESULTS.PCT_97_5),
				hifResultRecords.field(GET_HIF_RESULTS.PERCENTILES),
				DSL.val(null, String.class).as("formatted_results_2sf"),
				DSL.val(null, String.class).as("formatted_results_3sf")
				)
				.from(hifResultRecords)
				.join(HEALTH_IMPACT_FUNCTION).on(hifResultRecords.field(GET_HIF_RESULTS.HIF_ID).eq(HEALTH_IMPACT_FUNCTION.ID))
				.join(HIF_RESULT_FUNCTION_CONFIG).on(HIF_RESULT_FUNCTION_CONFIG.HIF_RESULT_DATASET_ID.eq(id)
						.and(HIF_RESULT_FUNCTION_CONFIG.HIF_ID.eq(hifResultRecords.field(GET_HIF_RESULTS.HIF_ID)))
						.and(HIF_RESULT_FUNCTION_CONFIG.HIF_INSTANCE_ID.eq(hifResultRecords.field(GET_HIF_RESULTS.HIF_INSTANCE_ID))))
				.join(ENDPOINT).on(ENDPOINT.ID.eq(HEALTH_IMPACT_FUNCTION.ENDPOINT_ID))
				.join(RACE).on(HIF_RESULT_FUNCTION_CONFIG.RACE_ID.eq(RACE.ID))
				.join(ETHNICITY).on(HIF_RESULT_FUNCTION_CONFIG.ETHNICITY_ID.eq(ETHNICITY.ID))
				.join(GENDER).on(HIF_RESULT_FUNCTION_CONFIG.GENDER_ID.eq(GENDER.ID))
				.join(POLLUTANT_METRIC).on(HIF_RESULT_FUNCTION_CONFIG.METRIC_ID.eq(POLLUTANT_METRIC.ID))
				.leftJoin(inc).on(HIF_RESULT_FUNCTION_CONFIG.INCIDENCE_DATASET_ID.eq(inc.ID))
				.leftJoin(pre).on(HIF_RESULT_FUNCTION_CONFIG.PREVALENCE_DATASET_ID.eq((pre.ID)))
				.leftJoin(SEASONAL_METRIC).on(HIF_RESULT_FUNCTION_CONFIG.SEASONAL_METRIC_ID.eq(SEASONAL_METRIC.ID))
				.join(STATISTIC_TYPE).on(HIF_RESULT_FUNCTION_CONFIG.METRIC_STATISTIC.eq(STATISTIC_TYPE.ID))
				.offset((page * rowsPerPage) - rowsPerPage)
				.limit(rowsPerPage)
				//.fetchSize(100000) //JOOQ doesn't like this when Postgres is in autoCommmit mode
				.fetch();
			
			
			if(hifRecords.isEmpty()) {
				CoreApi.getErrorResponseNotFound(request, response);
				return;
			}

			//If results are geing aggregated, recalc mean, variance, std deviation, and percent of baseline
			if(HIFApi.getBaselineGridForHifResults(id) != gridId) {
				for(Record res : hifRecords) {
					DescriptiveStatistics stats = new DescriptiveStatistics();
					Double[] pct = res.getValue(GET_HIF_RESULTS.PERCENTILES);
					for (int i = 0; i < pct.length; i++) {
						stats.addValue(pct[i]);
					}
					
					res.setValue(GET_HIF_RESULTS.MEAN, stats.getMean());
					res.setValue(GET_HIF_RESULTS.VARIANCE, stats.getVariance());
					res.setValue(DSL.field("standard_deviation", Double.class), stats.getStandardDeviation());
					res.setValue(DSL.field("percent_of_baseline", Double.class), stats.getMean() / res.getValue(GET_HIF_RESULTS.BASELINE) * 100.0);
				}
				
			}

			for (Record res : hifRecords) {
				res.setValue(DSL.field("formatted_results_2sf", String.class), 
								ApiUtil.createFormattedResultsString(res.get("point_estimate", Double.class), res.get("pct_2_5", Double.class), res.get("pct_97_5", Double.class), 2));
				res.setValue(DSL.field("formatted_results_3sf", String.class), 
								ApiUtil.createFormattedResultsString(res.get("point_estimate", Double.class), res.get("pct_2_5", Double.class), res.get("pct_97_5", Double.class), 3));
			}
		
			//TODO: Can we remove percentiles?
			response.type("application/json");
			hifRecords.formatJSON(response.raw().getWriter(),
					new JSONFormat().header(false).recordFormat(RecordFormat.OBJECT));
			
		} catch(DataAccessException e) {
			e.printStackTrace();
			response.status(400);
			return;
		} catch (Exception e) {
			log.error("Error formatting JSON", e);
			response.status(400);
			return;
		}
	}
	
	/**
	 * Exports hif results to a zip file.
	 * @param request
	 * @param response
	 * @param userProfile
	 */
	public static void getHifResultExport(Request request, Response response, Optional<UserProfile> userProfile) {
		 //*  :id (health impact function results dataset id (can also support task id))
		 //*  gridId= (aggregate the results to one or more grid definitions)
		 
		// TODO: Add user security enforcement
		//TODO: Implement sortBy, descending, and filter
		//TODO: I have (temporarily?) removed the paging and limit functionality. We will always give back all the rows.

		String idParam;
		Integer id;
		String gridIdParam;
		try {
			idParam = String.valueOf(request.params("id"));
			//If the id is 36 characters long, we'll assume it's a task uuid
			id = idParam.length() == 36 ? HIFApi.getHIFResultDatasetId(idParam) : Integer.valueOf(idParam);
			gridIdParam = ParameterUtil.getParameterValueAsString(request.raw().getParameter("gridId"), "");

		} catch (NumberFormatException e) {
			e.printStackTrace();
			CoreApi.getErrorResponseInvalidId(request, response);
			return;
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			CoreApi.getErrorResponseInvalidId(request, response);
			return;
		}
		
		int[] gridIds = (gridIdParam==null || gridIdParam.equals("")) ? null : Arrays.stream(gridIdParam.split(","))
			    .mapToInt(Integer::parseInt)
			    .toArray();


		// If a gridId wasn't provided, look up the baseline AQ grid grid for this resultset
		
		try {
			if(gridIds == null) {
				gridIds = new int[] {HIFApi.getBaselineGridForHifResults(id).intValue()};
			}
		} catch (NullPointerException e) {
			e.printStackTrace();
			response.status(400);
			return;
		}
		

		String taskFileName = ApplicationUtil.replaceNonValidCharacters(HIFApi.getHifTaskConfigFromDb(id).name);
		response.header("Content-Disposition", "attachment; filename=" + taskFileName + ".zip");
		response.header("Access-Control-Expose-Headers", "Content-Disposition");
		response.type("application/zip");

		// Get response output stream
		OutputStream responseOutputStream;
		ZipOutputStream zipStream;

		try {
			responseOutputStream = response.raw().getOutputStream();
			
			// Stream .ZIP file to response
			zipStream = new ZipOutputStream(responseOutputStream);
		} catch (java.io.IOException e1) {
			log.error("Error getting output stream", e1);
			return;
		}
		
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());

		Integer baselineGridId = HIFApi.getBaselineGridForHifResults(id);

		for(int i=0; i < gridIds.length; i++) {
			Result<?> hifRecordsClean = null;
			try {
				//If the crosswalk isn't there, create it now
				CrosswalksApi.ensureCrosswalkExists(baselineGridId, gridIds[i]);

				Table<GetHifResultsRecord> hifResultRecords = create.selectFrom(
					GET_HIF_RESULTS(
							id, 
							null, 
							gridIds[i]))
					.asTable("hif_result_records");
				log.info("Before fetch");
				Result<Record> hifRecords = create.select(
						hifResultRecords.field(GET_HIF_RESULTS.GRID_COL).as("column"),
						hifResultRecords.field(GET_HIF_RESULTS.GRID_ROW).as("row"),
						ENDPOINT.NAME.as("endpoint"),
						HEALTH_IMPACT_FUNCTION.AUTHOR,
						HEALTH_IMPACT_FUNCTION.FUNCTION_YEAR.as("year"),
						HEALTH_IMPACT_FUNCTION.LOCATION,
						HEALTH_IMPACT_FUNCTION.QUALIFIER,
						HIF_RESULT_FUNCTION_CONFIG.START_AGE,
						HIF_RESULT_FUNCTION_CONFIG.END_AGE,
						HEALTH_IMPACT_FUNCTION.BETA,
						RACE.NAME.as("race"),
						ETHNICITY.NAME.as("ethnicity"),
						GENDER.NAME.as("gender"),
						POLLUTANT_METRIC.NAME.as("metric"),
						SEASONAL_METRIC.NAME.as("seasonal_metric"),
						STATISTIC_TYPE.NAME.as("metric_statistic"),
						hifResultRecords.field(GET_HIF_RESULTS.POINT_ESTIMATE),
						hifResultRecords.field(GET_HIF_RESULTS.POPULATION),
						hifResultRecords.field(GET_HIF_RESULTS.DELTA_AQ),
						hifResultRecords.field(GET_HIF_RESULTS.BASELINE_AQ),
						hifResultRecords.field(GET_HIF_RESULTS.SCENARIO_AQ),
						//hifResultRecords.field(GET_HIF_RESULTS.INCIDENCE),
						hifResultRecords.field(GET_HIF_RESULTS.MEAN),
						hifResultRecords.field(GET_HIF_RESULTS.BASELINE),
						DSL.when(hifResultRecords.field(GET_HIF_RESULTS.BASELINE).eq(0.0), 0.0)
							.otherwise(hifResultRecords.field(GET_HIF_RESULTS.MEAN).div(hifResultRecords.field(GET_HIF_RESULTS.BASELINE)).times(100.0)).as("percent_of_baseline"),
						hifResultRecords.field(GET_HIF_RESULTS.STANDARD_DEV).as("standard_deviation"),
						hifResultRecords.field(GET_HIF_RESULTS.VARIANCE).as("variance"),
						hifResultRecords.field(GET_HIF_RESULTS.PCT_2_5),
						hifResultRecords.field(GET_HIF_RESULTS.PCT_97_5),
						HIFApi.getBaselineGridForHifResults(id) == gridIds[i] ? null : hifResultRecords.field(GET_HIF_RESULTS.PERCENTILES), //Only include percentiles if we're aggregating
						DSL.val(null, String.class).as("formatted_results_2sf"),
						DSL.val(null, String.class).as("formatted_results_3sf")
						)
						.from(hifResultRecords)
						.join(HEALTH_IMPACT_FUNCTION).on(hifResultRecords.field(GET_HIF_RESULTS.HIF_ID).eq(HEALTH_IMPACT_FUNCTION.ID))
						.join(HIF_RESULT_FUNCTION_CONFIG).on(HIF_RESULT_FUNCTION_CONFIG.HIF_RESULT_DATASET_ID.eq(id)
								.and(HIF_RESULT_FUNCTION_CONFIG.HIF_ID.eq(hifResultRecords.field(GET_HIF_RESULTS.HIF_ID)))
								.and(HIF_RESULT_FUNCTION_CONFIG.HIF_INSTANCE_ID.eq(hifResultRecords.field(GET_HIF_RESULTS.HIF_INSTANCE_ID))))
						.join(ENDPOINT).on(ENDPOINT.ID.eq(HEALTH_IMPACT_FUNCTION.ENDPOINT_ID))
						.join(RACE).on(HIF_RESULT_FUNCTION_CONFIG.RACE_ID.eq(RACE.ID))
						.join(ETHNICITY).on(HIF_RESULT_FUNCTION_CONFIG.ETHNICITY_ID.eq(ETHNICITY.ID))
						.join(GENDER).on(HIF_RESULT_FUNCTION_CONFIG.GENDER_ID.eq(GENDER.ID))
						.join(POLLUTANT_METRIC).on(HIF_RESULT_FUNCTION_CONFIG.METRIC_ID.eq(POLLUTANT_METRIC.ID))
						.leftJoin(SEASONAL_METRIC).on(HIF_RESULT_FUNCTION_CONFIG.SEASONAL_METRIC_ID.eq(SEASONAL_METRIC.ID))
						.join(STATISTIC_TYPE).on(HIF_RESULT_FUNCTION_CONFIG.METRIC_STATISTIC.eq(STATISTIC_TYPE.ID))
						.fetch();
				log.info("After fetch");
				
				//If results are being aggregated, recalc mean, variance, std deviation, and percent of baseline
				if(HIFApi.getBaselineGridForHifResults(id) != gridIds[i]) {
					for(Record res : hifRecords) {
						DescriptiveStatistics stats = new DescriptiveStatistics();
						Double[] pct = res.getValue(GET_HIF_RESULTS.PERCENTILES);
						for (int j = 0; j < pct.length; j++) {
							stats.addValue(pct[j]);
						}
						
						res.setValue(GET_HIF_RESULTS.MEAN, stats.getMean());
						
						//Add point estimate to the list before calculating variance and standard deviation to match approach of desktop
						stats.addValue(res.getValue(GET_HIF_RESULTS.POINT_ESTIMATE));
						res.setValue(GET_HIF_RESULTS.VARIANCE, stats.getVariance());
						res.setValue(DSL.field("standard_deviation", Double.class), stats.getStandardDeviation());
						
						res.setValue(DSL.field("percent_of_baseline", Double.class), stats.getMean() / res.getValue(GET_HIF_RESULTS.BASELINE) * 100.0);
					}
				}

				for (Record res : hifRecords) {
					res.setValue(DSL.field("formatted_results_2sf", String.class), 
									ApiUtil.createFormattedResultsString(res.get("point_estimate", Double.class), res.get("pct_2_5", Double.class), res.get("pct_97_5", Double.class), 2));
					res.setValue(DSL.field("formatted_results_3sf", String.class), 
									ApiUtil.createFormattedResultsString(res.get("point_estimate", Double.class), res.get("pct_2_5", Double.class), res.get("pct_97_5", Double.class), 3));
				}

				//Remove percentiles by keeping all other fields
				hifRecordsClean = hifRecords.into(hifRecords.fields(0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,29,30));
			} catch(DataAccessException e) {
				e.printStackTrace();
				response.status(400);
				return;
			}
			
			try {
				
					zipStream.putNextEntry(new ZipEntry(taskFileName + "_" + ApplicationUtil.replaceNonValidCharacters(GridDefinitionApi.getGridDefinitionName(gridIds[i])) + ".csv"));
					log.info("Before formatCSV");
					hifRecordsClean.formatCSV(zipStream);
					log.info("After formatCSV");
					zipStream.closeEntry();
					
			} catch (Exception e) {
				log.error("Error creating export file", e);
			} finally {

			}
		}
		
		try {
			zipStream.putNextEntry(new ZipEntry(taskFileName + "_TaskLog.txt"));
			HIFTaskLog hifTaskLog = HIFUtil.getTaskLog(id);
			zipStream.write(hifTaskLog.toString(userProfile).getBytes());
			zipStream.closeEntry();
			
			zipStream.close();
			responseOutputStream.flush();
		} catch (Exception e) {
			log.error("Error writing task log, closing and flushing export", e);
		}

		
	}

	/**
	 * Returns hif results needed for valuation purposes.
	 * @param id
	 * @param hifId
	 * @param incidenceAggregationGrid 
	 * @return hif results for a given hifId.
	 */
	public static Result<Record7<Long, Integer, Integer, Integer, Integer, Double, Double[]>> getHifResultsForValuation(Integer id, Integer hifId, Integer incidenceAggregationGrid) {
		
		Integer[] hifIds = new Integer[1];
		hifIds[0] = hifId;

		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());
		
		//If the crosswalk isn't there, create it now
		CrosswalksApi.ensureCrosswalkExists(HIFApi.getBaselineGridForHifResults(id), incidenceAggregationGrid);

		Table<GetHifResultsRecord> hifResultRecords = create.selectFrom(
				GET_HIF_RESULTS(
						id, 
						hifIds, 
						incidenceAggregationGrid))
				.asTable("hif_result_records");
		

		Result<Record7<Long, Integer, Integer, Integer, Integer, Double, Double[]>> hifRecords = create.select(
				DSL.val(0L).as("grid_cell_id"),
				hifResultRecords.field(GET_HIF_RESULTS.GRID_COL),
				hifResultRecords.field(GET_HIF_RESULTS.GRID_ROW),
				hifResultRecords.field(GET_HIF_RESULTS.HIF_ID),
				HEALTH_IMPACT_FUNCTION.ENDPOINT_GROUP_ID,
				hifResultRecords.field(GET_HIF_RESULTS.POINT_ESTIMATE),
				hifResultRecords.field(GET_HIF_RESULTS.PERCENTILES)
				)
				.from(hifResultRecords)
				.join(HIF_RESULT_FUNCTION_CONFIG).on(HIF_RESULT_FUNCTION_CONFIG.HIF_RESULT_DATASET_ID.eq(id)
						.and(HIF_RESULT_FUNCTION_CONFIG.HIF_ID.eq(hifResultRecords.field(GET_HIF_RESULTS.HIF_ID)))
						.and(HIF_RESULT_FUNCTION_CONFIG.HIF_INSTANCE_ID.eq(hifResultRecords.field(GET_HIF_RESULTS.HIF_INSTANCE_ID))))
				.join(HEALTH_IMPACT_FUNCTION).on(HEALTH_IMPACT_FUNCTION.ID.eq(hifResultRecords.field(GET_HIF_RESULTS.HIF_ID)))
				.orderBy(1, 2)
				.fetch();

		//We can't get the grid cell id from GET_HIF_RESULTS so we'll add it here
		for(Record res : hifRecords) {
			res.set(DSL.field("grid_cell_id", Long.class), ApiUtil.getCellId(res.get(1, Integer.class), res.get(2, Integer.class)));
		}
		
		
		return hifRecords;

	}

	/**
	 * 
	 * @param request
	 * @param response
	 * @param userProfile
	 * @return a JSON representation of all health impact functions.
	 */
	public static Object getAllHealthImpactFunctions(Request request, Response response, Optional<UserProfile> userProfile) {

		String userId = userProfile.get().getId();	
		int pollutantId;
		int hifGroupId;
		int page;
		int rowsPerPage;
		String sortBy;
		boolean descending;
		String filter;
		boolean showAll;
		try {
			pollutantId = ParameterUtil.getParameterValueAsInteger(request.raw().getParameter("pollutantId"), 0);
			hifGroupId = ParameterUtil.getParameterValueAsInteger(request.raw().getParameter("hifGroupId"), 0);
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
		
		setHealthImpactFunctionSortOrder(sortBy, descending, orderFields);

		Condition filterCondition = DSL.trueCondition();
		
		Condition pollutantCondition = DSL.trueCondition();

		if (pollutantId != 0) {
			pollutantCondition = DSL.field(HEALTH_IMPACT_FUNCTION.POLLUTANT_ID).eq(pollutantId);
			filterCondition = filterCondition.and(pollutantCondition);
		}

		Condition hifGroupCondition = DSL.trueCondition();

		if (hifGroupId != 0) {
			hifGroupCondition = DSL.field(HEALTH_IMPACT_FUNCTION_GROUP_MEMBER.HEALTH_IMPACT_FUNCTION_GROUP_ID).eq(hifGroupId);
			filterCondition = filterCondition.and(hifGroupCondition);
		}

		if (!"".equals(filter)) {
			filterCondition = filterCondition.and(buildHealthImpactFunctionFilterCondition(filter));
		}

		if(!showAll || !CoreApi.isAdmin(userProfile)) {
			filterCondition = filterCondition.and(HEALTH_IMPACT_FUNCTION.SHARE_SCOPE.eq(Constants.SHARING_ALL).or(HEALTH_IMPACT_FUNCTION.USER_ID.eq(userId)));
		}

		filterCondition = filterCondition.and(HEALTH_IMPACT_FUNCTION.ARCHIVED.eq((short) 0));

		Integer filteredRecordsCount = 
				DSL.using(JooqUtil.getJooqConfiguration()).select(DSL.count())
				.from(HEALTH_IMPACT_FUNCTION)
				.join(HEALTH_IMPACT_FUNCTION_GROUP_MEMBER).on(HEALTH_IMPACT_FUNCTION.ID.eq(HEALTH_IMPACT_FUNCTION_GROUP_MEMBER.HEALTH_IMPACT_FUNCTION_ID))
				.join(ENDPOINT_GROUP).on(HEALTH_IMPACT_FUNCTION.ENDPOINT_GROUP_ID.eq(ENDPOINT_GROUP.ID))				
				.join(ENDPOINT).on(HEALTH_IMPACT_FUNCTION.ENDPOINT_ID.eq(ENDPOINT.ID))
				.join(RACE).on(HEALTH_IMPACT_FUNCTION.RACE_ID.eq(RACE.ID))
				.join(GENDER).on(HEALTH_IMPACT_FUNCTION.GENDER_ID.eq(GENDER.ID))
				.join(ETHNICITY).on(HEALTH_IMPACT_FUNCTION.ETHNICITY_ID.eq(ETHNICITY.ID))
				.join(POLLUTANT).on(HEALTH_IMPACT_FUNCTION.POLLUTANT_ID.eq(POLLUTANT.ID))
				.join(POLLUTANT_METRIC).on(HEALTH_IMPACT_FUNCTION.METRIC_ID.eq(POLLUTANT_METRIC.ID))
				.where(filterCondition)
				.fetchOne(DSL.count());

		Result<Record> hifRecords = DSL.using(JooqUtil.getJooqConfiguration())
				.select(HEALTH_IMPACT_FUNCTION.asterisk()
						, ENDPOINT_GROUP.NAME.as("endpoint_group_name")
						, ENDPOINT.NAME.as("endpoint_name")
						, RACE.NAME.as("race_name")
						, GENDER.NAME.as("gender_name")
						, ETHNICITY.NAME.as("ethnicity_name")
						, POLLUTANT.FRIENDLY_NAME.as("pollutant")
						, POLLUTANT_METRIC.NAME.as("metric")
						)
				.from(HEALTH_IMPACT_FUNCTION)
				.join(HEALTH_IMPACT_FUNCTION_GROUP_MEMBER).on(HEALTH_IMPACT_FUNCTION.ID.eq(HEALTH_IMPACT_FUNCTION_GROUP_MEMBER.HEALTH_IMPACT_FUNCTION_ID))
				.join(ENDPOINT_GROUP).on(HEALTH_IMPACT_FUNCTION.ENDPOINT_GROUP_ID.eq(ENDPOINT_GROUP.ID))
				.join(ENDPOINT).on(HEALTH_IMPACT_FUNCTION.ENDPOINT_ID.eq(ENDPOINT.ID))
				.join(RACE).on(HEALTH_IMPACT_FUNCTION.RACE_ID.eq(RACE.ID))
				.join(GENDER).on(HEALTH_IMPACT_FUNCTION.GENDER_ID.eq(GENDER.ID))
				.join(ETHNICITY).on(HEALTH_IMPACT_FUNCTION.ETHNICITY_ID.eq(ETHNICITY.ID))
				.join(POLLUTANT).on(HEALTH_IMPACT_FUNCTION.POLLUTANT_ID.eq(POLLUTANT.ID))
				.join(POLLUTANT_METRIC).on(HEALTH_IMPACT_FUNCTION.METRIC_ID.eq(POLLUTANT_METRIC.ID))
				.where(filterCondition)
				.orderBy(orderFields)
				.offset((page * rowsPerPage) - rowsPerPage)
				.limit(rowsPerPage)
				.fetch();
		
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode data = mapper.createObjectNode();

		data.put("filteredRecordsCount", filteredRecordsCount);

		try {
			JsonFactory factory = mapper.getFactory();
			JsonParser jp = factory.createParser(
					hifRecords.formatJSON(new JSONFormat().header(false).recordFormat(RecordFormat.OBJECT)));
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

	/**
	 * 
	 * @param request
	 * @param response
	 * @param userProfile
	 * @return JSON representation of all hif groups for a given pollutant (pollutant id is a request parameter).
	 */
	public static Object getAllHifGroups(Request request, Response response, Optional<UserProfile> userProfile) {
		
		String userId = userProfile.get().getId();
		int pollutantId;
		try {
			pollutantId = ParameterUtil.getParameterValueAsInteger(request.raw().getParameter("pollutantId"), 0);
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return CoreApi.getErrorResponseInvalidId(request, response);
		}

		Result<Record4<String, Integer, String, Integer[]>> hifGroupRecords = DSL.using(JooqUtil.getJooqConfiguration())
				.select(HEALTH_IMPACT_FUNCTION_GROUP.NAME
						, HEALTH_IMPACT_FUNCTION_GROUP.ID
						, HEALTH_IMPACT_FUNCTION_GROUP.HELP_TEXT
						, DSL.arrayAggDistinct(HEALTH_IMPACT_FUNCTION_GROUP_MEMBER.HEALTH_IMPACT_FUNCTION_ID).as("functions")
						)
				.from(HEALTH_IMPACT_FUNCTION_GROUP)
				.join(HEALTH_IMPACT_FUNCTION_GROUP_MEMBER).on(HEALTH_IMPACT_FUNCTION_GROUP.ID.eq(HEALTH_IMPACT_FUNCTION_GROUP_MEMBER.HEALTH_IMPACT_FUNCTION_GROUP_ID))
				.join(HEALTH_IMPACT_FUNCTION).on(HEALTH_IMPACT_FUNCTION.ID.eq(HEALTH_IMPACT_FUNCTION_GROUP_MEMBER.HEALTH_IMPACT_FUNCTION_ID))
				.where(pollutantId == 0 ? DSL.noCondition() : HEALTH_IMPACT_FUNCTION.POLLUTANT_ID.eq(pollutantId)
					.and(HEALTH_IMPACT_FUNCTION_GROUP.SHARE_SCOPE.eq(Constants.SHARING_ALL).or(HEALTH_IMPACT_FUNCTION_GROUP.USER_ID.eq(userId))))
				.groupBy(HEALTH_IMPACT_FUNCTION_GROUP.NAME
						, HEALTH_IMPACT_FUNCTION_GROUP.ID)
				.orderBy(HEALTH_IMPACT_FUNCTION_GROUP.NAME.desc())
				.fetch();
		
		response.type("application/json");
		return hifGroupRecords.formatJSON(new JSONFormat().header(false).recordFormat(RecordFormat.OBJECT));
	}

	/*
	 * @param userProfile
	 * @return JSON representation of all hif groups for a given pollutant (pollutant id is a request parameter).
	 */
	public static Map<String, Integer> getAllHifGroupsByUser(String userId) {
			
		if(userId == null) {
            return null;
        }
		// int pollutantId;
		// try {
		// 	pollutantId = ParameterUtil.getParameterValueAsInteger(request.raw().getParameter("pollutantId"), 0);
		// } catch (NumberFormatException e) {
		// 	e.printStackTrace();
		// 	return CoreApi.getErrorResponseInvalidId(request, response);
		// }

		Map<String, Integer> hifGroupNameMap = DSL.using(JooqUtil.getJooqConfiguration())
				.select(DSL.lower(HEALTH_IMPACT_FUNCTION_GROUP.NAME), HEALTH_IMPACT_FUNCTION_GROUP.ID)
				.from(HEALTH_IMPACT_FUNCTION_GROUP)
				.where(HEALTH_IMPACT_FUNCTION_GROUP.USER_ID.equal(userId).or(HEALTH_IMPACT_FUNCTION_GROUP.SHARE_SCOPE.equal((short) 1)))
				.fetchMap(DSL.lower(HEALTH_IMPACT_FUNCTION_GROUP.NAME), HEALTH_IMPACT_FUNCTION_GROUP.ID);

		return hifGroupNameMap;
	}

	public static Object postHealthImpactFunctionData(Request request, Response response, Optional<UserProfile> userProfile) {
		request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));
		String hifGroupName;
		String description;
		String filename;
		LocalDateTime uploadDate;
		int pollutantId;
		
		try{
			pollutantId = ApiUtil.getMultipartFormParameterAsInteger(request, "pollutantId");
			hifGroupName = ApiUtil.getMultipartFormParameterAsString(request, "hifGroupName");
			description = ApiUtil.getMultipartFormParameterAsString(request, "description");
			filename = ApiUtil.getMultipartFormParameterAsString(request, "filename");
			uploadDate = ApiUtil.getMultipartFormParameterAsLocalDateTime(request, "uploadDate", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
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

				
		if(hifGroupName == null) {
			response.type("application/json");
			//response.status(400);
			validationMsg.success=false;
			validationMsg.messages.add(new ValidationMessage.Message("error","Missing required parameter: name."));
			return CoreApi.transformValMsgToJSON(validationMsg);
		}

		String userId = userProfile.get().getId();
		
		HealthImpactFunctionGroupRecord hifGroupRecord=null;
		HealthImpactFunctionRecord hifRecord=null;
		int endpointGroupIdx=-999;
		int endpointIdx=-999;
		int pollutantIdx=-999;
		int metricIdx=-999;
		int seasonalMetricIdx=-999;
		int metricStatisticIdx=-999;
		int timingIdx=-999;
		int authorIdx=-999;
		int studyYearIdx=-999;
		// int geogAreaIdx=-999;
		// int geogAreaFeatureIdx=-999;
		int studyLocIdx=-999;
		int otherPollutantIdx=-999;
		int qualifierIdx=-999;
		int referenceIdx=-999;
		int raceIdx=-999;
		int genderIdx=-999;
		int ethnicityIdx=-999;
		int startAgeIdx=-999;
		int endAgeIdx=-999;
		int functionIdx=-999;
		int baselineFunctionIdx=-999;
		int betaIdx=-999;
		int distBetaIdx=-999;
		int param1Idx=-999;
		int param2Idx=-999;
		int paramAIdx=-999;
		int paramANameIdx=-999;
		int paramBIdx=-999;
		int paramBNameIdx=-999;
		int paramCIdx=-999;
		int paramCNameIdx=-999;
		int distributionIdx=-999;
		int heroIdIdx=-999;
		int heroUrlIdx=-999;
		int accessUrlIdx=-999;
		
		Map<String, Integer> raceIdLookup = new HashMap<>();		
		Map<String, Integer> ethnicityIdLookup = new HashMap<>();		
		Map<String, Integer> genderIdLookup = new HashMap<>();
		HashMap<String,Map<String,Integer>> endpointIdLookup = new HashMap<String,Map<String,Integer>>();
		Map<String, Integer> endpointGroupIdLookup = new HashMap<>();
		Map<String, Integer> metricIdLookup = new HashMap<>();
		Map<String, Integer> timingIdLookup = new HashMap<>();

		int hifGroupId = 0;
		Map<String, Integer> hifGroupNameMap = getAllHifGroupsByUser(userId);

		if(hifGroupNameMap.containsKey(hifGroupName.toLowerCase())) {
			hifGroupId = hifGroupNameMap.get(hifGroupName.toLowerCase());
		} else {

			hifGroupRecord = DSL.using(JooqUtil.getJooqConfiguration())
				.insertInto(HEALTH_IMPACT_FUNCTION_GROUP
						, HEALTH_IMPACT_FUNCTION_GROUP.NAME
						, HEALTH_IMPACT_FUNCTION_GROUP.HELP_TEXT
						, HEALTH_IMPACT_FUNCTION_GROUP.USER_ID
						, HEALTH_IMPACT_FUNCTION_GROUP.SHARE_SCOPE
						)
				.values(hifGroupName, description, userId, Constants.SHARING_NONE)
				.returning(HEALTH_IMPACT_FUNCTION_GROUP.ID)
				.fetchOne();

			hifGroupId = hifGroupRecord.value1();
		}
		
		//remove built in tokens (e, pi, sin, etc.)
		//these were causing function arguments to get parsed incorrectly
		//not working as expected, need to find a different way to validate functions
		for(String s : mXparser.getBuiltinTokensToRemove()) {
			mXparser.removeBuiltinTokens(s);
		}

		try (InputStream is = request.raw().getPart("file").getInputStream()) {
			BOMInputStream bis = new BOMInputStream(is, false);
			CSVReader csvReader = new CSVReader (new InputStreamReader(bis));				

			String[] record;
			
			//step 1: verify column names 
			// Read the header
			// allow either "column" or "col"; "values" or "value"
			// todo: warn or abort when both "column" and "col" exist.
			record = csvReader.readNext();
			for(int i=0; i < record.length; i++) {
				switch(record[i].toLowerCase().replace(" ", "")) {
				case "endpoint":
					endpointIdx=i;
					break;
				case "endpointgroup":
					endpointGroupIdx=i;
					break;
				case "pollutant":
					pollutantIdx=i;
					break;
				case "metric":
					metricIdx=i;
					break;
				case "seasonalmetric":
					seasonalMetricIdx=i;
					break;
				case "metricstatistic":
					metricStatisticIdx=i;
					break;
				case "timing":
					timingIdx=i;
					break;
				case "studyauthor":
					authorIdx=i;
					break;
				case "studyyear":
					studyYearIdx=i;
					break;
				// case "geographicarea":
				// 	geogAreaIdx=i;
				// 	break;
				// case "geographicareafeature":
				// 	geogAreaFeatureIdx=i;
				// 	break;
				case "studylocation":
					studyLocIdx=i;
					break;
				case "otherpollutants":
					otherPollutantIdx=i;
					break;
				case "qualifier":
					qualifierIdx=i;
					break;
				case "reference":
					referenceIdx=i;
					break;
				case "race":
					raceIdx=i;
					break;
				case "gender":
					genderIdx=i;
					break;
				case "ethnicity":
					ethnicityIdx=i;
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
				case "baselinefunction":
					baselineFunctionIdx=i;
					break;	
				case "beta":
					betaIdx=i;
					break;			
				case "distributionbeta":
					distBetaIdx=i;
					break;	
				case "parameter1beta":
					param1Idx=i;
					break;
				case "parameter2beta":
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
				case "heroid":
					heroIdIdx=i;
					break;
				case "epaherourl":
					heroUrlIdx=i;
					break;	
				case "accessurl":
					accessUrlIdx=i;
					break;			
				default:
					System.out.println(record[i].toLowerCase().replace(" ", ""));
				}
			}

			// String tmp = HIFUtil.validateModelColumnHeadings(endpointGroupIdx, endpointIdx, pollutantIdx, metricIdx, seasonalMetricIdx, metricStatisticIdx, timingIdx, authorIdx, studyYearIdx, studyLocIdx, otherPollutantIdx, qualifierIdx, referenceIdx, raceIdx, genderIdx, ethnicityIdx, startAgeIdx, endAgeIdx, functionIdx, baselineFunctionIdx, betaIdx, distBetaIdx, param1Idx, param2Idx, paramAIdx, paramANameIdx, paramBIdx, paramBNameIdx, paramCIdx, paramCNameIdx, distributionIdx, heroIdIdx, heroUrlIdx, accessUrlIdx);
			String tmp = HIFUtil.validateModelColumnHeadings(endpointGroupIdx, endpointIdx, pollutantIdx, metricIdx, seasonalMetricIdx, metricStatisticIdx, timingIdx, authorIdx, studyYearIdx, studyLocIdx, otherPollutantIdx, qualifierIdx, referenceIdx, raceIdx, genderIdx, ethnicityIdx, startAgeIdx, endAgeIdx, functionIdx, baselineFunctionIdx, betaIdx, distBetaIdx, param1Idx, param2Idx, paramAIdx, paramANameIdx, paramBIdx, paramBNameIdx, paramCIdx, paramCNameIdx, distributionIdx);

			if(tmp.length() > 0) {
				log.debug("end age index is :" + endAgeIdx);

				log.debug("health impact function dataset posted - columns are missing: " + tmp);
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				msg.message = "The following columns are missing: " + tmp;
				msg.type = "error";
				validationMsg.messages.add(msg);
				response.type("application/json");
				return CoreApi.transformValMsgToJSON(validationMsg);
			}
			
			ethnicityIdLookup = HIFUtil.getEthnicityIdLookup();
			raceIdLookup = HIFUtil.getRaceIdLookup();
			genderIdLookup = HIFUtil.getGenderIdLookup();
			endpointGroupIdLookup = HIFUtil.getEndpointGroupIdLookup();
			metricIdLookup = HIFUtil.getMetricIdLookup(pollutantId);
			timingIdLookup = HIFUtil.getTimingIdLookup();

			
		// 	//We might also need to clean up the header. Or, maybe we should make this a transaction?
			
		// 	//step 2: make sure file has > 0 rows. Check rowCount after while loop.
			int rowCount = 0;
			int countStudyYearTypeError = 0;
			int countStartAgeTypeError = 0;
			int countEndAgeTypeError = 0;

			int countMissingEndpoint = 0;
			int countMissingEndpointGroup = 0;
			int countMissingMetric = 0;
			int countMissingTiming = 0;

			int countBetaError = 0;
			int countDistBetaError = 0;
			int countParam1TypeError = 0;
			int countParam1Error = 0;
			int countParam2TypeError = 0;
			int countParam2Error = 0;
			int countParamATypeError = 0;
			int countParamAError = 0;
			int countParamBTypeError = 0;
			int countParamBError = 0;
			int countParamCTypeError = 0;
			int countParamCError = 0;

			int countBaselineFunctionError = 0;
			int countFunctionError = 0;

			List<String> lstUndefinedEthnicities = new ArrayList<String>();
			List<String> lstUndefinedRaces = new ArrayList<String>();
			List<String> lstUndefinedGenders = new ArrayList<String>();
			List<String> lstUndefinedEndpoints = new ArrayList<String>();
			List<String> lstUndefinedEndpointGroups = new ArrayList<String>();
			List<String> lstUndefinedMetrics = new ArrayList<String>();
			List<String> lstUndefinedTimings = new ArrayList<String>();

			List<String> lstDupMetricCombo = new ArrayList<String>();
			
			Map<String, Integer> dicUniqueRecord = new HashMap<String,Integer>();	

			List<String> distTypes = new ArrayList<String>();
			distTypes.add("None");
			distTypes.add("Triangular");
			distTypes.add("Normal");
			distTypes.add("Weibull");
			distTypes.add("LogNormal");
			distTypes.add("Custom");
			distTypes.add("Uniform");

			while ((record = csvReader.readNext()) != null) {				
				rowCount ++;
				//endpoint id hashmap is a nested dictionary with the outer key being endpoint groups and values being hashmaps of endpoint names to ids
				String endpointGroupName = record[endpointGroupIdx].toLowerCase();
				if (!endpointIdLookup.containsKey(endpointGroupName)){
					Integer endpointGroupId = endpointGroupIdLookup.get(endpointGroupName);
					//endpoint group id is a short in endpoint data but an Integer in endpoint group data
					short shortEndpointGroupId = (short) (int) endpointGroupId;
					endpointIdLookup.put(endpointGroupName, IncidenceUtil.getEndpointIdLookup(shortEndpointGroupId));
				}

				//TODO: Update this validation code when we add lookup tables for timeframe, units, and/or distribution
				// Make sure this metric exists in the db. If not, update the corresponding error array to return useful error message
				String str = "";

				str = record[ethnicityIdx];
				if(!ethnicityIdLookup.containsKey(str.toLowerCase() ) && !str.equals("")) {
					if (!lstUndefinedEthnicities.contains(String.valueOf(str.toLowerCase()))) {
						lstUndefinedEthnicities.add(String.valueOf(str.toLowerCase()));
					}
				}
				
				str = record[raceIdx];
				if(!raceIdLookup.containsKey(str.toLowerCase()) && !str.equals("")) {
					if (!lstUndefinedRaces.contains(String.valueOf(str.toLowerCase()))) {
						lstUndefinedRaces.add(String.valueOf(str.toLowerCase()));
					}
				}

				
				str= record[genderIdx];
				if(!genderIdLookup.containsKey(str.toLowerCase()) && !str.equals("")) {
					if (!lstUndefinedGenders.contains(String.valueOf(str.toLowerCase()))) {
						lstUndefinedGenders.add(String.valueOf(str.toLowerCase()));
					}
				}

				str = record[endpointIdx];
				if(str == "") {
					countMissingEndpoint ++;
				} else if(!endpointIdLookup.get(endpointGroupName).containsKey(str.toLowerCase()) ) {
					if (!lstUndefinedEndpoints.contains(String.valueOf(str.toLowerCase()))) {
						lstUndefinedEndpoints.add(String.valueOf(str.toLowerCase()));
					}
				}

				str = record[endpointGroupIdx];
				if(str == "") {
					countMissingEndpointGroup ++;
				} else if(!endpointGroupIdLookup.containsKey(str.toLowerCase()) ) {
					if (!lstUndefinedEndpointGroups.contains(String.valueOf(str.toLowerCase()))) {
						lstUndefinedEndpointGroups.add(String.valueOf(str.toLowerCase()));
					}
				}

				str= record[metricIdx];
				if(str == "") {
					countMissingMetric ++;
				} else if(!metricIdLookup.containsKey(str.toLowerCase())) {
					if (!lstUndefinedMetrics.contains(String.valueOf(str.toLowerCase()))) {
						lstUndefinedMetrics.add(String.valueOf(str.toLowerCase()));
					}
				}

				str= record[timingIdx];
				if(str == "") {
					countMissingTiming ++;
				} else if(!timingIdLookup.containsKey(str.toLowerCase())) {
					if (!lstUndefinedTimings.contains(String.valueOf(str.toLowerCase()))) {
						lstUndefinedTimings.add(String.valueOf(str.toLowerCase()));
					}
				}
			
				
		// 		//step 3: Verify data types for each field
				//study year is required and should be an integer
				str = record[studyYearIdx];
				if(str=="" || !str.matches("-?\\d+")) {
					countStudyYearTypeError++;
				}	

				//start age is required and should be an integer
				str = record[startAgeIdx];
				//question: or use Integer.parseInt(str)??
				if(str=="" || !str.matches("-?\\d+")) {
					countStartAgeTypeError++;
				}	

				//end age is required and should be an integer
				str = record[endAgeIdx];
				//question: or use Integer.parseInt(str)??
				if(str=="" || !str.matches("-?\\d+")) {
					countEndAgeTypeError++;
				}	

				//beta should be a double and >= 0
				str = record[betaIdx];
				try {
					double dbl = Double.parseDouble(str);
				} catch(NumberFormatException e){
					countBetaError ++;
				}

				//dist beta should be a value in the distTypes list
				str = record[distBetaIdx];
				if(!distTypes.contains(str)) {
					countDistBetaError++;
				}

				//param 1 beta should be a double and >= 0
				str = record[param1Idx];
				try {
					double dbl = Double.parseDouble(str);
					if (dbl<0) {
						countParam1TypeError++;
					}
				} catch(NumberFormatException e){
					countParam1Error ++;
				}

				//param 2 beta should be a double and >= 0
				str = record[param2Idx];
				try {
					double dbl = Double.parseDouble(str);
					if (dbl<0) {
						countParam2TypeError++;
					}
				} catch(NumberFormatException e){
					countParam2Error ++;
				}

				//param a should be a double and >= 0
				str = record[paramAIdx];
				try {
					double dbl = Double.parseDouble(str);
					if (dbl<0) {
						countParamATypeError++;
					}
				} catch(NumberFormatException e){
					countParamAError ++;
				}

				//param b should be a double and >= 0
				str = record[paramBIdx];
				try {
					double dbl = Double.parseDouble(str);
					if (dbl<0) {
						countParamBTypeError++;
					}
				} catch(NumberFormatException e){
					countParamBError ++;
				}

				//param c should be a double and >= 0
				str = record[paramCIdx];
				try {
					double dbl = Double.parseDouble(str);
					if (dbl<0) {
						countParamCTypeError++;
					}
				} catch(NumberFormatException e){
					countParamCError ++;
				}

				//baselinefunction should be a valid formula
				// str = record[baselineFunctionIdx];
				// Expression e = new Expression(str);
				
				// String[] missingVars = e.getMissingUserDefinedArguments();

				// for (String varName : missingVars) {
				// 	e.addArguments(new Argument(varName + " = 1"));
				// }

				// if(!e.checkSyntax()) {
				// 	countBaselineFunctionError++;
				// }	

				// //function should be a valid formula
				// str = record[functionIdx];
				// e = new Expression(str);

				// missingVars = e.getMissingUserDefinedArguments();

				// for (String varName : missingVars) {
				// 	e.addArguments(new Argument(varName + " = 1"));
				// }

				// if(!e.checkSyntax()) {
				// 	countFunctionError++;
				// }	

		
		// //check that we don't have duplicate records for a given categorization and row/col
		// //update to include timeframe, units, distribution, and SE?
		// 		str = record[columnIdx].toString() 
		// 				+ "~" + record[rowIdx].toLowerCase() 
		// 				+ "~" + record[endpointGroupIdx].toLowerCase() 
		// 				+ "~" + record[endpointIdx].toLowerCase() 
		// 				+ "~" + record[raceIdx].toLowerCase()
		// 				+ "~" + record[genderIdx].toLowerCase()
		// 				+ "~" + record[ethnicityIdx].toLowerCase()
		// 				+ "~" + record[yearIdx].toLowerCase()
		// 				+ "~" + record[startAgeIdx].toLowerCase()
		// 				+ "~" + record[endAgeIdx].toLowerCase()
		// 				+ "~" + record[typeIdx].toLowerCase()
		// 				+ "~" + record[valueIdx].toLowerCase();
		// 		if(!dicUniqueRecord.containsKey(str)) {
		// 			dicUniqueRecord.put(str,rowCount + 1);
		// 		}
		// 		else {
		// 			if(!lstDupMetricCombo.contains(str)) {
		// 				lstDupMetricCombo.add(str);
		// 			}
		// 		}
			}	



			//summarize validation message
			//can probably remove all of the error checks for missing gender, ethnicity, race values because those can be null and would only be "" if cell doesn't exist?
			if(countStudyYearTypeError>0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countStudyYearTypeError == 1) {
					strRecord = String.valueOf(countStudyYearTypeError) + " record has a Study Year value that is not a valid integer.";
				}
				else {
					strRecord = String.valueOf(countStudyYearTypeError) + " records have Study Year values that are not valid integers.";
				}
				msg.message = strRecord + "";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}
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

			if(countBetaError > 0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countBetaError == 1) {
					strRecord = String.valueOf(countBetaError) + " record has a Beta value that is not a valid number.";
				}
				else {
					strRecord = String.valueOf(countBetaError) + " records have Beta values that are not valid numbers.";
				}
				msg.message = strRecord + "";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}

			if(countDistBetaError > 0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countDistBetaError == 1) {
					strRecord = String.valueOf(countDistBetaError) + " record has an invalid Distribution Beta value.";
				}
				else {
					strRecord = String.valueOf(countDistBetaError) + " records have invalid Distribution Beta values.";
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
					strRecord = String.valueOf(countParam1Error) + " record has a Parameter 1 Beta value that is not a valid number.";
				}
				else {
					strRecord = String.valueOf(countParam1Error) + " records have Parameter 1 Beta values that are not valid numbers.";
				}
				msg.message = strRecord + "";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}
			if(countParam1TypeError > 0) {
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countParam1TypeError == 1) {
					strRecord = String.valueOf(countParam1TypeError) + " record has";
				}
				else {
					strRecord = String.valueOf(countParam1TypeError) + " records have";
				}
				msg.message = strRecord + " Parameter 1 Beta values below zero.";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}

			if(countParam2Error > 0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countParam2Error == 1) {
					strRecord = String.valueOf(countParam2Error) + " record has a Parameter 2 Beta value that is not a valid number.";
				}
				else {
					strRecord = String.valueOf(countParam2Error) + " records have Parameter 2 Beta values that are not valid numbers.";
				}
				msg.message = strRecord + "";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}
			if(countParam2TypeError > 0) {
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countParam2Error == 1) {
					strRecord = String.valueOf(countParam2TypeError) + " record has";
				}
				else {
					strRecord = String.valueOf(countParam2TypeError) + " records have";
				}
				msg.message = strRecord + " Parameter 2 Beta values below zero.";
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
			if(countParamATypeError > 0) {
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countParamATypeError == 1) {
					strRecord = String.valueOf(countParamATypeError) + " record has";
				}
				else {
					strRecord = String.valueOf(countParamATypeError) + " records have";
				}
				msg.message = strRecord + " an A value below zero.";
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
			if(countParamBTypeError > 0) {
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countParamBTypeError == 1) {
					strRecord = String.valueOf(countParamBTypeError) + " record has";
				}
				else {
					strRecord = String.valueOf(countParamBTypeError) + " records have";
				}
				msg.message = strRecord + " a B value below zero.";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}

			if(countParamCError > 0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countParamCError == 1) {
					strRecord = String.valueOf(countParamCError) + " record has a C value that is not a valid number.";
				}
				else {
					strRecord = String.valueOf(countParamCError) + " records have C values that are not valid numbers.";
				}
				msg.message = strRecord + "";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}
			if(countParamCTypeError > 0) {
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countParamCTypeError == 1) {
					strRecord = String.valueOf(countParamCTypeError) + " record has";
				}
				else {
					strRecord = String.valueOf(countParamCTypeError) + " records have";
				}
				msg.message = strRecord + " a Cvalue below zero.";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}

			if(lstUndefinedEthnicities.size()>0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				msg.message = "The following Ethnicity values are not defined: " + String.join(",", lstUndefinedEthnicities) + ".";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}

			if(lstUndefinedRaces.size()>0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				msg.message = "The following Ethnicity values are not defined: " + String.join(",", lstUndefinedRaces) + ".";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}

			if(lstUndefinedGenders.size()>0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				msg.message = "The following Ethnicity values are not defined: " + String.join(",", lstUndefinedGenders) + ".";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}

			if(countMissingEndpoint>0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countMissingEndpoint == 1) {
					strRecord = String.valueOf(countMissingEndpoint) + " record is missing a Endpoint value.";
				}
				else {
					strRecord = String.valueOf(countMissingEndpoint) + " records are missing Endpoint values.";
				}
				msg.message = strRecord + "";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}

			if(lstUndefinedEndpoints.size()>0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				msg.message = "The following Endpoint values are not defined: " + String.join(",", lstUndefinedEndpoints) + ".";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}

			if(countMissingEndpointGroup>0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countMissingEndpointGroup == 1) {
					strRecord = String.valueOf(countMissingEndpointGroup) + " record is missing a Endpoint Group value.";
				}
				else {
					strRecord = String.valueOf(countMissingEndpointGroup) + " records are missing Endpoint Group values.";
				}
				msg.message = strRecord + "";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}

			if(countMissingMetric>0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countMissingMetric == 1) {
					strRecord = String.valueOf(countMissingMetric) + " record is missing a Metric value.";
				}
				else {
					strRecord = String.valueOf(countMissingMetric) + " records are missing Metric values.";
				}
				msg.message = strRecord + "";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}

			if(countMissingTiming>0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countMissingTiming == 1) {
					strRecord = String.valueOf(countMissingTiming) + " record is missing a Timing value.";
				}
				else {
					strRecord = String.valueOf(countMissingTiming) + " records are missing Timing values.";
				}
				msg.message = strRecord + "";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}

			if(lstUndefinedEndpointGroups.size()>0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				msg.message = "The following Endpoint Group values are not defined: " + String.join(",", lstUndefinedEndpointGroups) + ".";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}
			
			if(lstDupMetricCombo.size()>0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				msg.message = "The following Metric combinations are not unique: " + String.join(",", lstDupMetricCombo)+ ".";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}
			
			if(lstUndefinedMetrics.size()>0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				msg.message = "The following Metric values are not defined: " + String.join(",", lstUndefinedMetrics) + ".";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}

			if(lstUndefinedTimings.size()>0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				msg.message = "The following Timing values are not defined: " + String.join(",", lstUndefinedTimings) + ".";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}

			if(countBaselineFunctionError > 0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countBaselineFunctionError == 1) {
					strRecord = String.valueOf(countBaselineFunctionError) + " record has a baseline function value that is not a valid formula.";
				}
				else {
					strRecord = String.valueOf(countBaselineFunctionError) + " records have baseline function values that are not valid formulas.";
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
			log.error("Error validating incidence file", e);
			response.type("application/json");
			//response.status(400);
			validationMsg.success=false;
			validationMsg.messages.add(new ValidationMessage.Message("error","Error occurred during validation of incidence file."));
			return CoreApi.transformValMsgToJSON(validationMsg);
		}
		
		Integer hifDatasetId = null;
		
		// //import data
		try (InputStream is = request.raw().getPart("file").getInputStream()){
			CSVReader csvReader = new CSVReader (new InputStreamReader(is));
			String[] record;
			record = csvReader.readNext();
			while ((record = csvReader.readNext()) != null) {		

				String endpointGroupName = record[endpointGroupIdx].toLowerCase();
				String endpointName = record[endpointIdx].toLowerCase();
				int endpointGroupId = endpointGroupIdLookup.get(endpointGroupName);
				
				int endpointId = endpointIdLookup.get(endpointGroupName).get(endpointName);
				
				String metricName = record[metricIdx].toLowerCase();
				int metricId = metricIdLookup.get(metricName);

				String timingName = record[timingIdx].toLowerCase();
				int timingId = timingIdLookup.get(timingName);

				String raceName = record[raceIdx].toLowerCase();
				if (raceName.equals("")){
					raceName = "all";
				}
				int raceId = raceIdLookup.get(raceName);

				String genderName = record[genderIdx].toLowerCase();
				if (genderName.equals("")){
					genderName = "all";
				}
				int genderId = genderIdLookup.get(genderName);

				String ethnicityName = record[ethnicityIdx].toLowerCase();
				if (ethnicityName.equals("")){
					ethnicityName = "all";
				}
				int ethnicityId = ethnicityIdLookup.get(ethnicityName);

				int functionYear = Integer.valueOf(record[studyYearIdx]);
				short startAge = Short.valueOf(record[startAgeIdx]);
				short endAge = Short.valueOf(record[endAgeIdx]);

				int heroId = -1;
				if(record[heroIdIdx] != null && !record[heroIdIdx].equals("")) {
					heroId = Integer.valueOf(record[heroIdIdx]);
				}

				String heroUrl = null;
				if(record[heroUrlIdx] != null) {
					heroUrl = record[heroUrlIdx];
				}

				String accessUrl = null;
				if(record[accessUrlIdx] != null) {
					accessUrl = record[accessUrlIdx];
				}

				Double beta = 0.0;
				if(!record[betaIdx].equals("")){
					beta = Double.valueOf(record[betaIdx]);
				}

				Double p1beta = 0.0;
				if(!record[param1Idx].equals("")){
					p1beta = Double.valueOf(record[param1Idx]);
				}

				Double p2beta = 0.0;
				if(!record[param2Idx].equals("")){
					p2beta = Double.valueOf(record[param2Idx]);
				}

				Double valA = 0.0;
				if(!record[paramAIdx].equals("")){
					valA = Double.valueOf(record[paramAIdx]);
				}

				Double valB = 0.0;
				if(!record[paramBIdx].equals("")){
					valB = Double.valueOf(record[paramBIdx]);
				}

				Double valC = 0.0;
				if(!record[paramCIdx].equals("")){
					valC = Double.valueOf(record[paramCIdx]);
				}

				// String geogArea = "";
				// if(geogAreaIdx != -999) {
				// 	geogArea = record[geogAreaIdx];
				// }

				// String geogAreaFeature = "";
				// if(geogAreaFeatureIdx != -999) {
				// 	geogAreaFeature = record[geogAreaFeatureIdx];
				// }


				// short startDay = Short.valueOf(record[startDayIdx]);
				// short endDay = Short.valueOf(record[endDayIdx]);

				//Create the hif group record, if necessary


				//Create the hif record
				hifRecord = DSL.using(JooqUtil.getJooqConfiguration())
				.insertInto(HEALTH_IMPACT_FUNCTION
						, HEALTH_IMPACT_FUNCTION.HEALTH_IMPACT_FUNCTION_DATASET_ID
						, HEALTH_IMPACT_FUNCTION.ENDPOINT_GROUP_ID
						, HEALTH_IMPACT_FUNCTION.ENDPOINT_ID
						, HEALTH_IMPACT_FUNCTION.POLLUTANT_ID
						, HEALTH_IMPACT_FUNCTION.METRIC_ID
						, HEALTH_IMPACT_FUNCTION.TIMING_ID
						, HEALTH_IMPACT_FUNCTION.AUTHOR
						, HEALTH_IMPACT_FUNCTION.FUNCTION_YEAR
						, HEALTH_IMPACT_FUNCTION.LOCATION
						, HEALTH_IMPACT_FUNCTION.OTHER_POLLUTANTS
						, HEALTH_IMPACT_FUNCTION.QUALIFIER
						, HEALTH_IMPACT_FUNCTION.REFERENCE
						, HEALTH_IMPACT_FUNCTION.START_AGE
						, HEALTH_IMPACT_FUNCTION.END_AGE
						, HEALTH_IMPACT_FUNCTION.FUNCTION_TEXT
						, HEALTH_IMPACT_FUNCTION.BETA
						, HEALTH_IMPACT_FUNCTION.DIST_BETA
						, HEALTH_IMPACT_FUNCTION.P1_BETA
						, HEALTH_IMPACT_FUNCTION.P2_BETA
						, HEALTH_IMPACT_FUNCTION.VAL_A
						, HEALTH_IMPACT_FUNCTION.NAME_A
						, HEALTH_IMPACT_FUNCTION.VAL_B
						, HEALTH_IMPACT_FUNCTION.NAME_B
						, HEALTH_IMPACT_FUNCTION.VAL_C
						, HEALTH_IMPACT_FUNCTION.NAME_C
						, HEALTH_IMPACT_FUNCTION.BASELINE_FUNCTION_TEXT
						, HEALTH_IMPACT_FUNCTION.RACE_ID
						, HEALTH_IMPACT_FUNCTION.GENDER_ID
						, HEALTH_IMPACT_FUNCTION.ETHNICITY_ID
						// , HEALTH_IMPACT_FUNCTION.START_DAY
						// , HEALTH_IMPACT_FUNCTION.END_DAY
						, HEALTH_IMPACT_FUNCTION.HERO_ID
						, HEALTH_IMPACT_FUNCTION.EPA_HERO_URL
						, HEALTH_IMPACT_FUNCTION.ACCESS_URL
						, HEALTH_IMPACT_FUNCTION.USER_ID
						, HEALTH_IMPACT_FUNCTION.SHARE_SCOPE
						)
				.values(1, endpointGroupId, endpointId, pollutantId, metricId, timingId, record[authorIdx], functionYear, 
				record[studyLocIdx], record[otherPollutantIdx], record[qualifierIdx], record[referenceIdx], startAge, endAge, 
				record[functionIdx], beta, record[distBetaIdx], p1beta, p2beta, valA, record[paramANameIdx], valB, 
				record[paramBNameIdx], valC, record[paramCNameIdx], record[baselineFunctionIdx], raceId, genderId, ethnicityId, 
				(heroId != -1 ? heroId : null), heroUrl, accessUrl, userId, Constants.SHARING_NONE)
				.returning(HEALTH_IMPACT_FUNCTION.ID)
				.fetchOne();

				int hifRecordId = hifRecord.getId();

				DSL.using(JooqUtil.getJooqConfiguration())
				.insertInto(HEALTH_IMPACT_FUNCTION_GROUP_MEMBER
						, HEALTH_IMPACT_FUNCTION_GROUP_MEMBER.HEALTH_IMPACT_FUNCTION_GROUP_ID
						, HEALTH_IMPACT_FUNCTION_GROUP_MEMBER.HEALTH_IMPACT_FUNCTION_ID
						)
				.values(hifGroupId, hifRecordId)
				.execute();

			}
		
		} catch (Exception e) {
			log.error("Error importing health impact functions", e);
			response.type("application/json");
			validationMsg.success=false;
			validationMsg.messages.add(new ValidationMessage.Message("error","Error occurred during import of health impact functions."));
			// deleteIncidenceDataset(incidenceDatasetId, userProfile);
			return CoreApi.transformValMsgToJSON(validationMsg);
		}
		
		response.type("application/json");
		validationMsg.success = true;
		return CoreApi.transformValMsgToJSON(validationMsg); 
	}


	public static Object archiveHealthImpactFunction(Request request, Response response, Optional<UserProfile> userProfile) {
	
		ValidationMessage validationMsg = new ValidationMessage();
		Integer id;

		try {
			id = Integer.valueOf(request.params("id"));
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return CoreApi.getErrorResponseInvalidId(request, response);
		} 
		DSLContext create = DSL.using(JooqUtil.getJooqConfigurationUnquoted());
		
		HealthImpactFunctionRecord hifResult = create.selectFrom(HEALTH_IMPACT_FUNCTION).where(HEALTH_IMPACT_FUNCTION.ID.eq(id)).fetchAny();
		if(hifResult == null) {
			return CoreApi.getErrorResponseNotFound(request, response);
		}

		//Nobody can archive shared HIFs
		//All users can archive their own HIFs
		//Admins can archive any non-shared HIFs
		if(hifResult.getShareScope() == Constants.SHARING_ALL || !(hifResult.getUserId().equalsIgnoreCase(userProfile.get().getId()) || CoreApi.isAdmin(userProfile)) )  {
			return CoreApi.getErrorResponseForbidden(request, response);
		}

		hifResult.setArchived((short) 1);

		hifResult.store();

		response.type("application/json");
		validationMsg.success = true;
		return CoreApi.transformValMsgToJSON(validationMsg); 
	}

	/**
	 * 
	 * @param request
	 * @param response
	 * @param userProfile
	 * @return selected hif groups (hif group ids is a request parameter).
	 * 
	 */
	public static Object getSelectedHifGroups(Request request, Response response, Optional<UserProfile> userProfile) {
		
		String idsParam;
		int popYear;
		int defaultIncidencePrevalenceDataset;
		int pollutantId;
		int baselineId;
		int scenarioId;
		boolean userPrefered; //If true, BenMAP will use the incidence/prevalence selected by the user even when there is another dataset which matches the demo groups better.
		List<Integer> ids;

		try{
			idsParam = String.valueOf(request.params("ids").replace(" ", ""));
			popYear = ParameterUtil.getParameterValueAsInteger(request.raw().getParameter("popYear"), 0);
			defaultIncidencePrevalenceDataset = ParameterUtil.getParameterValueAsInteger(request.raw().getParameter("incidencePrevalenceDataset"), 0);
			pollutantId = ParameterUtil.getParameterValueAsInteger(request.raw().getParameter("pollutantId"), 0);
			baselineId = ParameterUtil.getParameterValueAsInteger(request.raw().getParameter("baselineId"), 0);
			scenarioId = ParameterUtil.getParameterValueAsInteger(request.raw().getParameter("scenarioId"), 0);
			userPrefered = ParameterUtil.getParameterValueAsBoolean(request.raw().getParameter("userPrefered"), false);
			ids = Stream.of(idsParam.split(",")).mapToInt(Integer::parseInt).boxed().collect(Collectors.toList());
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return CoreApi.getErrorResponseInvalidId(request, response);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return CoreApi.getErrorResponseInvalidId(request, response);
		}
		
		
		List<Integer> supportedMetricIds = null; 
		if(baselineId != 0 && scenarioId != 0) {
			supportedMetricIds = AirQualityUtil.getSupportedMetricIds(baselineId, scenarioId);
		}
		
		
		Result<Record> hifGroupRecords = DSL.using(JooqUtil.getJooqConfiguration())
				.select(HEALTH_IMPACT_FUNCTION_GROUP.NAME
						, HEALTH_IMPACT_FUNCTION_GROUP.ID
						, HEALTH_IMPACT_FUNCTION_GROUP.HELP_TEXT
						, HEALTH_IMPACT_FUNCTION.asterisk()
						, ENDPOINT_GROUP.NAME.as("endpoint_group_name")
						, ENDPOINT.NAME.as("endpoint_name")
						, RACE.NAME.as("race_name")
						, GENDER.NAME.as("gender_name")
						, ETHNICITY.NAME.as("ethnicity_name")
						)
				.from(HEALTH_IMPACT_FUNCTION_GROUP)
				.join(HEALTH_IMPACT_FUNCTION_GROUP_MEMBER).on(HEALTH_IMPACT_FUNCTION_GROUP.ID.eq(HEALTH_IMPACT_FUNCTION_GROUP_MEMBER.HEALTH_IMPACT_FUNCTION_GROUP_ID))
				.join(HEALTH_IMPACT_FUNCTION).on(HEALTH_IMPACT_FUNCTION_GROUP_MEMBER.HEALTH_IMPACT_FUNCTION_ID.eq(HEALTH_IMPACT_FUNCTION.ID))
				.join(ENDPOINT_GROUP).on(HEALTH_IMPACT_FUNCTION.ENDPOINT_GROUP_ID.eq(ENDPOINT_GROUP.ID))
				.join(ENDPOINT).on(HEALTH_IMPACT_FUNCTION.ENDPOINT_ID.eq(ENDPOINT.ID))
				.join(RACE).on(HEALTH_IMPACT_FUNCTION.RACE_ID.eq(RACE.ID))
				.join(GENDER).on(HEALTH_IMPACT_FUNCTION.GENDER_ID.eq(GENDER.ID))
				.join(ETHNICITY).on(HEALTH_IMPACT_FUNCTION.ETHNICITY_ID.eq(ETHNICITY.ID))
				.where(HEALTH_IMPACT_FUNCTION_GROUP.ID.in(ids)
						.and(HEALTH_IMPACT_FUNCTION.POLLUTANT_ID.eq(pollutantId))
				//TODO: Add this constrain back in once we have beter requirements.
				//		.and(supportedMetricIds == null ? DSL.noCondition() : HEALTH_IMPACT_FUNCTION.METRIC_ID.in(supportedMetricIds))
						)
				.orderBy(HEALTH_IMPACT_FUNCTION_GROUP.NAME)
				.fetch();

		if(hifGroupRecords.isEmpty()) {
			return CoreApi.getErrorResponseNotFound(request, response);
		}
		
		ObjectMapper mapper = new ObjectMapper();
		ArrayNode groups = mapper.createArrayNode();
		ObjectNode group = null;
		ArrayNode functions = null;
		int currentGroupId = -1;
		
		for(Record r : hifGroupRecords) {
			if(currentGroupId != r.getValue(HEALTH_IMPACT_FUNCTION_GROUP.ID)) {
				currentGroupId = r.getValue(HEALTH_IMPACT_FUNCTION_GROUP.ID);
				group = mapper.createObjectNode();
				group.put("id", currentGroupId);
				group.put("name", r.getValue(HEALTH_IMPACT_FUNCTION_GROUP.NAME));
				group.put("help_text", r.getValue(HEALTH_IMPACT_FUNCTION_GROUP.HELP_TEXT));
				functions = group.putArray("functions");
				groups.add(group);
			}
			
			ObjectNode function = mapper.createObjectNode();
			function.put("id", r.getValue(HEALTH_IMPACT_FUNCTION.ID));
			function.put("health_impact_function_dataset_id",r.getValue(HEALTH_IMPACT_FUNCTION.HEALTH_IMPACT_FUNCTION_DATASET_ID));
			function.put("endpoint_group_id",r.getValue(HEALTH_IMPACT_FUNCTION.ENDPOINT_GROUP_ID));
			function.put("endpoint_id",r.getValue(HEALTH_IMPACT_FUNCTION.ENDPOINT_ID));
			function.put("pollutant_id",r.getValue(HEALTH_IMPACT_FUNCTION.POLLUTANT_ID));
			function.put("metric_id",r.getValue(HEALTH_IMPACT_FUNCTION.METRIC_ID));
			function.put("seasonal_metric_id",r.getValue(HEALTH_IMPACT_FUNCTION.SEASONAL_METRIC_ID));
			function.put("metric_statistic",r.getValue(HEALTH_IMPACT_FUNCTION.METRIC_STATISTIC));
			function.put("author",r.getValue(HEALTH_IMPACT_FUNCTION.AUTHOR));
			function.put("function_year",r.getValue(HEALTH_IMPACT_FUNCTION.FUNCTION_YEAR));
			function.put("location",r.getValue(HEALTH_IMPACT_FUNCTION.LOCATION));
			function.put("other_pollutants",r.getValue(HEALTH_IMPACT_FUNCTION.OTHER_POLLUTANTS));
			function.put("qualifier",r.getValue(HEALTH_IMPACT_FUNCTION.QUALIFIER));
			function.put("reference",r.getValue(HEALTH_IMPACT_FUNCTION.REFERENCE));
			function.put("start_age",r.getValue(HEALTH_IMPACT_FUNCTION.START_AGE));
			function.put("end_age",r.getValue(HEALTH_IMPACT_FUNCTION.END_AGE));
			function.put("function_text",r.getValue(HEALTH_IMPACT_FUNCTION.FUNCTION_TEXT));
			function.put("beta",r.getValue(HEALTH_IMPACT_FUNCTION.BETA));
			function.put("dist_beta",r.getValue(HEALTH_IMPACT_FUNCTION.DIST_BETA));
			function.put("p1_beta",r.getValue(HEALTH_IMPACT_FUNCTION.P1_BETA));
			function.put("p2_beta",r.getValue(HEALTH_IMPACT_FUNCTION.P2_BETA));
			function.put("val_a",r.getValue(HEALTH_IMPACT_FUNCTION.VAL_A));
			function.put("name_a",r.getValue(HEALTH_IMPACT_FUNCTION.NAME_A));
			function.put("val_b",r.getValue(HEALTH_IMPACT_FUNCTION.VAL_B));
			function.put("name_b",r.getValue(HEALTH_IMPACT_FUNCTION.NAME_B));
			function.put("val_c",r.getValue(HEALTH_IMPACT_FUNCTION.VAL_C));
			function.put("name_c",r.getValue(HEALTH_IMPACT_FUNCTION.NAME_C));
			function.put("baseline_function_text",r.getValue(HEALTH_IMPACT_FUNCTION.BASELINE_FUNCTION_TEXT));
			function.put("race_id",r.getValue(HEALTH_IMPACT_FUNCTION.RACE_ID));
			function.put("gender_id",r.getValue(HEALTH_IMPACT_FUNCTION.GENDER_ID));
			function.put("ethnicity_id",r.getValue(HEALTH_IMPACT_FUNCTION.ETHNICITY_ID));
			function.put("start_day",r.getValue(HEALTH_IMPACT_FUNCTION.START_DAY));
			function.put("end_day",r.getValue(HEALTH_IMPACT_FUNCTION.END_DAY));
			function.put("endpoint_group_name",r.getValue("endpoint_group_name",String.class));
			function.put("endpoint_name",r.getValue("endpoint_name", String.class));
			function.put("race_name",r.getValue("race_name", String.class));
			function.put("gender_name",r.getValue("gender_name", String.class));
			function.put("ethnicity_name",r.getValue("ethnicity_name", String.class));
			
			functions.add(function);
			
		}
		
		response.type("application/json");
		return groups;
	}

	/**
	 * @param hifResultDatasetId
	 * @return a health impact function task configuration from a given hif result dataset id.
	 */
	public static HIFTaskConfig getHifTaskConfigFromDb(Integer hifResultDatasetId) {
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());

		HIFTaskConfig hifTaskConfig = new HIFTaskConfig();
		
		HifResultDatasetRecord hifTaskConfigRecord = create
				.selectFrom(HIF_RESULT_DATASET)
				.where(HIF_RESULT_DATASET.ID.eq(hifResultDatasetId))
				.fetchOne();
		
		hifTaskConfig.name = hifTaskConfigRecord.getName();
		hifTaskConfig.popId = hifTaskConfigRecord.getPopulationDatasetId();
		hifTaskConfig.popYear = hifTaskConfigRecord.getPopulationYear();
		hifTaskConfig.aqBaselineId = hifTaskConfigRecord.getBaselineAqLayerId();
		hifTaskConfig.aqScenarioId = hifTaskConfigRecord.getScenarioAqLayerId();
		//TODO: Add code to load the hif details
		
		return hifTaskConfig;
	}

	/**
	 * @param hifResultDatasetId
	 * @return an air quality layer grid definition id from a given hif result dataset id.
	 */
	public static Integer getBaselineGridForHifResults(int hifResultDatasetId) {
		
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());
		
		Record1<Integer> aqId = create
				.select(HIF_RESULT_DATASET.GRID_DEFINITION_ID)
				.from(HIF_RESULT_DATASET)
				.where(HIF_RESULT_DATASET.ID.eq(hifResultDatasetId))
				.fetchOne();

		if(aqId == null) {
			return null;
		}

		return aqId.value1();
	}
	
	/**
	 * @param request 
	 * @param response
	 * @param userProfile
	 * @return JSON representation of a single health impact function.
	 */
	public static Object getHealthImpactFunction(Request request, Response response, Optional<UserProfile> userProfile) {

		Integer id;
		try {
			id = Integer.valueOf(request.params("id"));
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return CoreApi.getErrorResponseInvalidId(request, response);
		}
		
		Result<Record> hifRecords = DSL.using(JooqUtil.getJooqConfiguration())
				.select(HEALTH_IMPACT_FUNCTION.asterisk()
						, ENDPOINT_GROUP.NAME.as("endpoint_group_name")
						, ENDPOINT.NAME.as("endpoint_name")
						, RACE.NAME.as("race_name")
						, GENDER.NAME.as("gender_name")
						, ETHNICITY.NAME.as("ethnicity_name")
						)
				.from(HEALTH_IMPACT_FUNCTION)
				.join(ENDPOINT_GROUP).on(HEALTH_IMPACT_FUNCTION.ENDPOINT_GROUP_ID.eq(ENDPOINT_GROUP.ID))
				.join(ENDPOINT).on(HEALTH_IMPACT_FUNCTION.ENDPOINT_ID.eq(ENDPOINT.ID))
				.join(RACE).on(HEALTH_IMPACT_FUNCTION.RACE_ID.eq(RACE.ID))
				.join(GENDER).on(HEALTH_IMPACT_FUNCTION.GENDER_ID.eq(GENDER.ID))
				.join(ETHNICITY).on(HEALTH_IMPACT_FUNCTION.ETHNICITY_ID.eq(ETHNICITY.ID))
				.where(HEALTH_IMPACT_FUNCTION.ID.eq(id))
				.orderBy(ENDPOINT_GROUP.NAME, ENDPOINT.NAME, HEALTH_IMPACT_FUNCTION.AUTHOR)
				.fetch();
		
		if(hifRecords.isEmpty()) {
			return CoreApi.getErrorResponseNotFound(request, response);
		}

		response.type("application/json");
		return hifRecords.formatJSON(new JSONFormat().header(false).recordFormat(RecordFormat.OBJECT));
	}

	/**
	 * @param hifTaskUuid
	 * @return a status message for the given hif task ("pending", "success", or "failed").
	 */
	public static String getHIFTaskStatus(String hifTaskUuid) {
		TaskCompleteRecord completedTask = DSL.using(JooqUtil.getJooqConfiguration())
				.select(TASK_COMPLETE.asterisk())
				.from(TASK_COMPLETE)
				.where(TASK_COMPLETE.TASK_UUID.eq(hifTaskUuid))
				.fetchOneInto(TASK_COMPLETE);
		
		if(completedTask == null) {
			return "pending";
		} else if(completedTask.getTaskSuccessful()) {
			return "success";
		}
		// We found the completed tasks, but it was not successful
		return "failed";
	}

	/**
	 * @param hifTaskUuid
	 * @return an hif result dataset id.
	 */
	public static Integer getHIFResultDatasetId(String hifTaskUuid) {

		HifResultDatasetRecord hifResultDataset = DSL.using(JooqUtil.getJooqConfiguration())
		.select(HIF_RESULT_DATASET.asterisk())
		.from(HIF_RESULT_DATASET)
		.where(HIF_RESULT_DATASET.TASK_UUID.eq(hifTaskUuid))
		.fetchOneInto(HIF_RESULT_DATASET);
		
		if(hifResultDataset == null) {
			return null;
		}
		return hifResultDataset.getId();
	}

	/**
	 * @param hifResultDatasetId
	 * @param hifIdList
	 * @param incidenceAggregationGrid 
	 * @return a count of the number of hif result records in the given list of hif ids after any aggregation has been applied.
	 */
	public static int getHifResultsRecordCount(Integer hifResultDatasetId, ArrayList<Integer> hifIdList, Integer incidenceAggregationGrid) {

		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());
		
		//If the crosswalk isn't there, create it now
		CrosswalksApi.ensureCrosswalkExists(HIFApi.getBaselineGridForHifResults(hifResultDatasetId), incidenceAggregationGrid);

		Record1<Integer> hifResultCount = create
				.select(DSL.count())
				.from(GET_HIF_RESULTS(
						hifResultDatasetId, 
						hifIdList.toArray(new Integer[0]), 
						incidenceAggregationGrid))
				.fetchOne();
		
		if(hifResultCount == null) {
			return 0;
		}
		return hifResultCount.value1().intValue();
	}

	/**
	 * @param request
	 * @param response
	 * @param userProfile
	 * @return a JSON representation of all hif result datasets.
	 */
	public static Object getHifResultDatasets(Request request, Response response, Optional<UserProfile> userProfile) {
		Result<Record> hifDatasetRecords = DSL.using(JooqUtil.getJooqConfiguration())
				.select(HIF_RESULT_DATASET.asterisk())
				.from(HIF_RESULT_DATASET)
				.orderBy(HIF_RESULT_DATASET.NAME)
				.fetch();
		
		response.type("application/json");
		return hifDatasetRecords.formatJSON(new JSONFormat().header(false).recordFormat(RecordFormat.OBJECT));
	}
	
	/**
	 * @param request
	 * @param response
	 * @param userProfile
	 * @return a JSON representation of the functions in a given hif result dataset.
	 */
	public static Object getHifResultDatasetFunctions(Request request, Response response, Optional<UserProfile> userProfile) {
		String idParam;
		Integer id;
		try {
			idParam = String.valueOf(request.params("id"));
			id = idParam.length() == 36 ? HIFApi.getHIFResultDatasetId(idParam) : Integer.valueOf(idParam);
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return CoreApi.getErrorResponseInvalidId(request, response);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return CoreApi.getErrorResponseInvalidId(request, response);
		}

		Result<Record> hifRecords = DSL.using(JooqUtil.getJooqConfiguration())
				.select(HEALTH_IMPACT_FUNCTION.asterisk()
						, ENDPOINT_GROUP.NAME.as("endpoint_group_name")
						, ENDPOINT.NAME.as("endpoint_name")
						, RACE.NAME.as("race_name")
						, GENDER.NAME.as("gender_name")
						, ETHNICITY.NAME.as("ethnicity_name")
						)
				.from(HEALTH_IMPACT_FUNCTION)
				.join(ENDPOINT_GROUP).on(HEALTH_IMPACT_FUNCTION.ENDPOINT_GROUP_ID.eq(ENDPOINT_GROUP.ID))
				.join(ENDPOINT).on(HEALTH_IMPACT_FUNCTION.ENDPOINT_ID.eq(ENDPOINT.ID))
				.join(RACE).on(HEALTH_IMPACT_FUNCTION.RACE_ID.eq(RACE.ID))
				.join(GENDER).on(HEALTH_IMPACT_FUNCTION.GENDER_ID.eq(GENDER.ID))
				.join(ETHNICITY).on(HEALTH_IMPACT_FUNCTION.ETHNICITY_ID.eq(ETHNICITY.ID))
				.join(HIF_RESULT_FUNCTION_CONFIG).on(HIF_RESULT_FUNCTION_CONFIG.HIF_ID.eq(HEALTH_IMPACT_FUNCTION.ID))
				.where(HIF_RESULT_FUNCTION_CONFIG.HIF_RESULT_DATASET_ID.eq(Integer.valueOf(id)))
				.orderBy(ENDPOINT_GROUP.NAME, ENDPOINT.NAME, HEALTH_IMPACT_FUNCTION.AUTHOR)
				.fetch();
		
		if(hifRecords.isEmpty()) {
			return CoreApi.getErrorResponseNotFound(request, response);
		}
		response.type("application/json");
		return hifRecords.formatJSON(new JSONFormat().header(false).recordFormat(RecordFormat.OBJECT));
	}

	/**
	 * Sets the sort order of the health impact functions.
	 * @param sortBy
	 * @param descending
	 * @param orderFields
	 */
	private static void setHealthImpactFunctionSortOrder(
			String sortBy, Boolean descending, List<OrderField<?>> orderFields) {
		
		if (!"".equals(sortBy)) {
			
			SortOrder sortDirection = SortOrder.ASC;
			Field<?> sortField = null;
			
			sortDirection = descending ? SortOrder.DESC : SortOrder.ASC;
			
			switch (sortBy) {
			case "endpoint_group_name":
				sortField = DSL.field(sortBy, String.class.getName());
				break;

			default:
				sortField = DSL.field(sortBy, String.class.getName());
				break;
			}
			
			orderFields.add(sortField.sort(sortDirection));
			
		} 
	}

	/**
	 * 
	 * @param filterValue
	 * @return a condition object representing a health impact function filter condition.
	 */
	private static Condition buildHealthImpactFunctionFilterCondition(String filterValue) {

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
				searchCondition.or(HEALTH_IMPACT_FUNCTION.AUTHOR
						.containsIgnoreCase(filterValue));

		searchCondition = 
				searchCondition.or(HEALTH_IMPACT_FUNCTION.LOCATION
						.containsIgnoreCase(filterValue));

		searchCondition = 
				searchCondition.or(HEALTH_IMPACT_FUNCTION.REFERENCE
						.containsIgnoreCase(filterValue));
		
		searchCondition = 
				searchCondition.or(HEALTH_IMPACT_FUNCTION.NAME_A
						.containsIgnoreCase(filterValue));

		searchCondition = 
				searchCondition.or(HEALTH_IMPACT_FUNCTION.NAME_B
						.containsIgnoreCase(filterValue));				

		searchCondition = 
				searchCondition.or(HEALTH_IMPACT_FUNCTION.NAME_C
						.containsIgnoreCase(filterValue));

		searchCondition = 
				searchCondition.or(RACE.NAME
						.containsIgnoreCase(filterValue));
		
		searchCondition = 
				searchCondition.or(ETHNICITY.NAME
						.containsIgnoreCase(filterValue));
		
		searchCondition = 
				searchCondition.or(GENDER.NAME
						.containsIgnoreCase(filterValue));

		searchCondition = 
				searchCondition.or(POLLUTANT_METRIC.NAME
						.containsIgnoreCase(filterValue));


		if (null != filterValueAsInteger) {
			searchCondition = 
					searchCondition.or(HEALTH_IMPACT_FUNCTION.HERO_ID.cast(String.class)
							.containsIgnoreCase(filterValue));
		}

		if (null != filterValueAsInteger) {
			searchCondition = 
					searchCondition.or(HEALTH_IMPACT_FUNCTION.FUNCTION_YEAR
							.eq(filterValueAsInteger));
		}

		if (null != filterValueAsInteger) {
			searchCondition = 
					searchCondition.or(HEALTH_IMPACT_FUNCTION.START_AGE
							.eq(filterValueAsInteger));
		}

		if (null != filterValueAsInteger) {
			searchCondition = 
					searchCondition.or(HEALTH_IMPACT_FUNCTION.END_AGE
							.eq(filterValueAsInteger));		
		}

		if (null != filterValueAsInteger) {
			searchCondition = 
					searchCondition.or(HEALTH_IMPACT_FUNCTION.ID
							.eq(filterValueAsInteger));		
		}
		
		filterCondition = filterCondition.and(searchCondition);

		return filterCondition;
	}

}

package gov.epa.bencloud.api;

import static gov.epa.bencloud.server.database.jooq.data.Tables.*;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jooq.DSLContext;
import org.jooq.JSONFormat;
import org.jooq.Result;
import org.jooq.Table;
import org.jooq.JSONFormat.RecordFormat;
import org.jooq.exception.DataAccessException;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Record4;
import org.jooq.Record7;
import org.jooq.impl.DSL;
import org.pac4j.core.profile.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import gov.epa.bencloud.api.model.HIFTaskConfig;
import gov.epa.bencloud.api.model.HIFTaskLog;
import gov.epa.bencloud.api.util.AirQualityUtil;
import gov.epa.bencloud.api.util.HIFUtil;
import gov.epa.bencloud.server.database.JooqUtil;
import gov.epa.bencloud.server.database.jooq.data.tables.records.GetHifResultsRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.HifResultDatasetRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.TaskCompleteRecord;
import gov.epa.bencloud.server.util.ApplicationUtil;
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
			gridId = 47; // HARDCODE GLOBAL OZONE ParameterUtil.getParameterValueAsInteger(request.raw().getParameter("gridId"), 0);
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


		Table<GetHifResultsRecord> hifResultRecords = create.selectFrom(
				GET_HIF_RESULTS(
						id, 
						hifIds == null ? null : hifIds.toArray(new Integer[0]), 
						gridId))
				.asTable("hif_result_records");

		try{
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
				hifResultRecords.field(GET_HIF_RESULTS.PERCENTILES)
				)
				.from(hifResultRecords)
				.join(HEALTH_IMPACT_FUNCTION).on(hifResultRecords.field(GET_HIF_RESULTS.HIF_ID).eq(HEALTH_IMPACT_FUNCTION.ID))
				.join(HIF_RESULT_FUNCTION_CONFIG).on(HIF_RESULT_FUNCTION_CONFIG.HIF_RESULT_DATASET_ID.eq(id).and(HIF_RESULT_FUNCTION_CONFIG.HIF_ID.eq(hifResultRecords.field(GET_HIF_RESULTS.HIF_ID))))
				.join(ENDPOINT).on(ENDPOINT.ID.eq(HEALTH_IMPACT_FUNCTION.ENDPOINT_ID))
				.join(RACE).on(HIF_RESULT_FUNCTION_CONFIG.RACE_ID.eq(RACE.ID))
				.join(ETHNICITY).on(HIF_RESULT_FUNCTION_CONFIG.ETHNICITY_ID.eq(ETHNICITY.ID))
				.join(GENDER).on(HIF_RESULT_FUNCTION_CONFIG.GENDER_ID.eq(GENDER.ID))
				.join(POLLUTANT_METRIC).on(HIF_RESULT_FUNCTION_CONFIG.METRIC_ID.eq(POLLUTANT_METRIC.ID))
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

		for(int i=0; i < gridIds.length; i++) {
			Result<?> hifRecordsClean = null;
			try {
				Table<GetHifResultsRecord> hifResultRecords = create.selectFrom(
					GET_HIF_RESULTS(
							id, 
							null, 
							gridIds[i]))
					.asTable("hif_result_records");
	
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
						hifResultRecords.field(GET_HIF_RESULTS.PERCENTILES)
						)
						.from(hifResultRecords)
						.join(HEALTH_IMPACT_FUNCTION).on(hifResultRecords.field(GET_HIF_RESULTS.HIF_ID).eq(HEALTH_IMPACT_FUNCTION.ID))
						.join(HIF_RESULT_FUNCTION_CONFIG).on(HIF_RESULT_FUNCTION_CONFIG.HIF_RESULT_DATASET_ID.eq(id).and(HIF_RESULT_FUNCTION_CONFIG.HIF_ID.eq(hifResultRecords.field(GET_HIF_RESULTS.HIF_ID))))
						.join(ENDPOINT).on(ENDPOINT.ID.eq(HEALTH_IMPACT_FUNCTION.ENDPOINT_ID))
						.join(RACE).on(HIF_RESULT_FUNCTION_CONFIG.RACE_ID.eq(RACE.ID))
						.join(ETHNICITY).on(HIF_RESULT_FUNCTION_CONFIG.ETHNICITY_ID.eq(ETHNICITY.ID))
						.join(GENDER).on(HIF_RESULT_FUNCTION_CONFIG.GENDER_ID.eq(GENDER.ID))
						.join(POLLUTANT_METRIC).on(HIF_RESULT_FUNCTION_CONFIG.METRIC_ID.eq(POLLUTANT_METRIC.ID))
						.leftJoin(SEASONAL_METRIC).on(HIF_RESULT_FUNCTION_CONFIG.SEASONAL_METRIC_ID.eq(SEASONAL_METRIC.ID))
						.join(STATISTIC_TYPE).on(HIF_RESULT_FUNCTION_CONFIG.METRIC_STATISTIC.eq(STATISTIC_TYPE.ID))
						.fetch();
				
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
				//Remove percentiles by keeping all other fields
				hifRecordsClean = hifRecords.into(hifRecords.fields(0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27));
			} catch(DataAccessException e) {
				e.printStackTrace();
				response.status(400);
				return;
			}
			
			try {
					zipStream.putNextEntry(new ZipEntry(taskFileName + "_" + ApplicationUtil.replaceNonValidCharacters(GridDefinitionApi.getGridDefinitionName(gridIds[i])) + ".csv"));
					hifRecordsClean.formatCSV(zipStream);
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
	 * @return hif results for a given hifId.
	 */
	public static Result<Record7<Long, Integer, Integer, Integer, Integer, Double, Double[]>> getHifResultsForValuation(Integer id, Integer hifId) {
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());
		Result<Record7<Long, Integer, Integer, Integer, Integer, Double, Double[]>> hifRecords = create.select(
				HIF_RESULT.GRID_CELL_ID,
				HIF_RESULT.GRID_COL,
				HIF_RESULT.GRID_ROW,
				HIF_RESULT.HIF_ID,
				HEALTH_IMPACT_FUNCTION.ENDPOINT_GROUP_ID,
				HIF_RESULT.RESULT,
				HIF_RESULT.PERCENTILES
				)
				.from(HIF_RESULT)
				.join(HIF_RESULT_FUNCTION_CONFIG).on(HIF_RESULT_FUNCTION_CONFIG.HIF_RESULT_DATASET_ID.eq(HIF_RESULT.HIF_RESULT_DATASET_ID).and(HIF_RESULT_FUNCTION_CONFIG.HIF_ID.eq(HIF_RESULT.HIF_ID)))
				.join(HEALTH_IMPACT_FUNCTION).on(HEALTH_IMPACT_FUNCTION.ID.eq(HIF_RESULT.HIF_ID))
				.where(HIF_RESULT.HIF_RESULT_DATASET_ID.eq(id)
						.and(HIF_RESULT.HIF_ID.eq(hifId)))
				.orderBy(HIF_RESULT.GRID_COL, HIF_RESULT.GRID_ROW).fetch();

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
				.orderBy(ENDPOINT_GROUP.NAME, ENDPOINT.NAME, HEALTH_IMPACT_FUNCTION.AUTHOR)
				.fetch();
		
		response.type("application/json");
		return hifRecords.formatJSON(new JSONFormat().header(false).recordFormat(RecordFormat.OBJECT));
	}

	/**
	 * 
	 * @param request
	 * @param response
	 * @param userProfile
	 * @return JSON representation of all hif groups for a given pollutant (pollutant id is a request parameter).
	 */
	public static Object getAllHifGroups(Request request, Response response, Optional<UserProfile> userProfile) {
			
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
				.where(pollutantId == 0 ? DSL.noCondition() : HEALTH_IMPACT_FUNCTION.POLLUTANT_ID.eq(pollutantId))
				.groupBy(HEALTH_IMPACT_FUNCTION_GROUP.NAME
						, HEALTH_IMPACT_FUNCTION_GROUP.ID)
				.orderBy(HEALTH_IMPACT_FUNCTION_GROUP.NAME.desc())
				.fetch();
		
		response.type("application/json");
		return hifGroupRecords.formatJSON(new JSONFormat().header(false).recordFormat(RecordFormat.OBJECT));
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
			function.put("variable_dataset_id",r.getValue(HEALTH_IMPACT_FUNCTION.VARIABLE_DATASET_ID));
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
			
			//This will select the most appropriate incidence/prevalence dataset and year based on user selection and function definition
			HIFUtil.setIncidencePrevalence(function, popYear, defaultIncidencePrevalenceDataset,r.getValue(HEALTH_IMPACT_FUNCTION.INCIDENCE_DATASET_ID), r.getValue(HEALTH_IMPACT_FUNCTION.PREVALENCE_DATASET_ID), userPrefered);
			
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
				.select(AIR_QUALITY_LAYER.GRID_DEFINITION_ID)
				.from(HIF_RESULT_DATASET)
				.join(AIR_QUALITY_LAYER).on(AIR_QUALITY_LAYER.ID.eq(HIF_RESULT_DATASET.BASELINE_AQ_LAYER_ID))
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
	 * @return a count of the number of hif result records in the given list of hif ids.
	 */
	public static int getHifResultsRecordCount(Integer hifResultDatasetId, ArrayList<Integer> hifIdList) {
		Record1<Integer> hifResultCount = DSL.using(JooqUtil.getJooqConfiguration())
		.select(DSL.count())
		.from(HIF_RESULT)
		.where(HIF_RESULT.HIF_RESULT_DATASET_ID.eq(hifResultDatasetId)
				.and(HIF_RESULT.HIF_ID.in(hifIdList)))
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

}

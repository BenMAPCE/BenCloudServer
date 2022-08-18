package gov.epa.bencloud.api;

import static gov.epa.bencloud.server.database.jooq.data.Tables.*;

import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.jooq.Cursor;
import org.jooq.DSLContext;
import org.jooq.JSONFormat;
import org.jooq.Result;
import org.jooq.Table;
import org.jooq.exception.DataAccessException;
import org.jooq.JSONFormat.RecordFormat;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Record21;
import org.jooq.impl.DSL;
import org.pac4j.core.profile.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.epa.bencloud.api.model.HIFTaskLog;
import gov.epa.bencloud.api.model.ValuationTaskConfig;
import gov.epa.bencloud.api.model.ValuationTaskLog;
import gov.epa.bencloud.api.util.HIFUtil;
import gov.epa.bencloud.api.util.ValuationUtil;
import gov.epa.bencloud.server.database.JooqUtil;
import gov.epa.bencloud.server.database.jooq.data.tables.records.GetValuationResultsRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.ValuationResultDatasetRecord;
import gov.epa.bencloud.server.util.ApplicationUtil;
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
		
		try {
			if(gridId == 0) {
				gridId = ValuationApi.getBaselineGridForValuationResults(id).intValue();
			}
		} catch (NullPointerException e) {
			e.printStackTrace();
			response.status(400);
			return;
		}
		
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());

		Table<GetValuationResultsRecord> vfResultRecords = create.selectFrom(
				GET_VALUATION_RESULTS(
						id, 
						hifIds == null ? null : hifIds.toArray(new Integer[0]),
						vfIds == null ? null : vfIds.toArray(new Integer[0]),
								gridId))
				.asTable("vf_result_records");
		
		Cursor<Record21<Integer, Integer, String, String, String, Integer, String, String, String, String, String, String, String, Integer, Integer, Double, Double, Double, Double, Double, Double>> vfRecords;
		try {
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
				vfResultRecords.field(GET_VALUATION_RESULTS.PCT_97_5)
				)
				.from(vfResultRecords)
				.join(VALUATION_RESULT_FUNCTION_CONFIG)
				.on(VALUATION_RESULT_FUNCTION_CONFIG.VALUATION_RESULT_DATASET_ID.eq(id)
						.and(VALUATION_RESULT_FUNCTION_CONFIG.HIF_ID.eq(vfResultRecords.field(GET_VALUATION_RESULTS.HIF_ID)))
						.and(VALUATION_RESULT_FUNCTION_CONFIG.VF_ID.eq(vfResultRecords.field(GET_VALUATION_RESULTS.VF_ID))))
				.join(VALUATION_RESULT_DATASET)
				.on(VALUATION_RESULT_FUNCTION_CONFIG.VALUATION_RESULT_DATASET_ID.eq(VALUATION_RESULT_DATASET.ID))
				.join(HIF_RESULT_FUNCTION_CONFIG)
				.on(VALUATION_RESULT_DATASET.HIF_RESULT_DATASET_ID.eq(HIF_RESULT_FUNCTION_CONFIG.HIF_RESULT_DATASET_ID)
						.and(VALUATION_RESULT_FUNCTION_CONFIG.HIF_ID.eq(HIF_RESULT_FUNCTION_CONFIG.HIF_ID)))
				.join(VALUATION_FUNCTION).on(VALUATION_FUNCTION.ID.eq(vfResultRecords.field(GET_VALUATION_RESULTS.VF_ID)))
				.join(HEALTH_IMPACT_FUNCTION).on(vfResultRecords.field(GET_VALUATION_RESULTS.HIF_ID).eq(HEALTH_IMPACT_FUNCTION.ID))
				.join(ENDPOINT).on(ENDPOINT.ID.eq(VALUATION_FUNCTION.ENDPOINT_ID))
				.join(RACE).on(HIF_RESULT_FUNCTION_CONFIG.RACE_ID.eq(RACE.ID))
				.join(ETHNICITY).on(HIF_RESULT_FUNCTION_CONFIG.ETHNICITY_ID.eq(ETHNICITY.ID))
				.join(GENDER).on(HIF_RESULT_FUNCTION_CONFIG.GENDER_ID.eq(GENDER.ID))
				.join(POLLUTANT_METRIC).on(HIF_RESULT_FUNCTION_CONFIG.METRIC_ID.eq(POLLUTANT_METRIC.ID))
				.leftJoin(SEASONAL_METRIC).on(HIF_RESULT_FUNCTION_CONFIG.SEASONAL_METRIC_ID.eq(SEASONAL_METRIC.ID))
				.join(STATISTIC_TYPE).on(HIF_RESULT_FUNCTION_CONFIG.METRIC_STATISTIC.eq(STATISTIC_TYPE.ID))
				.offset((page * rowsPerPage) - rowsPerPage)
				.limit(rowsPerPage)
				//.fetchSize(100000) //JOOQ doesn't like this when Postgres is in autoCommmit mode
				.fetchLazy();
		} catch (DataAccessException e) {
			e.printStackTrace();
			response.status(400);
			return;
		}
		
		try {
			response.type("application/json");
			vfRecords.formatJSON(response.raw().getWriter(), new JSONFormat().header(false).recordFormat(RecordFormat.OBJECT));
		} catch (Exception e) {
			log.error("Error in Valuation export", e);
		} finally {
			if(vfRecords != null && !vfRecords.isClosed()) {
				vfRecords.close();
			}
		}
		
	}
	
	/**
	 * Exports all valuation results to a zip file.
	 * @param request
	 * @param response
	 * @param userProfile
	 */
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
			Result<Record21<Integer, Integer, String, String, String, Integer, String, String, String, String, String, String, String, Integer, Integer, Double, Double, Double, Double, Double, Double>> vfRecords;
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
						VALUATION_FUNCTION.START_AGE,
						VALUATION_FUNCTION.END_AGE,
						vfResultRecords.field(GET_VALUATION_RESULTS.POINT_ESTIMATE),
						vfResultRecords.field(GET_VALUATION_RESULTS.MEAN),
						vfResultRecords.field(GET_VALUATION_RESULTS.STANDARD_DEV).as("standard_deviation"),
						vfResultRecords.field(GET_VALUATION_RESULTS.VARIANCE).as("variance"),
						vfResultRecords.field(GET_VALUATION_RESULTS.PCT_2_5),
						vfResultRecords.field(GET_VALUATION_RESULTS.PCT_97_5)
						)
						.from(vfResultRecords)
						.join(VALUATION_RESULT_FUNCTION_CONFIG)
						.on(VALUATION_RESULT_FUNCTION_CONFIG.VALUATION_RESULT_DATASET_ID.eq(id)
								.and(VALUATION_RESULT_FUNCTION_CONFIG.HIF_ID.eq(vfResultRecords.field(GET_VALUATION_RESULTS.HIF_ID)))
								.and(VALUATION_RESULT_FUNCTION_CONFIG.VF_ID.eq(vfResultRecords.field(GET_VALUATION_RESULTS.VF_ID))))
						.join(VALUATION_RESULT_DATASET)
						.on(VALUATION_RESULT_FUNCTION_CONFIG.VALUATION_RESULT_DATASET_ID.eq(VALUATION_RESULT_DATASET.ID))
						.join(HIF_RESULT_FUNCTION_CONFIG)
						.on(VALUATION_RESULT_DATASET.HIF_RESULT_DATASET_ID.eq(HIF_RESULT_FUNCTION_CONFIG.HIF_RESULT_DATASET_ID)
								.and(VALUATION_RESULT_FUNCTION_CONFIG.HIF_ID.eq(HIF_RESULT_FUNCTION_CONFIG.HIF_ID)))
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
			} catch (DataAccessException e) {
				e.printStackTrace();
				response.status(400);
				return;
			}
			try {
				zipStream.putNextEntry(new ZipEntry(taskFileName + "_" + ApplicationUtil.replaceNonValidCharacters(GridDefinitionApi.getGridDefinitionName(gridIds[i])) + ".csv"));
				vfRecords.formatCSV(zipStream);
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
		
		Record1<Integer> aqId = create
				.select(AIR_QUALITY_LAYER.GRID_DEFINITION_ID)
				.from(VALUATION_RESULT_DATASET)
				.join(HIF_RESULT_DATASET).on(VALUATION_RESULT_DATASET.HIF_RESULT_DATASET_ID.eq(HIF_RESULT_DATASET.ID))
				.join(AIR_QUALITY_LAYER).on(AIR_QUALITY_LAYER.ID.eq(HIF_RESULT_DATASET.BASELINE_AQ_LAYER_ID))
				.where(VALUATION_RESULT_DATASET.ID.eq(valuationResultDatasetId))
				.fetchOne();

		if(aqId == null) {
			return null;
		}
		return aqId.value1();
	}
}

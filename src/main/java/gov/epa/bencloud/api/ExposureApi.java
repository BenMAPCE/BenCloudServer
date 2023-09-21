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
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.JSONFormat;
import org.jooq.Result;
import org.jooq.Table;
import org.jooq.JSONFormat.RecordFormat;
import org.jooq.exception.DataAccessException;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Record19;
import org.jooq.Record4;
import org.jooq.Record7;
import org.jooq.impl.DSL;
import org.pac4j.core.profile.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import gov.epa.bencloud.api.model.ExposureTaskConfig;
import gov.epa.bencloud.api.model.ExposureTaskLog;
import gov.epa.bencloud.api.model.HIFTaskConfig;
import gov.epa.bencloud.api.model.HIFTaskLog;
import gov.epa.bencloud.api.util.AirQualityUtil;
import gov.epa.bencloud.api.util.ApiUtil;
import gov.epa.bencloud.api.util.ExposureUtil;
import gov.epa.bencloud.api.util.HIFUtil;
import gov.epa.bencloud.server.database.JooqUtil;
import gov.epa.bencloud.server.database.jooq.data.tables.records.ExposureResultDatasetRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.GetExposureResultsRecord;
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
public class ExposureApi {
	private static final Logger log = LoggerFactory.getLogger(ExposureApi.class);

	/**
	 * 
	 * @param request
	 * @param response
	 * @param userProfile
	 * @return JSON representation of all exposure function groups.
	 */
	public static Object getAllExposureGroups(Request request, Response response, Optional<UserProfile> userProfile) {

		Result<Record4<String, Integer, String, Integer[]>> exposureGroupRecords = DSL.using(JooqUtil.getJooqConfiguration())
				.select(EXPOSURE_FUNCTION_GROUP.NAME
						, EXPOSURE_FUNCTION_GROUP.ID
						, EXPOSURE_FUNCTION_GROUP.HELP_TEXT
						, DSL.arrayAggDistinct(EXPOSURE_FUNCTION_GROUP_MEMBER.EXPOSURE_FUNCTION_ID).as("functions")
						)
				.from(EXPOSURE_FUNCTION_GROUP)
				.join(EXPOSURE_FUNCTION_GROUP_MEMBER).on(EXPOSURE_FUNCTION_GROUP.ID.eq(EXPOSURE_FUNCTION_GROUP_MEMBER.EXPOSURE_FUNCTION_GROUP_ID))
				.join(EXPOSURE_FUNCTION).on(EXPOSURE_FUNCTION.ID.eq(EXPOSURE_FUNCTION_GROUP_MEMBER.EXPOSURE_FUNCTION_ID))
				.groupBy(EXPOSURE_FUNCTION_GROUP.NAME
						, EXPOSURE_FUNCTION_GROUP.ID)
				.orderBy(EXPOSURE_FUNCTION_GROUP.NAME)
				.fetch();
		
		response.type("application/json");
		return exposureGroupRecords.formatJSON(new JSONFormat().header(false).recordFormat(RecordFormat.OBJECT));
	}

	/**
	 * 
	 * @param request
	 * @param response
	 * @param userProfile
	 * @return selected exposure group (exposure group ids is a request parameter).
	 * 
	 */
	public static Object getSelectedExposureGroups(Request request, Response response, Optional<UserProfile> userProfile) {
		
		String idsParam;
		List<Integer> ids;

		try{
			idsParam = String.valueOf(request.params("ids").replace(" ", ""));
			ids = Stream.of(idsParam.split(",")).mapToInt(Integer::parseInt).boxed().collect(Collectors.toList());
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return CoreApi.getErrorResponseInvalidId(request, response);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return CoreApi.getErrorResponseInvalidId(request, response);
		}
		
		
		List<Integer> supportedMetricIds = null; 
		
		Result<Record> exposureGroupRecords = DSL.using(JooqUtil.getJooqConfiguration())
				.select(EXPOSURE_FUNCTION_GROUP.NAME
						, EXPOSURE_FUNCTION_GROUP.ID
						, EXPOSURE_FUNCTION_GROUP.HELP_TEXT
						, EXPOSURE_FUNCTION.asterisk()
						, RACE.NAME.as("race_name")
						, GENDER.NAME.as("gender_name")
						, ETHNICITY.NAME.as("ethnicity_name")
						, VARIABLE_ENTRY.NAME.as("variable_name")
						)
				.from(EXPOSURE_FUNCTION_GROUP)
				.join(EXPOSURE_FUNCTION_GROUP_MEMBER).on(EXPOSURE_FUNCTION_GROUP.ID.eq(EXPOSURE_FUNCTION_GROUP_MEMBER.EXPOSURE_FUNCTION_GROUP_ID))
				.join(EXPOSURE_FUNCTION).on(EXPOSURE_FUNCTION_GROUP_MEMBER.EXPOSURE_FUNCTION_ID.eq(EXPOSURE_FUNCTION.ID))
				.join(RACE).on(EXPOSURE_FUNCTION.RACE_ID.eq(RACE.ID))
				.join(GENDER).on(EXPOSURE_FUNCTION.GENDER_ID.eq(GENDER.ID))
				.join(ETHNICITY).on(EXPOSURE_FUNCTION.ETHNICITY_ID.eq(ETHNICITY.ID))
				.leftJoin(VARIABLE_ENTRY).on(EXPOSURE_FUNCTION.VARIABLE_ID.eq(VARIABLE_ENTRY.ID))
				.where(EXPOSURE_FUNCTION_GROUP.ID.in(ids))
				.orderBy(EXPOSURE_FUNCTION_GROUP.NAME)
				.fetch();

		if(exposureGroupRecords.isEmpty()) {
			return CoreApi.getErrorResponseNotFound(request, response);
		}
		
		ObjectMapper mapper = new ObjectMapper();
		ArrayNode groups = mapper.createArrayNode();
		ObjectNode group = null;
		ArrayNode functions = null;
		int currentGroupId = -1;
		
		for(Record r : exposureGroupRecords) {
			if(currentGroupId != r.getValue(EXPOSURE_FUNCTION_GROUP.ID)) {
				currentGroupId = r.getValue(EXPOSURE_FUNCTION_GROUP.ID);
				group = mapper.createObjectNode();
				group.put("id", currentGroupId);
				group.put("name", r.getValue(EXPOSURE_FUNCTION_GROUP.NAME));
				group.put("help_text", r.getValue(EXPOSURE_FUNCTION_GROUP.HELP_TEXT));
				functions = group.putArray("functions");
				groups.add(group);
			}
			
			ObjectNode function = mapper.createObjectNode();
			function.put("id", r.getValue(EXPOSURE_FUNCTION.ID));
			function.put("exposure_dataset_id",r.getValue(EXPOSURE_FUNCTION.EXPOSURE_DATASET_ID));
			function.put("population_group",r.getValue(EXPOSURE_FUNCTION.POPULATION_GROUP));
			function.put("start_age",r.getValue(EXPOSURE_FUNCTION.START_AGE));
			function.put("end_age",r.getValue(EXPOSURE_FUNCTION.END_AGE));
			function.put("function_text",r.getValue(EXPOSURE_FUNCTION.FUNCTION_TEXT));
			function.put("variable_id",r.getValue(EXPOSURE_FUNCTION.VARIABLE_ID));
			function.put("race_id",r.getValue(EXPOSURE_FUNCTION.RACE_ID));
			function.put("gender_id",r.getValue(EXPOSURE_FUNCTION.GENDER_ID));
			function.put("ethnicity_id",r.getValue(EXPOSURE_FUNCTION.ETHNICITY_ID));
			function.put("variable_name",r.getValue("variable_name", String.class));
			function.put("race_name",r.getValue("race_name", String.class));
			function.put("gender_name",r.getValue("gender_name", String.class));
			function.put("ethnicity_name",r.getValue("ethnicity_name", String.class));
						
			functions.add(function);
			
		}
		
		response.type("application/json");
		return groups;
	}

	/**
	 * Gets the exposure results and stores them in the response parameters.
	 * Request contains the exposure result dataset id.
	 * @param request
	 * @param response
	 * @param userProfile
	 */
	public static void getExposureResultContents(Request request, Response response, Optional<UserProfile> userProfile) {
		
		 //*  :id (exposure function results dataset id (can also support task id))
		 //*  gridId= (aggregate the results to another grid definition)
		 //*  efId= (filter results to those from one or more functions via comma delimited list)
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
			id = idParam.length() == 36 ? ExposureApi.getExposureResultDatasetId(idParam) : Integer.valueOf(idParam);
		} catch (NumberFormatException e) {
			e.printStackTrace();
			CoreApi.getErrorResponseInvalidId(request, response);
			return;
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			CoreApi.getErrorResponseInvalidId(request, response);
			return;
		}
			
		String efIdsParam;
		int gridId;
		int page;
		int rowsPerPage;
		String sortBy;
		boolean descending;
		String filter;

		try {
			efIdsParam = ParameterUtil.getParameterValueAsString(request.raw().getParameter("efId"), "");
			gridId = ParameterUtil.getParameterValueAsInteger(request.raw().getParameter("gridId"), 20);
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
		List<Integer> efIds = (efIdsParam == null || efIdsParam.equals("")) ? null : Stream.of(efIdsParam.split(",")).mapToInt(Integer::parseInt).boxed().collect(Collectors.toList());
		
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());

		Table<GetExposureResultsRecord> efResultRecords = create.selectFrom(
				GET_EXPOSURE_RESULTS(
						id, 
						efIds == null ? null : efIds.toArray(new Integer[0]), 
						gridId))
				.asTable("ef_result_records");

		try{
			Result<Record19<Integer, Integer, String, Integer, Integer, String, String, String, String, Double, Double, Double, Double, Double, Double, Double, Double, String, String>> efRecords = create.select(
				efResultRecords.field(GET_EXPOSURE_RESULTS.GRID_COL).as("column"),
				efResultRecords.field(GET_EXPOSURE_RESULTS.GRID_ROW).as("row"),
				EXPOSURE_FUNCTION.POPULATION_GROUP,
				EXPOSURE_FUNCTION.START_AGE,
				EXPOSURE_FUNCTION.END_AGE,
				RACE.NAME.as("race"),
				ETHNICITY.NAME.as("ethnicity"),
				GENDER.NAME.as("gender"),
				VARIABLE_ENTRY.NAME.as("variable"),
				efResultRecords.field(GET_EXPOSURE_RESULTS.DELTA_AQ),
				efResultRecords.field(GET_EXPOSURE_RESULTS.BASELINE_AQ),
				efResultRecords.field(GET_EXPOSURE_RESULTS.SCENARIO_AQ),
				DSL.when(efResultRecords.field(GET_EXPOSURE_RESULTS.BASELINE_AQ).eq(0.0), 0.0)
					.otherwise(efResultRecords.field(GET_EXPOSURE_RESULTS.DELTA_AQ).div(efResultRecords.field(GET_EXPOSURE_RESULTS.BASELINE_AQ)).times(100.0)).as("delta_aq_percent"),
				efResultRecords.field(GET_EXPOSURE_RESULTS.RESULT).as("result"),
				efResultRecords.field(GET_EXPOSURE_RESULTS.SUBGROUP_POPULATION),
				efResultRecords.field(GET_EXPOSURE_RESULTS.ALL_POPULATION),
				DSL.when(efResultRecords.field(GET_EXPOSURE_RESULTS.ALL_POPULATION).eq(0.0), 0.0)
					.otherwise(efResultRecords.field(GET_EXPOSURE_RESULTS.SUBGROUP_POPULATION).div(efResultRecords.field(GET_EXPOSURE_RESULTS.ALL_POPULATION)).times(100.0)).as("percent_of_population"),
				DSL.val(null, String.class).as("formatted_results_2sf"),
				DSL.val(null, String.class).as("formatted_results_3sf")
				)
				.from(efResultRecords)
				.join(EXPOSURE_FUNCTION).on(efResultRecords.field(GET_EXPOSURE_RESULTS.EXPOSURE_FUNCTION_ID).eq(EXPOSURE_FUNCTION.ID))
				//TODO: Change this join to use the function instance id?
				.join(EXPOSURE_RESULT_FUNCTION_CONFIG).on(EXPOSURE_RESULT_FUNCTION_CONFIG.EXPOSURE_RESULT_DATASET_ID.eq(id).and(EXPOSURE_RESULT_FUNCTION_CONFIG.EXPOSURE_FUNCTION_ID.eq(efResultRecords.field(GET_EXPOSURE_RESULTS.EXPOSURE_FUNCTION_ID))))
				.join(RACE).on(EXPOSURE_RESULT_FUNCTION_CONFIG.RACE_ID.eq(RACE.ID))
				.join(ETHNICITY).on(EXPOSURE_RESULT_FUNCTION_CONFIG.ETHNICITY_ID.eq(ETHNICITY.ID))
				.join(GENDER).on(EXPOSURE_RESULT_FUNCTION_CONFIG.GENDER_ID.eq(GENDER.ID))
				.leftJoin(VARIABLE_ENTRY).on(EXPOSURE_RESULT_FUNCTION_CONFIG.VARIABLE_ID.eq(VARIABLE_ENTRY.ID))
				.offset((page * rowsPerPage) - rowsPerPage)
				.limit(rowsPerPage)
				.fetch();
			
			
			if(efRecords.isEmpty()) {
				CoreApi.getErrorResponseNotFound(request, response);
				return;
			}

			for (Record res : efRecords) {
				res.setValue(DSL.field("formatted_results_2sf", String.class), 
								ApiUtil.getValueSigFigs(res.get("result", Double.class), 2));
				res.setValue(DSL.field("formatted_results_3sf", String.class), 
								ApiUtil.getValueSigFigs(res.get("result", Double.class), 3));
			}
		
			response.type("application/json");
			efRecords.formatJSON(response.raw().getWriter(),
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
	 * Exports exposure results to a zip file.
	 * @param request
	 * @param response
	 * @param userProfile
	 */
	public static void getExposureResultExport(Request request, Response response, Optional<UserProfile> userProfile) {
		 //*  :id (exposure function results dataset id (can also support task id))
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
			id = idParam.length() == 36 ? ExposureApi.getExposureResultDatasetId(idParam) : Integer.valueOf(idParam);
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
				gridIds = new int[] {ExposureApi.getBaselineGridForExposureResults(id).intValue()};
			}
		} catch (NullPointerException e) {
			e.printStackTrace();
			response.status(400);
			return;
		}
		

		String taskFileName = ApplicationUtil.replaceNonValidCharacters(ExposureApi.getExposureTaskConfigFromDb(id).name);
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
			Result<?> efRecordsClean = null;
			try {
				Table<GetExposureResultsRecord> efResultRecords = create.selectFrom(
					GET_EXPOSURE_RESULTS(
							id, 
							null, 
							gridIds[i]))
					.asTable("ef_result_records");
				
				Result<Record19<Integer, Integer, String, Integer, Integer, String, String, String, String, Double, Double, Double, Double, Double, Double, Double, Double, String, String>> efRecords = create.select(
						efResultRecords.field(GET_EXPOSURE_RESULTS.GRID_COL).as("column"),
						efResultRecords.field(GET_EXPOSURE_RESULTS.GRID_ROW).as("row"),
						EXPOSURE_FUNCTION.POPULATION_GROUP,
						EXPOSURE_RESULT_FUNCTION_CONFIG.START_AGE,
						EXPOSURE_RESULT_FUNCTION_CONFIG.END_AGE,
						RACE.NAME.as("race"),
						ETHNICITY.NAME.as("ethnicity"),
						GENDER.NAME.as("gender"),
						VARIABLE_ENTRY.NAME.as("variable"),
						efResultRecords.field(GET_EXPOSURE_RESULTS.DELTA_AQ),
						efResultRecords.field(GET_EXPOSURE_RESULTS.BASELINE_AQ),
						efResultRecords.field(GET_EXPOSURE_RESULTS.SCENARIO_AQ),
						DSL.when(efResultRecords.field(GET_EXPOSURE_RESULTS.BASELINE_AQ).eq(0.0), 0.0)
							.otherwise(efResultRecords.field(GET_EXPOSURE_RESULTS.DELTA_AQ).div(efResultRecords.field(GET_EXPOSURE_RESULTS.BASELINE_AQ)).times(100.0)).as("delta_aq_percent"),
						efResultRecords.field(GET_EXPOSURE_RESULTS.RESULT).as("result"),
						efResultRecords.field(GET_EXPOSURE_RESULTS.SUBGROUP_POPULATION),
						efResultRecords.field(GET_EXPOSURE_RESULTS.ALL_POPULATION),
						DSL.when(efResultRecords.field(GET_EXPOSURE_RESULTS.ALL_POPULATION).eq(0.0), 0.0)
							.otherwise(efResultRecords.field(GET_EXPOSURE_RESULTS.SUBGROUP_POPULATION).div(efResultRecords.field(GET_EXPOSURE_RESULTS.ALL_POPULATION)).times(100.0)).as("percent_of_population"),
						DSL.val(null, String.class).as("formatted_results_2sf"),
						DSL.val(null, String.class).as("formatted_results_3sf")
						)
						.from(efResultRecords)
						.join(EXPOSURE_FUNCTION).on(efResultRecords.field(GET_EXPOSURE_RESULTS.EXPOSURE_FUNCTION_ID).eq(EXPOSURE_FUNCTION.ID))
						.join(EXPOSURE_RESULT_FUNCTION_CONFIG).on(EXPOSURE_RESULT_FUNCTION_CONFIG.EXPOSURE_RESULT_DATASET_ID.eq(id).and(EXPOSURE_RESULT_FUNCTION_CONFIG.EXPOSURE_FUNCTION_ID.eq(efResultRecords.field(GET_EXPOSURE_RESULTS.EXPOSURE_FUNCTION_ID))))
						.join(RACE).on(EXPOSURE_RESULT_FUNCTION_CONFIG.RACE_ID.eq(RACE.ID))
						.join(ETHNICITY).on(EXPOSURE_RESULT_FUNCTION_CONFIG.ETHNICITY_ID.eq(ETHNICITY.ID))
						.join(GENDER).on(EXPOSURE_RESULT_FUNCTION_CONFIG.GENDER_ID.eq(GENDER.ID))
						.leftJoin(VARIABLE_ENTRY).on(EXPOSURE_RESULT_FUNCTION_CONFIG.VARIABLE_ID.eq(VARIABLE_ENTRY.ID))

						.fetch();

				for (Record res : efRecords) {
					res.setValue(DSL.field("formatted_results_2sf", String.class), 
									ApiUtil.getValueSigFigs(res.get("result", Double.class), 2));
					res.setValue(DSL.field("formatted_results_3sf", String.class), 
									ApiUtil.getValueSigFigs(res.get("result", Double.class), 3));
				}

				efRecordsClean = efRecords; 
			} catch(DataAccessException e) {
				e.printStackTrace();
				response.status(400);
				return;
			}
			
			try {
				
					zipStream.putNextEntry(new ZipEntry(taskFileName + "_" + ApplicationUtil.replaceNonValidCharacters(GridDefinitionApi.getGridDefinitionName(gridIds[i])) + ".csv"));
					log.info("Before formatCSV");
					efRecordsClean.formatCSV(zipStream);
					log.info("After formatCSV");
					zipStream.closeEntry();
					
			} catch (Exception e) {
				log.error("Error creating export file", e);
			} finally {

			}
		}
		
		try {
			zipStream.putNextEntry(new ZipEntry(taskFileName + "_TaskLog.txt"));
			ExposureTaskLog efTaskLog = ExposureUtil.getTaskLog(id);
			zipStream.write(efTaskLog.toString(userProfile).getBytes());
			zipStream.closeEntry();
			
			zipStream.close();
			responseOutputStream.flush();
		} catch (Exception e) {
			log.error("Error writing task log, closing and flushing export", e);
		}

		
	}
	/**
	 * @param efTaskUuid
	 * @return an exposure result dataset id.
	 */
	public static Integer getExposureResultDatasetId(String efTaskUuid) {

		ExposureResultDatasetRecord efResultDataset = DSL.using(JooqUtil.getJooqConfiguration())
		.select(EXPOSURE_RESULT_DATASET.asterisk())
		.from(EXPOSURE_RESULT_DATASET)
		.where(EXPOSURE_RESULT_DATASET.TASK_UUID.eq(efTaskUuid))
		.fetchOneInto(EXPOSURE_RESULT_DATASET);
		
		if(efResultDataset == null) {
			return null;
		}
		return efResultDataset.getId();
	}
	
	/**
	 * @param efResultDatasetId
	 * @return an air quality layer grid definition id from a given exposure result dataset id.
	 */
	public static Integer getBaselineGridForExposureResults(int efResultDatasetId) {
		
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());
		
		Record1<Integer> aqId = create
				.select(EXPOSURE_RESULT_DATASET.GRID_DEFINITION_ID)
				.from(EXPOSURE_RESULT_DATASET)
				.where(EXPOSURE_RESULT_DATASET.ID.eq(efResultDatasetId))
				.fetchOne();

		if(aqId == null) {
			return null;
		}

		return aqId.value1();
	}
	
	/**
	 * @param efResultDatasetId
	 * @return a exposure function task configuration from a given exposure result dataset id.
	 */
	public static ExposureTaskConfig getExposureTaskConfigFromDb(Integer efResultDatasetId) {
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());

		ExposureTaskConfig exposureTaskConfig = new ExposureTaskConfig();
		
		ExposureResultDatasetRecord efTaskConfigRecord = create
				.selectFrom(EXPOSURE_RESULT_DATASET)
				.where(EXPOSURE_RESULT_DATASET.ID.eq(efResultDatasetId))
				.fetchOne();
		
		exposureTaskConfig.name = efTaskConfigRecord.getName();
		exposureTaskConfig.popId = efTaskConfigRecord.getPopulationDatasetId();
		exposureTaskConfig.popYear = efTaskConfigRecord.getPopulationYear();
		exposureTaskConfig.aqBaselineId = efTaskConfigRecord.getBaselineAqLayerId();
		exposureTaskConfig.aqScenarioId = efTaskConfigRecord.getScenarioAqLayerId();
		//TODO: Add code to load the  details
		
		return exposureTaskConfig;
	}
}

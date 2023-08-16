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
import gov.epa.bencloud.api.util.ApiUtil;
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
	 * @return selected exposure group (hif group ids is a request parameter).
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



}

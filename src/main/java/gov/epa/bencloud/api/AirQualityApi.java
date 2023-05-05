package gov.epa.bencloud.api;

import static gov.epa.bencloud.server.database.jooq.data.Tables.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.MultipartConfigElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.InsertValuesStep8;
import org.jooq.JSON;
import org.jooq.JSONFormat;
import org.jooq.JSONFormat.RecordFormat;
import org.jooq.OrderField;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Record10;
import org.jooq.Record13;
import org.jooq.Record14;
import org.jooq.Record15;
import org.jooq.Record16;
import org.jooq.Record18;
import org.jooq.Record21;
import org.jooq.Record22;
import org.jooq.Record6;
import org.jooq.Record7;
import org.jooq.Result;
import org.jooq.SortOrder;
import org.jooq.Table;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jooq.tools.csv.CSVReader;
import org.pac4j.core.profile.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import gov.epa.bencloud.Constants;
import gov.epa.bencloud.api.model.AirQualityCell;
import gov.epa.bencloud.api.model.AirQualityCellMetric;
import gov.epa.bencloud.api.model.ValidationMessage;
import gov.epa.bencloud.api.util.AirQualityUtil;
import gov.epa.bencloud.api.util.ApiUtil;
import gov.epa.bencloud.server.database.JooqUtil;
import gov.epa.bencloud.server.database.jooq.data.tables.records.AirQualityCellRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.AirQualityLayerRecord;
import gov.epa.bencloud.server.util.DataConversionUtil;
import gov.epa.bencloud.server.util.ParameterUtil;
import spark.Request;
import spark.Response;

/*
 * Methods 
 */
public class AirQualityApi {
	private static final Logger log = LoggerFactory.getLogger(AirQualityApi.class);

	/**
	 * 
	 * @param request
	 * @param response
	 * @param userProfile
	 * @return a JSON representation of the air quality layer definitions for the given request.
	 */
	public static Object getAirQualityLayerDefinitions(Request request, Response response, Optional<UserProfile> userProfile) {
		
		int pollutantId;
		int page;
		int rowsPerPage;
		String sortBy;
		boolean descending;
		String filter;
		boolean showAll;
		try {
			pollutantId = ParameterUtil.getParameterValueAsInteger(request.raw().getParameter("pollutantId"), 0);
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
		
		String userId = userProfile.get().getId();

//		System.out.println("");
//		System.out.println("page: " + page);
//		System.out.println("pollutantId: " + pollutantId);
//		System.out.println("rowsPerPage: " + rowsPerPage);
//		System.out.println("sortBy: " + sortBy);
//		System.out.println("descending" + descending);
//		System.out.println("filter: " + filter);
		
		List<OrderField<?>> orderFields = new ArrayList<>();
		
		setAirQualityLayersSortOrder(sortBy, descending, orderFields);
		
		//Condition searchCondition = DSL.falseCondition();
		Condition filterCondition = DSL.trueCondition();
		
		Condition pollutantCondition = DSL.trueCondition();

		if (pollutantId != 0) {
			pollutantCondition = DSL.field(AIR_QUALITY_LAYER.POLLUTANT_ID).eq(pollutantId);
			filterCondition = filterCondition.and(pollutantCondition);
		}

		if (!"".equals(filter)) {
			filterCondition = filterCondition.and(buildAirQualityLayersFilterCondition(filter));

			// System.out.println(filterCondition);
		}
		// Skip the following for an admin user that wants to see all data
		if(!showAll || !CoreApi.isAdmin(userProfile)) {
			filterCondition = filterCondition.and(AIR_QUALITY_LAYER.SHARE_SCOPE.eq(Constants.SHARING_ALL).or(AIR_QUALITY_LAYER.USER_ID.eq(userId)));
		}

		//System.out.println(orderFields);
	

		Integer filteredRecordsCount = 
				DSL.using(JooqUtil.getJooqConfiguration()).select(DSL.count())
				.from(AIR_QUALITY_LAYER)
				.join(POLLUTANT).on(POLLUTANT.ID.eq(AIR_QUALITY_LAYER.POLLUTANT_ID))				
				.join(GRID_DEFINITION).on(GRID_DEFINITION.ID.eq(AIR_QUALITY_LAYER.GRID_DEFINITION_ID))
				
				.where(filterCondition)
				.fetchOne(DSL.count());
		
		//System.out.println("filteredRecordsCount: " + filteredRecordsCount);
	
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());
		
		Table<Record13<Integer, Integer, String, Integer, String, Integer, String, Integer, Double, Double, Double, Double, Double>> metricStatistics = create.select(
				AIR_QUALITY_LAYER_METRICS.AIR_QUALITY_LAYER_ID,
				AIR_QUALITY_LAYER_METRICS.METRIC_ID,
				POLLUTANT_METRIC.NAME.as("metric_name"),
				AIR_QUALITY_LAYER_METRICS.SEASONAL_METRIC_ID,
				SEASONAL_METRIC.NAME.as("seasonal_metric_name"),
				AIR_QUALITY_LAYER_METRICS.ANNUAL_STATISTIC_ID,
				STATISTIC_TYPE.NAME.as("annual_statistic_name"),
				AIR_QUALITY_LAYER_METRICS.CELL_COUNT,
				AIR_QUALITY_LAYER_METRICS.MIN_VALUE,
				AIR_QUALITY_LAYER_METRICS.MAX_VALUE,
				AIR_QUALITY_LAYER_METRICS.MEAN_VALUE,
				AIR_QUALITY_LAYER_METRICS.PCT_2_5,
				AIR_QUALITY_LAYER_METRICS.PCT_97_5)
				.from(AIR_QUALITY_LAYER_METRICS)
				.join(POLLUTANT_METRIC).on(POLLUTANT_METRIC.ID.eq(AIR_QUALITY_LAYER_METRICS.METRIC_ID))
				.join(SEASONAL_METRIC).on(SEASONAL_METRIC.ID.eq(AIR_QUALITY_LAYER_METRICS.SEASONAL_METRIC_ID))
				.join(STATISTIC_TYPE).on(STATISTIC_TYPE.ID.eq(AIR_QUALITY_LAYER_METRICS.ANNUAL_STATISTIC_ID))
				.asTable("metric_statistics");
		
		@SuppressWarnings("unchecked")
		@NotNull Result<Record16<Integer, String, String, Short, Integer, Integer, String, String, String, String, String, String, String, String, LocalDateTime,JSON>> aqRecords = 
			create.select(
						AIR_QUALITY_LAYER.ID, 
						AIR_QUALITY_LAYER.NAME,
						AIR_QUALITY_LAYER.USER_ID,
						AIR_QUALITY_LAYER.SHARE_SCOPE,
						AIR_QUALITY_LAYER.GRID_DEFINITION_ID,
						AIR_QUALITY_LAYER.POLLUTANT_ID,
						POLLUTANT.NAME.as("pollutant_name"), 
						POLLUTANT.FRIENDLY_NAME.as("pollutant_friendly_name"),
						GRID_DEFINITION.NAME.as("grid_definition_name"),
						AIR_QUALITY_LAYER.AQ_YEAR,
						AIR_QUALITY_LAYER.DESCRIPTION,
						AIR_QUALITY_LAYER.SOURCE,
						AIR_QUALITY_LAYER.DATA_TYPE,
						AIR_QUALITY_LAYER.FILENAME,
						AIR_QUALITY_LAYER.UPLOAD_DATE,
						DSL.jsonArrayAgg(DSL.jsonbObject(
								metricStatistics.field("metric_id"),
								metricStatistics.field("metric_name"),
								metricStatistics.field("seasonal_metric_id"),
								metricStatistics.field("seasonal_metric_name"),
								metricStatistics.field("annual_statistic_id"),
								metricStatistics.field("annual_statistic_name"),
								metricStatistics.field("cell_count"),
								metricStatistics.field("min_value"),
								metricStatistics.field("max_value"),
								metricStatistics.field("mean_value"),
								metricStatistics.field("pct_2_5"),
								metricStatistics.field("pct_97_5")
								)).as("metric_statistics")
						)
				.from(AIR_QUALITY_LAYER)
				.leftJoin(metricStatistics).on(((Field<Integer>)metricStatistics.field("air_quality_layer_id")).eq(AIR_QUALITY_LAYER.ID))
				.join(POLLUTANT).on(POLLUTANT.ID.eq(AIR_QUALITY_LAYER.POLLUTANT_ID))				
				.join(GRID_DEFINITION).on(GRID_DEFINITION.ID.eq(AIR_QUALITY_LAYER.GRID_DEFINITION_ID))
				.where(filterCondition)
				.groupBy(AIR_QUALITY_LAYER.ID, 
						AIR_QUALITY_LAYER.NAME,
						AIR_QUALITY_LAYER.USER_ID,
						AIR_QUALITY_LAYER.SHARE_SCOPE,
						AIR_QUALITY_LAYER.GRID_DEFINITION_ID,
						AIR_QUALITY_LAYER.POLLUTANT_ID,
						POLLUTANT.NAME, 
						POLLUTANT.FRIENDLY_NAME,
						GRID_DEFINITION.NAME,
						AIR_QUALITY_LAYER.AQ_YEAR,
						AIR_QUALITY_LAYER.DESCRIPTION,
						AIR_QUALITY_LAYER.SOURCE,
						AIR_QUALITY_LAYER.DATA_TYPE,
						AIR_QUALITY_LAYER.FILENAME,
						AIR_QUALITY_LAYER.UPLOAD_DATE)
				.orderBy(orderFields)
				.offset((page * rowsPerPage) - rowsPerPage)
				.limit(rowsPerPage)
				.fetch();
	
		
		//System.out.println("aqRecords: " + aqRecords.size());

		ObjectMapper mapper = new ObjectMapper();
		ObjectNode data = mapper.createObjectNode();
		
		data.put("filteredRecordsCount", filteredRecordsCount);

		try {
			JsonFactory factory = mapper.getFactory();
			JsonParser jp = factory.createParser(
					aqRecords.formatJSON(new JSONFormat().header(false).recordFormat(RecordFormat.OBJECT)));
			JsonNode actualObj = mapper.readTree(jp);
			data.set("records", actualObj);
		} catch (JsonParseException e) {
			log.error("Error parsing JSON",e);
		} catch (JsonProcessingException e) {
			log.error("Error processing JSON",e);
		} catch (IOException e) {
			log.error("IO Exception", e);
		}
		
		//System.out.println(data);
		//System.out.println(aqRecords.formatJSON(new JSONFormat().header(false).recordFormat(RecordFormat.OBJECT)));
		
		response.type("application/json");
		return data;
	}
	
	/**
	 * 
	 * @param request
	 * @param response
	 * @param userProfile
	 * @return a JSON representation of the air quality layer definitions by metric for the given request.
	 */
	public static Object getAirQualityLayerDefinitionsByMetric(Request request, Response response, Optional<UserProfile> userProfile) {
		
		String userId = userProfile.get().getId();		
		int pollutantId;
		int page;
		int rowsPerPage;
		String sortBy;
		boolean descending;
		String filter;
		boolean showAll;
		try {
			pollutantId = ParameterUtil.getParameterValueAsInteger(request.raw().getParameter("pollutantId"), 0);
			page = ParameterUtil.getParameterValueAsInteger(request.raw().getParameter("page"), 1);
			rowsPerPage = ParameterUtil.getParameterValueAsInteger(request.raw().getParameter("rowsPerPage"), 10);
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
		
//		System.out.println("");
//		System.out.println("page: " + page);
//		System.out.println("pollutantId: " + pollutantId);
//		System.out.println("rowsPerPage: " + rowsPerPage);
//		System.out.println("sortBy: " + sortBy);
//		System.out.println("descending" + descending);
//		System.out.println("filter: " + filter);
		
		List<OrderField<?>> orderFields = new ArrayList<>();
		
		setAirQualityLayersSortOrder(sortBy, descending, orderFields);
		
		//Condition searchCondition = DSL.falseCondition();
		Condition filterCondition = DSL.trueCondition();
		
		Condition pollutantCondition = DSL.trueCondition();

		if (pollutantId != 0) {
			pollutantCondition = DSL.field(AIR_QUALITY_LAYER.POLLUTANT_ID).eq(pollutantId);
			filterCondition = filterCondition.and(pollutantCondition);
		}
		
		if (!"".equals(filter)) {
			filterCondition = filterCondition.and(buildAirQualityLayersFilterCondition(filter));

			// System.out.println(filterCondition);
		}
		// Skip the following for an admin user that wants to see all data
		if(!showAll || !CoreApi.isAdmin(userProfile)) {
			filterCondition = filterCondition.and(AIR_QUALITY_LAYER.SHARE_SCOPE.eq(Constants.SHARING_ALL).or(AIR_QUALITY_LAYER.USER_ID.eq(userId)));
		}
		//System.out.println(orderFields);
	

		Integer filteredRecordsCount = 
				DSL.using(JooqUtil.getJooqConfiguration()).select(DSL.count())
				.from(AIR_QUALITY_LAYER)
				.join(AIR_QUALITY_LAYER_METRICS).on(AIR_QUALITY_LAYER_METRICS.AIR_QUALITY_LAYER_ID.eq(AIR_QUALITY_LAYER.ID))
				.join(POLLUTANT).on(POLLUTANT.ID.eq(AIR_QUALITY_LAYER.POLLUTANT_ID))				
				.join(GRID_DEFINITION).on(GRID_DEFINITION.ID.eq(AIR_QUALITY_LAYER.GRID_DEFINITION_ID))
				
				.where(filterCondition)
				.fetchOne(DSL.count());
		
		//System.out.println("filteredRecordsCount: " + filteredRecordsCount);
		
		Result<Record21<Integer, String, Integer, String, Integer, String, Integer, String, Integer, Double, Double, Double, Double, Double, String, Short, Integer, String, Integer, String, String>> aqRecords = 
			DSL.using(JooqUtil.getJooqConfiguration())
				.select(
						AIR_QUALITY_LAYER.ID, 
						AIR_QUALITY_LAYER.NAME,
						AIR_QUALITY_LAYER_METRICS.METRIC_ID,
						POLLUTANT_METRIC.NAME.as("metric"),
						AIR_QUALITY_LAYER_METRICS.SEASONAL_METRIC_ID,
						SEASONAL_METRIC.NAME.as("seasonal_metric"),
						AIR_QUALITY_LAYER_METRICS.ANNUAL_STATISTIC_ID,
						STATISTIC_TYPE.NAME.as("statistic"),
						AIR_QUALITY_LAYER_METRICS.CELL_COUNT,
						AIR_QUALITY_LAYER_METRICS.MIN_VALUE,
						AIR_QUALITY_LAYER_METRICS.MAX_VALUE,
						AIR_QUALITY_LAYER_METRICS.MEAN_VALUE,
						AIR_QUALITY_LAYER_METRICS.PCT_2_5,
						AIR_QUALITY_LAYER_METRICS.PCT_97_5,
						AIR_QUALITY_LAYER.USER_ID,
						AIR_QUALITY_LAYER.SHARE_SCOPE,
						AIR_QUALITY_LAYER.GRID_DEFINITION_ID,
						GRID_DEFINITION.NAME.as("grid_definition_name"),
						AIR_QUALITY_LAYER.POLLUTANT_ID,
						POLLUTANT.NAME.as("pollutant_name"),
						POLLUTANT.FRIENDLY_NAME.as("pollutant_friendly_name")
						)
				.from(AIR_QUALITY_LAYER)
				.join(AIR_QUALITY_LAYER_METRICS).on(AIR_QUALITY_LAYER_METRICS.AIR_QUALITY_LAYER_ID.eq(AIR_QUALITY_LAYER.ID))
				.join(POLLUTANT).on(POLLUTANT.ID.eq(AIR_QUALITY_LAYER.POLLUTANT_ID))				
				.join(GRID_DEFINITION).on(GRID_DEFINITION.ID.eq(AIR_QUALITY_LAYER.GRID_DEFINITION_ID))
				.join(POLLUTANT_METRIC).on(POLLUTANT_METRIC.ID.eq(AIR_QUALITY_LAYER_METRICS.METRIC_ID))
				.join(SEASONAL_METRIC).on(SEASONAL_METRIC.ID.eq(AIR_QUALITY_LAYER_METRICS.SEASONAL_METRIC_ID))
				.join(STATISTIC_TYPE).on(STATISTIC_TYPE.ID.eq(AIR_QUALITY_LAYER_METRICS.ANNUAL_STATISTIC_ID))
				.where(filterCondition)
				.orderBy(orderFields)
				.offset((page * rowsPerPage) - rowsPerPage)
				.limit(rowsPerPage)
				.fetch();
	
		
		//System.out.println("aqRecords: " + aqRecords.size());

		ObjectMapper mapper = new ObjectMapper();
		ObjectNode data = mapper.createObjectNode();
		
		data.put("filteredRecordsCount", filteredRecordsCount);

		try {
			JsonFactory factory = mapper.getFactory();
			JsonParser jp = factory.createParser(
					aqRecords.formatJSON(new JSONFormat().header(false).recordFormat(RecordFormat.OBJECT)));
			JsonNode actualObj = mapper.readTree(jp);
			data.set("records", actualObj);
		} catch (JsonParseException e) {
			log.error("Error parsing JSON",e);
		} catch (JsonProcessingException e) {
			log.error("Error processing JSON",e);
		} catch (IOException e) {
			log.error("IO Exception", e);
		}
		
		//System.out.println(data);
		//System.out.println(aqRecords.formatJSON(new JSONFormat().header(false).recordFormat(RecordFormat.OBJECT)));
		
		response.type("application/json");
		return data;
	}
		
	
	/**
	 * @param request - expected to contain id param
	 * @param response
	 * @param optional
	 * @return Single air quality layer definition as json string 
	 */
	public static Object getAirQualityLayerDefinition(Request request, Response response, Optional<UserProfile> userProfile) {
		
		Integer id;
		try {
			id = Integer.valueOf(request.params("id"));
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return CoreApi.getErrorResponseInvalidId(request, response);
		}
		
		Record16<Integer, String, String, Short, Integer, Integer, String, String, String, String, String, LocalDateTime, String, String, String, JSON> aqRecord = getAirQualityLayerDefinition(id, userProfile);
		response.type("application/json");
		if(aqRecord == null) {
			return CoreApi.getErrorResponseNotFound(request, response);
		} else {
			return aqRecord.formatJSON(new JSONFormat().header(false).recordFormat(RecordFormat.OBJECT));
		}
	}
	
	
	/**
	 * 
	 * @param id
	 * @param userProfile
	 * @return a representation of an air quality layer definition.
	 */
	@SuppressWarnings("unchecked")
	public static @Nullable Record16<Integer, String, String, Short, Integer, Integer, String, String, String, String, String, LocalDateTime, String, String, String, JSON> getAirQualityLayerDefinition(Integer id, Optional<UserProfile> userProfile) {
		String userId = userProfile.get().getId();
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());
		
		Table<Record13<Integer, Integer, String, Integer, String, Integer, String, Integer, Double, Double, Double, Double, Double>> metricStatistics = create.select(
				AIR_QUALITY_LAYER_METRICS.AIR_QUALITY_LAYER_ID,
				AIR_QUALITY_LAYER_METRICS.METRIC_ID,
				POLLUTANT_METRIC.NAME.as("metric_name"),
				AIR_QUALITY_LAYER_METRICS.SEASONAL_METRIC_ID,
				SEASONAL_METRIC.NAME.as("seasonal_metric_name"),
				AIR_QUALITY_LAYER_METRICS.ANNUAL_STATISTIC_ID,
				STATISTIC_TYPE.NAME.as("annual_statistic_name"),
				AIR_QUALITY_LAYER_METRICS.CELL_COUNT,
				AIR_QUALITY_LAYER_METRICS.MIN_VALUE,
				AIR_QUALITY_LAYER_METRICS.MAX_VALUE,
				AIR_QUALITY_LAYER_METRICS.MEAN_VALUE,
				AIR_QUALITY_LAYER_METRICS.PCT_2_5,
				AIR_QUALITY_LAYER_METRICS.PCT_97_5)
				.from(AIR_QUALITY_LAYER_METRICS)
				.join(POLLUTANT_METRIC).on(POLLUTANT_METRIC.ID.eq(AIR_QUALITY_LAYER_METRICS.METRIC_ID))
				.leftJoin(SEASONAL_METRIC).on(SEASONAL_METRIC.ID.eq(AIR_QUALITY_LAYER_METRICS.SEASONAL_METRIC_ID))
				.leftJoin(STATISTIC_TYPE).on(STATISTIC_TYPE.ID.eq(AIR_QUALITY_LAYER_METRICS.ANNUAL_STATISTIC_ID))
				.asTable("metric_statistics");
		

		return create.select(
				AIR_QUALITY_LAYER.ID, 
				AIR_QUALITY_LAYER.NAME,
				AIR_QUALITY_LAYER.USER_ID,
				AIR_QUALITY_LAYER.SHARE_SCOPE,
				AIR_QUALITY_LAYER.GRID_DEFINITION_ID,
				AIR_QUALITY_LAYER.POLLUTANT_ID,
				AIR_QUALITY_LAYER.AQ_YEAR,
				AIR_QUALITY_LAYER.DESCRIPTION,
				AIR_QUALITY_LAYER.SOURCE,
				AIR_QUALITY_LAYER.DATA_TYPE,
				AIR_QUALITY_LAYER.FILENAME,
				AIR_QUALITY_LAYER.UPLOAD_DATE,
				POLLUTANT.NAME.as("pollutant_name"), 
				POLLUTANT.FRIENDLY_NAME.as("pollutant_friendly_name"),
				GRID_DEFINITION.NAME.as("grid_definition_name"),
				DSL.jsonArrayAgg(DSL.jsonbObject(
						metricStatistics.field("metric_id"),
						metricStatistics.field("metric_name"),
						metricStatistics.field("seasonal_metric_id"),
						metricStatistics.field("seasonal_metric_name"),
						metricStatistics.field("annual_statistic_id"),
						metricStatistics.field("annual_statistic_name"),
						metricStatistics.field("cell_count"),
						metricStatistics.field("min_value"),
						metricStatistics.field("max_value"),
						metricStatistics.field("mean_value"),
						metricStatistics.field("pct_2_5"),
						metricStatistics.field("pct_97_5")
						)).as("metric_statistics")
				)
		.from(AIR_QUALITY_LAYER)
		.join(metricStatistics).on(((Field<Integer>)metricStatistics.field("air_quality_layer_id")).eq(AIR_QUALITY_LAYER.ID))
		.join(POLLUTANT).on(POLLUTANT.ID.eq(AIR_QUALITY_LAYER.POLLUTANT_ID))				
		.join(GRID_DEFINITION).on(GRID_DEFINITION.ID.eq(AIR_QUALITY_LAYER.GRID_DEFINITION_ID))
		.where(AIR_QUALITY_LAYER.ID.eq(id))
		.and(AIR_QUALITY_LAYER.SHARE_SCOPE.eq(Constants.SHARING_ALL).or(AIR_QUALITY_LAYER.USER_ID.eq(userId)).or(CoreApi.isAdmin(userProfile) ? DSL.trueCondition() : DSL.falseCondition()))
		.groupBy(AIR_QUALITY_LAYER.ID
				, AIR_QUALITY_LAYER.NAME
				, AIR_QUALITY_LAYER.USER_ID
				, AIR_QUALITY_LAYER.SHARE_SCOPE
				, AIR_QUALITY_LAYER.GRID_DEFINITION_ID
				, AIR_QUALITY_LAYER.POLLUTANT_ID
				, AIR_QUALITY_LAYER.AQ_YEAR
				, AIR_QUALITY_LAYER.DESCRIPTION
				, AIR_QUALITY_LAYER.SOURCE
				, AIR_QUALITY_LAYER.DATA_TYPE
				, AIR_QUALITY_LAYER.FILENAME
				, AIR_QUALITY_LAYER.UPLOAD_DATE				
				, POLLUTANT.NAME
				, POLLUTANT.FRIENDLY_NAME
				, GRID_DEFINITION.NAME
				
				)
		.fetchOne();
	}

	/**
	 * 
	 * @param id
	 * @return an air quality layer grid definition id for the given air quality layer id.
	 */
	public static Integer getAirQualityLayerGridId(Integer id) {
		return DSL.using(JooqUtil.getJooqConfiguration())
		.select(
				AIR_QUALITY_LAYER.GRID_DEFINITION_ID)
		.from(AIR_QUALITY_LAYER)
		.where(AIR_QUALITY_LAYER.ID.eq(id))
		.fetchOne().value1();
	}
	
	public static String getAirQualityLayerName(Integer id) {
		return DSL.using(JooqUtil.getJooqConfiguration())
		.select(
				AIR_QUALITY_LAYER.NAME)
		.from(AIR_QUALITY_LAYER)
		.where(AIR_QUALITY_LAYER.ID.eq(id))
		.fetchOne().value1();	
	}
	
	/**
	 * 
	 * @param request
	 * @param response
	 * @param userProfile
	 * @return a JSON representation of the air quality layer details for a given air quality layer (aq layer id is a request parameter).
	 */
	public static Object getAirQualityLayerDetails(Request request, Response response, Optional<UserProfile> userProfile) {
		
		String userId = userProfile.get().getId();
		//System.out.println("in getAirQualityLayerDetails");
		
		int id;
		int page;
		int rowsPerPage;
		String sortBy;
		boolean descending;
		String filter;
		try {
			id = Integer.valueOf(request.params("id"));
			page = ParameterUtil.getParameterValueAsInteger(request.raw().getParameter("page"), 1);
			rowsPerPage = ParameterUtil.getParameterValueAsInteger(request.raw().getParameter("rowsPerPage"), 10);
			sortBy = ParameterUtil.getParameterValueAsString(request.raw().getParameter("sortBy"), "");
			descending = ParameterUtil.getParameterValueAsBoolean(request.raw().getParameter("descending"), false);
			filter = ParameterUtil.getParameterValueAsString(request.raw().getParameter("filter"), "");
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return CoreApi.getErrorResponseInvalidId(request, response);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return CoreApi.getErrorResponseInvalidId(request, response);
		}
		//System.out.println("id: " + id);
		//System.out.println("filter: " + filter);
		//System.out.println("rowsPerPage: " + rowsPerPage);

		Condition filterCondition = DSL.trueCondition();
		Condition airQualityLayerCondition = DSL.trueCondition();
		
		if (id != 0) {
			airQualityLayerCondition = DSL.field(AIR_QUALITY_CELL.AIR_QUALITY_LAYER_ID).eq(id);
			filterCondition = filterCondition.and(airQualityLayerCondition);
		}

		if (!"".equals(filter)) {
			filterCondition = filterCondition.and(buildAirQualityCellsFilterCondition(filter));
		}
		filterCondition = filterCondition.and(AIR_QUALITY_LAYER.SHARE_SCOPE.eq(Constants.SHARING_ALL).or(AIR_QUALITY_LAYER.USER_ID.eq(userId)).or(CoreApi.isAdmin(userProfile) ? DSL.trueCondition() : DSL.falseCondition()));
	
		List<OrderField<?>> orderFields = new ArrayList<>();
		
		//System.out.println("sortBy: " + sortBy);
		
		setAirQualityCellsSortOrder(sortBy, descending, orderFields);


		Integer filteredRecordsCount = 
				DSL.using(JooqUtil.getJooqConfiguration()).select(DSL.count())
				.from(AIR_QUALITY_CELL)
				.join(AIR_QUALITY_LAYER).on(AIR_QUALITY_CELL.AIR_QUALITY_LAYER_ID.eq(AIR_QUALITY_LAYER.ID))
				.leftJoin(POLLUTANT_METRIC).on(AIR_QUALITY_CELL.METRIC_ID.eq(POLLUTANT_METRIC.ID))
				.leftJoin(SEASONAL_METRIC).on(AIR_QUALITY_CELL.SEASONAL_METRIC_ID.eq(SEASONAL_METRIC.ID))
				.where(filterCondition)
				.fetchOne(DSL.count());

		//System.out.println("filteredRecordsCount: " + filteredRecordsCount);

		Result<Record6<Integer, Integer, String, String, String, Double>> aqRecords = DSL.using(JooqUtil.getJooqConfiguration())
				.select(
						AIR_QUALITY_CELL.GRID_COL,
						AIR_QUALITY_CELL.GRID_ROW,
						POLLUTANT_METRIC.NAME.as("metric"),
						SEASONAL_METRIC.NAME.as("seasonal_metric"),
						STATISTIC_TYPE.NAME.as("annual_statistic"),
						AIR_QUALITY_CELL.VALUE
						)
				.from(AIR_QUALITY_CELL)
				.join(AIR_QUALITY_LAYER).on(AIR_QUALITY_CELL.AIR_QUALITY_LAYER_ID.eq(AIR_QUALITY_LAYER.ID))
				.leftJoin(POLLUTANT_METRIC).on(AIR_QUALITY_CELL.METRIC_ID.eq(POLLUTANT_METRIC.ID))
				.leftJoin(SEASONAL_METRIC).on(AIR_QUALITY_CELL.SEASONAL_METRIC_ID.eq(SEASONAL_METRIC.ID))
				.leftJoin(STATISTIC_TYPE).on(AIR_QUALITY_CELL.ANNUAL_STATISTIC_ID.eq(STATISTIC_TYPE.ID))
				.where(filterCondition)
				.orderBy(orderFields)
				.offset((page * rowsPerPage) - rowsPerPage)
				.limit(rowsPerPage)
				.fetch();
		
		Record1<String> layerInfo = DSL.using(JooqUtil.getJooqConfiguration())
				.select(AIR_QUALITY_LAYER.NAME)
				.from(AIR_QUALITY_LAYER)
				.where(AIR_QUALITY_LAYER.ID.eq(id))
				.fetchOne();

		if(request.headers("Accept").equalsIgnoreCase("text/csv")) {
			String fileName = createFilename(layerInfo.get(AIR_QUALITY_LAYER.NAME));
			response.type("text/csv");
			response.header("Content-Disposition", "attachment; filename="+ fileName);
			response.header("Access-Control-Expose-Headers", "Content-Disposition");
						
			return aqRecords.formatCSV();
		} else {

			//System.out.println("aqRecords: " + aqRecords.size());

			ObjectMapper mapper = new ObjectMapper();
			ObjectNode data = mapper.createObjectNode();
			
			data.put("filteredRecordsCount", filteredRecordsCount);

			try {
				JsonFactory factory = mapper.getFactory();
				JsonParser jp = factory.createParser(
						aqRecords.formatJSON(new JSONFormat().header(false).recordFormat(RecordFormat.OBJECT)));
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
	}

	/**
	 * 
	 * @param layerName
	 * @return a csv file name for a given layer name.
	 */
	public static String createFilename(String layerName) {
		// Currently allowing periods so we don't break extensions. Need to improve this.
		return layerName.replaceAll("[^A-Za-z0-9._-]+", "") + ".csv";
	}

	/**
	 * 
	 * @param id air quality layer id
	 * @return Map<gridCellId, <metricId + seasonalMetricId + annualMetric, value>>
	 */
	public static Map<Long, AirQualityCell> getAirQualityLayerMap(Integer id) {
/*
		Map<Integer, AirQualityCellRecord> aqRecords = DSL.using(JooqUtil.getJooqConfiguration())
				.selectFrom(AIR_QUALITY_CELL)
				.where(AIR_QUALITY_CELL.AIR_QUALITY_LAYER_ID.eq(id))
				.orderBy(AIR_QUALITY_CELL.GRID_COL, AIR_QUALITY_CELL.GRID_ROW)
				.fetchMap(AIR_QUALITY_CELL.GRID_CELL_ID);	
*/
		
		Result<Record7<Long, Integer, Integer, Integer, Integer, Integer, Double>> aqRecords = DSL.using(JooqUtil.getJooqConfiguration())
				.select(AIR_QUALITY_CELL.GRID_CELL_ID
						, AIR_QUALITY_CELL.GRID_COL
						, AIR_QUALITY_CELL.GRID_ROW
						, AIR_QUALITY_CELL.METRIC_ID
						, AIR_QUALITY_CELL.SEASONAL_METRIC_ID
						, AIR_QUALITY_CELL.ANNUAL_STATISTIC_ID
						, AIR_QUALITY_CELL.VALUE)
				.from(AIR_QUALITY_CELL)
				.where(AIR_QUALITY_CELL.AIR_QUALITY_LAYER_ID.eq(id))
				.fetch();
		
		
		Map<Long, AirQualityCell> aqMap = new HashMap<Long, AirQualityCell>();
		
		for(Record7<Long, Integer, Integer, Integer, Integer, Integer, Double> r : aqRecords) {
			//Do we have a top-level entry in aqMap for this cell?
			// If not, add it
			Long cellId = r.get(AIR_QUALITY_CELL.GRID_CELL_ID);
			
			//Get the cellAqMetrics from the map (or add it to the map)
			AirQualityCell cellAq = aqMap.get(cellId);
			if (cellAq == null) {
				cellAq = new AirQualityCell(r.get(AIR_QUALITY_CELL.GRID_COL), r.get(AIR_QUALITY_CELL.GRID_ROW));
				aqMap.put(cellId, cellAq);
			}
			
			// cell = <cell id, (col, row, aqvalues)>
			//   aqvalues = <metricId, <seasonalMetricId, aqValue>
			//      aqValue = annnual statistic, value
			// id=0 for metric or seasonal metric means empty
			// 
			
			//Each cellAq object contains the cell's col, row, and a map of aq values indexed by metric id and seasonal metric id
			Map<Integer, Map<Integer, AirQualityCellMetric>> cellAqMetrics = cellAq.getCellMetrics();
			
			AirQualityCellMetric cellAqMetric = new AirQualityCellMetric(r.get(AIR_QUALITY_CELL.METRIC_ID),r.get(AIR_QUALITY_CELL.SEASONAL_METRIC_ID), r.get(AIR_QUALITY_CELL.ANNUAL_STATISTIC_ID), r.get(AIR_QUALITY_CELL.VALUE));
			
			Map<Integer, AirQualityCellMetric> cellAqSeasonalMetrics = cellAqMetrics.get(r.get(AIR_QUALITY_CELL.METRIC_ID));
			if(cellAqSeasonalMetrics == null) {
				cellAqSeasonalMetrics = new HashMap<Integer, AirQualityCellMetric>();	
				cellAqMetrics.put(r.get(AIR_QUALITY_CELL.METRIC_ID), cellAqSeasonalMetrics);
			}	
					
			if(r.get(AIR_QUALITY_CELL.SEASONAL_METRIC_ID) != null && r.get(AIR_QUALITY_CELL.SEASONAL_METRIC_ID) != 0) {
				cellAqSeasonalMetrics.put(r.get(AIR_QUALITY_CELL.SEASONAL_METRIC_ID), cellAqMetric);				
			} else {
				cellAqSeasonalMetrics.put(0, cellAqMetric);
			}
		}
		
		return aqMap;
	}
	
	/*
	 * 
	 */
	public static Object postAirQualityLayer(Request request, Response response, Optional<UserProfile> userProfile) {
		request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));
		String layerName;
		Integer pollutantId;
		Integer gridId;
		String aqYear;
		String description;
		String source;
		String dataType;
		String filename;
		LocalDateTime uploadDate;
		
		try{
			layerName = ApiUtil.getMultipartFormParameterAsString(request, "name");
			pollutantId = ApiUtil.getMultipartFormParameterAsInteger(request, "pollutantId");
			gridId = ApiUtil.getMultipartFormParameterAsInteger(request, "gridId");
			aqYear = ApiUtil.getMultipartFormParameterAsString(request, "aqYear");
			description = ApiUtil.getMultipartFormParameterAsString(request, "description");
			source = ApiUtil.getMultipartFormParameterAsString(request, "source");
			dataType = ApiUtil.getMultipartFormParameterAsString(request, "dataType");
			filename = ApiUtil.getMultipartFormParameterAsString(request, "filename");
			uploadDate = ApiUtil.getMultipartFormParameterAsLocalDateTime(request, "uploadDate", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return CoreApi.getErrorResponseInvalidId(request, response);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return CoreApi.getErrorResponseInvalidId(request, response);
		}
		
		// Add this in later when we start supporting other air quality surface types such as monitor data
		//String layerType = IOUtils.toString(request.raw().getPart("type").getInputStream(), StandardCharsets.UTF_8);

		//Validate csv file
		String errorMsg= ""; //stores more detailed info. Not used in report for now but may need in the future?
		ValidationMessage validationMsg = new ValidationMessage();

				
		if(layerName == null || pollutantId == null || gridId == null) {
			response.type("application/json");
			//response.status(400);
			validationMsg.success=false;
			validationMsg.messages.add(new ValidationMessage.Message("error","Missing one or more required parameters: name, pollutantId, gridId."));
			return transformValMsgToJSON(validationMsg);
		}

		//TODO: REMOVE THIS. IT'S JUST A WORKAROUND FOR A TEMPORARY UI BUG
		if(pollutantId.equals(0)) {
			pollutantId = 6;
		}
		
		//step 0: make sure layerName is not the same as any existing ones
		
		List<String>layerNames = AirQualityUtil.getExistingLayerNames(pollutantId);
		if (layerNames.contains(layerName.toLowerCase())) {
			validationMsg.success = false;
			validationMsg.messages.add(new ValidationMessage.Message("error","A layer named " + layerName + " already exists. Please enter a different name."));
			response.type("application/json");
			return transformValMsgToJSON(validationMsg);
		}
		
		AirQualityLayerRecord aqRecord=null;
		int columnIdx=-999;
		int rowIdx=-999;
		int metricIdx=-999;
		int seasonalMetricIdx=-999;
		int annualMetricIdx=-999;
		int valuesIdx=-999;
		
		Map<String, Integer> pollutantMetricIdLookup = new HashMap<>();		
		Map<String, Integer> seasonalMetricIdLookup = new HashMap<>();		
		Map<String, Integer> statisticIdLookup = new HashMap<>();
		
		try (InputStream is = request.raw().getPart("file").getInputStream()) {
			
			CSVReader csvReader = new CSVReader (new InputStreamReader(is));				

			String[] record;
			
			//step 1: verify column names 
			// Read the header
			// allow either "column" or "col"; "values" or "value"
			// todo: warn or abort when both "column" and "col" exist.
			record = csvReader.readNext();
			for(int i=0; i < record.length; i++) {
				switch(record[i].toLowerCase().replace(" ", "")) {
				case "column":					
					if(columnIdx==-999) {
						columnIdx=i;
					}
					else {
						validationMsg.success = false;
						ValidationMessage.Message msg = new ValidationMessage.Message();
						msg.message = "File has both 'col' and 'column' fields.";
						msg.type = "error";
						validationMsg.messages.add(msg);
					}
					break;
				case "col":
					if(columnIdx==-999) {
						columnIdx=i;
					}
					else {
						validationMsg.success = false;
						ValidationMessage.Message msg = new ValidationMessage.Message();
						msg.message = "File has both 'col' and 'column' fields";
						msg.type = "error";
						validationMsg.messages.add(msg);
					}
					break;
				case "row":
					rowIdx=i;
					break;
				case "metric":
					metricIdx=i;
					break;
				case "seasonalmetric":
					seasonalMetricIdx=i;
					break;
				case "annualmetric":
					annualMetricIdx=i;
					break;
				case "statistic":
					annualMetricIdx=i;
					break;
				case "values":
					valuesIdx=i;
					break;
				case "value":
					valuesIdx=i;
					break;
				}
			}
			String tmp = AirQualityUtil.validateModelColumnHeadings(columnIdx, rowIdx, metricIdx, seasonalMetricIdx, annualMetricIdx, valuesIdx);
			if(tmp.length() > 0) {
				
				log.debug("AQ dataset posted - columns are missing: " + tmp);
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				msg.message = "The following columns are missing: " + tmp;
				msg.type = "error";
				validationMsg.messages.add(msg);
				response.type("application/json");
				return transformValMsgToJSON(validationMsg);
			}
			
			pollutantMetricIdLookup = AirQualityUtil.getPollutantMetricIdLookup(pollutantId);
			seasonalMetricIdLookup = AirQualityUtil.getSeasonalMetricIdLookup(pollutantId);
			statisticIdLookup = ApiUtil.getStatisticIdLookup();
			
			//We might also need to clean up the header. Or, maybe we should make this a transaction?
			
			//step 2: make sure file has > 0 rows. Check rowCount after while loop.
			int rowCount = 0;
			int countColTypeError = 0;
			int countRowTypeError = 0;
			int countMissingMetric = 0;
			int countValueTypeError = 0;
			int countValueError = 0;
			List<String> lstUndefinedMetric = new ArrayList<String>();
			List<String> lstUndefinedSeasonalMetric = new ArrayList<String>();
			List<String> lstUndefinedStatistics = new ArrayList<String>();
			List<String> lstDupMetricCombo = new ArrayList<String>();
			
			Map<String, Integer> dicUniqueMetric = new HashMap<String,Integer>();	
			
			while ((record = csvReader.readNext()) != null) {				
				rowCount ++;
				
				//step 3: Verify data types for each field
				String str = "";
				//column is required and should be an integer
				str = record[columnIdx];
				if(str=="" || !str.matches("-?\\d+")) {
					//errorMsg +="record #" + String.valueOf(rowCount + 1) + ": " +  "column value " + str + " is not a valid integer." + "\r\n";
					countColTypeError++;
				}	
				//row is required and should be an integer
				str = record[rowIdx];
				//question: or use Integer.parseInt(str)??
				if(str=="" || !str.matches("-?\\d+")) {
					//errorMsg +="record #" + String.valueOf(rowCount + 1) + ": " +  "row value " + str + " is not a valid integer."+ "\r\n";
					countRowTypeError++;
				}	
				//metric is required and should be defined.
				str = record[metricIdx].toLowerCase();
				if(str=="") {
					//errorMsg +="record #" + String.valueOf(rowCount + 1) + ": " +  "Metric value is missing on line " + Integer.toString(rowCount)+ "\r\n";
					countMissingMetric ++;
				}
				else if(!pollutantMetricIdLookup.containsKey(str) ) {
					//errorMsg +="record #" + String.valueOf(rowCount + 1) + ": " +  "Metric value " + str + " is not defined."+ "\r\n";
					if (!lstUndefinedMetric.contains(String.valueOf(str))) {
						lstUndefinedMetric.add(String.valueOf(str));
					}
				}
				
				//seasonal metric is not required and should be defined (seasonalMetricIdLookup = pollutant metric + "~" + seasonal metric).
				//SKIP THIS VALIDATION FOR NOW SINCE WE'LL CREATE IT DYNAMICALLY BELOW
				/*
				str = record[metricIdx].toLowerCase() + "~" + record[seasonalMetricIdx].toLowerCase();
				if("".equals(str)) {
					//Seasonal metric is not required.
				}
				else if(!seasonalMetricIdLookup.containsKey(str) ) {
					//errorMsg +="record #" + String.valueOf(rowCount + 1) + ": " +  "Seasonal Metric value " + record[seasonalMetricIdx] + " is not defined."+ "\r\n";
					if (!lstUndefinedSeasonalMetric.contains(String.valueOf(str))) {
						lstUndefinedSeasonalMetric.add(String.valueOf(str));
					}
				}
				*/
				//annual metric aka annual statistic can be either blank or a valid value
				str = record[annualMetricIdx].toLowerCase();
				if(!statisticIdLookup.containsKey(str) && str !="") {
					//errorMsg +="record #" + String.valueOf(rowCount + 1) + ": " +  "Annual Metric value " + str + " is not a valid method."+ "\r\n";
					if (!lstUndefinedStatistics.contains(String.valueOf(str))) {
						lstUndefinedStatistics.add(String.valueOf(str));
					}
				}
				
				//value/values should be a double and >= 0
				str = record[valuesIdx];
				try {
					double dbl = Double.parseDouble(str);
					if (dbl<0) {
						//errorMsg +="record #" + String.valueOf(rowCount + 1) + ": " +  "Value " + str + " is not a valid as it is less than 0."+ "\r\n";
						countValueTypeError ++;
					}
				}
				catch(NumberFormatException e){
					//errorMsg +="record #" + String.valueOf(rowCount + 1) + ": " +  "Value " + str + " is not a valid double."+ "\r\n";
					countValueError ++;
				}
				
				//metric-seasonal metric-annual statistics should be unique
				str = record[columnIdx].toString() 
						+ "~" + record[rowIdx].toLowerCase() 
						+ "~" + record[metricIdx].toLowerCase() 
						+ "~" + record[seasonalMetricIdx].toLowerCase() 
						+ "~" + record[annualMetricIdx].toLowerCase();
				if(!dicUniqueMetric.containsKey(str)) {
					dicUniqueMetric.put(str,rowCount + 1);
				}
				else {
					//errorMsg += "record #" + String.valueOf(rowCount + 1) + ": " + "Duplicate metric" +  "\r\n";
					if(!lstDupMetricCombo.contains(str)) {
						lstDupMetricCombo.add(str);
					}
				}
			}	
			
			//summarize validation message
			if(countColTypeError>0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countColTypeError == 1) {
					strRecord = String.valueOf(countColTypeError) + " record has Column values not a valid integer.";
				}
				else {
					strRecord = String.valueOf(countColTypeError) + " records have Column values that are not valid integers.";
				}
				msg.message = strRecord + "";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}
			if(countRowTypeError>0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countRowTypeError == 1) {
					strRecord = String.valueOf(countRowTypeError) + " record has Row values not a valid integer.";
				}
				else {
					strRecord = String.valueOf(countRowTypeError) + " records have Row values that are not valid integers.";
				}
				msg.message = strRecord + "";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}
			if(countValueTypeError > 0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countValueTypeError == 1) {
					strRecord = String.valueOf(countValueTypeError) + " record has air quality values that is not a valid number.";
				}
				else {
					strRecord = String.valueOf(countValueTypeError) + " records have  air quality values that are not valid numbers.";
				}
				msg.message = strRecord + "";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}
			if(countValueError > 0) {
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countValueError == 1) {
					strRecord = String.valueOf(countValueError) + " record has";
				}
				else {
					strRecord = String.valueOf(countValueError) + " records have";
				}
				msg.message = strRecord + " air quality values below zero.";
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
			if(lstUndefinedMetric.size()>0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				msg.message = "The following Metrics are not defined: " + String.join(",", lstUndefinedMetric) + ".";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}
			if(lstUndefinedSeasonalMetric.size()>0) {
				//lstUndefinedSeasonalMetric currently contains Metric~SeasonalMetric. Extract Seasonal Metric
				for (int i = 0; i < lstUndefinedSeasonalMetric.size();i++) {
					String smold = lstUndefinedSeasonalMetric.get(i);					
					lstUndefinedSeasonalMetric.set(i,smold.substring(smold.lastIndexOf('~') + 1));
				}
				
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				msg.message = "The following Seasonal Metrics are not defined: " + String.join(",", lstUndefinedSeasonalMetric)+ ".";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}
			if(lstUndefinedStatistics.size()>0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				msg.message = "The following Annual Statistics are not valid: " + String.join(",", lstUndefinedStatistics)+ ".";
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
			
			if(!validationMsg.success) {
				response.type("application/json");
				//response.status(400);
				return transformValMsgToJSON(validationMsg); 
			}
							
			
			//---End of csv validation
			
		} catch (Exception e) {
			log.error("Error validating AQ file", e);
			response.type("application/json");
			//response.status(400);
			validationMsg.success=false;
			validationMsg.messages.add(new ValidationMessage.Message("error","Error occurred during validation of air quality file."));
			return transformValMsgToJSON(validationMsg);
		}
		
		Integer aqLayerId = null;
		
		//import data
		try (InputStream is = request.raw().getPart("file").getInputStream()){
			CSVReader csvReader = new CSVReader (new InputStreamReader(is));
			String[] record;
			record = csvReader.readNext();
			
			//Create the air_quality_layer record
			aqRecord = DSL.using(JooqUtil.getJooqConfiguration())
			.insertInto(AIR_QUALITY_LAYER
					, AIR_QUALITY_LAYER.NAME
					, AIR_QUALITY_LAYER.POLLUTANT_ID
					, AIR_QUALITY_LAYER.GRID_DEFINITION_ID
					, AIR_QUALITY_LAYER.USER_ID
					, AIR_QUALITY_LAYER.SHARE_SCOPE
					, AIR_QUALITY_LAYER.AQ_YEAR
					, AIR_QUALITY_LAYER.DESCRIPTION
					, AIR_QUALITY_LAYER.SOURCE
					, AIR_QUALITY_LAYER.DATA_TYPE
					, AIR_QUALITY_LAYER.FILENAME
					, AIR_QUALITY_LAYER.UPLOAD_DATE)
			.values(layerName, pollutantId, gridId, userProfile.get().getId(), Constants.SHARING_NONE
					,aqYear, description, source, dataType, filename, uploadDate)
			.returning(AIR_QUALITY_LAYER.ID, AIR_QUALITY_LAYER.NAME, AIR_QUALITY_LAYER.POLLUTANT_ID, AIR_QUALITY_LAYER.GRID_DEFINITION_ID)
			.fetchOne();
			
			aqLayerId = aqRecord.value1();
		
			// Read the data rows and write to the db	
			InsertValuesStep8<AirQualityCellRecord, Integer, Integer, Integer, Long, Integer, Integer, Integer, Double> batch = DSL.using(JooqUtil.getJooqConfiguration())
					.insertInto(
							AIR_QUALITY_CELL, 
							AIR_QUALITY_CELL.AIR_QUALITY_LAYER_ID,
							AIR_QUALITY_CELL.GRID_COL, 
							AIR_QUALITY_CELL.GRID_ROW,
							AIR_QUALITY_CELL.GRID_CELL_ID,
							AIR_QUALITY_CELL.METRIC_ID,
							AIR_QUALITY_CELL.SEASONAL_METRIC_ID,
							AIR_QUALITY_CELL.ANNUAL_STATISTIC_ID,
							AIR_QUALITY_CELL.VALUE
							);
			
			while ((record = csvReader.readNext()) != null) {
				//Make sure this metric exists in the db. If not, add it and update pollutantMetricIdLookup now 
				String metricNameLowerCase = record[metricIdx].toLowerCase();
				
				if(!pollutantMetricIdLookup.containsKey(metricNameLowerCase ) ) {
					pollutantMetricIdLookup.put(
							metricNameLowerCase, 
							AirQualityUtil.createNewPollutantMetric(pollutantId, record[metricIdx]));
				}
				
				String seasonalMetricLowerCase = metricNameLowerCase + "~" + record[seasonalMetricIdx].toLowerCase();
				
				if(!seasonalMetricIdLookup.containsKey(seasonalMetricLowerCase)) {
					seasonalMetricIdLookup.put(
							seasonalMetricLowerCase,
							AirQualityUtil.createNewSeasonalMetric(pollutantMetricIdLookup.get(metricNameLowerCase), record[seasonalMetricIdx]));
				}

				String statisticLowerCase = record[seasonalMetricIdx].toLowerCase();
				Integer statisticId=0;
				if(statisticIdLookup.containsKey(seasonalMetricLowerCase)) {
					statisticId = statisticIdLookup.get(seasonalMetricLowerCase);
				} else {
					//TODO: Throw up an error message here. We can't allow this process to add statistics. We just need to ignore those we don't support
					
				}
				
				// Add a record to the batch
				batch.values(
						aqLayerId, 
						Integer.valueOf(record[columnIdx]), 
						Integer.valueOf(record[rowIdx]),
						ApiUtil.getCellId(Integer.valueOf(record[columnIdx]), Integer.valueOf(record[rowIdx])),
						pollutantMetricIdLookup.get(metricNameLowerCase), 
						seasonalMetricIdLookup.get(seasonalMetricLowerCase), 
						statisticId,
						Double.valueOf(record[valuesIdx])
					);
			}
			
		    batch.execute();
		    
		    
			//Now that the rows are in the database, let's get the cell count, mean, min, max and create the metric summary records
			DSL.using(JooqUtil.getJooqConfiguration())
			.insertInto(AIR_QUALITY_LAYER_METRICS
					, AIR_QUALITY_LAYER_METRICS.AIR_QUALITY_LAYER_ID
					, AIR_QUALITY_LAYER_METRICS.METRIC_ID
					, AIR_QUALITY_LAYER_METRICS.SEASONAL_METRIC_ID
					, AIR_QUALITY_LAYER_METRICS.ANNUAL_STATISTIC_ID
					, AIR_QUALITY_LAYER_METRICS.CELL_COUNT
					, AIR_QUALITY_LAYER_METRICS.MIN_VALUE
					, AIR_QUALITY_LAYER_METRICS.MAX_VALUE
					, AIR_QUALITY_LAYER_METRICS.MEAN_VALUE
					, AIR_QUALITY_LAYER_METRICS.PCT_2_5
					, AIR_QUALITY_LAYER_METRICS.PCT_97_5
					)
			.select(
    		DSL.select(
    				AIR_QUALITY_CELL.AIR_QUALITY_LAYER_ID
    				, AIR_QUALITY_CELL.METRIC_ID
    				, AIR_QUALITY_CELL.SEASONAL_METRIC_ID
    				, AIR_QUALITY_CELL.ANNUAL_STATISTIC_ID
    				, DSL.count().as("cell_count")
    				, DSL.min(AIR_QUALITY_CELL.VALUE).as("min_value")
    				, DSL.max(AIR_QUALITY_CELL.VALUE).as("max_value")
    				, DSL.avg(AIR_QUALITY_CELL.VALUE).cast(Double.class).as("mean_value")
    				, DSL.percentileCont(0.025).withinGroupOrderBy(AIR_QUALITY_CELL.VALUE).cast(Double.class).as("pct_2_5")
    				, DSL.percentileCont(0.975).withinGroupOrderBy(AIR_QUALITY_CELL.VALUE).cast(Double.class).as("pct_97_5")
    				)
    		.from(AIR_QUALITY_CELL)
			.where(AIR_QUALITY_CELL.AIR_QUALITY_LAYER_ID.eq(aqLayerId))
			.groupBy(
					AIR_QUALITY_CELL.AIR_QUALITY_LAYER_ID
					, AIR_QUALITY_CELL.METRIC_ID
					, AIR_QUALITY_CELL.SEASONAL_METRIC_ID
					, AIR_QUALITY_CELL.ANNUAL_STATISTIC_ID
			))
			.execute();
		
		} catch (Exception e) {
			log.error("Error importing AQ file", e);
			
			response.type("application/json");
			//response.status(400);
			validationMsg.success=false;
			validationMsg.messages.add(new ValidationMessage.Message("error","Error occurred during import of air quality file."));
			deleteAirQualityLayerDefinition(aqLayerId, userProfile);
			return transformValMsgToJSON(validationMsg);
		}
		
		response.type("application/json");
		validationMsg.success = true;
		return transformValMsgToJSON(validationMsg); 
	}
	
	// This version of this method is used when an error occurs during AQ surface upload to clean up
	private static void deleteAirQualityLayerDefinition(Integer aqLayerId, Optional<UserProfile> userProfile) {
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());
		try {
			create.deleteFrom(AIR_QUALITY_CELL).where(AIR_QUALITY_CELL.AIR_QUALITY_LAYER_ID.eq(aqLayerId)).execute();
			create.deleteFrom(AIR_QUALITY_LAYER_METRICS).where(AIR_QUALITY_LAYER_METRICS.AIR_QUALITY_LAYER_ID.eq(aqLayerId)).execute();
			create.deleteFrom(AIR_QUALITY_LAYER).where(AIR_QUALITY_LAYER.ID.eq(aqLayerId)).execute();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			// We want to silently fail in this case. At least we tried to clean up.
		}
	}

	/**
	 * Deletes an air quality layer definition from the database (aq layer id is a request parameter).
	 * @param request
	 * @param response
	 * @param userProfile
	 * @return
	 */
	public static Object deleteAirQualityLayerDefinition(Request request, Response response, Optional<UserProfile> userProfile) {
		
		Integer id;
		try {
			id = Integer.valueOf(request.params("id"));
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return CoreApi.getErrorResponseInvalidId(request, response);
		} 
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());
		
		AirQualityLayerRecord layerResult = create.selectFrom(AIR_QUALITY_LAYER).where(AIR_QUALITY_LAYER.ID.eq(id)).fetchAny();
		if(layerResult == null) {
			return CoreApi.getErrorResponseNotFound(request, response);
		}
		
		//Nobody can delete shared layers
		//All users can delete their own layers
		//Admins can delete any non-shared layers
		if(layerResult.getShareScope() == Constants.SHARING_ALL || !(layerResult.getUserId().equalsIgnoreCase(userProfile.get().getId()) || CoreApi.isAdmin(userProfile)) )  {
			return CoreApi.getErrorResponseForbidden(request, response);
		}
		
		int cellRows = create.deleteFrom(AIR_QUALITY_CELL).where(AIR_QUALITY_CELL.AIR_QUALITY_LAYER_ID.eq(id)).execute();
		int statRows = create.deleteFrom(AIR_QUALITY_LAYER_METRICS).where(AIR_QUALITY_LAYER_METRICS.AIR_QUALITY_LAYER_ID.eq(id)).execute();
		int headerRows = create.deleteFrom(AIR_QUALITY_LAYER).where(AIR_QUALITY_LAYER.ID.eq(id)).execute();

		
		if(cellRows + statRows + headerRows == 0) {
			return CoreApi.getErrorResponse(request, response, 400, "Unknown error");
		} else {
			response.status(204);
			return response;
		}
	} 
	
	/**
	 * 
	 * @param filterValue
	 * @return a condition object representing an air quality layer filter condition.
	 */
	private static Condition buildAirQualityLayersFilterCondition(String filterValue) {

		Condition filterCondition = DSL.trueCondition();
		Condition searchCondition = DSL.falseCondition();

		Integer filterValueAsInteger = DataConversionUtil.getFilterValueAsInteger(filterValue);
		Long filterValueAsLong = DataConversionUtil.getFilterValueAsLong(filterValue);
		Double filterValueAsDouble = DataConversionUtil.getFilterValueAsDouble(filterValue);
		Date filterValueAsDate = DataConversionUtil.getFilterValueAsDate(filterValue, "MM/dd/yyyy");
		
		searchCondition = 
				searchCondition.or(AIR_QUALITY_LAYER.NAME
						.containsIgnoreCase(filterValue));

		searchCondition = 
				searchCondition.or(GRID_DEFINITION.NAME
						.containsIgnoreCase(filterValue));

		if (null != filterValueAsInteger) {
			searchCondition = 
					searchCondition.or(AIR_QUALITY_LAYER_METRICS.CELL_COUNT
							.eq(filterValueAsInteger));
		}

		if (null != filterValueAsDouble) {
			searchCondition = 
					searchCondition.or(AIR_QUALITY_LAYER_METRICS.MEAN_VALUE
							.eq(filterValueAsDouble));		
		}
		
		filterCondition = filterCondition.and(searchCondition);

		return filterCondition;
	}

	/**
	 * 
	 * @param filterValue
	 * @return a condition object representing an air quality cell filter condition.
	 */
	private static Condition buildAirQualityCellsFilterCondition(String filterValue) {

		Condition filterCondition = DSL.trueCondition();
		Condition searchCondition = DSL.falseCondition();

		Integer filterValueAsInteger = DataConversionUtil.getFilterValueAsInteger(filterValue);
		Long filterValueAsLong = DataConversionUtil.getFilterValueAsLong(filterValue);
		Double filterValueAsDouble = DataConversionUtil.getFilterValueAsDouble(filterValue);
		BigDecimal filterValueAsBigDecimal = DataConversionUtil.getFilterValueAsBigDecimal(filterValue);
		Date filterValueAsDate = DataConversionUtil.getFilterValueAsDate(filterValue, "MM/dd/yyyy");
		

		searchCondition = 
				searchCondition.or(POLLUTANT_METRIC.NAME
						.containsIgnoreCase(filterValue));

		searchCondition = 
				searchCondition.or(SEASONAL_METRIC.NAME
						.containsIgnoreCase(filterValue));

		searchCondition = 
				searchCondition.or(STATISTIC_TYPE.NAME
						.containsIgnoreCase(filterValue));


		if (null != filterValueAsInteger) {

			searchCondition = 
					searchCondition.or(AIR_QUALITY_CELL.GRID_COL
							.eq(filterValueAsInteger));

			searchCondition = 
					searchCondition.or(AIR_QUALITY_CELL.GRID_ROW
							.eq(filterValueAsInteger));
		}

		if (null != filterValueAsDouble) {
			searchCondition = 
					searchCondition.or(AIR_QUALITY_CELL.VALUE
							.eq(filterValueAsDouble));		
		}
		
		filterCondition = filterCondition.and(searchCondition);

		return filterCondition;
	}

	/**
	 * Sets the sort order of the air quality layers.
	 * @param sortBy
	 * @param descending
	 * @param orderFields
	 */
	private static void setAirQualityLayersSortOrder(
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
	 * Sets the sort order of the air quality cells.
	 * @param sortBy
	 * @param descending
	 * @param orderFields
	 */
	private static void setAirQualityCellsSortOrder(
			String sortBy, Boolean descending, List<OrderField<?>> orderFields) {
		
		if (null != sortBy) {
			
			SortOrder sortDirection = SortOrder.ASC;
			Field<?> sortField = null;
			
			sortDirection = descending ? SortOrder.DESC : SortOrder.ASC;
			
			switch (sortBy) {
			case "grid_col":
				sortField = DSL.field(sortBy, Integer.class.getName());
				orderFields.add(sortField.sort(sortDirection));
				break;

			case "grid_row":
				sortField = DSL.field(sortBy, Integer.class.getName());
				orderFields.add(sortField.sort(sortDirection));
				break;

			case "seasonal_metric":
				sortField = DSL.field(sortBy, String.class.getName());
				orderFields.add(sortField.sort(sortDirection));
				break;

			case "annual_metric":
				sortField = DSL.field(sortBy, String.class.getName());
				orderFields.add(sortField.sort(sortDirection));
				break;

			case "value":
				sortField = DSL.field(sortBy, Double.class.getName());
				orderFields.add(sortField.sort(sortDirection));
				break;

			default:
				//System.out.println("... in default...");
				orderFields.add(DSL.field("grid_col", Integer.class.getName()).sort(SortOrder.ASC));	
				orderFields.add(DSL.field("grid_row", Integer.class.getName()).sort(SortOrder.ASC));	
				break;
			}
			
		} else {
			orderFields.add(DSL.field("grid_col", Integer.class.getName()).sort(SortOrder.ASC));	
			orderFields.add(DSL.field("grid_row", Integer.class.getName()).sort(SortOrder.ASC));	
		}
	}

	/**
	 * Transforms records into a JsonNode.
	 * @param records
	 * @return the trasformed records as a JsonNode.
	 */
	private static JsonNode transformRecordsToJSON(Record records) {
		
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode data = mapper.createObjectNode();

        JsonNode recordsJSON = null;
		try {
			JsonFactory factory = mapper.getFactory();
			JsonParser jp = factory.createParser(
					records.formatJSON(new JSONFormat().header(false).recordFormat(RecordFormat.OBJECT)));
			recordsJSON = mapper.readTree(jp);
		} catch (JsonParseException e) {
			log.error("Error parsing JSON",e);
		} catch (JsonProcessingException e) {
			log.error("Error processing JSON",e);
		} catch (IOException e) {
			log.error("IO Exception", e);
		}
		
		return recordsJSON;
		
	}
	
	/**
	 * Transforms a validation message into a JsonNode
	 * @param validationMessage
	 * @return the transformed validation message as a JsonNode.
	 */
	private static JsonNode transformValMsgToJSON(ValidationMessage validationMessage) {
		
		ObjectMapper mapper = new ObjectMapper();
		JsonNode recordsJSON = null;
		//ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		try {
			recordsJSON = mapper.valueToTree(validationMessage);
		} catch (Exception e) {
			log.error("Error converting validation message to JSON",e);
		} 
		
		return recordsJSON;
		
	}

}

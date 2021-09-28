package gov.epa.bencloud.api;

import static gov.epa.bencloud.server.database.jooq.data.Tables.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.InsertValuesStep8;
import org.jooq.JSONFormat;
import org.jooq.JSONFormat.RecordFormat;
import org.jooq.OrderField;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Record12;
import org.jooq.Record13;
import org.jooq.Record14;
import org.jooq.Record6;
import org.jooq.Result;
import org.jooq.SortOrder;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jooq.tools.csv.CSVReader;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import gov.epa.bencloud.api.util.AirQualityUtil;
import gov.epa.bencloud.api.util.ApiUtil;
import gov.epa.bencloud.server.database.JooqUtil;
import gov.epa.bencloud.server.database.jooq.data.tables.records.AirQualityCellRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.AirQualityLayerRecord;
import gov.epa.bencloud.server.util.DataConversionUtil;
import gov.epa.bencloud.server.util.ParameterUtil;
import spark.Request;
import spark.Response;

public class AirQualityApi {

	public static Object getAirQualityLayerDefinitions(Request request, Response response) {
		
		int pollutantId = ParameterUtil.getParameterValueAsInteger(request.raw().getParameter("pollutantId"), 0);
		
		int page = ParameterUtil.getParameterValueAsInteger(request.raw().getParameter("page"), 1);
		int rowsPerPage = ParameterUtil.getParameterValueAsInteger(request.raw().getParameter("rowsPerPage"), 10);
		String sortBy = ParameterUtil.getParameterValueAsString(request.raw().getParameter("sortBy"), "");
		boolean descending = ParameterUtil.getParameterValueAsBoolean(request.raw().getParameter("descending"), false);
		String filter = ParameterUtil.getParameterValueAsString(request.raw().getParameter("filter"), "");
		
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
		
		//System.out.println(orderFields);
	

		Integer filteredRecordsCount = 
				DSL.using(JooqUtil.getJooqConfiguration()).select(DSL.count())
				.from(AIR_QUALITY_LAYER)
				.join(POLLUTANT).on(POLLUTANT.ID.eq(AIR_QUALITY_LAYER.POLLUTANT_ID))				
				.join(GRID_DEFINITION).on(GRID_DEFINITION.ID.eq(AIR_QUALITY_LAYER.GRID_DEFINITION_ID))
				
				.where(filterCondition)
				.fetchOne(DSL.count());
		
		System.out.println("filteredRecordsCount: " + filteredRecordsCount);
		
		Result<Record14<Integer, String, Integer, BigDecimal, BigDecimal, BigDecimal, BigDecimal, BigDecimal, Boolean, Integer, String, Integer, String, String>> aqRecords = 
			DSL.using(JooqUtil.getJooqConfiguration())
				.select(
						AIR_QUALITY_LAYER.ID, 
						AIR_QUALITY_LAYER.NAME,
						AIR_QUALITY_LAYER.CELL_COUNT,
						AIR_QUALITY_LAYER.MIN_VALUE,
						AIR_QUALITY_LAYER.MAX_VALUE,
						AIR_QUALITY_LAYER.MEAN_VALUE,
						AIR_QUALITY_LAYER.PCT_2_5,
						AIR_QUALITY_LAYER.PCT_97_5,
						AIR_QUALITY_LAYER.LOCKED,
						AIR_QUALITY_LAYER.GRID_DEFINITION_ID,
						GRID_DEFINITION.NAME.as("grid_definition_name"),
						AIR_QUALITY_LAYER.POLLUTANT_ID,
						POLLUTANT.NAME.as("pollutant_name"),
						POLLUTANT.FRIENDLY_NAME.as("pollutant_friendly_name")
						)
				.from(AIR_QUALITY_LAYER)
				.join(POLLUTANT).on(POLLUTANT.ID.eq(AIR_QUALITY_LAYER.POLLUTANT_ID))				
				.join(GRID_DEFINITION).on(GRID_DEFINITION.ID.eq(AIR_QUALITY_LAYER.GRID_DEFINITION_ID))
				
				.where(filterCondition)
				.orderBy(orderFields)
				.offset((page * rowsPerPage) - rowsPerPage)
				.limit(rowsPerPage)
				.fetch();
	
		
		System.out.println("aqRecords: " + aqRecords.size());

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
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//System.out.println(data);
		//System.out.println(aqRecords.formatJSON(new JSONFormat().header(false).recordFormat(RecordFormat.OBJECT)));
		
		response.type("application/json");
		return data;
	}
	
	/**
	 * @param request - expected to contain id param
	 * @param response
	 * @return Single air quality layer definition as json string 
	 */
	public static Object getAirQualityLayerDefinition(Request request, Response response) {
		Integer id = Integer.valueOf(request.params("id"));
		
		Record14<Integer, String, Integer, BigDecimal, BigDecimal, BigDecimal, BigDecimal, BigDecimal, Boolean, Integer, Integer, String, String, String> aqRecord = getAirQualityLayerDefinition(id);
		response.type("application/json");
		return aqRecord.formatJSON(new JSONFormat().header(false).recordFormat(RecordFormat.OBJECT));
	}
	
	private static Record14<Integer, String, Integer, BigDecimal, BigDecimal, BigDecimal, BigDecimal, BigDecimal, Boolean, Integer, Integer, String, String, String> getAirQualityLayerDefinition(Integer id) {
		return DSL.using(JooqUtil.getJooqConfiguration())
		.select(
				AIR_QUALITY_LAYER.ID, 
				AIR_QUALITY_LAYER.NAME,
				AIR_QUALITY_LAYER.CELL_COUNT,
				AIR_QUALITY_LAYER.MIN_VALUE,
				AIR_QUALITY_LAYER.MAX_VALUE,
				AIR_QUALITY_LAYER.MEAN_VALUE,
				AIR_QUALITY_LAYER.PCT_2_5,
				AIR_QUALITY_LAYER.PCT_97_5,
				AIR_QUALITY_LAYER.LOCKED,
				AIR_QUALITY_LAYER.GRID_DEFINITION_ID,
				AIR_QUALITY_LAYER.POLLUTANT_ID,
				POLLUTANT.NAME.as("pollutant_name"), 
				POLLUTANT.FRIENDLY_NAME.as("pollutant_friendly_name"),
				GRID_DEFINITION.NAME.as("grid_definition_name"))
		.from(AIR_QUALITY_LAYER)
		.join(POLLUTANT).on(POLLUTANT.ID.eq(AIR_QUALITY_LAYER.POLLUTANT_ID))				
		.join(GRID_DEFINITION).on(GRID_DEFINITION.ID.eq(AIR_QUALITY_LAYER.GRID_DEFINITION_ID))
		.where(AIR_QUALITY_LAYER.ID.eq(id))
		.fetchOne();
	}

	public static Integer getAirQualityLayerGridId(Integer id) {
		return DSL.using(JooqUtil.getJooqConfiguration())
		.select(
				AIR_QUALITY_LAYER.GRID_DEFINITION_ID)
		.from(AIR_QUALITY_LAYER)
		.where(AIR_QUALITY_LAYER.ID.eq(id))
		.fetchOne().value1();
	}
	
	public static Object getAirQualityLayerDetails(Request request, Response response) {
		
		Integer id = Integer.valueOf(request.params("id"));
		
		System.out.println("in getAirQualityLayerDetails");
		
		int page = ParameterUtil.getParameterValueAsInteger(request.raw().getParameter("page"), 1);
		int rowsPerPage = ParameterUtil.getParameterValueAsInteger(request.raw().getParameter("rowsPerPage"), 10);
		String sortBy = ParameterUtil.getParameterValueAsString(request.raw().getParameter("sortBy"), "");
		boolean descending = ParameterUtil.getParameterValueAsBoolean(request.raw().getParameter("descending"), false);
		String filter = ParameterUtil.getParameterValueAsString(request.raw().getParameter("filter"), "");

		System.out.println("id: " + id);
		System.out.println("filter: " + filter);
		System.out.println("rowsPerPage: " + rowsPerPage);

		Condition filterCondition = DSL.trueCondition();
		Condition airQualityLayerCondition = DSL.trueCondition();
		
		if (id != 0) {
			airQualityLayerCondition = DSL.field(AIR_QUALITY_CELL.AIR_QUALITY_LAYER_ID).eq(id);
			filterCondition = filterCondition.and(airQualityLayerCondition);
		}

		if (!"".equals(filter)) {
			filterCondition = filterCondition.and(buildAirQualityCellsFilterCondition(filter));
		}

	
		List<OrderField<?>> orderFields = new ArrayList<>();
		
		System.out.println("sortBy: " + sortBy);
		
		setAirQualityCellsSortOrder(sortBy, descending, orderFields);


		Integer filteredRecordsCount = 
				DSL.using(JooqUtil.getJooqConfiguration()).select(DSL.count())
				.from(AIR_QUALITY_CELL)
				.leftJoin(POLLUTANT_METRIC).on(AIR_QUALITY_CELL.METRIC_ID.eq(POLLUTANT_METRIC.ID))
				.leftJoin(SEASONAL_METRIC).on(AIR_QUALITY_CELL.SEASONAL_METRIC_ID.eq(SEASONAL_METRIC.ID))
				.where(filterCondition)
				.fetchOne(DSL.count());

		System.out.println("filteredRecordsCount: " + filteredRecordsCount);

		Result<Record6<Integer, Integer, String, String, String, BigDecimal>> aqRecords = DSL.using(JooqUtil.getJooqConfiguration())
				.select(
						AIR_QUALITY_CELL.GRID_COL,
						AIR_QUALITY_CELL.GRID_ROW,
						POLLUTANT_METRIC.NAME.as("metric"),
						SEASONAL_METRIC.NAME.as("seasonal_metric"),
						AIR_QUALITY_CELL.ANNUAL_METRIC,
						AIR_QUALITY_CELL.VALUE
						)
				.from(AIR_QUALITY_CELL)
				.leftJoin(POLLUTANT_METRIC).on(AIR_QUALITY_CELL.METRIC_ID.eq(POLLUTANT_METRIC.ID))
				.leftJoin(SEASONAL_METRIC).on(AIR_QUALITY_CELL.SEASONAL_METRIC_ID.eq(SEASONAL_METRIC.ID))

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

			System.out.println("aqRecords: " + aqRecords.size());

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
				e.printStackTrace();
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			response.type("application/json");
			return data;
		}
	}

	public static String createFilename(String layerName) {
		// Currently allowing periods so we don't break extensions. Need to improve this.
		return layerName.replaceAll("[^A-Za-z0-9._-]+", "") + ".csv";
	}


	public static Map<Integer, AirQualityCellRecord> getAirQualityLayerMap(Integer id) {

		Map<Integer, AirQualityCellRecord> aqRecords = DSL.using(JooqUtil.getJooqConfiguration())
				.selectFrom(AIR_QUALITY_CELL)
				.where(AIR_QUALITY_CELL.AIR_QUALITY_LAYER_ID.eq(id))
				.orderBy(AIR_QUALITY_CELL.GRID_COL, AIR_QUALITY_CELL.GRID_ROW)
				.fetchMap(AIR_QUALITY_CELL.GRID_CELL_ID);		
		return aqRecords;
	}
	
	public static Object postAirQualityLayer(Request request, String layerName, Integer pollutantId, Integer gridId, String layerType, Response response) {

		System.out.println("postAirQualityLayer");

		AirQualityLayerRecord aqRecord=null;
		
		//Create the air_quality_cell records
		try (InputStream is = request.raw().getPart("file").getInputStream()) {
			
			CSVReader csvReader = new CSVReader (new InputStreamReader(is));
			
			int columnIdx=-999;
			int rowIdx=-999;
			int metricIdx=-999;
			int seasonalMetricIdx=-999;
			int annualMetricIdx=-999;
			int valuesIdx=-999;

			String[] record;
			// Read the header
			record = csvReader.readNext();
			for(int i=0; i < record.length; i++) {
				switch(record[i].toLowerCase().replace(" ", "")) {
				case "column":
					columnIdx=i;
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
				case "values":
					valuesIdx=i;
					break;
				}
			}
			String tmp = AirQualityUtil.validateModelColumnHeadings(columnIdx, rowIdx, metricIdx, seasonalMetricIdx, annualMetricIdx, valuesIdx);
			if(tmp.length() > 0) {
				response.status(400);
				System.out.println("The following columns are missing: " + tmp);
				return "The following columns are missing: " + tmp;
			}
			
			Map<String, Integer> pollutantMetricIdLookup = AirQualityUtil.getPollutantMetricIdLookup(pollutantId);
			
			//TODO: Finish creating lookups for these metrics so we can fill them in properly in the insert below
			Map<String, Integer> seasonalMetricIdLookup = AirQualityUtil.getSeasonalMetricIdLookup(pollutantId);
			
			
			//TODO: Validate each record and abort before the batch.execute() if there's a problem.
			//We might also need to clean up the header. Or, maybe we should make this a transaction?
			
			//Create the air_quality_layer record
			aqRecord = DSL.using(JooqUtil.getJooqConfiguration())
			.insertInto(AIR_QUALITY_LAYER, AIR_QUALITY_LAYER.NAME, AIR_QUALITY_LAYER.POLLUTANT_ID, AIR_QUALITY_LAYER.GRID_DEFINITION_ID, AIR_QUALITY_LAYER.LOCKED)
			.values(layerName, pollutantId, gridId, false)
			.returning(AIR_QUALITY_LAYER.ID, AIR_QUALITY_LAYER.NAME, AIR_QUALITY_LAYER.POLLUTANT_ID, AIR_QUALITY_LAYER.GRID_DEFINITION_ID)
			.fetchOne();
			
			// Read the data rows and write to the db	
			InsertValuesStep8<AirQualityCellRecord, Integer, Integer, Integer, Integer, Integer, Integer, String, BigDecimal> batch = DSL.using(JooqUtil.getJooqConfiguration())
					.insertInto(
							AIR_QUALITY_CELL, 
							AIR_QUALITY_CELL.AIR_QUALITY_LAYER_ID,
							AIR_QUALITY_CELL.GRID_COL, 
							AIR_QUALITY_CELL.GRID_ROW,
							AIR_QUALITY_CELL.GRID_CELL_ID,
							AIR_QUALITY_CELL.METRIC_ID,
							AIR_QUALITY_CELL.SEASONAL_METRIC_ID,
							AIR_QUALITY_CELL.ANNUAL_METRIC,
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
				
				// Add a record to the batch
				batch.values(
						aqRecord.value1(), 
						Integer.valueOf(record[columnIdx]), 
						Integer.valueOf(record[rowIdx]),
						ApiUtil.getCellId(Integer.valueOf(record[columnIdx]), Integer.valueOf(record[rowIdx])),
						pollutantMetricIdLookup.get(metricNameLowerCase), 
						seasonalMetricIdLookup.get(seasonalMetricLowerCase), 
						record[annualMetricIdx],
						BigDecimal.valueOf(Double.valueOf(record[valuesIdx]))
					);
			}
			
		    batch.execute();
		    
		    
			//Now that the rows are in the database, let's get the cell count, mean, min, max and update the layer's header record
			DSL.using(JooqUtil.getJooqConfiguration())
			.update(AIR_QUALITY_LAYER)
			.set(DSL.row(AIR_QUALITY_LAYER.CELL_COUNT, AIR_QUALITY_LAYER.MIN_VALUE, AIR_QUALITY_LAYER.MAX_VALUE, AIR_QUALITY_LAYER.MEAN_VALUE, AIR_QUALITY_LAYER.PCT_2_5, AIR_QUALITY_LAYER.PCT_97_5),	
    		DSL.select(
				DSL.count().as("cell_count"),
				DSL.min(AIR_QUALITY_CELL.VALUE).as("min_value"),
				DSL.max(AIR_QUALITY_CELL.VALUE).as("max_value"),
				DSL.avg(AIR_QUALITY_CELL.VALUE).as("mean_value"),
				DSL.percentileCont(0.025).withinGroupOrderBy(AIR_QUALITY_CELL.VALUE).as("pct_2_5"),
				DSL.percentileCont(0.975).withinGroupOrderBy(AIR_QUALITY_CELL.VALUE).as("pct_97_5")
    		)
    		.from(AIR_QUALITY_CELL)
			.where(AIR_QUALITY_CELL.AIR_QUALITY_LAYER_ID.eq(aqRecord.value1()))
			)
			.where(AIR_QUALITY_LAYER.ID.eq(aqRecord.value1()))
			.execute();
		
			
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		response.type("application/json");
		return getAirQualityLayerDefinition(aqRecord.value1()).formatJSON(new JSONFormat().header(false).recordFormat(RecordFormat.OBJECT));
	}
	
	public static ObjectNode getAirQualityLayers(String userIdentifier) {

//		System.out.println("getCompletedTasks");
//		System.out.println("userIdentifier: " + userIdentifier);
		
//		System.out.println("length: " + postParameters.get("length")[0]);
//		System.out.println("start: " + postParameters.get("start")[0]);
//		System.out.println("searchValue: " + postParameters.get("searchValue")[0]);
//		System.out.println("sortColumn: " + postParameters.get("sortColumn")[0]);
//		System.out.println("sortDirection: " + postParameters.get("sortDirection")[0]);

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode data = mapper.createObjectNode();
        
        ArrayNode airQualityLayers = mapper.createArrayNode();
        ObjectNode airQualityLayer = mapper.createObjectNode();

        int records = 0;
        
		if (null != userIdentifier) {

			try {

				Result<Record12<Integer, String, Integer, BigDecimal, BigDecimal, BigDecimal, BigDecimal, BigDecimal, Integer, Integer, String, String>> aqRecords = DSL.using(JooqUtil.getJooqConfiguration())
						.select(
								AIR_QUALITY_LAYER.ID, 
								AIR_QUALITY_LAYER.NAME,
								AIR_QUALITY_LAYER.CELL_COUNT,
								AIR_QUALITY_LAYER.MIN_VALUE,
								AIR_QUALITY_LAYER.MAX_VALUE,
								AIR_QUALITY_LAYER.MEAN_VALUE,
								AIR_QUALITY_LAYER.PCT_2_5,
								AIR_QUALITY_LAYER.PCT_97_5,
								AIR_QUALITY_LAYER.GRID_DEFINITION_ID,
								AIR_QUALITY_LAYER.POLLUTANT_ID,
								POLLUTANT.NAME, 
								GRID_DEFINITION.NAME)
						.from(AIR_QUALITY_LAYER)
						.join(POLLUTANT).on(POLLUTANT.ID.eq(AIR_QUALITY_LAYER.POLLUTANT_ID))				
						.join(GRID_DEFINITION).on(GRID_DEFINITION.ID.eq(AIR_QUALITY_LAYER.GRID_DEFINITION_ID))
						.orderBy(AIR_QUALITY_LAYER.NAME)
						.fetch();

				for (Record record : aqRecords) {

					airQualityLayer = mapper.createObjectNode();

					airQualityLayer.put("id", record.getValue(AIR_QUALITY_LAYER.ID));
					airQualityLayer.put("name", record.getValue(AIR_QUALITY_LAYER.NAME));
					airQualityLayer.put("pollutant_name", record.getValue(POLLUTANT.NAME));
					airQualityLayer.put("grid_definition_name", record.getValue(GRID_DEFINITION.NAME));
					
					airQualityLayers.add(airQualityLayer);
					records++;
					
				}
				
				data.set("data", airQualityLayers);
				data.put("success", true);
				data.put("recordsFiltered", records);
				data.put("recordsTotal", records);
				
			} catch (DataAccessException e) {
				data.put("success", false);
				data.put("error_message", e.getMessage());
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				data.put("success", false);
				data.put("error_message", e.getMessage());
				e.printStackTrace();
			}
		}

		return data;
	}

	public static boolean deleteAirQualityLayerDefinition(Request request, Response response) {
		Integer id = Integer.valueOf(request.params("id"));
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());
		
		int cellRows = create.deleteFrom(AIR_QUALITY_CELL).where(AIR_QUALITY_CELL.AIR_QUALITY_LAYER_ID.eq(id)).execute();
		int headerRows = create.deleteFrom(AIR_QUALITY_LAYER).where(AIR_QUALITY_LAYER.ID.eq(id)).execute();
		
		if(cellRows + headerRows == 0) {
			return false;
		} else {
			return true;
		}
	} 
	
	private static Condition buildAirQualityLayersFilterCondition(String filterValue) {

		Condition filterCondition = DSL.trueCondition();
		Condition searchCondition = DSL.falseCondition();

		Integer filterValueAsInteger = DataConversionUtil.getFilterValueAsInteger(filterValue);
		Long filterValueAsLong = DataConversionUtil.getFilterValueAsLong(filterValue);
		Double filterValueAsDouble = DataConversionUtil.getFilterValueAsDouble(filterValue);
		BigDecimal filterValueAsBigDecimal = DataConversionUtil.getFilterValueAsBigDecimal(filterValue);
		Date filterValueAsDate = DataConversionUtil.getFilterValueAsDate(filterValue, "MM/dd/yyyy");
		
		searchCondition = 
				searchCondition.or(AIR_QUALITY_LAYER.NAME
						.containsIgnoreCase(filterValue));

		searchCondition = 
				searchCondition.or(GRID_DEFINITION.NAME
						.containsIgnoreCase(filterValue));

		if (null != filterValueAsInteger) {
			searchCondition = 
					searchCondition.or(AIR_QUALITY_LAYER.CELL_COUNT
							.eq(filterValueAsInteger));
		}

		if (null != filterValueAsBigDecimal) {
			searchCondition = 
					searchCondition.or(AIR_QUALITY_LAYER.MEAN_VALUE
							.eq(filterValueAsBigDecimal));		
		}
		
		filterCondition = filterCondition.and(searchCondition);

		return filterCondition;
	}

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
				searchCondition.or(AIR_QUALITY_CELL.ANNUAL_METRIC
						.containsIgnoreCase(filterValue));


		if (null != filterValueAsInteger) {

			searchCondition = 
					searchCondition.or(AIR_QUALITY_CELL.GRID_COL
							.eq(filterValueAsInteger));

			searchCondition = 
					searchCondition.or(AIR_QUALITY_CELL.GRID_ROW
							.eq(filterValueAsInteger));
		}

		if (null != filterValueAsBigDecimal) {
			searchCondition = 
					searchCondition.or(AIR_QUALITY_CELL.VALUE
							.eq(filterValueAsBigDecimal));		
		}
		
		filterCondition = filterCondition.and(searchCondition);

		return filterCondition;
	}

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
				System.out.println("... in default...");
				orderFields.add(DSL.field("grid_col", Integer.class.getName()).sort(SortOrder.ASC));	
				orderFields.add(DSL.field("grid_row", Integer.class.getName()).sort(SortOrder.ASC));	
				break;
			}
			
		} else {
			orderFields.add(DSL.field("grid_col", Integer.class.getName()).sort(SortOrder.ASC));	
			orderFields.add(DSL.field("grid_row", Integer.class.getName()).sort(SortOrder.ASC));	
		}
	}

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
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return recordsJSON;
		
	}
}

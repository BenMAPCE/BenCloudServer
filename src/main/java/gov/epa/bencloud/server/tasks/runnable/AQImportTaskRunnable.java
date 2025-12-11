package gov.epa.bencloud.server.tasks.runnable;




import static gov.epa.bencloud.server.database.jooq.data.Tables.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.feature.Property;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.AttributeType;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.feature.type.GeometryType;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.DataUtilities;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.geotools.util.URLs;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep8;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import gov.epa.bencloud.Constants;
import gov.epa.bencloud.api.AirQualityApi;
import gov.epa.bencloud.api.model.AirQualityImportFileConfig;
import gov.epa.bencloud.api.model.AirQualityImportTaskConfig;
import gov.epa.bencloud.api.model.AirQualityImportTaskLog;
import gov.epa.bencloud.api.model.GridImportTaskConfig;
import gov.epa.bencloud.api.model.GridImportTaskLog;
import gov.epa.bencloud.api.model.ValidationMessage;
import gov.epa.bencloud.api.util.AirQualityUtil;
import gov.epa.bencloud.api.util.ApiUtil;
import gov.epa.bencloud.api.util.FilestoreUtil;
import gov.epa.bencloud.api.util.GridImportUtil;
import gov.epa.bencloud.server.database.JooqUtil;
import gov.epa.bencloud.server.database.PooledDataSource;
import gov.epa.bencloud.server.database.jooq.data.tables.records.AirQualityCellRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.AirQualityLayerRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.GridDefinitionRecord;
import gov.epa.bencloud.server.tasks.TaskComplete;
import gov.epa.bencloud.server.tasks.TaskQueue;
import gov.epa.bencloud.server.tasks.model.Task;
import gov.epa.bencloud.server.tasks.model.TaskMessage;
import gov.epa.bencloud.server.util.ApplicationUtil;
import org.geotools.geometry.jts.ReferencedEnvelope;

/*
 * Import a user uploaded shapefile as a grid definition.
 */
public class AQImportTaskRunnable implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(AQImportTaskRunnable.class);
    protected static ObjectMapper objectMapper = new ObjectMapper();
    
	private String taskUuid;
	private String taskWorkerUuid;
	private ValidationMessage validationMsg = new ValidationMessage();


	/**
	 * Creates a AQImportTaskRunnable object with the given taskUuid and taskWorkerUuid
	 * @param taskUuid
	 * @param taskWorkerUuid
	 */
	public AQImportTaskRunnable(String taskUuid, String taskWorkerUuid) {
		this.taskUuid = taskUuid;
		this.taskWorkerUuid = taskWorkerUuid;
	}

	private boolean taskSuccessful = true;
	
	public void run() {
		
		log.info("Air Quality Import Task Begin: " + taskUuid);
		ObjectMapper mapper = new ObjectMapper();
		Task task = TaskQueue.getTaskFromQueueRecord(taskUuid);

		ArrayList<TaskMessage> messages = new ArrayList<TaskMessage>();
		
		try {
			AirQualityImportTaskConfig aqImportTaskConfig = null;
			aqImportTaskConfig = objectMapper.readValue(task.getParameters(), AirQualityImportTaskConfig.class);
			
			AirQualityImportTaskLog aqImportTaskLog = new AirQualityImportTaskLog(aqImportTaskConfig, task.getUserIdentifier());
			aqImportTaskLog.setDtStart(LocalDateTime.now());
			
			aqImportTaskLog.addMessage("Starting air quality import");
			messages.add(new TaskMessage("active", "Initializing"));
			
			TaskQueue.updateTaskPercentage(taskUuid, 1, mapper.writeValueAsString(messages));
			int idx = 0;
			Double pct = 0.0;

			for (AirQualityImportFileConfig theFile : aqImportTaskConfig.files) {
				messages.add(new TaskMessage("active", "Reading file " + theFile.layerName));
				TaskQueue.updateTaskPercentage(taskUuid, pct.intValue(), mapper.writeValueAsString(messages));

				Integer ret = importAQSurface(aqImportTaskConfig, theFile);
				if(ret == null || ret == 0) {
					// An issue occurred during import
					TaskComplete.addTaskToCompleteAndRemoveTaskFromQueue(taskUuid, taskWorkerUuid, false, "Task failed");
					log.error("Task failed " + taskUuid);
					return;
				}
				//Save the id for the surface that was created
				aqImportTaskConfig.files[idx].aqSurfaceId = ret;
				FilestoreUtil.deleteFile(theFile.filestoreId);
				pct = ((double) ++idx / aqImportTaskConfig.files.length) * 100;
				TaskQueue.updateTaskPercentage(taskUuid, pct.intValue());
				messages.get(messages.size()-1).setStatus("complete");
			}
			
			String completeMessage = String.format("Imported %d air quality surface" + (aqImportTaskConfig.files.length>1 ? "s" : "") , aqImportTaskConfig.files.length);
			aqImportTaskLog.addMessage(completeMessage);
			aqImportTaskLog.setSuccess(true);
			aqImportTaskLog.setDtEnd(LocalDateTime.now());
			
			AirQualityUtil.storeTaskLog(aqImportTaskLog);
			
			TaskComplete.addTaskToCompleteAndRemoveTaskFromQueue(taskUuid, taskWorkerUuid, taskSuccessful, completeMessage);

		} catch (Exception e) {
			TaskComplete.addTaskToCompleteAndRemoveTaskFromQueue(taskUuid, taskWorkerUuid, false, "Task failed");
			log.error("Task failed", e);
		}
		log.info("Air Quality Import Task Complete: " + taskUuid);
	}

	private Integer importAQSurface(AirQualityImportTaskConfig aqImportTaskConfig, AirQualityImportFileConfig theFile) {

		Integer aqLayerId = null;
		int columnIdx = -999;
		int rowIdx = -999;
		int metricIdx = -999;
		int seasonalMetricIdx = -999;
		int annualMetricIdx = -999;
		int valuesIdx = -999;
		Map<String, Integer> pollutantMetricIdLookup = new HashMap<>();		
		Map<String, Integer> seasonalMetricIdLookup = new HashMap<>();		
		Map<String, Integer> statisticIdLookup = new HashMap<>();

		/* OPEN FILE AND PREPARE FOR IMPORT */

		File csvFile = FilestoreUtil.getFile(theFile.filestoreId);

		CSVReader csvReader;
		try {
			csvReader = new CSVReader(new FileReader(csvFile));
		} catch (FileNotFoundException e) {
			// TODO: Log error message to validationMsg object
			return null;
		}
		
		// Read the header row
		String[] record;
		try {
			record = csvReader.readNext();


		} catch (CsvValidationException e) {
			// TODO: Log error message to validationMsg object
			try {
				csvReader.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} 

			return null;
		} catch (IOException e) {
			// TODO: Log error message to validationMsg object
			try {
				csvReader.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} 
			return null;
		} 

		// Identify required column positions on the header row
		// Allow either "column" or "col"; "values" or "value"
		for (int i = 0; i < record.length; i++) {
			switch (record[i].toLowerCase().replace(" ", "")) {
				case "column":
				case "col":
						columnIdx = i;
					break;
				case "row":
					rowIdx = i;
					break;
				case "metric":
					metricIdx = i;
					break;
				case "seasonalmetric":
					seasonalMetricIdx = i;
					break;
				case "annualmetric":
					annualMetricIdx = i;
					break;
				case "statistic":
					annualMetricIdx = i;
					break;
				case "values":
				case "value":
					valuesIdx = i;
					break;
			}
		}

		pollutantMetricIdLookup = AirQualityUtil.getPollutantMetricIdLookup(aqImportTaskConfig.pollutantId);
		seasonalMetricIdLookup = AirQualityUtil.getSeasonalMetricIdLookup(aqImportTaskConfig.pollutantId);
		statisticIdLookup = ApiUtil.getStatisticIdLookup();

	/* IMPORT THE DATA */
		try {
			AirQualityLayerRecord aqRecord = null;

			aqRecord=DSL.using(JooqUtil.getJooqConfiguration())
				.insertInto(
					AIR_QUALITY_LAYER,
					AIR_QUALITY_LAYER.NAME,
					AIR_QUALITY_LAYER.GROUP_NAME,
					AIR_QUALITY_LAYER.POLLUTANT_ID,
					AIR_QUALITY_LAYER.GRID_DEFINITION_ID,
					AIR_QUALITY_LAYER.USER_ID,
					AIR_QUALITY_LAYER.SHARE_SCOPE,
					AIR_QUALITY_LAYER.AQ_YEAR,
					AIR_QUALITY_LAYER.DESCRIPTION,
					AIR_QUALITY_LAYER.SOURCE,
					AIR_QUALITY_LAYER.DATA_TYPE,
					AIR_QUALITY_LAYER.FILENAME,
					AIR_QUALITY_LAYER.UPLOAD_DATE)
				.values(
					theFile.layerName,
					aqImportTaskConfig.groupName,
					aqImportTaskConfig.pollutantId,
					aqImportTaskConfig.gridId,
					aqImportTaskConfig.userId,
					Constants.SHARING_NONE,
					aqImportTaskConfig.aqYear.toString(),
					aqImportTaskConfig.description,
					aqImportTaskConfig.source,
					aqImportTaskConfig.dataType,
					theFile.layerName,
					LocalDateTime.now())
				.returning(
					AIR_QUALITY_LAYER.ID,
					AIR_QUALITY_LAYER.NAME,
					AIR_QUALITY_LAYER.POLLUTANT_ID,
					AIR_QUALITY_LAYER.GRID_DEFINITION_ID)
				.fetchOne();

			aqLayerId = aqRecord.value1();

			// Read the data rows and write to the db
			InsertValuesStep8<AirQualityCellRecord, Integer, Integer, Integer, Long, Integer, Integer, Integer, Double> batch = DSL
					.using(JooqUtil.getJooqConfiguration())
					.insertInto(
							AIR_QUALITY_CELL,
							AIR_QUALITY_CELL.AIR_QUALITY_LAYER_ID,
							AIR_QUALITY_CELL.GRID_COL,
							AIR_QUALITY_CELL.GRID_ROW,
							AIR_QUALITY_CELL.GRID_CELL_ID,
							AIR_QUALITY_CELL.METRIC_ID,
							AIR_QUALITY_CELL.SEASONAL_METRIC_ID,
							AIR_QUALITY_CELL.ANNUAL_STATISTIC_ID,
							AIR_QUALITY_CELL.VALUE);

			while((record=csvReader.readNext()) != null) {
				// Make sure this metric exists in the db. If not, add it and update pollutantMetricIdLookup now
				String metricNameLowerCase = record[metricIdx].toLowerCase();

				if (!pollutantMetricIdLookup.containsKey(metricNameLowerCase)) {
					pollutantMetricIdLookup.put(
							metricNameLowerCase,
							AirQualityUtil.createNewPollutantMetric(aqImportTaskConfig.pollutantId, record[metricIdx]));
				}

				String seasonalMetricLowerCase = metricNameLowerCase + "~" + record[seasonalMetricIdx].toLowerCase();

				if (!seasonalMetricIdLookup.containsKey(seasonalMetricLowerCase)) {
					seasonalMetricIdLookup.put(
							seasonalMetricLowerCase,
							AirQualityUtil.createNewSeasonalMetric(pollutantMetricIdLookup.get(metricNameLowerCase), record[seasonalMetricIdx]));
				}

				String statisticLowerCase = record[annualMetricIdx].toLowerCase();
				Integer statisticId = 0;
				if (statisticIdLookup.containsKey(statisticLowerCase)) {
					statisticId = statisticIdLookup.get(statisticLowerCase);
				} else {
					if (statisticLowerCase != "") {
						throw new Exception("Annual statistic contained an invalid value: " + statisticLowerCase);
					}
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
						Double.valueOf(record[valuesIdx]));
			}

			batch.execute();

			// Now that the rows are in the database, let's get the cell count, mean, min,
			// max and create the metric summary records
			DSL.using(JooqUtil.getJooqConfiguration())
				.insertInto(
					AIR_QUALITY_LAYER_METRICS,
					AIR_QUALITY_LAYER_METRICS.AIR_QUALITY_LAYER_ID,
					AIR_QUALITY_LAYER_METRICS.METRIC_ID,
					AIR_QUALITY_LAYER_METRICS.SEASONAL_METRIC_ID,
					AIR_QUALITY_LAYER_METRICS.ANNUAL_STATISTIC_ID,
					AIR_QUALITY_LAYER_METRICS.CELL_COUNT,
					AIR_QUALITY_LAYER_METRICS.MIN_VALUE,
					AIR_QUALITY_LAYER_METRICS.MAX_VALUE,
					AIR_QUALITY_LAYER_METRICS.MEAN_VALUE,
					AIR_QUALITY_LAYER_METRICS.PCT_2_5,
					AIR_QUALITY_LAYER_METRICS.PCT_97_5)
				.select(
					DSL.select(
						AIR_QUALITY_CELL.AIR_QUALITY_LAYER_ID,
						AIR_QUALITY_CELL.METRIC_ID,
						AIR_QUALITY_CELL.SEASONAL_METRIC_ID,
						AIR_QUALITY_CELL.ANNUAL_STATISTIC_ID,
						DSL.count().as("cell_count"),
						DSL.min(AIR_QUALITY_CELL.VALUE).as("min_value"),
						DSL.max(AIR_QUALITY_CELL.VALUE).as("max_value"),
						DSL.avg(AIR_QUALITY_CELL.VALUE).cast(Double.class).as("mean_value"),
						DSL.percentileCont(0.025).withinGroupOrderBy(AIR_QUALITY_CELL.VALUE).cast(Double.class).as("pct_2_5"),
						DSL.percentileCont(0.975).withinGroupOrderBy(AIR_QUALITY_CELL.VALUE).cast(Double.class).as("pct_97_5"))
					.from(AIR_QUALITY_CELL)
					.where(AIR_QUALITY_CELL.AIR_QUALITY_LAYER_ID.eq(aqLayerId))
					.groupBy(
						AIR_QUALITY_CELL.AIR_QUALITY_LAYER_ID,
						AIR_QUALITY_CELL.METRIC_ID,
						AIR_QUALITY_CELL.SEASONAL_METRIC_ID,
						AIR_QUALITY_CELL.ANNUAL_STATISTIC_ID))
					.execute();

			} catch (Exception e) {
				// Make sure we don't leave a partially imported AQ surface in the db
				log.error("Error importing AQ file", e);
				validationMsg.success=false;
				validationMsg.messages.add(new ValidationMessage.Message("error","Error occurred during import of air quality file."));
				AirQualityApi.deleteAirQualityLayerDefinition(aqLayerId, null);
				return null;
			} finally {
				try {
					csvReader.close();
				} catch (IOException e) {
					return null;
				}
			}
			return aqLayerId;
	}

}

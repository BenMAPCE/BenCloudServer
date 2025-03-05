package gov.epa.bencloud.server.tasks.local;

import static gov.epa.bencloud.server.database.jooq.data.Tables.ENDPOINT;
import static gov.epa.bencloud.server.database.jooq.data.Tables.ETHNICITY;
import static gov.epa.bencloud.server.database.jooq.data.Tables.EXPOSURE_FUNCTION;
import static gov.epa.bencloud.server.database.jooq.data.Tables.EXPOSURE_RESULT_DATASET;
import static gov.epa.bencloud.server.database.jooq.data.Tables.EXPOSURE_RESULT_FUNCTION_CONFIG;
import static gov.epa.bencloud.server.database.jooq.data.Tables.GENDER;
import static gov.epa.bencloud.server.database.jooq.data.Tables.GET_EXPOSURE_RESULTS;
import static gov.epa.bencloud.server.database.jooq.data.Tables.GET_HIF_RESULTS;
import static gov.epa.bencloud.server.database.jooq.data.Tables.GET_VALUATION_RESULTS;
import static gov.epa.bencloud.server.database.jooq.data.Tables.GRID_DEFINITION;
import static gov.epa.bencloud.server.database.jooq.data.Tables.HEALTH_IMPACT_FUNCTION;
import static gov.epa.bencloud.server.database.jooq.data.Tables.HIF_RESULT_DATASET;
import static gov.epa.bencloud.server.database.jooq.data.Tables.HIF_RESULT_FUNCTION_CONFIG;
import static gov.epa.bencloud.server.database.jooq.data.Tables.POLLUTANT_METRIC;
import static gov.epa.bencloud.server.database.jooq.data.Tables.RACE;
import static gov.epa.bencloud.server.database.jooq.data.Tables.SEASONAL_METRIC;
import static gov.epa.bencloud.server.database.jooq.data.Tables.STATISTIC_TYPE;
import static gov.epa.bencloud.server.database.jooq.data.Tables.TASK_COMPLETE;
import static gov.epa.bencloud.server.database.jooq.data.Tables.VALUATION_RESULT_DATASET;
import static gov.epa.bencloud.server.database.jooq.data.Tables.VALUATION_RESULT_FUNCTION_CONFIG;
import static gov.epa.bencloud.server.database.jooq.data.Tables.VARIABLE_ENTRY;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.Vector;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record19;
import org.jooq.Record3;
import org.jooq.Result;
import org.jooq.Table;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.mariuszgromada.math.mxparser.Expression;
import org.mariuszgromada.math.mxparser.mXparser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.epa.bencloud.Constants;
import gov.epa.bencloud.api.AirQualityApi;
import gov.epa.bencloud.api.CrosswalksApi;
import gov.epa.bencloud.api.ExposureApi;
import gov.epa.bencloud.api.GridDefinitionApi;
import gov.epa.bencloud.api.HIFApi;
import gov.epa.bencloud.api.PopulationApi;
import gov.epa.bencloud.api.TaskApi;
import gov.epa.bencloud.api.ValuationApi;
import gov.epa.bencloud.api.function.EFunction;
import gov.epa.bencloud.api.model.AirQualityCell;
import gov.epa.bencloud.api.model.AirQualityCellMetric;
import gov.epa.bencloud.api.model.BatchTaskConfig;
import gov.epa.bencloud.api.model.ExposureConfig;
import gov.epa.bencloud.api.model.ExposureTaskConfig;
import gov.epa.bencloud.api.model.ExposureTaskLog;
import gov.epa.bencloud.api.model.HIFTaskLog;
import gov.epa.bencloud.api.model.PopulationCategoryKey;
import gov.epa.bencloud.api.model.ResultExportTaskConfig;
import gov.epa.bencloud.api.model.ResultExportTaskLog;
import gov.epa.bencloud.api.model.ValuationConfig;
import gov.epa.bencloud.api.model.ValuationTaskLog;
import gov.epa.bencloud.api.util.ApiUtil;
import gov.epa.bencloud.api.util.ExposureUtil;
import gov.epa.bencloud.api.util.FilestoreUtil;
import gov.epa.bencloud.api.util.HIFUtil;
import gov.epa.bencloud.api.util.ValuationUtil;
import gov.epa.bencloud.server.database.JooqUtil;
import gov.epa.bencloud.server.database.jooq.data.tables.records.ExposureResultRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.GetExposureResultsRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.GetHifResultsRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.GetPopulationRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.GetValuationResultsRecord;
import gov.epa.bencloud.server.tasks.TaskComplete;
import gov.epa.bencloud.server.tasks.TaskQueue;
import gov.epa.bencloud.server.tasks.TaskWorker;
import gov.epa.bencloud.server.tasks.model.Task;
import gov.epa.bencloud.server.tasks.model.TaskMessage;
import gov.epa.bencloud.server.util.ApplicationUtil;

/*
 * Process an result export request.
 */
public class ResultExportTaskRunnable implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(ResultExportTaskRunnable.class);
    protected static ObjectMapper objectMapper = new ObjectMapper();
    
	private String taskUuid;
	private String taskWorkerUuid;

	/**
	 * Creates an ExposureTaskRunnable object with the given taskUuid and taskWorkerUuid
	 * @param taskUuid
	 * @param taskWorkerUuid
	 */
	public ResultExportTaskRunnable(String taskUuid, String taskWorkerUuid) {
		this.taskUuid = taskUuid;
		this.taskWorkerUuid = taskWorkerUuid;
	}

	private boolean taskSuccessful = true;

	public void run() {
		
		log.info("Result Export Task Begin: " + taskUuid);
		ObjectMapper mapper = new ObjectMapper();
		Task task = TaskQueue.getTaskFromQueueRecord(taskUuid);

		ArrayList<TaskMessage> messages = new ArrayList<TaskMessage>();

		try {
			ResultExportTaskConfig resultExportTaskConfig = null;
			resultExportTaskConfig = objectMapper.readValue(task.getParameters(), ResultExportTaskConfig.class);
			resultExportTaskConfig.userId = task.getUserIdentifier();
			
			ResultExportTaskLog resultExportTaskLog = new ResultExportTaskLog(resultExportTaskConfig, task.getUserIdentifier());
			resultExportTaskLog.setDtStart(LocalDateTime.now());
			
			resultExportTaskLog.addMessage("Starting result export");
			messages.add(new TaskMessage("active", "Loading results"));
			TaskQueue.updateTaskPercentage(taskUuid, 1, mapper.writeValueAsString(messages));
			
			/*
			 * parse the export request parameters
			 * process the export request
			 * place the zip file in the file store
			 * mark task complete and clean up
			 */
			
			Integer batchId = task.getBatchId();
			Integer[] gridIds = resultExportTaskConfig.gridIds;
			Boolean includeHealthImpact = resultExportTaskConfig.includeHealthImpact;
			Boolean includeValuation = resultExportTaskConfig.includeValuation;
			Boolean includeExposure = resultExportTaskConfig.includeExposure;
			String taskUuid = resultExportTaskConfig.taskUuid;
			String uuidType = resultExportTaskConfig.uuidType;
			
			BatchTaskConfig batchTaskConfig = TaskApi.getTaskBatchConfigFromDb(batchId);
			
			//set zip file name
			String zipFileName = ApplicationUtil.replaceNonValidCharacters(batchTaskConfig.name);
			StringBuilder batchTaskLog = new StringBuilder(); 
			
			// Get output stream
			ZipOutputStream zipStream;
			File tmpZipFile = null;
			String tmpDirectoryPath = System.getProperty("java.io.tmpdir");

			try {
				File tmpDirectory = new File(tmpDirectoryPath);
				tmpZipFile = File.createTempFile("resultExport",".zip", tmpDirectory);
				FileOutputStream fos = new FileOutputStream(tmpZipFile);
				
				// Stream .ZIP file to the temp file
				zipStream = new ZipOutputStream(fos);
			} catch (java.io.IOException e1) {
				TaskComplete.addTaskToCompleteAndRemoveTaskFromQueue(task.getUuid(), taskWorkerUuid, false, "Task Failed");
				log.error("Error getting output stream", e1);
				return;
			}
			
//			For now we we export EITHER exposure OR HIF/Valuation results. We may want to change the logic in the future.
			if(includeExposure) {
				includeHealthImpact=false;
				includeValuation=false;
			}
			
			if(includeExposure) {
				//Exposure results
				DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());
				//get valuation task ids 
				List<Integer> exposureResultDatasetIds;
				if(uuidType.equals("E")) {
					//export current scenario
					exposureResultDatasetIds = create.select()
							.from(EXPOSURE_RESULT_DATASET)
							.join(TASK_COMPLETE).on(EXPOSURE_RESULT_DATASET.TASK_UUID.eq(TASK_COMPLETE.TASK_UUID))
							.where(TASK_COMPLETE.TASK_UUID.eq(taskUuid))
							.fetch(EXPOSURE_RESULT_DATASET.ID);
				}
				else {
					//export all scenarios in this batch task
					exposureResultDatasetIds = create.select()
							.from(EXPOSURE_RESULT_DATASET)
							.join(TASK_COMPLETE).on(EXPOSURE_RESULT_DATASET.TASK_UUID.eq(TASK_COMPLETE.TASK_UUID))
							.where(TASK_COMPLETE.TASK_BATCH_ID.eq(batchId))
							.fetch(EXPOSURE_RESULT_DATASET.ID);
				}		
				
				//Loop through each function and each grid definition
				for(int exposureResultDatasetId : exposureResultDatasetIds) {
					//csv file name
					String taskFileName = ApplicationUtil.replaceNonValidCharacters(ExposureApi.getExposureTaskConfigFromDb(exposureResultDatasetId).name);
					for(int i=0; i < gridIds.length; i++) {
						Result<?> efRecordsClean = null;
						try {
							//If the crosswalk isn't there, create it now
							if(!CrosswalksApi.ensureCrosswalkExists(batchTaskConfig.gridDefinitionId, gridIds[i])) {
								List<Integer> gridDefinitionIds = Arrays.asList(batchTaskConfig.gridDefinitionId, gridIds[i]);
								List<String> gridDefinitionNames = create
										.select(GRID_DEFINITION.NAME)
										.from(GRID_DEFINITION)
										.where(GRID_DEFINITION.ID.in(gridDefinitionIds))
										.orderBy(GRID_DEFINITION.ID.sortAsc(gridDefinitionIds))
										.fetch(GRID_DEFINITION.NAME);
								String errorMessage = "Could not convert from grid \"" + gridDefinitionNames.get(0) + "\" to \"" + gridDefinitionNames.get(1) + "\"";
								TaskComplete.addTaskToCompleteAndRemoveTaskFromQueue(task.getUuid(), taskWorkerUuid, false, errorMessage);
								log.error("Task failed");
								return;
							}
							
							Table<GetExposureResultsRecord> efResultRecords = create.selectFrom(
									GET_EXPOSURE_RESULTS(
										exposureResultDatasetId, 
										null,
										gridIds[i]))
								.asTable("ef_result_records");
							
							Result<Record19<Integer, Integer, String, Integer, Integer, String, String, String, String, Double, Double, Double, Double, Double, Double, Double, Double, String, String>> efRecords = create.select(
									efResultRecords.field(GET_EXPOSURE_RESULTS.GRID_COL).as("column"),
									efResultRecords.field(GET_EXPOSURE_RESULTS.GRID_ROW).as("row"),
									EXPOSURE_RESULT_FUNCTION_CONFIG.POPULATION_GROUP,
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
									efResultRecords.field(GET_EXPOSURE_RESULTS.RESULT),
									efResultRecords.field(GET_EXPOSURE_RESULTS.SUBGROUP_POPULATION),
									efResultRecords.field(GET_EXPOSURE_RESULTS.ALL_POPULATION),
									DSL.when(efResultRecords.field(GET_EXPOSURE_RESULTS.ALL_POPULATION).eq(0.0), 0.0)
									.otherwise(efResultRecords.field(GET_EXPOSURE_RESULTS.SUBGROUP_POPULATION).div(efResultRecords.field(GET_EXPOSURE_RESULTS.ALL_POPULATION)).times(100.0)).as("percent_of_population"),
									DSL.val(null, String.class).as("formatted_results_2sf"),
									DSL.val(null, String.class).as("formatted_results_3sf")
									)
									.from(efResultRecords)
									.leftJoin(EXPOSURE_FUNCTION).on(efResultRecords.field(GET_EXPOSURE_RESULTS.EXPOSURE_FUNCTION_ID).eq(EXPOSURE_FUNCTION.ID))
									.join(EXPOSURE_RESULT_FUNCTION_CONFIG)
										.on(EXPOSURE_RESULT_FUNCTION_CONFIG.EXPOSURE_RESULT_DATASET_ID.eq(exposureResultDatasetId)
												.and(EXPOSURE_RESULT_FUNCTION_CONFIG.EXPOSURE_FUNCTION_INSTANCE_ID.eq(efResultRecords.field(GET_EXPOSURE_RESULTS.EXPOSURE_FUNCTION_INSTANCE_ID))))
									.leftJoin(RACE).on(EXPOSURE_RESULT_FUNCTION_CONFIG.RACE_ID.eq(RACE.ID))
									.join(ETHNICITY).on(EXPOSURE_RESULT_FUNCTION_CONFIG.ETHNICITY_ID.eq(ETHNICITY.ID))
									.join(GENDER).on(EXPOSURE_RESULT_FUNCTION_CONFIG.GENDER_ID.eq(GENDER.ID))
									.leftJoin(VARIABLE_ENTRY).on(EXPOSURE_RESULT_FUNCTION_CONFIG.VARIABLE_ID.eq(VARIABLE_ENTRY.ID))
	                                .orderBy(efResultRecords.field(GET_EXPOSURE_RESULTS.GRID_COL).asc(), efResultRecords.field(GET_EXPOSURE_RESULTS.GRID_ROW).asc(), EXPOSURE_RESULT_FUNCTION_CONFIG.HIDDEN_SORT_ORDER.asc())
									.fetch();

							for (Record res : efRecords) {
								res.setValue(DSL.field("formatted_results_2sf", String.class), 
												ApiUtil.getValueSigFigs(res.get("result", Double.class), 2));
								res.setValue(DSL.field("formatted_results_3sf", String.class), 
												ApiUtil.getValueSigFigs(res.get("result", Double.class), 3));
							}
							
							efRecordsClean = efRecords;
						} catch(DataAccessException e) {
							TaskComplete.addTaskToCompleteAndRemoveTaskFromQueue(task.getUuid(), taskWorkerUuid, false, "Task Failed");
							log.error("Task failed", e);
							return;
						}	
						try {						
							zipStream.putNextEntry(new ZipEntry(taskFileName + "_" + ApplicationUtil.replaceNonValidCharacters(GridDefinitionApi.getGridDefinitionName(gridIds[i])) + ".csv"));
							efRecordsClean.formatCSV(zipStream);
							zipStream.closeEntry();
							log.info(taskFileName + " added.");
							} 
						catch (Exception e) {
								log.error("Error creating export file", e);
							} 
						finally {
			
							}
						ExposureTaskLog efTaskLog = ExposureUtil.getTaskLog(exposureResultDatasetId);
						batchTaskLog.append(System.getProperty("line.separator"));
						//batchTaskLog.append(efTaskLog.toString(userProfile));
					}				
				}					
			}
			if(includeHealthImpact) {
				DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());
				//get hif task ids
				List<Integer> hifResultDatasetIds;
				if(uuidType.equals("H")) {
					//export all hif results from the same senario as taskUuid.
					hifResultDatasetIds = create.select()
							.from(HIF_RESULT_DATASET)
							.join(TASK_COMPLETE).on(HIF_RESULT_DATASET.TASK_UUID.eq(TASK_COMPLETE.TASK_UUID))
							.and(HIF_RESULT_DATASET.TASK_UUID.eq(taskUuid))
							.fetch(HIF_RESULT_DATASET.ID);
				}
				else if(uuidType.equals("V")) {
					hifResultDatasetIds = create.select()
							.from(HIF_RESULT_DATASET)
							.join(TASK_COMPLETE).on(HIF_RESULT_DATASET.TASK_UUID.eq(TASK_COMPLETE.TASK_UUID))
							.join(VALUATION_RESULT_DATASET).on(HIF_RESULT_DATASET.ID.eq(VALUATION_RESULT_DATASET.HIF_RESULT_DATASET_ID))
							.where(VALUATION_RESULT_DATASET.TASK_UUID.eq(taskUuid))
							.fetch(HIF_RESULT_DATASET.ID);
				}
				else {
					hifResultDatasetIds = create.select()
							.from(HIF_RESULT_DATASET)
							.join(TASK_COMPLETE).on(HIF_RESULT_DATASET.TASK_UUID.eq(TASK_COMPLETE.TASK_UUID))
							.where(TASK_COMPLETE.TASK_BATCH_ID.eq(batchId))
							.fetch(HIF_RESULT_DATASET.ID);
				}
				
				
				//Loop through each function and each grid definition
				for(int hifResultDatasetId : hifResultDatasetIds) {
					//csv file name
					String taskFileName = ApplicationUtil.replaceNonValidCharacters(HIFApi.getHifTaskConfigFromDb(hifResultDatasetId).name);
					Integer baselineGridId = HIFApi.getBaselineGridForHifResults(hifResultDatasetId);
					for(int i=0; i < gridIds.length; i++) {
						Result<?> hifRecordsClean = null;
						//Move the following to HIFApi.java? 
						//hifRecordsClean = HIFApi.getHifResultRecordsClean(gridIds[i], hifResultDatasetId) //use this instead?
						
						//If the crosswalk isn't there, create it now
						if(!CrosswalksApi.ensureCrosswalkExists(baselineGridId, gridIds[i])) {
							List<Integer> gridDefinitionIds = Arrays.asList(baselineGridId, gridIds[i]);
							List<String> gridDefinitionNames = create
									.select(GRID_DEFINITION.NAME)
									.from(GRID_DEFINITION)
									.where(GRID_DEFINITION.ID.in(gridDefinitionIds))
									.orderBy(GRID_DEFINITION.ID.sortAsc(gridDefinitionIds))
									.fetch(GRID_DEFINITION.NAME);
							String errorMessage = "Could not convert from grid \"" + gridDefinitionNames.get(0) + "\" to \"" + gridDefinitionNames.get(1) + "\"";
							TaskComplete.addTaskToCompleteAndRemoveTaskFromQueue(task.getUuid(), taskWorkerUuid, false, errorMessage);
							log.error("Task failed");
							return;
						}
						try {
							Table<GetHifResultsRecord> hifResultRecords = create.selectFrom(
								GET_HIF_RESULTS(
										hifResultDatasetId, 
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
									HIFApi.getBaselineGridForHifResults(hifResultDatasetId) == gridIds[i] ? null : hifResultRecords.field(GET_HIF_RESULTS.PERCENTILES) //Only include percentiles if we're aggregating
									)
									.from(hifResultRecords)
									.leftJoin(HEALTH_IMPACT_FUNCTION).on(hifResultRecords.field(GET_HIF_RESULTS.HIF_ID).eq(HEALTH_IMPACT_FUNCTION.ID))
									.join(HIF_RESULT_FUNCTION_CONFIG)
										.on(HIF_RESULT_FUNCTION_CONFIG.HIF_RESULT_DATASET_ID.eq(hifResultDatasetId)
												.and(HIF_RESULT_FUNCTION_CONFIG.HIF_INSTANCE_ID.eq(hifResultRecords.field(GET_HIF_RESULTS.HIF_INSTANCE_ID))))
									.join(ENDPOINT).on(ENDPOINT.ID.eq(HEALTH_IMPACT_FUNCTION.ENDPOINT_ID))
									.join(RACE).on(HIF_RESULT_FUNCTION_CONFIG.RACE_ID.eq(RACE.ID))
									.join(ETHNICITY).on(HIF_RESULT_FUNCTION_CONFIG.ETHNICITY_ID.eq(ETHNICITY.ID))
									.join(GENDER).on(HIF_RESULT_FUNCTION_CONFIG.GENDER_ID.eq(GENDER.ID))
									.join(POLLUTANT_METRIC).on(HIF_RESULT_FUNCTION_CONFIG.METRIC_ID.eq(POLLUTANT_METRIC.ID))
									.leftJoin(SEASONAL_METRIC).on(HIF_RESULT_FUNCTION_CONFIG.SEASONAL_METRIC_ID.eq(SEASONAL_METRIC.ID))
									.join(STATISTIC_TYPE).on(HIF_RESULT_FUNCTION_CONFIG.METRIC_STATISTIC.eq(STATISTIC_TYPE.ID))
									.fetch();
							
							//If results are being aggregated, recalculate mean, variance, std deviation, and percent of baseline
							if(HIFApi.getBaselineGridForHifResults(hifResultDatasetId) != gridIds[i]) {
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
							TaskComplete.addTaskToCompleteAndRemoveTaskFromQueue(task.getUuid(), taskWorkerUuid, false, "Task Failed");
							log.error("Task failed", e);
							return;
						}	
						try {						
							zipStream.putNextEntry(new ZipEntry(taskFileName + "_" + ApplicationUtil.replaceNonValidCharacters(GridDefinitionApi.getGridDefinitionName(gridIds[i])) + ".csv"));
							hifRecordsClean.formatCSV(zipStream);
							zipStream.closeEntry();
							log.info(taskFileName + " added.");
							} 
						catch (Exception e) {
								log.error("Error creating export file", e);
							} 
						finally {
			
							}
						HIFTaskLog hifTaskLog = HIFUtil.getTaskLog(hifResultDatasetId);
						batchTaskLog.append(System.getProperty("line.separator"));
						//batchTaskLog.append(hifTaskLog.toString(userProfile));
					}				
				}					
			}
			if(includeValuation) {
				//Valuation results
				DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());
				//get valuation task ids
				List<Integer> valuationResultDatasetIds;
				if(uuidType.equals("H")) {
					//export all val results from the same scenario as hif taskUuid.
					valuationResultDatasetIds = create.select()
							.from(VALUATION_RESULT_DATASET)
							.join(TASK_COMPLETE).on(VALUATION_RESULT_DATASET.TASK_UUID.eq(TASK_COMPLETE.TASK_UUID))
							.join(HIF_RESULT_DATASET).on(VALUATION_RESULT_DATASET.HIF_RESULT_DATASET_ID.eq(HIF_RESULT_DATASET.ID))
							.where(HIF_RESULT_DATASET.TASK_UUID.eq(taskUuid))
							.fetch(VALUATION_RESULT_DATASET.ID);
				}
				else if(uuidType.equals("V")) {
					valuationResultDatasetIds = create.select()
							.from(VALUATION_RESULT_DATASET)
							.join(TASK_COMPLETE).on(VALUATION_RESULT_DATASET.TASK_UUID.eq(TASK_COMPLETE.TASK_UUID))
							.and(VALUATION_RESULT_DATASET.TASK_UUID.eq(taskUuid))
							.fetch(VALUATION_RESULT_DATASET.ID);
				}
				else {
					valuationResultDatasetIds = create.select()
							.from(VALUATION_RESULT_DATASET)
							.join(TASK_COMPLETE).on(VALUATION_RESULT_DATASET.TASK_UUID.eq(TASK_COMPLETE.TASK_UUID))
							.where(TASK_COMPLETE.TASK_BATCH_ID.eq(batchId))
							.fetch(VALUATION_RESULT_DATASET.ID);
				}
				
				//Loop through each function and each grid definition
				for(int valuationResultDatasetId : valuationResultDatasetIds) {
					//csv file name
					String taskFileName = ApplicationUtil.replaceNonValidCharacters(ValuationApi.getValuationTaskConfigFromDb(valuationResultDatasetId).name);
					Integer baselineGridId = ValuationApi.getBaselineGridForValuationResults(valuationResultDatasetId);				
					for(int i=0; i < gridIds.length; i++) {
						Result<?> vfRecordsClean = null;
						//Move the following to ValuationApi.java? 
						//valuationRecordsClean = ValuationApi.getValuationResultRecordsClean(gridIds[i], valuationResultDatasetId) //use this instead?
						
						//If the crosswalk isn't there, create it now
						if(!CrosswalksApi.ensureCrosswalkExists(baselineGridId, gridIds[i])) {
							List<Integer> gridDefinitionIds = Arrays.asList(baselineGridId, gridIds[i]);
							List<String> gridDefinitionNames = create
									.select(GRID_DEFINITION.NAME)
									.from(GRID_DEFINITION)
									.where(GRID_DEFINITION.ID.in(gridDefinitionIds))
									.orderBy(GRID_DEFINITION.ID.sortAsc(gridDefinitionIds))
									.fetch(GRID_DEFINITION.NAME);
							String errorMessage = "Could not convert from grid \"" + gridDefinitionNames.get(0) + "\" to \"" + gridDefinitionNames.get(1) + "\"";
							TaskComplete.addTaskToCompleteAndRemoveTaskFromQueue(task.getUuid(), taskWorkerUuid, false, errorMessage);
							log.error("Task failed");
							return;
						}
						try {
							Table<GetValuationResultsRecord> vfResultRecords = create.selectFrom(
									GET_VALUATION_RESULTS(
										valuationResultDatasetId, 
										null, 
										null,
										gridIds[i]))
								.asTable("valuation_result_records");
							Result<Record> vfRecords;
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
									HEALTH_IMPACT_FUNCTION.START_AGE,
									HEALTH_IMPACT_FUNCTION.END_AGE,
									vfResultRecords.field(GET_VALUATION_RESULTS.POINT_ESTIMATE),
									vfResultRecords.field(GET_VALUATION_RESULTS.MEAN),
									vfResultRecords.field(GET_VALUATION_RESULTS.STANDARD_DEV).as("standard_deviation"),
									vfResultRecords.field(GET_VALUATION_RESULTS.VARIANCE).as("variance"),
									vfResultRecords.field(GET_VALUATION_RESULTS.PCT_2_5),
									vfResultRecords.field(GET_VALUATION_RESULTS.PCT_97_5),
									ValuationApi.getBaselineGridForValuationResults(valuationResultDatasetId) == gridIds[i] ? null : vfResultRecords.field(GET_VALUATION_RESULTS.PERCENTILES), //Only include percentiles if we're aggregating
									vfResultRecords.field(GET_VALUATION_RESULTS.VF_ID)
									)
									.from(vfResultRecords)
									.join(VALUATION_RESULT_FUNCTION_CONFIG)
										.on(VALUATION_RESULT_FUNCTION_CONFIG.VALUATION_RESULT_DATASET_ID.eq(valuationResultDatasetId)
											.and(VALUATION_RESULT_FUNCTION_CONFIG.HIF_INSTANCE_ID.eq(vfResultRecords.field(GET_VALUATION_RESULTS.HIF_INSTANCE_ID)))
											.and(VALUATION_RESULT_FUNCTION_CONFIG.VF_ID.eq(vfResultRecords.field(GET_VALUATION_RESULTS.VF_ID))))
									.join(VALUATION_RESULT_DATASET)
										.on(VALUATION_RESULT_FUNCTION_CONFIG.VALUATION_RESULT_DATASET_ID.eq(VALUATION_RESULT_DATASET.ID))
									.join(HIF_RESULT_FUNCTION_CONFIG)
										.on(VALUATION_RESULT_DATASET.HIF_RESULT_DATASET_ID.eq(HIF_RESULT_FUNCTION_CONFIG.HIF_RESULT_DATASET_ID)
											.and(VALUATION_RESULT_FUNCTION_CONFIG.HIF_ID.eq(HIF_RESULT_FUNCTION_CONFIG.HIF_ID)))
									.join(HEALTH_IMPACT_FUNCTION).on(vfResultRecords.field(GET_VALUATION_RESULTS.HIF_ID).eq(HEALTH_IMPACT_FUNCTION.ID))
									.join(RACE).on(HIF_RESULT_FUNCTION_CONFIG.RACE_ID.eq(RACE.ID))
									.join(ETHNICITY).on(HIF_RESULT_FUNCTION_CONFIG.ETHNICITY_ID.eq(ETHNICITY.ID))
									.join(GENDER).on(HIF_RESULT_FUNCTION_CONFIG.GENDER_ID.eq(GENDER.ID))
									.join(POLLUTANT_METRIC).on(HIF_RESULT_FUNCTION_CONFIG.METRIC_ID.eq(POLLUTANT_METRIC.ID))
									.leftJoin(SEASONAL_METRIC).on(HIF_RESULT_FUNCTION_CONFIG.SEASONAL_METRIC_ID.eq(SEASONAL_METRIC.ID))
									.join(STATISTIC_TYPE).on(HIF_RESULT_FUNCTION_CONFIG.METRIC_STATISTIC.eq(STATISTIC_TYPE.ID))
									.fetch();
							
							// Add in valuation function information
							ValuationTaskLog vfTaskLog = ValuationUtil.getTaskLog(valuationResultDatasetId);
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
							
							//If results are being aggregated, recalc mean, variance, std deviation, and percent of baseline
							if(ValuationApi.getBaselineGridForValuationResults(valuationResultDatasetId) != gridIds[i]) {
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
							//Remove percentiles by keeping all other fields
							vfRecordsClean = vfRecords.into(vfRecords.fields(0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20));
						} catch(DataAccessException e) {
							TaskComplete.addTaskToCompleteAndRemoveTaskFromQueue(task.getUuid(), taskWorkerUuid, false, "Task Failed");
							log.error("Task failed", e);
							return;
						}	
						try {						
							zipStream.putNextEntry(new ZipEntry(taskFileName + "_" + ApplicationUtil.replaceNonValidCharacters(GridDefinitionApi.getGridDefinitionName(gridIds[i])) + ".csv"));
							vfRecordsClean.formatCSV(zipStream);
							zipStream.closeEntry();
							log.info(taskFileName + " added.");
							} 
						catch (Exception e) {
								log.error("Error creating export file", e);
							} 
						finally {
			
							}
						ValuationTaskLog vfTaskLog = ValuationUtil.getTaskLog(valuationResultDatasetId);
						batchTaskLog.append(System.getProperty("line.separator"));
						batchTaskLog.append(vfTaskLog.toString());
					}				
				}					
			
			}
			
			
			// Add log file
			try {
				zipStream.putNextEntry(new ZipEntry(zipFileName + "_TaskLog.txt"));
				
				zipStream.write(batchTaskLog.toString().getBytes());
				zipStream.closeEntry();			
				zipStream.close();
				
			} catch (Exception e) {
				log.error("Error writing task log, closing and flushing export", e);
			}
			
			String fileMetadata = "{\"name\":\"" + zipFileName + ".zip\"}";
			try (FileInputStream fis = new FileInputStream(tmpZipFile)) {
				Integer fsid = FilestoreUtil.putFile(fis, zipFileName + ".zip", Constants.FILE_TYPE_RESULT_EXPORT, task.getUserIdentifier(), fileMetadata);
				resultExportTaskConfig.filestoreId = fsid;
			} // Ending try will close FileInputStream before delete

			Files.delete(Paths.get(tmpZipFile.getPath()));
			
			TaskQueue.updateTaskParameters(task.getUuid(), mapper.writeValueAsString(resultExportTaskConfig));
			
			messages.get(messages.size()-1).setStatus("complete");			

			Integer fileSize = 0;
			String completeMessage = String.format("Exported results", fileSize);
			resultExportTaskLog.addMessage(completeMessage);
			resultExportTaskLog.setSuccess(true);
			resultExportTaskLog.setDtEnd(LocalDateTime.now());
			// ExposureUtil.storeTaskLog(resultExportTaskLog);
			
			TaskComplete.addTaskToCompleteAndRemoveTaskFromQueue(task.getUuid(), taskWorkerUuid, taskSuccessful, completeMessage);

		} catch (Exception e) {
			TaskComplete.addTaskToCompleteAndRemoveTaskFromQueue(task.getUuid(), taskWorkerUuid, false, "Task Failed");
			log.error("Task failed", e);
		}
		log.info("Result Export Task Complete: " + taskUuid);
	}

}

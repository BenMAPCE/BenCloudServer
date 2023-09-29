package gov.epa.bencloud.server.tasks.local;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Vector;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.ParetoDistribution;
import org.apache.commons.math3.distribution.WeibullDistribution;
import org.apache.commons.math3.distribution.TriangularDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.commons.math3.distribution.LogNormalDistribution;
import org.apache.commons.math3.distribution.LogisticDistribution;
import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.commons.math3.distribution.CauchyDistribution;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.GammaDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jooq.Record;
import org.jooq.Record3;
import org.jooq.Result;
import org.mariuszgromada.math.mxparser.Expression;
import org.mariuszgromada.math.mxparser.mXparser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.epa.bencloud.api.AirQualityApi;
import gov.epa.bencloud.api.IncidenceApi;
import gov.epa.bencloud.api.PopulationApi;
import gov.epa.bencloud.api.function.HIFunction;
import gov.epa.bencloud.api.model.AirQualityCell;
import gov.epa.bencloud.api.model.AirQualityCellMetric;
import gov.epa.bencloud.api.model.BatchTaskConfig;
import gov.epa.bencloud.api.model.HIFConfig;
import gov.epa.bencloud.api.model.HIFTaskConfig;
import gov.epa.bencloud.api.model.HIFTaskLog;
import gov.epa.bencloud.api.model.PopulationCategoryKey;
import gov.epa.bencloud.api.util.ApiUtil;
import gov.epa.bencloud.api.util.HIFUtil;
import gov.epa.bencloud.server.database.PooledDataSource;
import gov.epa.bencloud.server.database.jooq.data.tables.records.AirQualityCellRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.GetPopulationRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.HealthImpactFunctionRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.HifResultRecord;
import gov.epa.bencloud.server.tasks.TaskComplete;
import gov.epa.bencloud.server.tasks.TaskQueue;
import gov.epa.bencloud.server.tasks.TaskWorker;
import gov.epa.bencloud.server.tasks.model.Task;
import gov.epa.bencloud.server.tasks.model.TaskMessage;

/*
 * Methods related to running HIF tasks.
 */
public class HIFTaskRunnable implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(HIFTaskRunnable.class);
    protected static ObjectMapper objectMapper = new ObjectMapper();
    
	private String taskUuid;
	private String taskWorkerUuid;


	/**
	 * Creates an HIFTaskRunnable object with the given taskUuid and taskWorkerUuid
	 * @param taskUuid
	 * @param taskWorkerUuid
	 */
	public HIFTaskRunnable(String taskUuid, String taskWorkerUuid) {
		this.taskUuid = taskUuid;
		this.taskWorkerUuid = taskWorkerUuid;
	}

	private boolean taskSuccessful = true;

	public void run() {
		
		log.info("HIF Task Begin: " + taskUuid);
		ObjectMapper mapper = new ObjectMapper();
		Task task = TaskQueue.getTaskFromQueueRecord(taskUuid);
		final int maxRowsInMemory = 100000;
		ArrayList<TaskMessage> messages = new ArrayList<TaskMessage>();
		int rowsSaved = 0;
		
		try {
			HIFTaskConfig hifTaskConfig = null;
			if(task.getBatchId() == null) {
				// This is an old task, from before batch tasks were implemented
				hifTaskConfig = new HIFTaskConfig(task);	
			} else {
				hifTaskConfig = objectMapper.readValue(task.getParameters(), HIFTaskConfig.class);
				hifTaskConfig.gridDefinitionId = AirQualityApi.getAirQualityLayerGridId(hifTaskConfig.aqBaselineId);
			}
			
			HIFTaskLog hifTaskLog = new HIFTaskLog(hifTaskConfig, task.getUserIdentifier());
			hifTaskLog.setDtStart(LocalDateTime.now());
			
			hifTaskLog.addMessage("Starting HIF analysis");
			messages.add(new TaskMessage("active", "Loading air quality data"));
			TaskQueue.updateTaskPercentage(taskUuid, 1, mapper.writeValueAsString(messages));
			
			//TODO: This will need to change as we start supporting more metrics within a single AQ layer.
			// Right now, it's assuming one record per cell only. In the future, this should be a map keyed on metric for each cell.
			Map<Long, AirQualityCell> baseline = AirQualityApi.getAirQualityLayerMap(hifTaskConfig.aqBaselineId);
			Map<Long, AirQualityCell> scenario = AirQualityApi.getAirQualityLayerMap(hifTaskConfig.aqScenarioId);
			
			ArrayList<HIFunction> hifFunctionList = new ArrayList<HIFunction>();
			ArrayList<HIFunction> hifBaselineList = new ArrayList<HIFunction>();
			
			// incidenceLists contains an array of incidence maps for each HIF
			//ArrayList<Map<Long, Map<Integer, Double>>> incidenceLists = new ArrayList<Map<Long, Map<Integer, Double>>>();
			//ArrayList<Map<Long, Map<Integer, Double>>> prevalenceLists = new ArrayList<Map<Long, Map<Integer, Double>>>();
			//YY:update incidence and prevalence key to include gender, race, ethnicity, and age range
			ArrayList<Map<Long, Map<PopulationCategoryKey, Double>>> incidenceLists = new ArrayList<Map<Long, Map<PopulationCategoryKey, Double>>>();
			ArrayList<Map<Long, Map<PopulationCategoryKey, Double>>> prevalenceLists = new ArrayList<Map<Long, Map<PopulationCategoryKey, Double>>>();
			
			// incidenceCachepMap is used inside addIncidenceEntryGroups to avoid querying for datasets we already have
			Map<String, Integer> incidenceCacheMap = new HashMap<String, Integer>();
			Map<String, Integer> prevalenceCacheMap = new HashMap<String, Integer>();
			
			ArrayList<double[]> hifBetaDistributionLists = new ArrayList<double[]>();
						
			hifTaskLog.addMessage("Loaded air quality data");
			messages.get(messages.size()-1).setStatus("complete");
			
			// Inspect each selected HIF and create parallel lists of math expressions and
			// HIF config records
			messages.add(new TaskMessage("active", "Loading incidence and prevalence data"));
			int idx=0;
			for (HIFConfig hif : hifTaskConfig.hifs) {
				hif.arrayIdx = idx++;	
			}

			idx=0;
			
			// For each HIF, keep track of which age groups (and what percentage) apply
			// Hashmap key is the population age range and the value is what percent of that range's population applies to the HIF
			ArrayList<HashMap<Integer, Double>> hifPopAgeRangeMapping = getPopAgeRangeMapping(hifTaskConfig);

			for (HIFConfig hif : hifTaskConfig.hifs) {	
				messages.get(messages.size()-1).setMessage("Loading incidence and prevalence for function " + ++idx + " of " + hifTaskConfig.hifs.size());
				TaskQueue.updateTaskPercentage(taskUuid, 2, mapper.writeValueAsString(messages));
				
				TaskWorker.updateTaskWorkerHeartbeat(taskWorkerUuid);

				HIFunction[] f = HIFUtil.getFunctionsForHIF(hif.hifId);
				hifFunctionList.add(f[0]);
				hifBaselineList.add(f[1]);
				
				Record h = HIFUtil.getFunctionDefinition(hif.hifId);
				hif.hifRecord = h.intoMap();

				// Override hif config where user has not provided a value
				updateHifConfigValues(hif, h);
				
				//This will get the incidence and prevalence dataset for the year specified in the hif config
				boolean ret = IncidenceApi.addIncidenceOrPrevalenceEntryGroups(hifTaskConfig, hifPopAgeRangeMapping, hif, true, h, incidenceLists, incidenceCacheMap);
				ret = IncidenceApi.addIncidenceOrPrevalenceEntryGroups(hifTaskConfig, hifPopAgeRangeMapping, hif, false, h, prevalenceLists, prevalenceCacheMap);
				
				double[] distPercentiles = getPercentilesFromDistribution(h);
				
				hifBetaDistributionLists.add(distPercentiles);
			}
			
			
			// Sort the hifs by endpoint_group and endpoint
			hifTaskConfig.hifs.sort(HIFConfig.HifConfigEndpointGroupComparator);
			
			messages.get(messages.size()-1).setStatus("complete");
			messages.get(messages.size()-1).setMessage("Loaded incidence and prevalence for " + hifTaskConfig.hifs.size() + " function" + (hifTaskConfig.hifs.size()==1 ? "" : "s"));
			hifTaskLog.addMessage(messages.get(messages.size()-1).getMessage());
			messages.add(new TaskMessage("active", "Loading population data"));
			TaskQueue.updateTaskPercentage(taskUuid, 3, mapper.writeValueAsString(messages));
			TaskWorker.updateTaskWorkerHeartbeat(taskWorkerUuid);
			
			
			// Load the population dataset
			Map<Long, Result<GetPopulationRecord>> populationMap = PopulationApi.getPopulationEntryGroups(hifTaskConfig);

			// Load data for the selected HIFs
			// Determine the race/gender/ethnicity groups and age ranges needed for the
			// selected HIFs
			// Load incidence, prevalence, and variables
			// For each AQ grid cell
			// For each population category
			// Run each HIF
			// Create list of results for each HIF. Columns include col, row, start age, end
			// age, point estimate, population, delta, mean, baseline, pct

			int totalCells = baseline.size();
			int currentCell = 0;
			int prevPct = -999;
			
			Vector<HifResultRecord> hifResults = new Vector<HifResultRecord>(maxRowsInMemory);
			//System.out.println("hifResults initial capacity: " + hifResults.capacity());
			mXparser.setToOverrideBuiltinTokens();
			mXparser.disableUlpRounding();

			messages.get(messages.size()-1).setStatus("complete");
			hifTaskLog.addMessage("Loaded population data");
			messages.add(new TaskMessage("active", "Running health impact functions"));

			List<String> requiredVariableNames = hifTaskConfig.getVariableNames();

			// Variable dataset id, variable name, grid cell id
			Map<Integer, Map<String, Map<Long, Double>>> variables = new HashMap<Integer, Map<String, Map<Long, Double>>>();
			
			for (HIFConfig hifConfig : hifTaskConfig.hifs) {
				if (!variables.containsKey(hifConfig.variable)) {
					variables.put(hifConfig.variable, ApiUtil.getVariableValues(requiredVariableNames, hifConfig.variable, hifTaskConfig.gridDefinitionId));
				}
			}

			log.debug("VARIABLES ARRAY SIZE: ");


			/*
			 * FOR EACH CELL IN THE BASELINE AIR QUALITY SURFACE
			 */
			//TODO: Can we improve performance by moving parallelism to the outer loop?
			// Maybe for each HIF, for each cell...
			// That will make it more challenging to track progress. Maybe maintain a process counter in each hifConfig
			// and then put hif at idx=1 in charge of updating the task queue?
			for (Entry<Long, AirQualityCell> baselineEntry : baseline.entrySet()) {
				// updating task percentage
				int currentPct = Math.round(currentCell * 100 / totalCells);
				currentCell++;

				if (prevPct != currentPct) {
					TaskQueue.updateTaskPercentage(taskUuid, currentPct, mapper.writeValueAsString(messages));
					TaskWorker.updateTaskWorkerHeartbeat(taskWorkerUuid);
					prevPct = currentPct;
				}

				AirQualityCell baselineCell = baselineEntry.getValue();
				AirQualityCell scenarioCell = scenario.getOrDefault(baselineEntry.getKey(), null);
				if (scenarioCell == null) {
					continue;
				}
				
				
				Result<GetPopulationRecord> populationCell = populationMap.getOrDefault(baselineEntry.getKey(), null);
				if (populationCell == null) {
					continue;
				}

				Boolean preserveLegacyBehavior = hifTaskConfig.preserveLegacyBehavior;
				/*
				 * FOR EACH FUNCTION THE USER SELECTED
				 */
				
				hifTaskConfig.hifs.parallelStream().forEach((hifConfig) -> {
					HIFunction hifFunction = hifFunctionList.get(hifConfig.arrayIdx);
					HIFunction hifBaselineFunction = hifBaselineList.get(hifConfig.arrayIdx);

					Map<String, Object> hifRecord = hifConfig.hifRecord;
					double[] betaDist = hifBetaDistributionLists.get(hifConfig.arrayIdx);
					
					Map<Integer, Map<Integer, AirQualityCellMetric>> baselineCellMetrics = baselineCell.getCellMetrics();
					Map<Integer, Map<Integer, AirQualityCellMetric>> scenarioCellMetrics = scenarioCell.getCellMetrics();
					
					//TODO: This temporary code is always selecting the first metric we have for this cell
					// Need to update to use metric, seasonal metric, and statistic
					Map<Integer, AirQualityCellMetric> baselineCellFirstMetric = baselineCellMetrics.get(baselineCellMetrics.keySet().toArray()[0]);
					Map<Integer, AirQualityCellMetric> scenarioCellFirstMetric = scenarioCellMetrics.get(scenarioCellMetrics.keySet().toArray()[0]);
					
					 
					double baselineValue = baselineCellFirstMetric.get(baselineCellFirstMetric.keySet().toArray()[0]).getValue();
					double scenarioValue = scenarioCellFirstMetric.get(scenarioCellFirstMetric.keySet().toArray()[0]).getValue();
					
					double seasonalScalar = 1.0;
					if((int)hifRecord.get("metric_statistic") == 0) { // NONE
						seasonalScalar = hifConfig.totalDays.doubleValue();
					}
										
					double beta = ((Double) hifRecord.get("beta")).doubleValue();

					// BenMAP-CE stores air quality values as floats but performs HIF estimates using doubles.
					// Testing has shown that float to double conversion can cause small changes in values 
					// Normal operation in BenCloud will use all doubles but, during validation with BenMAP results, it may be useful to preserve the legacy behavior
					baselineValue = preserveLegacyBehavior ? (float)baselineValue : baselineValue;
					scenarioValue = preserveLegacyBehavior ? (float)scenarioValue : scenarioValue;
					double deltaQ = baselineValue - scenarioValue;	

					Expression hifFunctionExpression = null;
					Expression hifBaselineExpression = null;
					
					if(hifFunction.nativeFunction == null) {
						hifFunctionExpression = hifFunction.interpretedFunction;
						hifFunctionExpression.setArgumentValue("DELTAQ",deltaQ);
						hifFunctionExpression.setArgumentValue("Q0", scenarioValue);
						hifFunctionExpression.setArgumentValue("Q1", baselineValue);

						for (Entry<String, Map<Long, Double>> variable : variables.get(hifConfig.variable).entrySet()) {
							hifFunctionExpression.setArgumentValue(variable.getKey(), variable.getValue().getOrDefault(populationCell.get(0).getGridCellId(), 0.0));
						}

					} else {
						hifFunction.hifArguments.deltaQ = deltaQ;
						hifFunction.hifArguments.q0 = scenarioValue;
						hifFunction.hifArguments.q1 = baselineValue;
						for (Entry<String, Map<Long, Double>> variable : variables.get(hifConfig.variable).entrySet()) { 
							hifFunction.hifArguments.otherArguments.put(variable.getKey(), variable.getValue().getOrDefault(populationCell.get(0).getGridCellId(), 0.0));	
						}
					}

					if(hifBaselineFunction.nativeFunction == null) {
						hifBaselineExpression = hifBaselineFunction.interpretedFunction;
						hifBaselineExpression.setArgumentValue("DELTAQ",deltaQ);
						hifBaselineExpression.setArgumentValue("Q0", scenarioValue);
						hifBaselineExpression.setArgumentValue("Q1", baselineValue);

						for (Entry<String, Map<Long, Double>> variable : variables.get(hifConfig.variable).entrySet()) {
							hifBaselineExpression.setArgumentValue(variable.getKey(), variable.getValue().getOrDefault(populationCell.get(0).getGridCellId(), 0.0));
						}
					} else {
						hifBaselineFunction.hifArguments.deltaQ = deltaQ;
						hifBaselineFunction.hifArguments.q0 = scenarioValue;
						hifBaselineFunction.hifArguments.q1 = baselineValue;
						for (Entry<String, Map<Long, Double>> variable : variables.get(hifConfig.variable).entrySet()) { 
							hifBaselineFunction.hifArguments.otherArguments.put(variable.getKey(), variable.getValue().getOrDefault(populationCell.get(0).getGridCellId(), 0.0));	
						}
					}

					HashMap<Integer, Double> popAgeRangeHifMap = hifPopAgeRangeMapping.get(hifConfig.arrayIdx);
					Map<Long, Map<PopulationCategoryKey, Double>> incidenceMap = incidenceLists.get(hifConfig.arrayIdx);
					Map<Long, Map<PopulationCategoryKey, Double>> prevalenceMap = prevalenceLists.get(hifConfig.arrayIdx);
					Map<PopulationCategoryKey, Double> incidenceCell = incidenceMap.get(baselineEntry.getKey());
					Map<PopulationCategoryKey, Double> prevalenceCell = prevalenceMap.get(baselineEntry.getKey());

					/*
					 * ACCUMULATE THE ESTIMATE FOR EACH AGE CATEGORY IN THIS CELL
					 */

					double totalPop = 0.0;
					double hifFunctionEstimate = 0.0;
					double hifBaselineEstimate = 0.0;
					double incidence = 0.0;
					double prevalence = 0.0;
					Double[] resultPercentiles = new Double[20];
					Arrays.fill(resultPercentiles, 0.0);
					
					for (GetPopulationRecord popCategory : populationCell) {
						// <gridCellId, race, gender, ethnicity, agerange, pop>
						Integer popAgeRange = popCategory.getAgeRangeId();
						Integer popRace = popCategory.getRaceId();
						Integer popEthnicity = popCategory.getEthnicityId();
						Integer popGender = popCategory.getGenderId();
						
						PopulationCategoryKey popCatKey = new PopulationCategoryKey(popAgeRange, null, null, null); //popRace, popEthnicity, popGender);						
						
						if (popAgeRangeHifMap.containsKey(popAgeRange) 
								&& (hifConfig.race == 5 || hifConfig.race == popRace)
								&& (hifConfig.ethnicity == 3 || hifConfig.ethnicity == popEthnicity)
								&& (hifConfig.gender == 3 || hifConfig.gender == popGender)) {

							double rangePop = popCategory.getPopValue().doubleValue() * popAgeRangeHifMap.get(popAgeRange);
							
							incidence = incidenceCell == null ? 0.0 : incidenceCell.getOrDefault(popCatKey, 0.0);
							prevalence = prevalenceCell == null ? 0.0 : prevalenceCell.getOrDefault(popCatKey, 0.0);
							
							totalPop += rangePop;

							if(hifFunction.nativeFunction == null) {
								hifFunctionExpression.setArgumentValue("BETA", beta);
								hifFunctionExpression.setArgumentValue("INCIDENCE", incidence);
								hifFunctionExpression.setArgumentValue("PREVALENCE", prevalence);
								hifFunctionExpression.setArgumentValue("POPULATION", rangePop);

								hifFunctionEstimate += hifFunctionExpression.calculate() * seasonalScalar;
								for(int i=0; i < resultPercentiles.length; i++) {
									hifFunctionExpression.setArgumentValue("BETA", betaDist[i]);								
									resultPercentiles[i] += hifFunctionExpression.calculate() * seasonalScalar;
								}
							} else {
								hifFunction.hifArguments.beta = beta;
								hifFunction.hifArguments.incidence = incidence;
								hifFunction.hifArguments.prevalence = prevalence;
								hifFunction.hifArguments.population = rangePop;

								hifFunctionEstimate += hifFunction.nativeFunction.calculate(hifFunction.hifArguments) * seasonalScalar;
								for(int i=0; i < resultPercentiles.length; i++) {
									hifFunction.hifArguments.beta = betaDist[i];								
									resultPercentiles[i] += hifFunction.nativeFunction.calculate(hifFunction.hifArguments) * seasonalScalar;
								}
							}

							if(hifBaselineFunction.nativeFunction == null) {
								hifBaselineExpression.setArgumentValue("INCIDENCE", incidence);
								hifBaselineExpression.setArgumentValue("PREVALENCE", prevalence);
								hifBaselineExpression.setArgumentValue("POPULATION", rangePop);
								
								hifBaselineEstimate += hifBaselineExpression.calculate() * seasonalScalar;
							} else {
								hifBaselineFunction.hifArguments.incidence = incidence;
								hifBaselineFunction.hifArguments.prevalence = prevalence;
								hifBaselineFunction.hifArguments.population = rangePop;

								hifBaselineEstimate += hifBaselineFunction.nativeFunction.calculate(hifBaselineFunction.hifArguments) * seasonalScalar;
							}

						}
					}
					// This can happen if we're running multiple functions but we don't have any
					// of the population ranges that this function wants
					if (totalPop != 0.0) {
						HifResultRecord rec = new HifResultRecord();
						rec.setGridCellId(baselineEntry.getKey());
						rec.setGridCol(baselineCell.getGridCol());
						rec.setGridRow(baselineCell.getGridRow());
						rec.setHifId(hifConfig.hifId);
						rec.setPopulation(totalPop);
						rec.setDeltaAq(deltaQ);
						rec.setBaselineAq(baselineValue);
						rec.setScenarioAq(scenarioValue);
						rec.setIncidence(incidence);
						rec.setResult(hifFunctionEstimate);
						rec.setPct_2_5(resultPercentiles[0]);
						rec.setPct_97_5(resultPercentiles[19]);

						rec.setPercentiles(resultPercentiles);

						DescriptiveStatistics stats = new DescriptiveStatistics();
						for (int i = 0; i < resultPercentiles.length; i++) {
							stats.addValue(resultPercentiles[i]);
						}
						rec.setResultMean(stats.getMean());
						
						//Add point estimate to the list before calculating variance and standard deviation to match approach of desktop version
						stats.addValue(hifFunctionEstimate);
						rec.setStandardDev(stats.getStandardDeviation());
						rec.setResultVariance(stats.getVariance());
						
						rec.setBaseline(hifBaselineEstimate);

						hifResults.add(rec);
						
					}

				});
				
				// Control the size of the results vector by saving partial results along the way
				if(hifResults.size() >= maxRowsInMemory) {
					rowsSaved += hifResults.size();
					messages.get(messages.size()-1).setMessage("Saving progress...");
					TaskQueue.updateTaskPercentage(taskUuid, currentPct, mapper.writeValueAsString(messages));
					HIFUtil.storeResults(task, hifTaskConfig, hifResults);
					hifResults.clear();
					messages.get(messages.size()-1).setMessage("Running health impact functions");
					TaskQueue.updateTaskPercentage(taskUuid, currentPct, mapper.writeValueAsString(messages));
				}
				
			}
			rowsSaved += hifResults.size();
			messages.get(messages.size()-1).setStatus("complete");
			hifTaskLog.addMessage("Health impact function calculations complete");
			messages.add(new TaskMessage("active", String.format("Saving %,d results", rowsSaved)));
			TaskQueue.updateTaskPercentage(taskUuid, 100, mapper.writeValueAsString(messages));
			TaskWorker.updateTaskWorkerHeartbeat(taskWorkerUuid);
			HIFUtil.storeResults(task, hifTaskConfig, hifResults);
			messages.get(messages.size()-1).setStatus("complete");
			
			String completeMessage = String.format("Saved %,d results", rowsSaved);
			hifTaskLog.addMessage(completeMessage);
			hifTaskLog.setSuccess(true);
			hifTaskLog.setDtEnd(LocalDateTime.now());
			HIFUtil.storeTaskLog(hifTaskLog);
			
			TaskComplete.addTaskToCompleteAndRemoveTaskFromQueue(taskUuid, taskWorkerUuid, taskSuccessful, completeMessage);

		} catch (Exception e) {
			TaskComplete.addTaskToCompleteAndRemoveTaskFromQueue(taskUuid, taskWorkerUuid, false, "Task Failed");
			log.error("Task failed", e);
		}
		log.info("HIF Task Complete: " + taskUuid);
	}

	/**
	 * Load the HIFConfig data from the database
	 * @param hif
	 * @param h
	 */
	private void updateHifConfigValues(HIFConfig hif, Record h) {
		if(hif.startAge == null) {
			hif.startAge = h.get("start_age", Integer.class);
		}
		if(hif.endAge == null) {
			hif.endAge = h.get("end_age", Integer.class);	
		}
		if(hif.race == null) {
			hif.race = h.get("race_id", Integer.class);
		}
		if(hif.gender == null) {
			hif.gender = h.get("gender_id", Integer.class);
		}
		if(hif.ethnicity == null) {
			hif.ethnicity = h.get("ethnicity_id", Integer.class);
		}
		if(hif.incidence == null) {
			hif.incidence = h.get("incidence_dataset_id", Integer.class);
		}
		if(hif.prevalence == null) {
			hif.prevalence = h.get("prevalence_dataset_id", Integer.class);
		}
		if(hif.metric == null) {
			hif.metric = h.get("metric_id", Integer.class);
		}
		if(hif.seasonalMetric == null) {
			hif.seasonalMetric = h.get("seasonal_metric_id", Integer.class);
		}
		if(hif.metricStatistic == null) {
			hif.metricStatistic = h.get("metric_statistic", Integer.class);
		}

		//This is a temporary solution to the fact that user's can't select incidence and 
		//the standard EPA functions don't have incidence assigned in the db
		// If the UI passes the year and incidence hints to the methods that get health impact functions, these should already be set
		// TODO: 8/25/2022 - This should be reviewed and, probably, removed at this point
		if(h.get("function_text", String.class).toLowerCase().contains("incidence")) {
			if(hif.incidence==null) {
				if(h.get("endpoint_group_id").equals(12)) {
					hif.incidence = 1; //Mortality Incidence
					hif.incidenceYear = 2020;
				} else {
					hif.incidence = 12; //Other Incidence
					hif.incidenceYear = 2014;
				}
			}			
		} else if (h.get("function_text", String.class).toLowerCase().contains("prevalence")) {
			if(hif.prevalence==null) {
					hif.prevalence = 19; //Prevalence
					hif.prevalenceYear = 2008;
			}				
		}

		if(hif.variable == null) {
			hif.variable = h.get("variable_dataset_id", Integer.class);
		}
		if(hif.startDay == null) {
			if(h.get("start_day") == null) {
				hif.startDay = 1;
			} else {
				hif.startDay = h.get("start_day", Integer.class);
			}
			// TODO: TEMPORARY OVERRIDE - With 1.5.8.15, the desktop changed the ozone season to April - September. 
			// The cloud db still has May - September. We are forcing the new season definition here for now
			// until we can revisit this topic
			if(h.get("pollutant_id", Integer.class) == 4) {
				hif.startDay = 90;
			}
		}
		if(hif.endDay == null) {
			if(h.get("end_day") == null) {
				hif.endDay = 365;
			} else {
				hif.endDay = h.get("end_day", Integer.class);
			}
		}
		
		if(hif.startDay > hif.endDay) {
			hif.totalDays = 365 - (hif.startDay - hif.endDay) + 1;
		} else {
			hif.totalDays = hif.endDay - hif.startDay + 1;
		}
	}


	/**
	 * Gets the full list of age ranges for the population.
	 * For each hif, adds a map of the relevant age ranges and percentages.
	 * @param hifTaskConfig
	 * @return a list of maps with keys = population age range, 
	 * 			and values = percentage of population in that age range that applies to a given HIF.
	 */
	public static ArrayList<HashMap<Integer, Double>> getPopAgeRangeMapping(HIFTaskConfig hifTaskConfig) {
		ArrayList<HashMap<Integer, Double>> hifPopAgeRangeMapping = new ArrayList<HashMap<Integer, Double>>();
		
		// Get the full list of age ranges for the population
		// for each hif, add a map of the relevant age ranges and percentages
		Result<Record3<Integer, Short, Short>> popAgeRanges = PopulationApi.getPopAgeRanges(hifTaskConfig.popId);
		
		// We're getting the hifs from hifTaskConfig in the order they were originally placed
		for(int idx = 0; idx < hifTaskConfig.hifs.size(); idx++) {
			HIFConfig hif = null;
			
			// Find the hif with arrayIdx = idx

			for(int i = 0; i < hifTaskConfig.hifs.size(); i ++) {
				if(hifTaskConfig.hifs.get(i).arrayIdx == idx) {
					hif = hifTaskConfig.hifs.get(i);
					break;
				}
			}
			
			HashMap<Integer, Double> hifPopAgeRanges = new HashMap<Integer, Double>();
			for(Record3<Integer, Short, Short> ageRange : popAgeRanges) {
				Integer ageRangeId = ageRange.value1();
				Short startAge = ageRange.value2();
				Short endAge = ageRange.value3();
				
				if(hif.startAge <= endAge && hif.endAge >= startAge) {
					if ((startAge >= hif.startAge || hif.startAge == -1) && (endAge <= hif.endAge || hif.endAge == -1)) {
						// The population age range is fully contained in the hif's age range
						hifPopAgeRanges.put(ageRangeId, 1.0);
					}
					else
					{
						// calculate the percentage of the population age range that falls within the hif's age range
						double dDiv = 1;
						if (startAge < hif.startAge) {
							dDiv = (double)(endAge - hif.startAge + 1) / (double)(endAge - startAge + 1);
							if (endAge > hif.endAge) {
								dDiv = (double)(hif.endAge - hif.startAge + 1) / (double)(endAge - startAge + 1);
							}
						} else if (endAge > hif.endAge) {
							dDiv = (double)(hif.endAge - startAge + 1) / (double)(endAge - startAge + 1);
						}
						hifPopAgeRanges.put(ageRangeId, dDiv);
					}
				}
			}
			hifPopAgeRangeMapping.add(hifPopAgeRanges);	
		}

		return hifPopAgeRangeMapping;
	}
	
	/* 
	 * Gets the 2.5, 7.5, ... percentiles from a distribution defined by a record h
	 */
	private double[] getPercentilesFromDistribution(Record h) {
		double[] percentiles = new double[20];
		
		String distribution_name = h.get("dist_beta", String.class);
		RealDistribution distribution;

		double beta = h.get("beta", Double.class).doubleValue();
		double p1 = h.get("p1_beta", Double.class).doubleValue();
		double p2 = h.get("p2_beta", Double.class).doubleValue();

		/* AT THE TIME OF WRITING THIS CODE, WE ONLY HAVE TEST DATA FOR NORMAL DISTRIBUTIONS.
		 * AS SUCH, I HAVEN'T TESTED THE CODE FOR OTHER DISTRIBUTIONS FOR BUGS.
		 */

		switch (distribution_name.toLowerCase()) {
		case "none":
			for (int i = 0; i < percentiles.length; i++) {
				percentiles[i] = beta;
			}
			return percentiles;
		case "normal":
			// mean, standard deviation
			distribution = new NormalDistribution(beta, p1);
			break;
		case "weibull":
			// shape, scale (parameters are flipped to match order from desktop version)
			distribution = new WeibullDistribution(p2, p1);
			break;
		case "lognormal":
			// scale, shape
			distribution = new LogNormalDistribution(p1, p2);
			break;
		case "triangular":
			// lower, mode, upper
			distribution = new TriangularDistribution(p1, beta, p2);
			break;
		case "exponential":
			// mean
			distribution = new ExponentialDistribution(p1);
			break;
		case "uniform":
			// lower, upper
			distribution = new UniformRealDistribution(p1, p2);
			break;
		case "gamma":
			// shape, scale
			distribution = new GammaDistribution(p1, p2);
			break;
		case "logistic":
			// mean, scale
			distribution = new LogisticDistribution(p1, p2);
			break;
		case "beta":
			// alpha, beta
			distribution = new BetaDistribution(p1, p2);
			break;
		case "pareto":
			// scale, shape
			distribution = new ParetoDistribution(p1, p2);
			break;
		case "cauchy":
			// median, width
			distribution = new CauchyDistribution(p1, p2);
			break;
		default:
			// TODO: Report error back to user? 
			return null;
		}

		double step = 100.0 / percentiles.length;
		for (int i = 0; i < percentiles.length; i++) {
			double p = (step / 2) + (step)*i;
			percentiles[i] = distribution.inverseCumulativeProbability(p / 100.0);
		}
		
		return percentiles;
	}

}

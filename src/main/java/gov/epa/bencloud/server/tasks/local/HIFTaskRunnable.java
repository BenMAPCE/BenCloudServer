package gov.epa.bencloud.server.tasks.local;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Vector;

import org.apache.commons.math3.distribution.NormalDistribution;
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
import gov.epa.bencloud.api.model.HIFConfig;
import gov.epa.bencloud.api.model.HIFTaskConfig;
import gov.epa.bencloud.api.model.HIFTaskLog;
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

public class HIFTaskRunnable implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(HIFTaskRunnable.class);
	
	private String taskUuid;
	private String taskWorkerUuid;

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
			HIFTaskConfig hifTaskConfig = new HIFTaskConfig(task);
			HIFTaskLog hifTaskLog = new HIFTaskLog(hifTaskConfig);
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
			ArrayList<Map<Long, Map<Integer, Double>>> incidenceLists = new ArrayList<Map<Long, Map<Integer, Double>>>();
			ArrayList<Map<Long, Map<Integer, Double>>> prevalenceLists = new ArrayList<Map<Long, Map<Integer, Double>>>();
			
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
				hif.arrayIdx = idx;
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
				boolean ret = IncidenceApi.addIncidenceOrPrevalenceEntryGroups(hifTaskConfig, hif, true, h, incidenceLists, incidenceCacheMap);
				ret = IncidenceApi.addIncidenceOrPrevalenceEntryGroups(hifTaskConfig, hif, false, h, prevalenceLists, prevalenceCacheMap);
				
				double[] distSamples = getDistributionSamples(h);
				double[] distBeta = new double[20];
				
				int idxMedian = 0 + distSamples.length / distBeta.length / 2; //the median of the first segment
				for(int i=0; i < distBeta.length; i++) {
					// Grab the median from each of the 20 slices of distSamples
					distBeta[i] = (distSamples[idxMedian]+distSamples[idxMedian-1])/2.0;
					idxMedian += distSamples.length / distBeta.length;
				}
				
				hifBetaDistributionLists.add(distBeta);
			}
			
			
			// Sort the hifs by endpoint_group and endpoint
			hifTaskConfig.hifs.sort(HIFConfig.HifConfigEndpointGroupComparator);
			
			messages.get(messages.size()-1).setStatus("complete");
			messages.get(messages.size()-1).setMessage("Loaded incidence and prevalence for " + hifTaskConfig.hifs.size() + " function" + (hifTaskConfig.hifs.size()==1 ? "" : "s"));
			hifTaskLog.addMessage(messages.get(messages.size()-1).getMessage());
			messages.add(new TaskMessage("active", "Loading population data"));
			TaskQueue.updateTaskPercentage(taskUuid, 3, mapper.writeValueAsString(messages));
			TaskWorker.updateTaskWorkerHeartbeat(taskWorkerUuid);
			
			// For each HIF, keep track of which age groups (and what percentage) apply
			// Hashmap key is the population age range and the value is what percent of that range's population applies to the HIF
			ArrayList<HashMap<Integer, Double>> hifPopAgeRangeMapping = getPopAgeRangeMapping(hifTaskConfig);
			
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
					baselineValue = hifTaskConfig.preserveLegacyBehavior ? (float)baselineValue : baselineValue;
					scenarioValue = hifTaskConfig.preserveLegacyBehavior ? (float)scenarioValue : scenarioValue;
					double deltaQ = baselineValue - scenarioValue;	

					Expression hifFunctionExpression = null;
					Expression hifBaselineExpression = null;
					
					if(hifFunction.nativeFunction == null) {
						hifFunctionExpression = hifFunction.interpretedFunction;
						hifFunctionExpression.setArgumentValue("DELTAQ",deltaQ);
						hifFunctionExpression.setArgumentValue("Q0", baselineValue);
						hifFunctionExpression.setArgumentValue("Q1", scenarioValue);
					} else {
						hifFunction.hifArguments.deltaQ = deltaQ;
						hifFunction.hifArguments.q0 = baselineValue;
						hifFunction.hifArguments.q1 = scenarioValue;
					}

					if(hifBaselineFunction.nativeFunction == null) {
						hifBaselineExpression = hifBaselineFunction.interpretedFunction;
						hifBaselineExpression.setArgumentValue("DELTAQ",deltaQ);
						hifBaselineExpression.setArgumentValue("Q0", baselineValue);
						hifBaselineExpression.setArgumentValue("Q1", scenarioValue);
					} else {
						hifBaselineFunction.hifArguments.deltaQ = deltaQ;
						hifBaselineFunction.hifArguments.q0 = baselineValue;
						hifBaselineFunction.hifArguments.q1 = scenarioValue;
					}

					HashMap<Integer, Double> popAgeRangeHifMap = hifPopAgeRangeMapping.get(hifConfig.arrayIdx);
					Map<Long, Map<Integer, Double>> incidenceMap = incidenceLists.get(hifConfig.arrayIdx);
					Map<Long, Map<Integer, Double>> prevalenceMap = prevalenceLists.get(hifConfig.arrayIdx);
					Map<Integer, Double> incidenceCell = incidenceMap.get(baselineEntry.getKey());
					Map<Integer, Double> prevalenceCell = prevalenceMap.get(baselineEntry.getKey());

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
						
						if (popAgeRangeHifMap.containsKey(popAgeRange)) {
							//TODO: Add average incidence calculation here so we can store that in the record when complete. What we're storing right now is wrong.
							double rangePop = popCategory.getPopValue().doubleValue() * popAgeRangeHifMap.get(popAgeRange);
							incidence = incidenceCell == null ? 0.0 : incidenceCell.getOrDefault(popAgeRange, 0.0);
							prevalence = prevalenceCell == null ? 0.0 : prevalenceCell.getOrDefault(popAgeRange, 0.0);
							
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
					//System.out.println("hifResults capacity after clear: " + hifResults.capacity());
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
			
			//TODO: Right here is the place where we want to look and see if there is a child valuation task
			// If so, then let's go ahead and call ValuationTaskRunnable.run() right here and run it in the same thread
			// We just have to be really sure we avoid also spawning another runner
			
			// 1. Mark the val job as started
			// 2. Mark this hif job as complete
			// 3. Run the val job inside it's own try/catch so any error can be used to just fail the val job
			// ACTUALLY, when the time comes, look at chaining tasks in the BenCloudTaskRunner instead of here.
			
			
			TaskComplete.addTaskToCompleteAndRemoveTaskFromQueue(taskUuid, taskWorkerUuid, taskSuccessful, completeMessage);

		} catch (Exception e) {
			TaskComplete.addTaskToCompleteAndRemoveTaskFromQueue(taskUuid, taskWorkerUuid, false, "Task Failed");
			log.error("Task failed", e);
		}
		log.info("HIF Task Complete: " + taskUuid);
	}

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

	private ArrayList<HashMap<Integer, Double>> getPopAgeRangeMapping(HIFTaskConfig hifTaskConfig) {
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
	
	private double[] getDistributionSamples(Record h) {
		//TODO: At the moment, all HIFs are normal distribution. Need to build this out to support other types.
		double[] samples = new double[10000];
		
		RealDistribution distribution = new NormalDistribution(h.get("beta", Double.class), h.get("p1_beta", Double.class));
		
		Random rng = new Random(1);
		for (int i = 0; i < samples.length; i++)
		{
			double x = distribution.inverseCumulativeProbability(rng.nextDouble());
			samples[i]=x;
		}
		
		Arrays.sort(samples);
		return samples;
	}

}

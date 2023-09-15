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
import gov.epa.bencloud.api.function.EFunction;
import gov.epa.bencloud.api.function.HIFunction;
import gov.epa.bencloud.api.model.AirQualityCell;
import gov.epa.bencloud.api.model.AirQualityCellMetric;
import gov.epa.bencloud.api.model.BatchTaskConfig;
import gov.epa.bencloud.api.model.ExposureConfig;
import gov.epa.bencloud.api.model.ExposureTaskConfig;
import gov.epa.bencloud.api.model.ExposureTaskLog;
import gov.epa.bencloud.api.model.HIFConfig;
import gov.epa.bencloud.api.model.HIFTaskConfig;
import gov.epa.bencloud.api.model.HIFTaskLog;
import gov.epa.bencloud.api.model.PopulationCategoryKey;
import gov.epa.bencloud.api.util.ApiUtil;
import gov.epa.bencloud.api.util.ExposureUtil;
import gov.epa.bencloud.api.util.HIFUtil;
import gov.epa.bencloud.server.database.PooledDataSource;
import gov.epa.bencloud.server.database.jooq.data.tables.records.AirQualityCellRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.ExposureResultRecord;
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
public class ExposureTaskRunnable implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(ExposureTaskRunnable.class);
    protected static ObjectMapper objectMapper = new ObjectMapper();
    
	private String taskUuid;
	private String taskWorkerUuid;


	/**
	 * Creates an ExposureTaskRunnable object with the given taskUuid and taskWorkerUuid
	 * @param taskUuid
	 * @param taskWorkerUuid
	 */
	public ExposureTaskRunnable(String taskUuid, String taskWorkerUuid) {
		this.taskUuid = taskUuid;
		this.taskWorkerUuid = taskWorkerUuid;
	}

	private boolean taskSuccessful = true;

	public void run() {
		
		log.info("Exposure Task Begin: " + taskUuid);
		ObjectMapper mapper = new ObjectMapper();
		Task task = TaskQueue.getTaskFromQueueRecord(taskUuid);
		final int maxRowsInMemory = 100000;
		ArrayList<TaskMessage> messages = new ArrayList<TaskMessage>();
		int rowsSaved = 0;
		
		try {
			ExposureTaskConfig exposureTaskConfig = null;
			exposureTaskConfig = objectMapper.readValue(task.getParameters(), ExposureTaskConfig.class);
			exposureTaskConfig.gridDefinitionId = AirQualityApi.getAirQualityLayerGridId(exposureTaskConfig.aqBaselineId);

			
			ExposureTaskLog exposureTaskLog = new ExposureTaskLog(exposureTaskConfig, task.getUserIdentifier());
			exposureTaskLog.setDtStart(LocalDateTime.now());
			
			exposureTaskLog.addMessage("Starting Exposure analysis");
			messages.add(new TaskMessage("active", "Loading air quality data"));
			TaskQueue.updateTaskPercentage(taskUuid, 1, mapper.writeValueAsString(messages));
			
			//TODO: This will need to change as we start supporting more metrics within a single AQ layer.
			// Right now, it's assuming one record per cell only. In the future, this should be a map keyed on metric for each cell.
			Map<Long, AirQualityCell> baseline = AirQualityApi.getAirQualityLayerMap(exposureTaskConfig.aqBaselineId);
			Map<Long, AirQualityCell> scenario = AirQualityApi.getAirQualityLayerMap(exposureTaskConfig.aqScenarioId);
			
			ArrayList<EFunction> exposureFunctionList = new ArrayList<EFunction>();
												
			exposureTaskLog.addMessage("Loaded air quality data");
			messages.get(messages.size()-1).setStatus("complete");
			
			// Inspect each selected HIF and create parallel lists of math expressions and
			// HIF config records
			messages.add(new TaskMessage("active", "Loading incidence and prevalence data"));
			int idx=0;
			for (ExposureConfig exposureConfig : exposureTaskConfig.exposureFunctions) {
				exposureConfig.arrayIdx = idx++;	
			}

			idx=0;
			
			for (ExposureConfig exposureConfig : exposureTaskConfig.exposureFunctions) {	
				messages.get(messages.size()-1).setMessage("Loading configuration for function " + ++idx + " of " + exposureTaskConfig.exposureFunctions.size());
				TaskQueue.updateTaskPercentage(taskUuid, 2, mapper.writeValueAsString(messages));
				
				TaskWorker.updateTaskWorkerHeartbeat(taskWorkerUuid);

				EFunction f = ExposureUtil.getFunctionForEF(exposureConfig.efId);
				exposureFunctionList.add(f);
				
				Record e = ExposureUtil.getFunctionDefinition(exposureConfig.efId);
				exposureConfig.efRecord = e.intoMap();

				// Override exposure config where user has not provided a value
				updateExposureConfigValues(exposureConfig, e);
				
			}
			
			// For each exposure function, keep track of which age groups (and what percentage) apply
			// Hashmap key is the population age range and the value is what percent of that range's population applies to the exposure function
			ArrayList<HashMap<Integer, Double>> exposurePopAgeRangeMapping = getPopAgeRangeMapping(exposureTaskConfig);
			
			
			// Sort the exposure functions by population_group
			exposureTaskConfig.exposureFunctions.sort(ExposureConfig.ExposureConfigPopulationGroupComparator);
			
			messages.get(messages.size()-1).setStatus("complete");
			messages.get(messages.size()-1).setMessage("Loaded configurations for " + exposureTaskConfig.exposureFunctions.size() + " function" + (exposureTaskConfig.exposureFunctions.size()==1 ? "" : "s"));
			exposureTaskLog.addMessage(messages.get(messages.size()-1).getMessage());
			messages.add(new TaskMessage("active", "Loading population data"));
			TaskQueue.updateTaskPercentage(taskUuid, 3, mapper.writeValueAsString(messages));
			TaskWorker.updateTaskWorkerHeartbeat(taskWorkerUuid);
			
			
			// Load the population dataset
			Map<Long, Result<GetPopulationRecord>> populationMap = PopulationApi.getPopulationEntryGroups(exposureTaskConfig);

			// Load data for the selected exposure functions
			// Determine the race/gender/ethnicity groups and age ranges needed for the
			// selected exposure functions
			// Load variables
			// For each AQ grid cell
			// For each population category
			// Run each exposure function
			// Create list of results for each exposure functions. Columns include col, row, start age, end
			// age, result, population, delta

			int totalCells = baseline.size();
			int currentCell = 0;
			int prevPct = -999;
			
			Vector<ExposureResultRecord> exposureResults = new Vector<ExposureResultRecord>(maxRowsInMemory);
			//System.out.println("exposureResults initial capacity: " + exposureResults.capacity());
			mXparser.setToOverrideBuiltinTokens();
			mXparser.disableUlpRounding();

			/*
			 * Variables in exposure tasks are handled a bit differently. In the exposure function texts, there's a "VARIABLE" that 
			 * acts as a placeholder. The variable id that will be used for each function is stored in the exposureConfig.variable field.
			 * 
			 * Note that the exposureConfig.efRecord field in the JSON definition is *not* used.
			 */
			List<Integer> requiredVariableIds = exposureTaskConfig.getRequiredVariableIds();
			// Variable id, grid cell id
			Map<Integer, Map<Long, Double>> variables = ApiUtil.getVariableValuesFromIds(requiredVariableIds, exposureTaskConfig.gridDefinitionId);
			
			messages.get(messages.size()-1).setStatus("complete");
			exposureTaskLog.addMessage("Loaded population data");
			messages.add(new TaskMessage("active", "Running exposure functions"));
			/*
			 * FOR EACH CELL IN THE BASELINE AIR QUALITY SURFACE
			 */
			//TODO: Can we improve performance by moving parallelism to the outer loop?
			// Maybe for each exposure, for each cell...
			// That will make it more challenging to track progress. Maybe maintain a process counter in each exposureConfig
			// and then put exposure function at idx=1 in charge of updating the task queue?
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
				
				exposureTaskConfig.exposureFunctions.parallelStream().forEach((exposureConfig) -> {
					EFunction exposureFunction = exposureFunctionList.get(exposureConfig.arrayIdx);

					Map<String, Object> efRecord = exposureConfig.efRecord;
					
					Map<Integer, Map<Integer, AirQualityCellMetric>> baselineCellMetrics = baselineCell.getCellMetrics();
					Map<Integer, Map<Integer, AirQualityCellMetric>> scenarioCellMetrics = scenarioCell.getCellMetrics();
					
					//TODO: This temporary code is always selecting the first metric we have for this cell
					// Need to update to use metric, seasonal metric, and statistic
					Map<Integer, AirQualityCellMetric> baselineCellFirstMetric = baselineCellMetrics.get(baselineCellMetrics.keySet().toArray()[0]);
					Map<Integer, AirQualityCellMetric> scenarioCellFirstMetric = scenarioCellMetrics.get(scenarioCellMetrics.keySet().toArray()[0]);
					
					 
					double baselineValue = baselineCellFirstMetric.get(baselineCellFirstMetric.keySet().toArray()[0]).getValue();
					double scenarioValue = scenarioCellFirstMetric.get(scenarioCellFirstMetric.keySet().toArray()[0]).getValue();
					
					double seasonalScalar = 1.0;
//					if((int)efRecord.get("metric_statistic") == 0) { // NONE
//						seasonalScalar = exposureConfig.totalDays.doubleValue();
//					}
										
					double deltaQ = baselineValue - scenarioValue;	
					double v1 = 1.0;
					if (variables.containsKey(exposureConfig.variable)) {
						v1 = variables.get(exposureConfig.variable).getOrDefault(baselineEntry.getKey(), 0.0);
					}
					
					
					Expression functionExpression = null;
					
					if(exposureFunction.nativeFunction == null) {
						functionExpression = exposureFunction.interpretedFunction;
						functionExpression.setArgumentValue("DELTA",deltaQ);
						functionExpression.setArgumentValue("Q1", baselineValue);
						functionExpression.setArgumentValue("Q0", scenarioValue);
						// Currently, the replacement of VARIABLE with the corresponding id is hard-coded, since it's the only one.
						if (variables.containsKey(exposureConfig.variable)) {
							functionExpression.setArgumentValue("VARIABLE", v1);
						}
					} else {
						exposureFunction.efArguments.deltaQ = deltaQ;
						exposureFunction.efArguments.q1 = baselineValue;
						exposureFunction.efArguments.q0 = scenarioValue;
						if (variables.containsKey(exposureConfig.variable)) {
							// Currently, the replacement of VARIABLE with the corresponding id is hard-coded, since it's the only one.
							exposureFunction.efArguments.v1 = v1;
						}
					}

					HashMap<Integer, Double> popAgeRangeExposureMap = exposurePopAgeRangeMapping.get(exposureConfig.arrayIdx);

					/*
					 * ACCUMULATE THE ESTIMATE FOR EACH AGE CATEGORY IN THIS CELL
					 */

					double totalSubgroupPop = 0.0;
					double totalAllPop = 0.0;
					double functionEstimate = 0.0;
					
					for (GetPopulationRecord popCategory : populationCell) {
						// <gridCellId, race, gender, ethnicity, agerange, pop>
						Integer popAgeRange = popCategory.getAgeRangeId();
						Integer popRace = popCategory.getRaceId();
						Integer popEthnicity = popCategory.getEthnicityId();
						Integer popGender = popCategory.getGenderId();
						
						PopulationCategoryKey popCatKey = new PopulationCategoryKey(popAgeRange, null, null, null); //popRace, popEthnicity, popGender);						
						
						if (popAgeRangeExposureMap.containsKey(popAgeRange) 
								&& (exposureConfig.race == 5 || exposureConfig.race == popRace)
								&& (exposureConfig.ethnicity == 3 || exposureConfig.ethnicity == popEthnicity)
								&& (exposureConfig.gender == 3 || exposureConfig.gender == popGender)) {

							double rangePop = popCategory.getPopValue().doubleValue() * popAgeRangeExposureMap.get(popAgeRange);

							totalSubgroupPop += rangePop;

							if(exposureFunction.nativeFunction == null) {
								functionExpression.setArgumentValue("POPULATION", rangePop);							
								functionEstimate += functionExpression.calculate() * seasonalScalar;

							} else {
								exposureFunction.efArguments.population = rangePop;
								functionEstimate += exposureFunction.nativeFunction.calculate(exposureFunction.efArguments) * seasonalScalar;
							}

						}
						totalAllPop += popCategory.getPopValue().doubleValue();
					}
					// This can happen if we're running multiple functions but we don't have any
					// of the population ranges that this function wants
					if (totalSubgroupPop != 0.0) {
						ExposureResultRecord rec = new ExposureResultRecord();
						rec.setGridCellId(baselineEntry.getKey());
						rec.setGridCol(baselineCell.getGridCol());
						rec.setGridRow(baselineCell.getGridRow());
						rec.setExposureFunctionId(exposureConfig.efId);
						
						//Use the variable to adjust the population if it's present. Else, v1 = 1.0 and will have no effect 
						rec.setSubgroupPopulation(totalSubgroupPop * v1);
						
						rec.setAllPopulation(totalAllPop);
						rec.setDeltaAq(deltaQ);
						rec.setBaselineAq(baselineValue);
						rec.setScenarioAq(scenarioValue);
						rec.setResult(functionEstimate);

						exposureResults.add(rec);
						
					}

				});
				
				// Control the size of the results vector by saving partial results along the way
				if(exposureResults.size() >= maxRowsInMemory) {
					rowsSaved += exposureResults.size();
					messages.get(messages.size()-1).setMessage("Saving progress...");
					TaskQueue.updateTaskPercentage(taskUuid, currentPct, mapper.writeValueAsString(messages));
					ExposureUtil.storeResults(task, exposureTaskConfig, exposureResults);
					exposureResults.clear();
					messages.get(messages.size()-1).setMessage("Running exposure functions");
					TaskQueue.updateTaskPercentage(taskUuid, currentPct, mapper.writeValueAsString(messages));
				}
				
			}
			rowsSaved += exposureResults.size();
			messages.get(messages.size()-1).setStatus("complete");
			exposureTaskLog.addMessage("Exposure calculations complete");
			messages.add(new TaskMessage("active", String.format("Saving %,d results", rowsSaved)));
			TaskQueue.updateTaskPercentage(taskUuid, 100, mapper.writeValueAsString(messages));
			TaskWorker.updateTaskWorkerHeartbeat(taskWorkerUuid);
			ExposureUtil.storeResults(task, exposureTaskConfig, exposureResults);
			messages.get(messages.size()-1).setStatus("complete");
			
			String completeMessage = String.format("Saved %,d results", rowsSaved);
			exposureTaskLog.addMessage(completeMessage);
			exposureTaskLog.setSuccess(true);
			exposureTaskLog.setDtEnd(LocalDateTime.now());
			ExposureUtil.storeTaskLog(exposureTaskLog);
			
			TaskComplete.addTaskToCompleteAndRemoveTaskFromQueue(taskUuid, taskWorkerUuid, taskSuccessful, completeMessage);

		} catch (Exception e) {
			TaskComplete.addTaskToCompleteAndRemoveTaskFromQueue(taskUuid, taskWorkerUuid, false, "Task Failed");
			log.error("Task failed", e);
		}
		log.info("Exposure Task Complete: " + taskUuid);
	}

	/**
	 * Load the ExposureConfig data from the database
	 * @param exposureConfig
	 * @param e
	 */
	private void updateExposureConfigValues(ExposureConfig exposureConfig, Record e) {
		if(exposureConfig.startAge == null) {
			exposureConfig.startAge = e.get("start_age", Integer.class);
		}
		if(exposureConfig.endAge == null) {
			exposureConfig.endAge = e.get("end_age", Integer.class);	
		}
		if(exposureConfig.race == null) {
			exposureConfig.race = e.get("race_id", Integer.class);
		}
		if(exposureConfig.gender == null) {
			exposureConfig.gender = e.get("gender_id", Integer.class);
		}
		if(exposureConfig.ethnicity == null) {
			exposureConfig.ethnicity = e.get("ethnicity_id", Integer.class);
		}

		if(exposureConfig.variable == null) {
			exposureConfig.variable = e.get("variable_id", Integer.class);
		}
		
//		if(exposureConfig.startDay == null) {
//			if(e.get("start_day") == null) {
//				exposureConfig.startDay = 1;
//			} else {
//				exposureConfig.startDay = e.get("start_day", Integer.class);
//			}
//		}
//		if(exposureConfig.endDay == null) {
//			if(e.get("end_day") == null) {
//				exposureConfig.endDay = 365;
//			} else {
//				exposureConfig.endDay = e.get("end_day", Integer.class);
//			}
//		}
		


	}


	/**
	 * Gets the full list of age ranges for the population.
	 * For each function, adds a map of the relevant age ranges and percentages.
	 * @param exposureTaskConfig
	 * @return a list of maps with keys = population age range, 
	 * 			and values = percentage of population in that age range that applies to a given exposure function.
	 */
	public static ArrayList<HashMap<Integer, Double>> getPopAgeRangeMapping(ExposureTaskConfig exposureTaskConfig) {
		ArrayList<HashMap<Integer, Double>> exposurePopAgeRangeMapping = new ArrayList<HashMap<Integer, Double>>();
		
		// Get the full list of age ranges for the population
		// for each exposure function, add a map of the relevant age ranges and percentages
		Result<Record3<Integer, Short, Short>> popAgeRanges = PopulationApi.getPopAgeRanges(exposureTaskConfig.popId);
		
		// We're getting the exposure functions from exposureTaskConfig in the order they were originally placed
		for(int idx = 0; idx < exposureTaskConfig.exposureFunctions.size(); idx++) {
			ExposureConfig exposureFunction = null;
			
			// Find the exposureFunction with arrayIdx = idx

			for(int i = 0; i < exposureTaskConfig.exposureFunctions.size(); i ++) {
				if(exposureTaskConfig.exposureFunctions.get(i).arrayIdx == idx) {
					exposureFunction = exposureTaskConfig.exposureFunctions.get(i);
					break;
				}
			}
			
			HashMap<Integer, Double> exposurePopAgeRanges = new HashMap<Integer, Double>();
			for(Record3<Integer, Short, Short> ageRange : popAgeRanges) {
				Integer ageRangeId = ageRange.value1();
				Short startAge = ageRange.value2();
				Short endAge = ageRange.value3();
				
				if(exposureFunction.startAge <= endAge && exposureFunction.endAge >= startAge) {
					if ((startAge >= exposureFunction.startAge || exposureFunction.startAge == -1) && (endAge <= exposureFunction.endAge || exposureFunction.endAge == -1)) {
						// The population age range is fully contained in the exposure function's age range
						exposurePopAgeRanges.put(ageRangeId, 1.0);
					}
					else
					{
						// calculate the percentage of the population age range that falls within the exposure function's age range
						double dDiv = 1;
						if (startAge < exposureFunction.startAge) {
							dDiv = (double)(endAge - exposureFunction.startAge + 1) / (double)(endAge - startAge + 1);
							if (endAge > exposureFunction.endAge) {
								dDiv = (double)(exposureFunction.endAge - exposureFunction.startAge + 1) / (double)(endAge - startAge + 1);
							}
						} else if (endAge > exposureFunction.endAge) {
							dDiv = (double)(exposureFunction.endAge - startAge + 1) / (double)(endAge - startAge + 1);
						}
						exposurePopAgeRanges.put(ageRangeId, dDiv);
					}
				}
			}
			exposurePopAgeRangeMapping.add(exposurePopAgeRanges);	
		}

		return exposurePopAgeRangeMapping;
	}

}

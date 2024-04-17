package gov.epa.bencloud.server.tasks.local;

import static gov.epa.bencloud.server.database.jooq.data.Tables.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.math3.distribution.LogNormalDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.distribution.TriangularDistribution;
import org.apache.commons.math3.distribution.WeibullDistribution;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jooq.Record;
import org.jooq.Record2;
import org.jooq.Record7;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.mariuszgromada.math.mxparser.Expression;
import org.mariuszgromada.math.mxparser.mXparser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.epa.bencloud.api.HIFApi;
import gov.epa.bencloud.api.function.VFunction;
import gov.epa.bencloud.api.model.HIFTaskConfig;
import gov.epa.bencloud.api.model.ValuationConfig;
import gov.epa.bencloud.api.model.ValuationTaskConfig;
import gov.epa.bencloud.api.model.ValuationTaskLog;
import gov.epa.bencloud.api.util.ApiUtil;
import gov.epa.bencloud.api.util.ValuationUtil;
import gov.epa.bencloud.server.database.jooq.data.tables.records.ValuationResultRecord;
import gov.epa.bencloud.server.tasks.TaskComplete;
import gov.epa.bencloud.server.tasks.TaskQueue;
import gov.epa.bencloud.server.tasks.TaskWorker;
import gov.epa.bencloud.server.tasks.model.Task;
import gov.epa.bencloud.server.tasks.model.TaskMessage;

public class ValuationTaskRunnable implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(ValuationTaskRunnable.class);
    protected static ObjectMapper objectMapper = new ObjectMapper();
	
	private String taskUuid;
	private String taskWorkerUuid;

	public ValuationTaskRunnable(String taskUuid, String taskWorkerUuid) {
		this.taskUuid = taskUuid;
		this.taskWorkerUuid = taskWorkerUuid;
	}

	private boolean taskSuccessful = true;

	public void run() {
		log.info("Valuation Task Begin: " + taskUuid);
		
		ObjectMapper mapper = new ObjectMapper();
		Task task = TaskQueue.getTaskFromQueueRecord(taskUuid);
		final int maxRowsInMemory = 100000;
		ArrayList<TaskMessage> messages = new ArrayList<TaskMessage>();
		int rowsSaved = 0;
		
		try {
			messages.add(new TaskMessage("active", "Loading datasets"));
			TaskQueue.updateTaskPercentage(taskUuid, 1, mapper.writeValueAsString(messages));
			TaskWorker.updateTaskWorkerHeartbeat(taskWorkerUuid);
			
			ValuationTaskConfig valuationTaskConfig = null;
			if(task.getBatchId() == null) {
				// This is an old task, from before batch tasks were implemented
				valuationTaskConfig = new ValuationTaskConfig(task);	
			} else {
				valuationTaskConfig = objectMapper.readValue(task.getParameters(), ValuationTaskConfig.class);
			}	
			
			ValuationTaskLog valuationTaskLog = new ValuationTaskLog(valuationTaskConfig, task.getUserIdentifier());
			valuationTaskLog.setDtStart(LocalDateTime.now());
			
			messages.get(messages.size()-1).setStatus("complete");
			
			messages.add(new TaskMessage("active", "Preparing datasets for valuation"));
			TaskQueue.updateTaskPercentage(taskUuid, 2, mapper.writeValueAsString(messages));
			
			valuationTaskConfig.hifResultDatasetId = HIFApi.getHIFResultDatasetId(valuationTaskConfig.hifTaskUuid);
			
			if(valuationTaskConfig.hifResultDatasetId == null) {
				TaskComplete.addTaskToCompleteAndRemoveTaskFromQueue(taskUuid, taskWorkerUuid, false, "Unable to load HIF estimates");
				return;
			}
			
			List<VFunction> valuationFunctionList = new ArrayList<VFunction>();

			List<Record> vfDefinitionList = new ArrayList<Record>();
			ArrayList<double[]> vfBetaDistributionLists = new ArrayList<double[]>();
			ArrayList<Integer> hifIdList = new ArrayList<Integer>();
			
			// Inspect each selected valuation function and create parallel lists of math expressions and valuation function config records
			for (ValuationConfig vfConfig : valuationTaskConfig.valuationFunctions) {
				if(!hifIdList.contains(vfConfig.hifId)) {
					hifIdList.add(vfConfig.hifId);
				}
				valuationFunctionList.add(ValuationUtil.getFunctionForVF(vfConfig.vfId));
				
				Record vfDefinition = ValuationUtil.getFunctionDefinition(vfConfig.vfId);
				vfDefinitionList.add(vfDefinition);
				
				vfConfig.vfRecord = vfDefinition.intoMap();
				
				double[] vfDistPercentiles = getPercentilesFromDistribution(vfDefinition);
				vfBetaDistributionLists.add(vfDistPercentiles);
			}
			
			
			HIFTaskConfig hifTaskConfig = HIFApi.getHifTaskConfigFromDb(valuationTaskConfig.hifResultDatasetId);

			//If valuationTaskConfig.incidenceAggregationGrid is null, set it to the HIF result grid
			if(valuationTaskConfig.gridDefinitionId == null ) {
				valuationTaskConfig.gridDefinitionId = HIFApi.getBaselineGridForHifResults(valuationTaskConfig.hifResultDatasetId);
			}
			
			//TEMP OVERRIDE for testing
			//valuationTaskConfig.gridDefinitionId = 18; //county
			
			//TODO: 2020 is the hardcoded max based on current data. This could be more dynamic.
			valuationTaskConfig.inflationYear = hifTaskConfig.popYear > 2020 ? 2020 : hifTaskConfig.popYear;			
			Map<String, Double> inflationIndices = ApiUtil.getInflationIndices(4, valuationTaskConfig.inflationYear, valuationTaskConfig.useInflationFactors);

			//TODO: 2050 is the hardcoded max based on current data. This could be more dynamic.
			valuationTaskConfig.incomeGrowthYear = hifTaskConfig.popYear > 2050 ? 2050 : hifTaskConfig.popYear;
			Map<Short, Record2<Short, Double>> incomeGrowthFactors = ApiUtil.getIncomeGrowthFactors(2, valuationTaskConfig.incomeGrowthYear, valuationTaskConfig.useGrowthFactors);
			
			//<variableName, <gridCellId, value>>
			List<String> requiredVariableNames = valuationTaskConfig.getRequiredVariableNames();
			Map<String, Map<Long, Double>> variables = ApiUtil.getVariableValues(requiredVariableNames, valuationTaskConfig.variableDatasetId, valuationTaskConfig.gridDefinitionId);
			
			Result<Record7<Long, Integer, Integer, Integer, Integer, Double, Double[]>> hifResults = null; //HIFApi.getHifResultsForValuation(valuationTaskConfig.hifResultDatasetId);

			ArrayList<ValuationResultRecord> valuationResults = new ArrayList<ValuationResultRecord>(maxRowsInMemory);
			mXparser.setToOverrideBuiltinTokens();
			
			int totalCells = HIFApi.getHifResultsRecordCount(valuationTaskConfig.hifResultDatasetId, hifIdList, valuationTaskConfig.gridDefinitionId);
			
			int currentCell = 0;
			int prevPct = -999;
			
			/*
			 * FOR EACH HEALTH IMPACT FUNCTION IN THE RUN
			 */
			
			messages.get(messages.size()-1).setStatus("complete");
			valuationTaskLog.addMessage("Loaded required datasets");

			messages.add(new TaskMessage("active", "Running valuation functions"));
			
			for(Integer hifId : hifIdList) {

				hifResults = HIFApi.getHifResultsForValuation(valuationTaskConfig.hifResultDatasetId, hifId, valuationTaskConfig.gridDefinitionId);					

				/*
				 * FOR EACH ROW IN THE HIF RESULTS
				 */
				for (Record7<Long, Integer, Integer, Integer, Integer, Double, Double[]> hifResult : hifResults) {
					
					// updating task percentage
					int currentPct = Math.round(currentCell * 100 / totalCells);
					currentCell++;

					if (prevPct != currentPct) {
						
						TaskQueue.updateTaskPercentage(taskUuid, currentPct, mapper.writeValueAsString(messages));
						TaskWorker.updateTaskWorkerHeartbeat(taskWorkerUuid);
						prevPct = currentPct;
					}

					/*
					 * RUN THE APPROPRIATE VALUATION FUNCTION(S) AND CAPTURE RESULTS
					 */
					for (int vfIdx = 0; vfIdx < valuationTaskConfig.valuationFunctions.size(); vfIdx++) {

						ValuationConfig vfConfig = valuationTaskConfig.valuationFunctions.get(vfIdx);
						if (vfConfig.hifId.equals(hifResult.get(HIF_RESULT.HIF_ID))) {
							
							// Use 1.0 if growth factor is not found or was disabled via valuationTaskConfig.useGrowthFactorsuse
							Record2<Short, Double> tmp = incomeGrowthFactors != null ? incomeGrowthFactors.getOrDefault(hifResult.get(4, Integer.class).shortValue(), null) : null;
							double incomeGrowthFactor = tmp == null ? 1.0 : tmp.value2().doubleValue();
							
							double hifEstimate = hifResult.get(5, Double.class).doubleValue();
							
							VFunction valuationFunction = valuationFunctionList.get(vfIdx);

							Record vfDefinition = vfDefinitionList.get(vfIdx);
							double[] betaDist = vfBetaDistributionLists.get(vfIdx);

							valuationFunction.vfArguments.allGoodsIndex = inflationIndices.get("AllGoodsIndex");
							valuationFunction.vfArguments.medicalCostIndex = inflationIndices.get("MedicalCostIndex");
							valuationFunction.vfArguments.wageIndex = inflationIndices.get("WageIndex");


							double valuationFunctionEstimate = 0.0;
							if(valuationFunction.nativeFunction == null) {
								Expression valuationFunctionExpression = valuationFunction.interpretedFunction;
								for(Entry<String, Map<Long, Double>> variable  : variables.entrySet()) {
									valuationFunctionExpression.setArgumentValue(variable.getKey(), variable.getValue().getOrDefault(hifResult.get(0), 0.0));		
								}
								valuationFunctionExpression.setArgumentValue("AllGoodsIndex", valuationFunction.vfArguments.allGoodsIndex);
								valuationFunctionExpression.setArgumentValue("MedicalCostIndex", valuationFunction.vfArguments.medicalCostIndex);
								valuationFunctionExpression.setArgumentValue("WageIndex", valuationFunction.vfArguments.wageIndex);

								valuationFunctionEstimate = valuationFunctionExpression.calculate();
							} else {

								for(Entry<String, Map<Long, Double>> variable  : variables.entrySet()) {
									valuationFunction.vfArguments.otherArguments.put(variable.getKey(), variable.getValue().getOrDefault(hifResult.get(0), 0.0));
								}

								valuationFunctionEstimate = valuationFunction.nativeFunction.calculate(valuationFunction.vfArguments);
							}

							valuationFunctionEstimate = valuationFunctionEstimate * incomeGrowthFactor * hifEstimate;
							
							DescriptiveStatistics distStats;
							Double[] hifPercentiles = (Double[]) hifResult.get("percentiles");
							
							distStats = combineUncertainty(hifPercentiles, betaDist, vfDefinition, valuationFunctionEstimate, hifEstimate);
							
							ValuationResultRecord rec = createResultsRecord(hifResult, vfConfig, valuationFunctionEstimate, distStats);
							valuationResults.add(rec);

							// Control the size of the results vector by saving partial results along the way
							if(valuationResults.size() >= maxRowsInMemory) {
								rowsSaved += valuationResults.size();
								messages.get(messages.size()-1).setMessage("Saving progress...");
								TaskQueue.updateTaskPercentage(taskUuid, currentPct, mapper.writeValueAsString(messages));
								ValuationUtil.storeResults(task, valuationTaskConfig, valuationResults);
								valuationResults.clear();
								messages.get(messages.size()-1).setMessage("Running valuation functions");
								TaskQueue.updateTaskPercentage(taskUuid, currentPct, mapper.writeValueAsString(messages));
							}
						}
					}
				}
			
			}
			
			
			
			rowsSaved += valuationResults.size();
			
			messages.get(messages.size()-1).setStatus("complete");
			valuationTaskLog.addMessage("Valuation function calculations complete");
			
			messages.add(new TaskMessage("active", String.format("Saving %,d results", rowsSaved)));
			TaskQueue.updateTaskPercentage(taskUuid, 100, mapper.writeValueAsString(messages));
			
			TaskWorker.updateTaskWorkerHeartbeat(taskWorkerUuid);
			ValuationUtil.storeResults(task, valuationTaskConfig, valuationResults);
			messages.get(messages.size()-1).setStatus("complete");
			
			String completeMessage = String.format("Saved %,d results", rowsSaved);
			valuationTaskLog.addMessage(completeMessage);
			valuationTaskLog.setSuccess(true);
			valuationTaskLog.setDtEnd(LocalDateTime.now());
			ValuationUtil.storeTaskLog(valuationTaskLog);
			TaskComplete.addTaskToCompleteAndRemoveTaskFromQueue(taskUuid, taskWorkerUuid, taskSuccessful, completeMessage);

		} catch (Exception e) {
			TaskComplete.addTaskToCompleteAndRemoveTaskFromQueue(taskUuid, taskWorkerUuid, false, "Task Failed");
			log.error("Task failed", e);
		}
		log.info("Valuation Task Complete: " + taskUuid);
	}

	private ValuationResultRecord createResultsRecord(
			Record7<Long, Integer, Integer, Integer, Integer, Double, Double[]> hifResult, ValuationConfig vfConfig,
			double valuationFunctionEstimate, DescriptiveStatistics distStats) {
		ValuationResultRecord rec = new ValuationResultRecord();
		rec.setGridCellId(hifResult.get(DSL.field("grid_cell_id", Long.class)));
		rec.setGridCol(hifResult.get(GET_HIF_RESULTS.GRID_COL));
		rec.setGridRow(hifResult.get(GET_HIF_RESULTS.GRID_ROW));
		rec.setHifId(vfConfig.hifId);
		rec.setHifInstanceId(vfConfig.hifInstanceId);
		rec.setVfId(vfConfig.vfId);

		rec.setResult(valuationFunctionEstimate);
		try {

			if (valuationFunctionEstimate == 0.0) {
				rec.setPct_2_5(0.0);
				rec.setPct_97_5(0.00);
				Double[] percentiles20 = new Double[20];
				Arrays.fill(percentiles20, 0.0);
				rec.setPercentiles(percentiles20);
				rec.setResultMean(0.0);
				rec.setStandardDev(0.0);
				rec.setResultVariance(0.0);
			} else {
				Double[] percentiles20 = new Double[20];
				// The old code used to grab the percentiles, put them into a new DescriptiveStatistics, and calculate the mean and
				// variance from that. It's probably better to do the statistics directly on distStats
				for (int i = 0; i < percentiles20.length; i++) {
					double p = (100.0 / percentiles20.length * i) + (100.0 / percentiles20.length / 2.0); // 2.5, 7.5, ... 97.5
					percentiles20[i] = distStats.getPercentile(p);
				}
				rec.setPct_2_5(distStats.getPercentile(2.5));
				rec.setPct_97_5(distStats.getPercentile(97.5));
				rec.setPercentiles(percentiles20);
				rec.setResultMean(distStats.getMean());
			
				rec.setStandardDev(distStats.getStandardDeviation());
				rec.setResultVariance(distStats.getVariance());

			}
		} catch (Exception e) {
			rec.setPct_2_5(0.0);
			rec.setPct_97_5(0.0);
			Double[] percentiles20 = new Double[20];
			Arrays.fill(percentiles20, 0.0);
			rec.setPercentiles(percentiles20);
			rec.setStandardDev(0.0);
			rec.setResultMean(0.0);
			rec.setResultVariance(0.0);
			log.info("Error populating valuation estimate", e);
		}

		return rec;
	}

	/**
	 * 
	 * Returns the 2.5, 7.5, ... percentiles from the provided valuation function's distribution.
	 * @param vfRecord
	 * @return
	 */
	private double[] getPercentilesFromDistribution(Record vfRecord) {
		double[] percentiles = new double[20];
		String distributionType = vfRecord.get("dist_a", String.class).toLowerCase();
		RealDistribution distribution;

		switch (distributionType) {
		case "none":
			double value = vfRecord.get("val_a", Double.class).doubleValue();
			for (int i = 0; i < percentiles.length; i++) {
				percentiles[i] = value;
			}
			return percentiles;
		case "normal":
			distribution = new NormalDistribution(vfRecord.get("val_a", Double.class).doubleValue(), vfRecord.get("p1a", Double.class).doubleValue());
			break;
		case "weibull":
			distribution = new WeibullDistribution(vfRecord.get("p2a", Double.class).doubleValue(), vfRecord.get("p1a", Double.class).doubleValue());
			break;
		case "lognormal":
			distribution = new LogNormalDistribution(vfRecord.get("p1a", Double.class).doubleValue(), vfRecord.get("p2a", Double.class).doubleValue());
			break;
		case "triangular":
			//lower, mode, upper
			distribution = new TriangularDistribution(vfRecord.get("p1a", Double.class).doubleValue(), vfRecord.get("val_a", Double.class).doubleValue(), vfRecord.get("p2a", Double.class).doubleValue());
			break;
		default:
			return null;
		}

		for (int i = 0; i < percentiles.length; i++) {
			double p = (100.0/percentiles.length/2) + i*(100.0/percentiles.length);
			percentiles[i] = distribution.inverseCumulativeProbability(p / 100.0);
		}

		return percentiles;
	}

	private DescriptiveStatistics combineUncertainty(Double[] hifPercentiles, double[] betaDist, Record vfDefinition, double valuationFunctionEstimate, double hifEstimate) {
		DescriptiveStatistics distStats = new DescriptiveStatistics();

		for(int hifPctIdx=0; hifPctIdx < hifPercentiles.length; hifPctIdx++) {
			for(int betaIdx=0; betaIdx < betaDist.length; betaIdx++) {
				//valuation estimate * hif percentiles * betaDist / hif point estimate * A
				if(vfDefinition.get("val_a", Double.class) == null || vfDefinition.get("val_a", Double.class).doubleValue() == 0.0) {
					distStats.addValue(valuationFunctionEstimate * hifPercentiles[hifPctIdx].doubleValue() / hifEstimate);
				} else {
					distStats.addValue(valuationFunctionEstimate * hifPercentiles[hifPctIdx].doubleValue() * betaDist[betaIdx] / (hifEstimate * vfDefinition.get("val_a", Double.class).doubleValue()));			
				}
			}
		}

		return distStats;
	}


}

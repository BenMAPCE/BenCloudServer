package gov.epa.bencloud.server.tasks.local;

import static gov.epa.bencloud.server.database.jooq.data.Tables.HEALTH_IMPACT_FUNCTION;
import static gov.epa.bencloud.server.database.jooq.data.Tables.HIF_RESULT;
import static gov.epa.bencloud.server.database.jooq.data.Tables.HIF_RESULT_FUNCTION_CONFIG;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.commons.math3.distribution.LogNormalDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.distribution.TriangularDistribution;
import org.apache.commons.math3.distribution.WeibullDistribution;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jooq.Record13;
import org.jooq.Record2;
import org.jooq.Record7;
import org.jooq.Result;
import org.mariuszgromada.math.mxparser.Expression;
import org.mariuszgromada.math.mxparser.mXparser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.epa.bencloud.api.HIFApi;
import gov.epa.bencloud.api.model.HIFConfig;
import gov.epa.bencloud.api.model.HIFTaskConfig;
import gov.epa.bencloud.api.model.ValuationConfig;
import gov.epa.bencloud.api.model.ValuationTaskConfig;
import gov.epa.bencloud.api.util.ApiUtil;
import gov.epa.bencloud.api.util.HIFUtil;
import gov.epa.bencloud.api.util.ValuationUtil;
import gov.epa.bencloud.server.database.jooq.data.tables.records.ValuationFunctionRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.ValuationResultRecord;
import gov.epa.bencloud.server.tasks.TaskComplete;
import gov.epa.bencloud.server.tasks.TaskQueue;
import gov.epa.bencloud.server.tasks.TaskWorker;
import gov.epa.bencloud.server.tasks.model.Task;

public class ValuationTaskRunnable implements Runnable {

	private String taskUuid;
	private String taskWorkerUuid;

	public ValuationTaskRunnable(String taskUuid, String taskWorkerUuid) {
		this.taskUuid = taskUuid;
		this.taskWorkerUuid = taskWorkerUuid;
	}

	private boolean taskSuccessful = true;
	private String taskCompleteMessage = "Task Complete";

	public void run() {

		Task task = TaskQueue.getTaskFromQueueRecord(taskUuid);
		final int maxRowsInMemory = 100000;
		
		int rowsSaved = 0;
		
		try {
			TaskQueue.updateTaskPercentage(taskUuid, 1, "Loading datasets");
			TaskWorker.updateTaskWorkerHeartbeat(taskWorkerUuid);
			
			ValuationTaskConfig valuationTaskConfig = parseTaskParameters(task);

			//Use the hifTaskUuid to wait here until the HIF run is no longer pending
			String hifTaskStatus = HIFApi.getHIFTaskStatus(valuationTaskConfig.hifTaskUuid);
			
			if(hifTaskStatus.equals("pending")) {
				TaskQueue.updateTaskPercentage(taskUuid, 0, "Waiting for HIF analysis");
				
				// Check again every 10 seconds. Keep the heartbeat updated so the worker doesn't look dead.
				while(hifTaskStatus.equals("pending")) {
					Thread.sleep(10000);
					//System.out.println("Valuation task waiting for: " + valuationTaskConfig.name);
					TaskWorker.updateTaskWorkerHeartbeat(taskWorkerUuid);
					hifTaskStatus = HIFApi.getHIFTaskStatus(valuationTaskConfig.hifTaskUuid);
				}
			}
				
			//If the HIF task is failed, let's also fail the valuation task			
			if(hifTaskStatus.equals("failed")) {
				TaskComplete.addTaskToCompleteAndRemoveTaskFromQueue(taskUuid, taskWorkerUuid, false, "Associated HIF task failed");
				return;
			}

			
			// If we get here, the HIF task isn't pending or failed, so it must have succeeded
			TaskQueue.updateTaskPercentage(taskUuid, 1, "Preparing datasets for valuation");
			
			valuationTaskConfig.hifResultDatasetId = HIFApi.getHIFResultDatasetId(valuationTaskConfig.hifTaskUuid);
			
			if(valuationTaskConfig.hifResultDatasetId == null) {
				TaskComplete.addTaskToCompleteAndRemoveTaskFromQueue(taskUuid, taskWorkerUuid, false, "Unable to load HIF estimates");
				return;
			}
			
			List<Expression> valuationFunctionExpressionList = new ArrayList<Expression>();

			List<ValuationFunctionRecord> vfDefinitionList = new ArrayList<ValuationFunctionRecord>();
			ArrayList<double[]> vfBetaDistributionLists = new ArrayList<double[]>();
			ArrayList<Integer> hifIdList = new ArrayList<Integer>();
			
			// Inspect each selected valuation function and create parallel lists of math expressions and valuation function config records
			for (ValuationConfig vfConfig : valuationTaskConfig.valuationFunctions) {
				if(!hifIdList.contains(vfConfig.hifId)) {
					hifIdList.add(vfConfig.hifId);
				}
				valuationFunctionExpressionList.add(ValuationUtil.getFunctionExpression(vfConfig.vfId));
				ValuationFunctionRecord vfDefinition = ValuationUtil.getFunctionDefinition(vfConfig.vfId);
				vfDefinitionList.add(vfDefinition);
				
				double[] distBetas = new double[100];
				double[] distSamples = getDistributionSamples(vfDefinition);
				int idxMedian = 0 + distSamples.length / distBetas.length / 2; //the median of the first segment
				
				for(int i=0; i < distBetas.length; i++) {
					// Grab the median from each of the 100 slices of distList
					distBetas[i] = (distSamples[idxMedian]+distSamples[idxMedian-1])/2.0;
					idxMedian += distSamples.length / distBetas.length;
				}
				vfBetaDistributionLists.add(distBetas);
			}
			
			
			HIFTaskConfig hifTaskConfig = HIFApi.getHifTaskConfigFromDb(valuationTaskConfig.hifResultDatasetId);

			int inflationYear = hifTaskConfig.popYear > 2020 ? 2020 : hifTaskConfig.popYear;
			Map<String, Double> inflationIndices = ApiUtil.getInflationIndices(4, inflationYear);
			Map<Short, Record2<Short, Double>> incomeGrowthFactors = ApiUtil.getIncomeGrowthFactors(2, hifTaskConfig.popYear);
			
			//<variableName, <gridCellId, value>>
			Map<String, Map<Integer, Double>> variables = ApiUtil.getVariableValues(valuationTaskConfig, vfDefinitionList);
			
			Result<Record7<Integer, Integer, Integer, Integer, Integer, Double, Double[]>> hifResults = null; //HIFApi.getHifResultsForValuation(valuationTaskConfig.hifResultDatasetId);

			ArrayList<ValuationResultRecord> valuationResults = new ArrayList<ValuationResultRecord>(maxRowsInMemory);
			mXparser.setToOverrideBuiltinTokens();
			
			int totalCells = HIFApi.getHifResultsRecordCount(valuationTaskConfig.hifResultDatasetId, hifIdList);
			
			int currentCell = 0;
			int prevPct = -999;
			
			/*
			 * FOR EACH HEALTH IMPACT FUNCTION IN THE RUN
			 */
			
			for(Integer hifId : hifIdList) {

				hifResults = HIFApi.getHifResultsForValuation(valuationTaskConfig.hifResultDatasetId, hifId);					

				/*
				 * FOR EACH ROW IN THE HIF RESULTS
				 */
				for (Record7<Integer, Integer, Integer, Integer, Integer, Double, Double[]> hifResult : hifResults) {
					
					// updating task percentage
					int currentPct = Math.round(currentCell * 100 / totalCells);
					currentCell++;

					if (prevPct != currentPct) {
						TaskQueue.updateTaskPercentage(taskUuid, currentPct, "Running valuation functions");
						TaskWorker.updateTaskWorkerHeartbeat(taskWorkerUuid);
						prevPct = currentPct;
					}

					/*
					 * RUN THE APPROPRIATE VALUATION FUNCTION(S) AND CAPTURE RESULTS
					 */
					for (int vfIdx = 0; vfIdx < valuationTaskConfig.valuationFunctions.size(); vfIdx++) {

						ValuationConfig vfConfig = valuationTaskConfig.valuationFunctions.get(vfIdx);
						if (vfConfig.hifId.equals(hifResult.get(HIF_RESULT.HIF_ID))) {
							Record2<Short, Double> tmp = incomeGrowthFactors.getOrDefault(hifResult.get(HEALTH_IMPACT_FUNCTION.ENDPOINT_GROUP_ID).shortValue(), null);
							double incomeGrowthFactor = tmp == null ? 1.0 : tmp.value2().doubleValue();
							
							double hifEstimate = hifResult.get(HIF_RESULT.RESULT).doubleValue();
							
							Expression valuationFunctionExpression = valuationFunctionExpressionList.get(vfIdx);

							ValuationFunctionRecord vfDefinition = vfDefinitionList.get(vfIdx);
							double[] betaDist = vfBetaDistributionLists.get(vfIdx);

							//If the function uses a variable that was loaded, set the appropriate argument value for this cell
							for(Entry<String, Map<Integer, Double>> variable  : variables.entrySet()) {
								if(valuationFunctionExpression.getArgument(variable.getKey()) != null) {
									valuationFunctionExpression.setArgumentValue(variable.getKey(), variable.getValue().getOrDefault(hifResult.get(HIF_RESULT.GRID_CELL_ID), 0.0));		
								}
							}
							valuationFunctionExpression.setArgumentValue("AllGoodsIndex", inflationIndices.get("AllGoodsIndex"));
							valuationFunctionExpression.setArgumentValue("MedicalCostIndex", inflationIndices.get("MedicalCostIndex"));
							valuationFunctionExpression.setArgumentValue("WageIndex", inflationIndices.get("WageIndex"));

							double valuationFunctionEstimate = valuationFunctionExpression.calculate();
							valuationFunctionEstimate = valuationFunctionEstimate * incomeGrowthFactor * hifEstimate;
							
							DescriptiveStatistics distStats = new DescriptiveStatistics();
							Double[] hifPercentiles = hifResult.get(HIF_RESULT.PERCENTILES);
							
							for(int hifPctIdx=0; hifPctIdx < hifPercentiles.length; hifPctIdx++) {
								for(int betaIdx=0; betaIdx < betaDist.length; betaIdx++) {
									//valuation estimate * hif percentiles * betaDist / hif point estimate * A
									if(vfDefinition.getValA() == null || vfDefinition.getValA().doubleValue() == 0.0) {
										distStats.addValue(valuationFunctionEstimate * hifPercentiles[hifPctIdx].doubleValue() / hifEstimate);
										
									} else {
										distStats.addValue(valuationFunctionEstimate * hifPercentiles[hifPctIdx].doubleValue() * betaDist[betaIdx] / (hifEstimate * vfDefinition.getValA().doubleValue()));			
									}
								}
							}
							
							ValuationResultRecord rec = new ValuationResultRecord();
							rec.setGridCellId(hifResult.get(HIF_RESULT.GRID_CELL_ID));
							rec.setGridCol(hifResult.get(HIF_RESULT.GRID_COL));
							rec.setGridRow(hifResult.get(HIF_RESULT.GRID_ROW));
							rec.setHifId(vfConfig.hifId);
							rec.setVfId(vfConfig.vfId);

							rec.setResult(valuationFunctionEstimate);
							try {

								if (valuationFunctionEstimate == 0.0) {

								} else {

									double[] percentiles = new double[100];
									Double[] percentiles20 = new Double[20];
									double[] distValues = distStats.getSortedValues();
									int idxMedian = distValues.length / percentiles.length / 2; // the median of the first segment
									int idxMedian20 = distValues.length / percentiles20.length / 2; // the median of the first segment
									DescriptiveStatistics statsPercentiles = new DescriptiveStatistics();
									for (int i = 0; i < percentiles.length; i++) {
										// Grab the median from each of the 100 slices of distStats
										percentiles[i] = (distValues[idxMedian] + distValues[idxMedian - 1]) / 2.0;
										statsPercentiles.addValue(percentiles[i]);
										idxMedian += distValues.length / percentiles.length;
									}
									for (int i = 0; i < percentiles20.length; i++) {
										// Grab the median from each of the 20 slices of distStats
										percentiles20[i] = (distValues[idxMedian20] + distValues[idxMedian20 - 1]) / 2.0;
										//statsPercentiles.addValue(percentiles[i]);
										idxMedian20 += distValues.length / percentiles20.length;
									}
									rec.setPct_2_5((percentiles[1] + percentiles[2]) / 2.0);
									rec.setPct_97_5((percentiles[96] + percentiles[97]) / 2.0);
									rec.setPercentiles(percentiles20);
									rec.setStandardDev(statsPercentiles.getStandardDeviation());
									rec.setResultMean(statsPercentiles.getMean());
									rec.setResultVariance(statsPercentiles.getVariance());
								}
							} catch (Exception e) {
								rec.setPct_2_5(0.0);
								rec.setPct_97_5(0.0);
								rec.setStandardDev(0.0);
								rec.setResultMean(0.0);
								rec.setResultVariance(0.0);
								e.printStackTrace();
							}


							
							valuationResults.add(rec);

							// Control the size of the results vector by saving partial results along the way
							if(valuationResults.size() >= maxRowsInMemory) {
								rowsSaved += valuationResults.size();
								TaskQueue.updateTaskPercentage(taskUuid, currentPct, "Saving progress...");
								ValuationUtil.storeResults(task, valuationTaskConfig, valuationResults);
								valuationResults.clear();
							}
						}
					}
				}
			
			}
			
			
			
			rowsSaved += valuationResults.size();
			TaskQueue.updateTaskPercentage(taskUuid, 100, String.format("Saving %,d results", rowsSaved));
			TaskWorker.updateTaskWorkerHeartbeat(taskWorkerUuid);
			ValuationUtil.storeResults(task, valuationTaskConfig, valuationResults);

			TaskComplete.addTaskToCompleteAndRemoveTaskFromQueue(taskUuid, taskWorkerUuid, taskSuccessful, taskCompleteMessage);

		} catch (Exception e) {
			TaskComplete.addTaskToCompleteAndRemoveTaskFromQueue(taskUuid, taskWorkerUuid, false, "Task Failed");
			e.printStackTrace();
		}
	}

	public ValuationTaskConfig parseTaskParameters(Task task) {

		ValuationTaskConfig valuationTaskConfig = new ValuationTaskConfig();

		try {
			String paramString = task.getParameters();
			ObjectMapper mapper = new ObjectMapper();
			JsonNode params = mapper.readTree(paramString);

			valuationTaskConfig.name = task.getName();
			valuationTaskConfig.hifTaskUuid = params.get("hif_task_uuid").asText();
			valuationTaskConfig.hifResultDatasetId = params.get("hif_result_dataset_id").asInt();
			valuationTaskConfig.variableDatasetId = params.get("variable_dataset_id")==null || params.get("variable_dataset_id").isEmpty() ? 1 : params.get("variable_dataset_id").asInt();

			JsonNode functions = params.get("functions");
			parseFunctions(functions, valuationTaskConfig);
			
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return valuationTaskConfig;
	}

	private void parseFunctions(JsonNode functions, ValuationTaskConfig valuationTaskConfig) {
		for (JsonNode function : functions) {
			ValuationConfig valuationConfig = new ValuationConfig();
			valuationConfig.hifId = function.get("hif_id").asInt();
			valuationConfig.vfId = function.get("vf_id").asInt();
			valuationTaskConfig.valuationFunctions.add(valuationConfig);
		}
	}
	
	private double[] getDistributionSamples(ValuationFunctionRecord vfRecord) {
		double[] samples = new double[10000];
		Random rng = new Random(1);
		RealDistribution distribution;
		
		switch (vfRecord.getDistA().toLowerCase()) {
		case "none":		
			for (int i = 0; i < samples.length; i++)
			{
				samples[i]=vfRecord.getValA().doubleValue();
			}
			return samples;
		case "normal":
			distribution = new NormalDistribution(vfRecord.getValA().doubleValue(), vfRecord.getP1a().doubleValue());
			break;
		case "weibull":
			distribution = new WeibullDistribution(vfRecord.getP2a().doubleValue(), vfRecord.getP1a().doubleValue());
			break;
		case "lognormal":
			distribution = new LogNormalDistribution(vfRecord.getP1a().doubleValue(), vfRecord.getP2a().doubleValue());
			break;
		case "triangular":
			//lower, mode, upper
			distribution = new TriangularDistribution(vfRecord.getP1a().doubleValue(), vfRecord.getValA().doubleValue(), vfRecord.getP2a().doubleValue());
			break;
		default:
			return null;
		}
		
		for (int i = 0; i < samples.length; i++)
		{
			double x = distribution.inverseCumulativeProbability(rng.nextDouble());
			samples[i]=x;
		}
		Arrays.sort(samples);
		return samples;
	}

}

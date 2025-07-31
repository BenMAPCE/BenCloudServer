package gov.epa.bencloud.server.tasks;

import static gov.epa.bencloud.server.database.jooq.data.Tables.*;

import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.pac4j.core.profile.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.epa.bencloud.api.util.FilestoreUtil;
import gov.epa.bencloud.server.database.JooqUtil;

public class TaskUtil {

	private static final Logger log = LoggerFactory.getLogger(TaskUtil.class);

	
	public static void deleteHifResults(String uuid, Boolean deleteTask) {
		
		try {

			DSL.using(JooqUtil.getJooqConfiguration())
					.transaction(ctx -> {

				Result<Record1<Integer>> hifResultDatasets = 
						DSL.using(ctx).select(HIF_RESULT_DATASET.ID).from(HIF_RESULT_DATASET)
						.where(HIF_RESULT_DATASET.TASK_UUID.eq(uuid))
						.fetch();
				
				if (hifResultDatasets.size() == 0 && deleteTask) {
					DSL.using(ctx).deleteFrom(TASK_COMPLETE)
					.where(TASK_COMPLETE.TASK_UUID.eq(uuid))
					.execute();
				} else if (hifResultDatasets.size() > 1) {
					log.info("recieved more than 1 HIF Result Dataset record");
				} else {

					Record hifResultDataset = hifResultDatasets.get(0);

//					Result<Record1<Integer>> hifResults = 
//							DSL.using(ctx).select(HIF_RESULT.HIF_RESULT_DATASET_ID).from(HIF_RESULT)
//					.where(HIF_RESULT.HIF_RESULT_DATASET_ID.eq(hifResultDataset.get(HIF_RESULT_DATASET.ID)))
//					.fetch();

					DSL.using(ctx).deleteFrom(HIF_RESULT)
					.where(HIF_RESULT.HIF_RESULT_DATASET_ID.eq(hifResultDataset.get(HIF_RESULT_DATASET.ID)))
					.execute();

					DSL.using(ctx).deleteFrom(HIF_RESULT_AGG)
					.where(HIF_RESULT_AGG.HIF_RESULT_DATASET_ID.eq(hifResultDataset.get(HIF_RESULT_DATASET.ID)))
					.execute();
					
					DSL.using(ctx).deleteFrom(HIF_RESULT_DATASET)
					.where(HIF_RESULT_DATASET.ID.eq(hifResultDataset.get(HIF_RESULT_DATASET.ID)))
					.execute();

					DSL.using(ctx).deleteFrom(HIF_RESULT_FUNCTION_CONFIG)
					.where(HIF_RESULT_FUNCTION_CONFIG.HIF_RESULT_DATASET_ID.eq(hifResultDataset.get(HIF_RESULT_DATASET.ID)))
					.execute();
					
					if(deleteTask) {
						DSL.using(ctx).deleteFrom(TASK_COMPLETE)
						.where(TASK_COMPLETE.TASK_UUID.eq(uuid))
						.execute();
					}

					
				}
			});
		} catch (Exception e) {
			log.error("Error deleting HIF results", e);
		} finally {

		}

	}

	public static void deleteValuationResults(String uuid, Boolean deleteTask) {
		
		try {

			DSL.using(JooqUtil.getJooqConfiguration())
					.transaction(ctx -> {

				Result<Record1<Integer>> valuationResultDatasets = 
						DSL.using(ctx).select(VALUATION_RESULT_DATASET.ID).from(VALUATION_RESULT_DATASET)
						.where(VALUATION_RESULT_DATASET.TASK_UUID.eq(uuid))
						.fetch();

				if (valuationResultDatasets.size() == 0 && deleteTask) {
					DSL.using(ctx).deleteFrom(TASK_COMPLETE)
					.where(TASK_COMPLETE.TASK_UUID.eq(uuid))
					.execute();
				} else if (valuationResultDatasets.size() > 1) {
					log.info("recieved more than 1 Valuation Result Dataset record");
				} else {

					Record valuationResultDataset = valuationResultDatasets.get(0);

//					Result<Record1<Integer>> ValuationResults = 
//							DSL.using(ctx).select(VALUATION_RESULT.VALUATION_RESULT_DATASET_ID).from(VALUATION_RESULT)
//					.where(VALUATION_RESULT.VALUATION_RESULT_DATASET_ID.eq(valuationResultDataset.get(VALUATION_RESULT_DATASET.ID)))
//					.fetch();

					DSL.using(ctx).deleteFrom(VALUATION_RESULT)
					.where(VALUATION_RESULT.VALUATION_RESULT_DATASET_ID.eq(valuationResultDataset.get(VALUATION_RESULT_DATASET.ID)))
					.execute();

					DSL.using(ctx).deleteFrom(VALUATION_RESULT_AGG)
					.where(VALUATION_RESULT_AGG.VALUATION_RESULT_DATASET_ID.eq(valuationResultDataset.get(VALUATION_RESULT_DATASET.ID)))
					.execute();
					
					DSL.using(ctx).deleteFrom(VALUATION_RESULT_DATASET)
					.where(VALUATION_RESULT_DATASET.ID.eq(valuationResultDataset.get(VALUATION_RESULT_DATASET.ID)))
					.execute();

					DSL.using(ctx).deleteFrom(VALUATION_RESULT_FUNCTION_CONFIG)
					.where(VALUATION_RESULT_FUNCTION_CONFIG.VALUATION_RESULT_DATASET_ID.eq(valuationResultDataset.get(VALUATION_RESULT_DATASET.ID)))
					.execute();
					
					if(deleteTask) {
						DSL.using(ctx).deleteFrom(TASK_COMPLETE)
						.where(TASK_COMPLETE.TASK_UUID.eq(uuid))
						.execute();
					}

					
				}
			});
		} catch (Exception e) {
			log.error("Error deleting valuation results", e);
		} finally {

		}

	}

	public static void deleteExposureResults(String uuid, Boolean deleteTask) {
		
		try {

			DSL.using(JooqUtil.getJooqConfiguration())
					.transaction(ctx -> {

				Result<Record1<Integer>> exposureResultDatasets = 
						DSL.using(ctx).select(EXPOSURE_RESULT_DATASET.ID).from(EXPOSURE_RESULT_DATASET)
						.where(EXPOSURE_RESULT_DATASET.TASK_UUID.eq(uuid))
						.fetch();

				if (exposureResultDatasets.size() == 0 && deleteTask) {
					DSL.using(ctx).deleteFrom(TASK_COMPLETE)
					.where(TASK_COMPLETE.TASK_UUID.eq(uuid))
					.execute();
				} else if (exposureResultDatasets.size() > 1) {
					log.info("recieved more than 1 Exposure Result Dataset record");
				} else {

					Record exposureResultDataset = exposureResultDatasets.get(0);

					DSL.using(ctx).deleteFrom(EXPOSURE_RESULT)
					.where(EXPOSURE_RESULT.EXPOSURE_RESULT_DATASET_ID.eq(exposureResultDataset.get(EXPOSURE_RESULT_DATASET.ID)))
					.execute();

					DSL.using(ctx).deleteFrom(EXPOSURE_RESULT_AGG)
					.where(EXPOSURE_RESULT_AGG.EXPOSURE_RESULT_DATASET_ID.eq(exposureResultDataset.get(EXPOSURE_RESULT_DATASET.ID)))
					.execute();
					
					DSL.using(ctx).deleteFrom(EXPOSURE_RESULT_DATASET)
					.where(EXPOSURE_RESULT_DATASET.ID.eq(exposureResultDataset.get(EXPOSURE_RESULT_DATASET.ID)))
					.execute();

					DSL.using(ctx).deleteFrom(EXPOSURE_RESULT_FUNCTION_CONFIG)
					.where(EXPOSURE_RESULT_FUNCTION_CONFIG.EXPOSURE_RESULT_DATASET_ID.eq(exposureResultDataset.get(EXPOSURE_RESULT_DATASET.ID)))
					.execute();
					
					if(deleteTask) {
						DSL.using(ctx).deleteFrom(TASK_COMPLETE)
						.where(TASK_COMPLETE.TASK_UUID.eq(uuid))
						.execute();
					}

					
				}
			});
		} catch (Exception e) {
			log.error("Error deleting exposure results", e);
		} finally {

		}

	}

	public static void deleteResultExportResults(String uuid, Boolean deleteTask) {
		
		try {
			DSL.using(JooqUtil.getJooqConfiguration())
					.transaction(ctx -> {

				Record1<String> taskParameterRecord = DSL.using(ctx)
						.select(TASK_COMPLETE.TASK_PARAMETERS)
						.from(TASK_COMPLETE)
						.where(TASK_COMPLETE.TASK_UUID.eq(uuid))
						.fetchOne();

				Integer filestoreId = null;
				try {
					ObjectMapper objectMapper = new ObjectMapper();
					JsonNode json = objectMapper.readTree(taskParameterRecord.value1());
					JsonNode filestoreIdJsonNode = json.get("filestoreId");
					if (filestoreIdJsonNode != null) {
						filestoreId = filestoreIdJsonNode.asInt();
					}
				} catch (JsonMappingException e) {
					e.printStackTrace();
				} catch (JsonProcessingException e) {
					e.printStackTrace();
				} 
				
				if (filestoreId != null) {
					FilestoreUtil.deleteFile(filestoreId);
				}

				if(deleteTask) {
					DSL.using(ctx).deleteFrom(TASK_COMPLETE)
					.where(TASK_COMPLETE.TASK_UUID.eq(uuid))
					.execute();
				}
			});
		} catch (Exception e) {
			log.error("Error deleting result export results", e);
		} finally {

		}

	}
}

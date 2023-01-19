package gov.epa.bencloud.server.tasks;

import static gov.epa.bencloud.server.database.jooq.data.Tables.*;

import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.pac4j.core.profile.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.epa.bencloud.server.database.JooqUtil;

public class TaskUtil {

	private static final Logger log = LoggerFactory.getLogger(TaskUtil.class);

	
	public static void deleteHifResults(String uuid) {
		
		try {

			DSL.using(JooqUtil.getJooqConfiguration())
					.transaction(ctx -> {

				Result<Record1<Integer>> hifResultDatasets = 
						DSL.using(ctx).select(HIF_RESULT_DATASET.ID).from(HIF_RESULT_DATASET)
						.where(HIF_RESULT_DATASET.TASK_UUID.eq(uuid))
						.fetch();
				
				if (hifResultDatasets.size() == 0) {
					DSL.using(ctx).deleteFrom(TASK_COMPLETE)
					.where(TASK_COMPLETE.TASK_UUID.eq(uuid))
					.execute();
				} else if (hifResultDatasets.size() > 1) {
					log.info("recieved more than 1 HIF Result Dataset record");
				} else {

					Record hifResultDataset = hifResultDatasets.get(0);

					Result<Record1<Integer>> hifResults = 
							DSL.using(ctx).select(HIF_RESULT.HIF_RESULT_DATASET_ID).from(HIF_RESULT)
					.where(HIF_RESULT.HIF_RESULT_DATASET_ID.eq(hifResultDataset.get(HIF_RESULT_DATASET.ID)))
					.fetch();

					DSL.using(ctx).deleteFrom(HIF_RESULT)
					.where(HIF_RESULT.HIF_RESULT_DATASET_ID.eq(hifResultDataset.get(HIF_RESULT_DATASET.ID)))
					.execute();
					
					DSL.using(ctx).deleteFrom(HIF_RESULT_DATASET)
					.where(HIF_RESULT_DATASET.ID.eq(hifResultDataset.get(HIF_RESULT_DATASET.ID)))
					.execute();

					DSL.using(ctx).deleteFrom(HIF_RESULT_FUNCTION_CONFIG)
					.where(HIF_RESULT_FUNCTION_CONFIG.HIF_RESULT_DATASET_ID.eq(hifResultDataset.get(HIF_RESULT_DATASET.ID)))
					.execute();

					DSL.using(ctx).deleteFrom(TASK_COMPLETE)
					.where(TASK_COMPLETE.TASK_UUID.eq(uuid))
					.execute();
				}
			});
		} catch (Exception e) {
			log.error("Error deleting HIF results", e);
		} finally {

		}

	}

	public static void deleteValuationResults(String uuid) {
		
		try {

			DSL.using(JooqUtil.getJooqConfiguration())
					.transaction(ctx -> {

				Result<Record1<Integer>> valuationResultDatasets = 
						DSL.using(ctx).select(VALUATION_RESULT_DATASET.ID).from(VALUATION_RESULT_DATASET)
						.where(VALUATION_RESULT_DATASET.TASK_UUID.eq(uuid))
						.fetch();

				if (valuationResultDatasets.size() == 0) {
					DSL.using(ctx).deleteFrom(TASK_COMPLETE)
					.where(TASK_COMPLETE.TASK_UUID.eq(uuid))
					.execute();
				} else if (valuationResultDatasets.size() > 1) {
					log.info("recieved more than 1 Valuation Result Dataset record");
				} else {

					Record valuationResultDataset = valuationResultDatasets.get(0);

					Result<Record1<Integer>> ValuationResults = 
							DSL.using(ctx).select(VALUATION_RESULT.VALUATION_RESULT_DATASET_ID).from(VALUATION_RESULT)
					.where(VALUATION_RESULT.VALUATION_RESULT_DATASET_ID.eq(valuationResultDataset.get(VALUATION_RESULT_DATASET.ID)))
					.fetch();

					DSL.using(ctx).deleteFrom(VALUATION_RESULT)
					.where(VALUATION_RESULT.VALUATION_RESULT_DATASET_ID.eq(valuationResultDataset.get(VALUATION_RESULT_DATASET.ID)))
					.execute();
					
					DSL.using(ctx).deleteFrom(VALUATION_RESULT_DATASET)
					.where(VALUATION_RESULT_DATASET.ID.eq(valuationResultDataset.get(VALUATION_RESULT_DATASET.ID)))
					.execute();

					DSL.using(ctx).deleteFrom(VALUATION_RESULT_FUNCTION_CONFIG)
					.where(VALUATION_RESULT_FUNCTION_CONFIG.VALUATION_RESULT_DATASET_ID.eq(valuationResultDataset.get(VALUATION_RESULT_DATASET.ID)))
					.execute();

					DSL.using(ctx).deleteFrom(TASK_COMPLETE)
					.where(TASK_COMPLETE.TASK_UUID.eq(uuid))
					.execute();
				}
			});
		} catch (Exception e) {
			log.error("Error deleting valuation results", e);
		} finally {

		}

	}

}

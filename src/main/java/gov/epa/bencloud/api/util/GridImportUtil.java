package gov.epa.bencloud.api.util;

import static gov.epa.bencloud.server.database.jooq.data.Tables.HIF_RESULT_DATASET;

import org.jooq.DSLContext;
import org.jooq.JSON;
import org.jooq.impl.DSL;

import gov.epa.bencloud.api.model.GridImportTaskLog;
import gov.epa.bencloud.api.model.HIFTaskLog;
import gov.epa.bencloud.server.database.JooqUtil;

public class GridImportUtil {
	public static void storeTaskLog(GridImportTaskLog gridImportTaskLog) {
		
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());
		JSON taskLogJson = JSON.json(gridImportTaskLog.toJsonString());
		
		//TODO: Figure out the base place to store the grid import task log
		// Probably should go in the grid_definition table?
		
//		create.update()
//			.set(HIF_RESULT_DATASET.TASK_LOG, taskLogJson)
//			.where(HIF_RESULT_DATASET.ID.eq(gridImportTaskLog.getGridImportTaskConfig().resultDatasetId))
//			.execute();
		
	}
}

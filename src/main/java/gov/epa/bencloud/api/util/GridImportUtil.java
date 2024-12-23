package gov.epa.bencloud.api.util;

import static gov.epa.bencloud.server.database.jooq.data.Tables.GRID_DEFINITION;

import org.jooq.DSLContext;
import org.jooq.JSON;
import org.jooq.impl.DSL;

import gov.epa.bencloud.api.model.GridImportTaskLog;
import gov.epa.bencloud.server.database.JooqUtil;

public class GridImportUtil {
	public static void storeTaskLog(GridImportTaskLog gridImportTaskLog) {
		
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());
		JSON taskLogJson = JSON.json(gridImportTaskLog.toJsonString());
		
		create.update(GRID_DEFINITION)
			.set(GRID_DEFINITION.TASK_LOG, taskLogJson)
			.where(GRID_DEFINITION.ID.eq(gridImportTaskLog.getGridImportTaskConfig().gridDefinitionId))
			.execute();
		
	}
}

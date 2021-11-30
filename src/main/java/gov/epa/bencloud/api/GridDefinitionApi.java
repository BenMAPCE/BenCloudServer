package gov.epa.bencloud.api;

import static gov.epa.bencloud.server.database.jooq.data.Tables.*;

import org.jooq.JSONFormat;
import org.jooq.Result;
import org.jooq.JSONFormat.RecordFormat;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.impl.DSL;
import gov.epa.bencloud.server.database.JooqUtil;
import spark.Response;

public class GridDefinitionApi {

	public static Object getAllGridDefinitions(Response response) {
		Result<Record> gridRecords = DSL.using(JooqUtil.getJooqConfiguration())
				.select(GRID_DEFINITION.asterisk())
				.from(GRID_DEFINITION)
				.orderBy(GRID_DEFINITION.NAME)
				.fetch();
		
		response.type("application/json");
		return gridRecords.formatJSON(new JSONFormat().header(false).recordFormat(RecordFormat.OBJECT));
	}
	
	public static String getGridDefinitionName(int gridId) {

		Record1<String> gridRecord = DSL.using(JooqUtil.getJooqConfiguration())
				.select(GRID_DEFINITION.NAME)
				.from(GRID_DEFINITION)
				.where(GRID_DEFINITION.ID.eq(gridId))
				.fetchOne();
		
		return gridRecord.value1();
	}
}

package gov.epa.bencloud.api;

import static gov.epa.bencloud.server.database.jooq.data.Tables.*;

import java.util.Optional;

import org.jooq.JSONFormat;
import org.jooq.Result;
import org.jooq.JSONFormat.RecordFormat;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Record3;
import org.jooq.impl.DSL;
import org.pac4j.core.profile.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.epa.bencloud.server.database.JooqUtil;
import spark.Request;
import spark.Response;

public class GridDefinitionApi {
	private static final Logger log = LoggerFactory.getLogger(GridDefinitionApi.class);
	
	public static Object getAllGridDefinitions(Request request, Response response, Optional<UserProfile> userProfile) {
		UserProfile u = userProfile.get();

		Result<Record> gridRecords = DSL.using(JooqUtil.getJooqConfiguration())
				.select(GRID_DEFINITION.asterisk())
				.from(GRID_DEFINITION)
				.orderBy(GRID_DEFINITION.NAME)
				.fetch();
		log.debug("Requested all grid definitions: " + (userProfile.isPresent() ? userProfile.get().getId() : "Anonymous"));
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
	
	public static Record3<String, Integer, Integer> getGridDefinitionInfo(int gridId) {

		Record3<String, Integer, Integer> gridRecord = DSL.using(JooqUtil.getJooqConfiguration())
				.select(GRID_DEFINITION.NAME, GRID_DEFINITION.COL_COUNT, GRID_DEFINITION.ROW_COUNT)
				.from(GRID_DEFINITION)
				.where(GRID_DEFINITION.ID.eq(gridId))
				.fetchOne();
		
		return gridRecord;
	}
}

package gov.epa.bencloud.api;

import static gov.epa.bencloud.server.database.jooq.data.Tables.*;

import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.jooq.JSONFormat;
import org.jooq.Result;
import org.jooq.SelectConditionStep;
import org.jooq.SelectJoinStep;
import org.jooq.JSONFormat.RecordFormat;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Record3;
import org.jooq.Record8;
import org.jooq.impl.DSL;
import org.locationtech.jts.geom.Geometry;
import org.pac4j.core.profile.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.epa.bencloud.server.database.JooqUtil;
import gov.epa.bencloud.server.util.ParameterUtil;
import spark.Request;
import spark.Response;

/*
 * Methods for retreiving data related to grid definitions.
 */
public class GridDefinitionApi {
	private static final Logger log = LoggerFactory.getLogger(GridDefinitionApi.class);
	
	/**
	 * 
	 * @param request
	 * @param response
	 * @param userProfile
	 * @return a JSON representation of all grid definitions, ordered by grid definition name.
	 */
	public static Object getAllGridDefinitions(Request request, Response response, Optional<UserProfile> userProfile) {
		UserProfile u = userProfile.get();

		Result<Record> gridRecords = DSL.using(JooqUtil.getJooqConfiguration())
				.select(GRID_DEFINITION.asterisk())
				.from(GRID_DEFINITION)
				.orderBy(GRID_DEFINITION.NAME)
				.fetch();
		//log.debug("Requested all grid definitions: " + (userProfile.isPresent() ? userProfile.get().getId() : "Anonymous"));
		response.type("application/json");
		return gridRecords.formatJSON(new JSONFormat().header(false).recordFormat(RecordFormat.OBJECT));
	}

	/**
	 * 
	 * @param request
	 * @param response
	 * @param userProfile
	 * @return a JSON representation of all grid definitions, ordered by grid definition name. includes row and col counts
	 */
	public static Object getAllGridDefinitionsInfo(Request request, Response response, Optional<UserProfile> userProfile) {
		UserProfile u = userProfile.get();

		Result<Record> gridRecords = DSL.using(JooqUtil.getJooqConfiguration())
				.select(GRID_DEFINITION.asterisk(), GRID_DEFINITION.COL_COUNT, GRID_DEFINITION.ROW_COUNT)
				.from(GRID_DEFINITION)
				.orderBy(GRID_DEFINITION.NAME)
				.fetch();
		//log.debug("Requested all grid definitions: " + (userProfile.isPresent() ? userProfile.get().getId() : "Anonymous"));
		response.type("application/json");
		return gridRecords.formatJSON(new JSONFormat().header(false).recordFormat(RecordFormat.OBJECT));
	}

	
	
	/**
	 * 
	 * @param gridId
	 * @return the grid definition name for the given grid definition id.
	 */
	public static String getGridDefinitionName(int gridId) {

		Record1<String> gridRecord = DSL.using(JooqUtil.getJooqConfiguration())
				.select(GRID_DEFINITION.NAME)
				.from(GRID_DEFINITION)
				.where(GRID_DEFINITION.ID.eq(gridId))
				.fetchOne();
		
		return gridRecord.value1();
	}
	
	/**
	 * 
	 * @param gridId
	 * @return the grid definition name, column count, and row count for the given grid definition id.
	 */
	public static Record3<String, Integer, Integer> getGridDefinitionInfo(int gridId) {

		Record3<String, Integer, Integer> gridRecord = DSL.using(JooqUtil.getJooqConfiguration())
				.select(GRID_DEFINITION.NAME, GRID_DEFINITION.COL_COUNT, GRID_DEFINITION.ROW_COUNT)
				.from(GRID_DEFINITION)
				.where(GRID_DEFINITION.ID.eq(gridId))
				.fetchOne();
		
		return gridRecord;
	}
	
	/**
	 * gets the row, column, geometries from the grids schema- might use later when maps are added to display grid definitions
	 * @param request
	 * @param response
	 * @param userProfile
	 * @return
	 */
	public static Object getGridGeometries(Request request, Response response, Optional<UserProfile> userProfile) {
		int id;
		int page;
		int rowsPerPage;
		String sortBy;
		boolean descending;
		String filter;
		try {
			id = Integer.valueOf(request.params("id"));
			page = ParameterUtil.getParameterValueAsInteger(request.raw().getParameter("page"), 1);
			rowsPerPage = ParameterUtil.getParameterValueAsInteger(request.raw().getParameter("rowsPerPage"), 10);
			sortBy = ParameterUtil.getParameterValueAsString(request.raw().getParameter("sortBy"), "");
			descending = ParameterUtil.getParameterValueAsBoolean(request.raw().getParameter("descending"), false);
			filter = ParameterUtil.getParameterValueAsString(request.raw().getParameter("filter"), "");
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return CoreApi.getErrorResponseInvalidId(request, response);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return CoreApi.getErrorResponseInvalidId(request, response);
		}
		Record1<String> g1 = DSL.using(JooqUtil.getJooqConfiguration()).select(GRID_DEFINITION.TABLE_NAME).from(GRID_DEFINITION).where(GRID_DEFINITION.ID.eq(id)).fetchOne();
		String gridName = g1.value1();
		Result<Record3<Integer, Integer, Object>> records = DSL.using(JooqUtil.getJooqConfiguration())
            .select(
                    DSL.field(gridName + ".col", Integer.class),
                    DSL.field(gridName + ".row", Integer.class), 
                    DSL.field(gridName + ".geom", Object.class)
            )
			.from(gridName)
			.fetch();
		
		response.type("application/json");
		System.out.println(records.formatJSON(new JSONFormat().header(false).recordFormat(RecordFormat.OBJECT)));
		return records.formatJSON(new JSONFormat().header(false).recordFormat(RecordFormat.OBJECT));
		
	}

	
}

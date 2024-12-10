package gov.epa.bencloud.api;

import static gov.epa.bencloud.server.database.jooq.data.Tables.*;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import javax.servlet.MultipartConfigElement;

import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import gov.epa.bencloud.Constants;
import gov.epa.bencloud.api.model.ValidationMessage;
import gov.epa.bencloud.api.util.ApiUtil;
import gov.epa.bencloud.api.util.FilestoreUtil;
import gov.epa.bencloud.server.database.JooqUtil;
import gov.epa.bencloud.server.database.jooq.data.tables.records.AirQualityLayerRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.TaskBatchRecord;
import gov.epa.bencloud.server.tasks.TaskQueue;
import gov.epa.bencloud.server.tasks.model.Task;
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

	/*
	 * 
	 */
	public static Object postGridDefinitionShapefile(Request request, Response response, Optional<UserProfile> userProfile) {
		request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));

		String gridName;
		String filename;
		LocalDateTime uploadDate;
		ValidationMessage validationMsg = new ValidationMessage();
		Integer filestoreId;	
		
		try{
			filename = ApiUtil.getMultipartFormParameterAsString(request, "filename");
			gridName = ApiUtil.getMultipartFormParameterAsString(request, "name");	
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return CoreApi.getErrorResponseInvalidId(request, response);
		}
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode paramsNode = mapper.createObjectNode();

		paramsNode.put("name", gridName);
		paramsNode.put("userId", userProfile.get().getId());

		
		
		// Store file in Filestore
		try (InputStream is = request.raw().getPart("file").getInputStream()) {
			filestoreId = FilestoreUtil.putFile(is, filename, Constants.FILE_TYPE_GRID, userProfile.get().getId(), paramsNode.toString());
			
			// TODO: Make sure it's a zip file and perform other validation here
		} catch (Exception e) {
			log.error("Error saving shape file", e);
			response.type("application/json");
			validationMsg.success=false;
			validationMsg.messages.add(new ValidationMessage.Message("error","Error occurred saving your shape file."));
			return CoreApi.transformValMsgToJSON(validationMsg);
		}
		
		// Add filestoreId so task processor can find the uploaded file
		paramsNode.put("filestoreId", filestoreId);
		
		// Add records to task_batch and task_queue to import the new grid
		TaskBatchRecord rec = DSL.using(JooqUtil.getJooqConfiguration())
				.insertInto(TASK_BATCH, TASK_BATCH.NAME, TASK_BATCH.PARAMETERS, TASK_BATCH.USER_ID, TASK_BATCH.SHARING_SCOPE)
				.values("Grid import: " + filename, paramsNode.toString(), userProfile.get().getId(), Constants.SHARING_NONE)
				.returning(TASK_BATCH.ID).fetchOne();
		Integer batchTaskId = rec.getId();

		Task gridImportTask = new Task();
		gridImportTask.setUserIdentifier(userProfile.get().getId());
		gridImportTask.setType(Constants.TASK_TYPE_GRID_IMPORT);
		gridImportTask.setBatchId(batchTaskId);
		gridImportTask.setName("Grid import: " + filename);
		gridImportTask.setParameters(paramsNode.toString());
		String gridImportTaskUUID = UUID.randomUUID().toString();
		gridImportTask.setUuid(gridImportTaskUUID);
		TaskQueue.writeTaskToQueue(gridImportTask);
		
		// Return success
		return CoreApi.getSuccessResponse(request, response, 200, "Shapefile saved for processing: " + filestoreId);
	}
	
	/**
	 * Deletes a grid definition from the database (grid id is a request parameter).
	 * @param request
	 * @param response
	 * @param userProfile
	 * @return
	 */
	public static Object deleteGridDefinition(Request request, Response response, Optional<UserProfile> userProfile) {
		
		Integer id;
		try {
			id = Integer.valueOf(request.params("id"));
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return CoreApi.getErrorResponseInvalidId(request, response);
		} 
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());
		
		//TODO: Delete table from grids schema, all crosswalks related to this grid, and then delete record from grid_definition table
		// Users can delete their own grids. Admins can delete any non-shared grid
		// We should add validation to prevent deletion if the grid is tied to any result sets or other datasets
		// TODO: Add list of places to check for previous usage of grid before deleting
		
		response.status(204);
		return response;

	} 
}

package gov.epa.bencloud.api;

import static gov.epa.bencloud.server.database.jooq.data.Tables.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.servlet.MultipartConfigElement;

import org.apache.commons.io.FileUtils;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.util.URLs;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.JSONFormat;
import org.jooq.Result;
import org.jooq.SelectConditionStep;
import org.jooq.SelectJoinStep;
import org.jooq.exception.DataAccessException;
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
import gov.epa.bencloud.server.database.jooq.data.tables.records.GridDefinitionRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.TaskBatchRecord;
import gov.epa.bencloud.server.tasks.TaskQueue;
import gov.epa.bencloud.server.tasks.model.Task;
import gov.epa.bencloud.server.util.ApplicationUtil;
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
		
		if (gridRecord==null){
			return "<grid removed>";
		}
		else{
			return gridRecord.value1();
		}
				
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

		// Make sure the name is unique among this user's grid definitions and shared ones
		List<String> gridDefinitionNames = DSL.using(JooqUtil.getJooqConfiguration())
			.select(GRID_DEFINITION.NAME)
			.from(GRID_DEFINITION)
			.where(GRID_DEFINITION.USER_ID.eq(userProfile.get().getId()))
			.or(GRID_DEFINITION.SHARE_SCOPE.eq((short)1))
			.orderBy(GRID_DEFINITION.USER_ID)
			.fetch(GRID_DEFINITION.NAME);
		if (gridDefinitionNames.contains(gridName)) {
			log.error("A grid definition named " + gridName + " already exists.");
			response.type("application/json");
			validationMsg.success=false;
			validationMsg.messages.add(new ValidationMessage.Message("error", "A grid definition named " + gridName + " already exists. Please enter a different name."));
			return CoreApi.transformValMsgToJSON(validationMsg);
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
		
		// Unzip File
		Path filestoreFilePath = FilestoreUtil.getFilePath(filestoreId);
		String tempFolder = System.getProperty("java.io.tmpdir") + File.separator + filestoreId;
		List<Path> shpFiles;
		try {
			ApiUtil.unzip(filestoreFilePath.toString(), tempFolder);

			shpFiles = ApiUtil.findFilesByExtension(tempFolder, ".shp");
		} catch (IOException e) {
			try {
				FileUtils.deleteDirectory(new File(tempFolder));
			} catch (IOException e1) {
				log.error("Error deleteing temp folder", e1);
			}
			FilestoreUtil.deleteFile(filestoreId);

			log.error("Error unzipping file", e);
			response.type("application/json");
			validationMsg.success=false;
			validationMsg.messages.add(new ValidationMessage.Message("error","Error occurred unzipping your shape file."));
			return CoreApi.transformValMsgToJSON(validationMsg);
		}

		// Ensure exactly 1 shape file in folder
		int shpFileSize = shpFiles.size(); 
		if(shpFileSize != 1) {
			try {
				FileUtils.deleteDirectory(new File(tempFolder));
			} catch (IOException e) {
				log.error("Error deleting temp folder", e);
			}
			FilestoreUtil.deleteFile(filestoreId);

			log.error(shpFileSize + " shape files in zip");
			response.type("application/json");
			validationMsg.success=false;
			validationMsg.messages.add(new ValidationMessage.Message("error", shpFileSize + " shape files in zip."));
			return CoreApi.transformValMsgToJSON(validationMsg);
		}

		// Ensure DBF and SHX files in folder
		List<Path> dbfFiles; 
		List<Path> shxFiles;
		try {
			dbfFiles = ApiUtil.findFilesByExtension(tempFolder, ".dbf");
			shxFiles = ApiUtil.findFilesByExtension(tempFolder, ".shx");
		} catch (IOException e) {
			try {
				FileUtils.deleteDirectory(new File(tempFolder));
			} catch (IOException e1) {
				log.error("Error deleteing temp folder", e1);
			}
			FilestoreUtil.deleteFile(filestoreId);

			log.error("Error finding DBF or SHX files");
			response.type("application/json");
			validationMsg.success=false;
			validationMsg.messages.add(new ValidationMessage.Message("error", "Error finding DBF or SHX files"));
			return CoreApi.transformValMsgToJSON(validationMsg);
		}

		Boolean hasDbf = dbfFiles.size() > 0;
		Boolean hasShx = shxFiles.size() > 0;
		if (!hasDbf || !hasShx) {
			try {
				FileUtils.deleteDirectory(new File(tempFolder));
			} catch (IOException e) {
				log.error("Error deleteing temp folder", e);
			}
			FilestoreUtil.deleteFile(filestoreId);

			String errorMessage = "Missing ";
			if (hasDbf && !hasShx) {
				errorMessage = errorMessage + "\"SHX\" file";
			}
			else if (!hasDbf && hasShx) {
				errorMessage = errorMessage + "\"DBF\" file";
			}
			else if (!hasDbf && !hasShx) {
				errorMessage = errorMessage + "\"SHX\" and \"DBF\" file";
			}
			log.error(errorMessage);
			response.type("application/json");
			validationMsg.success=false;
			validationMsg.messages.add(new ValidationMessage.Message("error",errorMessage));
			return CoreApi.transformValMsgToJSON(validationMsg);
		}
		
		// Validate Rows and Columns
		File inFile = shpFiles.get(0).toFile();
		try {
			DataStore inputDataStore = DataStoreFinder.getDataStore(Collections.singletonMap("url", URLs.fileToUrl(inFile)));
			String inputTypeName = inputDataStore.getTypeNames()[0];
			SimpleFeatureType inputType = inputDataStore.getSchema(inputTypeName);

			List<AttributeDescriptor> attributeDescriptors = inputType.getAttributeDescriptors();

			boolean hasColumn = false;
			boolean hasRow = false;
			for (AttributeDescriptor desc : attributeDescriptors) {
				String name = desc.getLocalName().toLowerCase();
				if (name.equals("col")) {
					hasColumn = true;
				}
				if (name.equals("row")) {
					hasRow = true;
				}
			}

			if (!hasColumn || !hasRow) {
				try {
					FileUtils.deleteDirectory(new File(tempFolder));
				} catch (IOException e1) {
					log.error("Error deleteing temp folder", e1);
				}
				FilestoreUtil.deleteFile(filestoreId);

				String errorMessage = "Shape file missing ";
				if (hasColumn && !hasRow) {
					errorMessage = errorMessage + "\"row\" attribute";
				}
				else if (!hasColumn && hasRow) {
					errorMessage = errorMessage + "\"col\" attribute";
				}
				else if (!hasColumn && !hasRow) {
					errorMessage = errorMessage + "\"row\" and \"col\" attributes";
				}
				log.error(errorMessage);
				response.type("application/json");
				validationMsg.success=false;
				validationMsg.messages.add(new ValidationMessage.Message("error",errorMessage));
				return CoreApi.transformValMsgToJSON(validationMsg);
			}

		} catch (Exception e) {
			try {
				FileUtils.deleteDirectory(new File(tempFolder));
			} catch (IOException e1) {
				log.error("Error deleteing temp folder", e1);
			}
			FilestoreUtil.deleteFile(filestoreId);

			log.error("Error reading shapefile", e);
			response.type("application/json");
			validationMsg.success=false;
			validationMsg.messages.add(new ValidationMessage.Message("error","Error reading shape file."));
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
		DSLContext create = DSL.using(JooqUtil.getJooqConfigurationUnquoted());
		
		GridDefinitionRecord gridDefinitionResult = create.selectFrom(GRID_DEFINITION).where(GRID_DEFINITION.ID.eq(id)).fetchAny();
		if(gridDefinitionResult == null) {
			return CoreApi.getErrorResponseNotFound(request, response);
		}

		//Nobody can delete shared layers
		//All users can delete their own layers
		//Admins can delete any non-shared layers
		if(gridDefinitionResult.getShareScope() == Constants.SHARING_ALL || !(gridDefinitionResult.getUserId().equalsIgnoreCase(userProfile.get().getId()) || CoreApi.isAdmin(userProfile)) )  {
			return CoreApi.getErrorResponseForbidden(request, response);
		}

		// Block if Air Quality Layer has Grid Definition
		if(create.fetchExists(create.selectFrom(AIR_QUALITY_LAYER).where(AIR_QUALITY_LAYER.GRID_DEFINITION_ID.eq(id)))){
			return CoreApi.getErrorResponse(request, response, 403, "Cannot delete because grid definition is used in air quality layer.");
		}

		// Block if Exposure Result Dataset has Grid Definition
		if(create.fetchExists(create.selectFrom(EXPOSURE_RESULT_DATASET).where(EXPOSURE_RESULT_DATASET.GRID_DEFINITION_ID.eq(id)))){
			return CoreApi.getErrorResponse(request, response, 403, "Cannot delete because grid definition is used in exposure result dataset.");
		}

		// Block if Incidence Dataset has Grid Definition
		if(create.fetchExists(create.selectFrom(INCIDENCE_DATASET).where(INCIDENCE_DATASET.GRID_DEFINITION_ID.eq(id)))){
			return CoreApi.getErrorResponse(request, response, 403, "Cannot delete because grid definition is used in incidence dataset.");
		}

		// Block if Population Dataset has Grid Definition
		if(create.fetchExists(create.selectFrom(POPULATION_DATASET).where(POPULATION_DATASET.GRID_DEFINITION_ID.eq(id)))){
			return CoreApi.getErrorResponse(request, response, 403, "Cannot delete because grid definition is used in population dataset.");
		}

		// Block if Variable Entry has Grid Definition
		if(create.fetchExists(create.selectFrom(VARIABLE_ENTRY).where(VARIABLE_ENTRY.GRID_DEFINITION_ID.eq(id)))){
			return CoreApi.getErrorResponse(request, response, 403, "Cannot delete because grid definition is used in variable entry.");
		}

		// Block if HIF Result Dataset has Grid Definition
		if(create.fetchExists(create.selectFrom(HIF_RESULT_DATASET).where(HIF_RESULT_DATASET.GRID_DEFINITION_ID.eq(id)))){
			return CoreApi.getErrorResponse(request, response, 403, "Cannot delete because grid definition is used in HIF result dataset.");
		}

		// Block if Valuation Result Dataset has Grid Definition
		if(create.fetchExists(create.selectFrom(VALUATION_RESULT_DATASET).where(VALUATION_RESULT_DATASET.GRID_DEFINITION_ID.eq(id)))){
			return CoreApi.getErrorResponse(request, response, 403, "Cannot delete because grid definition is used in valuation result dataset.");
		}

		// Delete Crosswalks
		Result<Record1<Integer>> crosswalkIdResults = create
				.select(CROSSWALK_DATASET.ID)
				.from(CROSSWALK_DATASET)
				.where(CROSSWALK_DATASET.SOURCE_GRID_ID.eq(id)
						.or(CROSSWALK_DATASET.TARGET_GRID_ID.eq(id)))
				.fetch();

		if (crosswalkIdResults != null && crosswalkIdResults.size() > 0) {

			ArrayList<Integer> crosswalkIds = new ArrayList<Integer>();
			for (Record1<Integer> crosswalkIdRecord : crosswalkIdResults) {
				crosswalkIds.add(crosswalkIdRecord.value1());
			}

			create.deleteFrom(CROSSWALK_ENTRY)
					.where(CROSSWALK_ENTRY.CROSSWALK_ID.in(crosswalkIds))
					.execute();

			create.deleteFrom(CROSSWALK_DATASET)
					.where(CROSSWALK_DATASET.ID.in(crosswalkIds))
					.execute();
		}

		// Drop the table
		try {
			create.dropTable(gridDefinitionResult.getTableName()).execute();
		} catch (DataAccessException e) {
			return CoreApi.getErrorResponse(request, response, 400, "Error dropping table");
		}

		// Finally, delete the grid definition from the grid definition table
		int numDeletedGridDefinitions = create.deleteFrom(GRID_DEFINITION)
			.where(GRID_DEFINITION.ID.eq(id))
			.execute();

		// If no grid definition were deleted, return an error
		if(numDeletedGridDefinitions == 0) {
			return CoreApi.getErrorResponse(request, response, 400, "Error deleting grid definition");
		}

			// Load GeoServer URL and credentials from properties
			Map<String, String> geoserverInfo = ApplicationUtil.getGeoserverInfo();
			log.info(geoserverInfo.toString());

		// Request grid layer deletion at GeoServer
		try {
			String gridTableName = gridDefinitionResult.getTableName().substring(6);
			String auth = "Basic " + Base64.getEncoder().encodeToString((geoserverInfo.get("GEOSERVER_ADMIN_USER") + ":" + geoserverInfo.get("GEOSERVER_ADMIN_PASSWORD")).getBytes());

			URL url = new URL(geoserverInfo.get("GEOSERVER_URL") + "/layers/" + gridTableName + "?recurse=true");
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("DELETE");
			connection.setRequestProperty("Authorization", auth);
			connection.setDoOutput(true);

			int responseCode = connection.getResponseCode();
			if (responseCode != HttpURLConnection.HTTP_OK) {
				try (InputStream errorStream = connection.getErrorStream()) {
					String message = connection.getResponseMessage();
					if (errorStream != null) {
						message = new String(errorStream.readAllBytes(), StandardCharsets.UTF_8);
					}
					log.error("Failed to delete grid from GeoServer. Response code: " + responseCode + ". Message: " + message);
				}
			}
		} catch (Exception e) {
			log.error("Error deleting grid from GeoServer", e);
		}
		
		// We should add validation to prevent deletion if the grid is tied to any result sets or other datasets
		// TODO: Add list of places to check for previous usage of grid before deleting
		
		response.status(204);
		return response;

	} 

	/**
	 * Renames a grid definition from the database (grid id is a request parameter).
	 * @param request
	 * @param response
	 * @param userProfile
	 * @return
	 */
	public static Object renameGridDefinition(Request request, Response response, Optional<UserProfile> userProfile) {
		request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));

		Integer id;
		String newName;

		try {
			id = Integer.valueOf(request.params("id"));
			newName = ApiUtil.getMultipartFormParameterAsString(request, "newName");
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return CoreApi.getErrorResponseInvalidId(request, response);
		} 
		
		// Users can only edit grid definitions created by themselves 		
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());	
		Result<Record1<Integer>> res = create.select(GRID_DEFINITION.ID)
				.from(GRID_DEFINITION)
				.where(GRID_DEFINITION.USER_ID.eq(userProfile.get().getId()))
				.and(GRID_DEFINITION.ID.eq(id))
				.fetch();
		if(res==null) {
			return CoreApi.getErrorResponse(request, response, 400, "You can only rename grid definitions created by yourself.");
		}

		// Make sure the new name is unique among this user's grid definitions and shared ones
		List<String> gridDefinitionNames = DSL.using(JooqUtil.getJooqConfiguration())
			.select(GRID_DEFINITION.NAME)
			.from(GRID_DEFINITION)
			.where(GRID_DEFINITION.USER_ID.eq(userProfile.get().getId()))
			.or(GRID_DEFINITION.SHARE_SCOPE.eq((short)1))
			.orderBy(GRID_DEFINITION.USER_ID)
			.fetch(GRID_DEFINITION.NAME);
		if (gridDefinitionNames.contains(newName)) {
			String errorMsg = "A grid definition named " + newName + " already exists. Please enter a different name.";
			return CoreApi.getErrorResponse(request, response, 409, errorMsg);
		}	
		
		// Rename grid definition
		int countRows = create.update(GRID_DEFINITION)
			.set(GRID_DEFINITION.NAME, newName)
			.where(GRID_DEFINITION.ID.eq(id))
			.execute();
		if(countRows == 0) {
			return CoreApi.getErrorResponse(request, response, 400, "Unknown error");
		}

		// Return success
		return CoreApi.getSuccessResponse(request, response, 200, "Successfully renamed.");
	}
}


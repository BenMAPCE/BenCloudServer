package gov.epa.bencloud.server.tasks.local;




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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.feature.Property;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.AttributeType;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.feature.type.GeometryType;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.DataUtilities;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.geotools.util.URLs;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.epa.bencloud.Constants;
import gov.epa.bencloud.api.model.GridImportTaskConfig;
import gov.epa.bencloud.api.model.GridImportTaskLog;
import gov.epa.bencloud.api.model.ValidationMessage;
import gov.epa.bencloud.api.util.ApiUtil;
import gov.epa.bencloud.api.util.FilestoreUtil;
import gov.epa.bencloud.api.util.GridImportUtil;
import gov.epa.bencloud.server.database.JooqUtil;
import gov.epa.bencloud.server.database.PooledDataSource;
import gov.epa.bencloud.server.database.jooq.data.tables.records.GridDefinitionRecord;
import gov.epa.bencloud.server.tasks.TaskComplete;
import gov.epa.bencloud.server.tasks.TaskQueue;
import gov.epa.bencloud.server.tasks.model.Task;
import gov.epa.bencloud.server.tasks.model.TaskMessage;
import gov.epa.bencloud.server.util.ApplicationUtil;
import org.geotools.geometry.jts.ReferencedEnvelope;

/*
 * Import a user uploaded shapefile as a grid definition.
 */
public class GridImportTaskRunnable implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(GridImportTaskRunnable.class);
    protected static ObjectMapper objectMapper = new ObjectMapper();
    
	private String taskUuid;
	private String taskWorkerUuid;
	private ValidationMessage validationMsg = new ValidationMessage();


	/**
	 * Creates a GridImportTaskRunnable object with the given taskUuid and taskWorkerUuid
	 * @param taskUuid
	 * @param taskWorkerUuid
	 */
	public GridImportTaskRunnable(String taskUuid, String taskWorkerUuid) {
		this.taskUuid = taskUuid;
		this.taskWorkerUuid = taskWorkerUuid;
	}

	private boolean taskSuccessful = true;
	
	public void run() {
		
		log.info("Grid Import Task Begin: " + taskUuid);
		ObjectMapper mapper = new ObjectMapper();
		Task task = TaskQueue.getTaskFromQueueRecord(taskUuid);

		ArrayList<TaskMessage> messages = new ArrayList<TaskMessage>();
		
		try {
			GridImportTaskConfig gridImportTaskConfig = null;
			gridImportTaskConfig = objectMapper.readValue(task.getParameters(), GridImportTaskConfig.class);
			
			GridImportTaskLog gridImportTaskLog = new GridImportTaskLog(gridImportTaskConfig, task.getUserIdentifier());
			gridImportTaskLog.setDtStart(LocalDateTime.now());
			
			gridImportTaskLog.addMessage("Starting grid import");
			messages.add(new TaskMessage("active", "Reading shapefile"));
			
			TaskQueue.updateTaskPercentage(taskUuid, 1, mapper.writeValueAsString(messages));
			
			// Use gridImportTaskConfig to get file id and FilestoreUtil.getFilePath(id) to access file.

			Path filestoreFilePath = FilestoreUtil.getFilePath(gridImportTaskConfig.filestoreId);

			String tempFolder = System.getProperty("java.io.tmpdir") + File.separator + gridImportTaskConfig.filestoreId;
			
			ApiUtil.unzip(filestoreFilePath.toString(), tempFolder);
			
			List<Path> shpFiles = ApiUtil.findFilesByExtension(tempFolder, ".shp");
			if(shpFiles.size() != 1) {
				messages.add(new TaskMessage("complete", "Uploaded zip must have exactly 1 file with .shp extension. " + shpFiles.size() + " were found."));
				taskSuccessful = false;
			}
			Path shapefilePath = shpFiles.get(0);
			
			// Import shapefile into grids schema using the unique name.
			// This function will guarantee that the geometry column will be named "geom"
			//  and "col" and "row" will be lowercase
			String gridTableName=null;
			gridTableName = importShapefile(shapefilePath);
			
			if(gridTableName == null) {
				throw new Exception("Grid file processing failed.");
			}
			
			DSLContext create = DSL.using(JooqUtil.getJooqConfiguration("BenMAP JDBC"));
			
			Record gridStats = create.fetchOne("select count(distinct col) as col_count, count(distinct row) as row_count from grids." + gridTableName);
			
			 // Calculate native bounds and determine CRS
            ReferencedEnvelope bounds = calculateNativeBounds(shapefilePath);

			// Retrieve CRS from the database
			String srs = "EPSG:4269"; // Default to EPSG:4269
			Record crsRecord = create.fetchOne("SELECT Find_SRID('grids', ?, 'geom') AS srid", gridTableName);
			if (crsRecord != null && crsRecord.get("srid") != null) {
				srs = "EPSG:" + crsRecord.get("srid");
			}
            
			// TODO: Make sure and update the messages object with meaningful progress along the way and also increment the task percentage to show progress

			// Create record in grid_definition table with grid name, table name, and user info
			GridDefinitionRecord gridRecord = create
					.insertInto(GRID_DEFINITION,
					GRID_DEFINITION.NAME,
					GRID_DEFINITION.TABLE_NAME,
					GRID_DEFINITION.COL_COUNT,
					GRID_DEFINITION.ROW_COUNT,
					GRID_DEFINITION.USER_ID,
					GRID_DEFINITION.SHARE_SCOPE)
					.values(
					gridImportTaskConfig.name, 
					"grids."+gridTableName, 
					gridStats.get(0, Integer.class),
					gridStats.get(1, Integer.class),
					gridImportTaskConfig.userId, 
					Constants.SHARING_NONE)
			.returning(GRID_DEFINITION.ID)
			.fetchOne();
			
			gridImportTaskLog.getGridImportTaskConfig().gridDefinitionId = gridRecord.getId();

			// Load GeoServer URL and credentials from properties
			Map<String, String> geoserverInfo = ApplicationUtil.getGeoserverInfo();
			//log.info(geoserverInfo.toString());

			//Publish the new grid to GeoServer
			try {
				String auth = "Basic " + Base64.getEncoder().encodeToString((geoserverInfo.get("GEOSERVER_ADMIN_USER") + ":" + geoserverInfo.get("GEOSERVER_ADMIN_PASSWORD")).getBytes());
				String jsonPayload = "{ \"featureType\": {"
				+ " \"name\": \"" + gridTableName + "\","
				+ " \"nativeName\": \"" + gridTableName + "\","
				+ " \"title\": \"" + gridTableName + "\","
				+ " \"srs\": \"" + srs + "\","
				+ " \"nativeBoundingBox\": {"
				+ "   \"minx\": " + bounds.getMinX() + ","
				+ "   \"maxx\": " + bounds.getMaxX() + ","
				+ "   \"miny\": " + bounds.getMinY() + ","
				+ "   \"maxy\": " + bounds.getMaxY() + ","
				+ "   \"crs\": \"" + srs + "\""
				+ " },"
				+ " \"store\": { \"name\": \"" + geoserverInfo.get("GEOSERVER_STORE") + "\" },"
				+ " \"attributes\": {"
				+ "   \"attribute\": ["
				+ "     { \"name\": \"col\", \"binding\": \"java.lang.Integer\" },"
				+ "     { \"name\": \"row\", \"binding\": \"java.lang.Integer\" },"
				+ "     { \"name\": \"geom\", \"binding\": \"org.locationtech.jts.geom.Geometry\" }"
				+ "   ]"
				+ " }"
				+ "} }";
				URL url = new URL(geoserverInfo.get("GEOSERVER_URL") + "/workspaces/" + geoserverInfo.get("GEOSERVER_WORKSPACE") + "/datastores/" + geoserverInfo.get("GEOSERVER_STORE") + "/featuretypes");

				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setRequestMethod("POST");
				connection.setRequestProperty("Authorization", auth);
				connection.setRequestProperty("Content-Type", "application/json");
				connection.setDoOutput(true);

				try (OutputStream os = connection.getOutputStream()) {
					byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
					os.write(input, 0, input.length);
				}

				int responseCode = connection.getResponseCode();
				if (responseCode != HttpURLConnection.HTTP_CREATED) {
					try (InputStream errorStream = connection.getErrorStream()) {
						String message = connection.getResponseMessage();
						if (errorStream != null) {
							message = new String(errorStream.readAllBytes(), StandardCharsets.UTF_8);
						}
						log.error("Failed to publish layer to GeoServer. Response code: " + responseCode + ". Message: " + message);
					}
					validationMsg.success = false;
					validationMsg.messages.add(new ValidationMessage.Message("error", "Failed to publish grid to GeoServer."));
				}
			} catch (Exception e) {
				log.error("Error publishing grid to GeoServer", e);
				validationMsg.success = false;
				validationMsg.messages.add(new ValidationMessage.Message("error", "Error publishing grid to GeoServer."));
			}
			
			// Remove temp files
			FileUtils.deleteDirectory(new File(tempFolder));
			
			// Remove shapefile from file store using FileStoreUtil.deleteFile()
			FilestoreUtil.deleteFile(gridImportTaskConfig.filestoreId);
			
			// Note: Do not generate crosswalks at this point. 
			//  We will integrate that into our other processes to ensure the crosswalk exists before calling db functions that use it
			//  This will avoid creating unnecessary crosswalks

			messages.get(messages.size()-1).setStatus("complete");
			
			String completeMessage = String.format("Imported grid definition");
			gridImportTaskLog.addMessage(completeMessage);
			gridImportTaskLog.setSuccess(true);
			gridImportTaskLog.setDtEnd(LocalDateTime.now());
			
			GridImportUtil.storeTaskLog(gridImportTaskLog);
			
			TaskComplete.addTaskToCompleteAndRemoveTaskFromQueue(taskUuid, taskWorkerUuid, taskSuccessful, completeMessage);

		} catch (Exception e) {
			TaskComplete.addTaskToCompleteAndRemoveTaskFromQueue(taskUuid, taskWorkerUuid, false, "Task failed");
			log.error("Task failed", e);
		}
		log.info("Grid Import Task Complete: " + taskUuid);
	}

	private ReferencedEnvelope calculateNativeBounds(Path shapefilePath) throws IOException {
		File inFile = shapefilePath.toFile();
		DataStore inputDataStore = DataStoreFinder.getDataStore(Collections.singletonMap("url", URLs.fileToUrl(inFile)));
		String inputTypeName = inputDataStore.getTypeNames()[0];
		FeatureSource<SimpleFeatureType, SimpleFeature> source = inputDataStore.getFeatureSource(inputTypeName);
		ReferencedEnvelope bounds = source.getBounds();
		inputDataStore.dispose();
		return bounds;
	}

	public static String importShapefile(Path filePath) throws IOException {

		File inFile = filePath.toFile();

		Map<String, Object> dbParams = new HashMap<>();
		dbParams.put(PostgisNGDataStoreFactory.DBTYPE.key, "postgis");
		dbParams.put(PostgisNGDataStoreFactory.USER.key, PooledDataSource.dbUser);
		dbParams.put(PostgisNGDataStoreFactory.PASSWD.key, PooledDataSource.dbPassword);
		dbParams.put(PostgisNGDataStoreFactory.HOST.key, PooledDataSource.dbHost);
		dbParams.put(PostgisNGDataStoreFactory.PORT.key, PooledDataSource.dbPort);
		dbParams.put(PostgisNGDataStoreFactory.DATABASE.key, PooledDataSource.dbName);
		dbParams.put(PostgisNGDataStoreFactory.SCHEMA.key, "grids");

		// Read the shapefile
		DataStore inputDataStore = DataStoreFinder.getDataStore(Collections.singletonMap("url", URLs.fileToUrl(inFile)));
		String inputTypeName = inputDataStore.getTypeNames()[0];
		SimpleFeatureType inputType = inputDataStore.getSchema(inputTypeName);
		FeatureSource<SimpleFeatureType, SimpleFeature> source = inputDataStore.getFeatureSource(inputTypeName);
		FeatureCollection<SimpleFeatureType, SimpleFeature> inputFeatureCollection = source.getFeatures();

		// Write to database
		DataStore dbDataStore = DataStoreFinder.getDataStore(dbParams);

		// Prepend the "g_" on the unique table name to avoid the need to quote it in
		// SQL if it starts with a number
		// Also, tables that begin with "g_" will be excluded during jOOQ code
		// generation
		String gridTableName = "g_" + UUID.randomUUID().toString().replace("-", "");

		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
		String geomColumnName = null;
		String colColumnName = null;
		String rowColumnName = null;
		AttributeTypeBuilder attributeBuilder = new AttributeTypeBuilder();

		// NAD83 PRJ from PostGIS
		// TODO: Find more elegant way to handle this issue with missing prj file?
		String wkt = "GEOGCS[\"NAD83\",DATUM[\"North_American_Datum_1983\",SPHEROID[\"GRS 1980\",6378137,298.257222101,AUTHORITY[\"EPSG\",\"7019\"]],TOWGS84[0,0,0,0,0,0,0],AUTHORITY[\"EPSG\",\"6269\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"degree\",0.0174532925199433,AUTHORITY[\"EPSG\",\"9122\"]],AUTHORITY[\"EPSG\",\"4269\"]]";
		CoordinateReferenceSystem crsNAD83 = null;
		try {
			crsNAD83 = CRS.parseWKT(wkt);
		} catch (FactoryException e) {
			e.printStackTrace();
			return null;
		}

		// Inspect the shapefile and make note of the names of the geom, col, and row fields
		// Sometimes the geom column might be named something else (e.g. the_geom or
		// geometry) and col/row might be proper or upper case (e.g. Col or COL)
		for (AttributeDescriptor att : inputType.getAttributeDescriptors()) {
			String name = att.getLocalName();
			AttributeType type = att.getType();
			if (type instanceof GeometryType) {
				// Remember this as the geom column name
				geomColumnName = name;
				if (inputType.getCoordinateReferenceSystem() == null) {
					// The geometry column doesn't have a CRS so let's hope it's NAD83 and try it
					builder.setCRS(crsNAD83);
					GeometryDescriptor g = inputType.getGeometryDescriptor();
					attributeBuilder.init(g);
					attributeBuilder.setCRS(crsNAD83);
					// GeometryDescriptor att2 = (GeometryDescriptor)
					// attributeBuilder.buildDescriptor(g.getLocalName());
					// Use "geom" as the column name instead of the original name
					GeometryDescriptor att2 = (GeometryDescriptor) attributeBuilder.buildDescriptor("geom");
					builder.add(att2);
					builder.setDefaultGeometry(att2.getLocalName());
				} else {
					builder.setCRS(inputType.getCoordinateReferenceSystem());
					attributeBuilder.init(att);
					// Use "geom" as the column name instead of the original name
					GeometryDescriptor att2 = (GeometryDescriptor) attributeBuilder.buildDescriptor("geom");
					builder.add(att2);
					builder.setDefaultGeometry("geom");
				}
			} else {
				// See if this is the col or row column
				if (name.equalsIgnoreCase("col")) {
					colColumnName = name;
					// Make sure col column is added as an integer
					builder.add("col", Integer.class);
				} else if (name.equalsIgnoreCase("row")) {
					rowColumnName = name;
					// Make sure row column is added as an integer
					builder.add("row", Integer.class);
				} else {
					builder.add(att);
				}
			}
		}

		builder.setName(gridTableName);
		builder.setSuperType((SimpleFeatureType) inputType.getSuper());
		SimpleFeatureType dbSchema = builder.buildFeatureType();

		dbDataStore.createSchema(dbSchema);
		SimpleFeatureStore dbFeatureStore = (SimpleFeatureStore) dbDataStore.getFeatureSource(gridTableName);
		FeatureIterator<SimpleFeature> featureIter = inputFeatureCollection.features();

		try {
			List<SimpleFeature> transformedFeatures = new ArrayList<>();
			SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(dbSchema);

			while (featureIter.hasNext()) {
				SimpleFeature feature = featureIter.next();

				// Map attributes from source to target schema with new names
				for (AttributeDescriptor targetAtt : dbSchema.getAttributeDescriptors()) {
					String targetName = targetAtt.getLocalName();

					if (targetName.equals("geom")) {
						// Map from original geometry column name
						featureBuilder.set("geom", feature.getAttribute(geomColumnName));
					} else if (targetName.equals("col") && colColumnName != null) {
						// Map from original col column name
						Object value = feature.getAttribute(colColumnName);
						if (value != null) {
							try {
								int intValue = Integer.parseInt(value.toString());
								featureBuilder.set("col", intValue);
							} catch (NumberFormatException e) {
								featureBuilder.set("col", null);
							}
						} else {
							featureBuilder.set("col", null);
						}
					} else if (targetName.equals("row") && rowColumnName != null) {
						// Map from original row column name
						Object value = feature.getAttribute(rowColumnName);
						if (value != null) {
							try {
								int intValue = Integer.parseInt(value.toString());
								featureBuilder.set("row", intValue);
							} catch (NumberFormatException e) {
								featureBuilder.set("row", null);
							}
						} else {
							featureBuilder.set("row", null);
						}
					} else {
						// Copy other attributes as is
						featureBuilder.set(targetName, feature.getAttribute(targetName));
					}
				}

				transformedFeatures.add(featureBuilder.buildFeature(feature.getID()));
			}

			dbFeatureStore.addFeatures(DataUtilities.collection(transformedFeatures));
		} finally {
			featureIter.close();
		}

		inputDataStore.dispose();
		dbDataStore.dispose();
		
		return gridTableName;
	}
}

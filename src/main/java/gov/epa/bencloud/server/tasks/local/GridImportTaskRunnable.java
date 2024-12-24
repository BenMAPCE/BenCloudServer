package gov.epa.bencloud.server.tasks.local;

import static gov.epa.bencloud.server.database.jooq.data.Tables.*;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.AttributeType;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.feature.type.GeometryType;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.NoSuchAuthorityCodeException;
import org.geotools.api.referencing.crs.CRSAuthorityFactory;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.feature.type.AttributeDescriptorImpl;
import org.geotools.feature.type.AttributeTypeImpl;
import org.geotools.feature.type.GeometryDescriptorImpl;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.factory.OrderedAxisAuthorityFactory;
import org.geotools.util.URLs;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;

import gov.epa.bencloud.Constants;
import gov.epa.bencloud.api.model.GridImportTaskConfig;
import gov.epa.bencloud.api.model.GridImportTaskLog;
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

/*
 * Import a user uploaded shapefile as a grid definition.
 */
public class GridImportTaskRunnable implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(GridImportTaskRunnable.class);
    protected static ObjectMapper objectMapper = new ObjectMapper();
    
	private String taskUuid;
	private String taskWorkerUuid;


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

			// TODO: Extract and validate shapefile. Add any errors to the task log and abort if file cannot be imported
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
			try {
				gridTableName = importShapefile(shapefilePath);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error("Error importing shapefile", e);
			}
			
			DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());
			
			Record gridStats = create.fetchOne("select count(distinct col) as col_count, count(distinct row) as row_count from grids." + gridTableName);
			
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
			
			// Remove temp files
			FileUtils.deleteDirectory(new File(tempFolder));
			
			// Remove shapefile from file store using FileStoreUtil.deleteFile()
			FilestoreUtil.deleteFile(gridImportTaskConfig.filestoreId);
			
			// Note: Do not generate crosswalks at this point. 
			//  We will integrate that into our other processes to ensure the crosswalk exists before calling db functions that use it
			//  This will avoid creating unnecessary crosswalks
			
			//TODO: Add TEST here to create a crosswalk for newly imported grid

			messages.get(messages.size()-1).setStatus("complete");
			
			String completeMessage = String.format("Imported grid definition");
			gridImportTaskLog.addMessage(completeMessage);
			gridImportTaskLog.setSuccess(true);
			gridImportTaskLog.setDtEnd(LocalDateTime.now());
			
			GridImportUtil.storeTaskLog(gridImportTaskLog);
			
			TaskComplete.addTaskToCompleteAndRemoveTaskFromQueue(taskUuid, taskWorkerUuid, taskSuccessful, completeMessage);

		} catch (Exception e) {
			TaskComplete.addTaskToCompleteAndRemoveTaskFromQueue(taskUuid, taskWorkerUuid, false, "Task Failed");
			log.error("Task failed", e);
		}
		log.info("Grid Import Task Complete: " + taskUuid);
	}

	public static String importShapefile(Path filePath) throws IOException {
		
		File inFile = filePath.toFile();
		
	    Map<String, Object> dbParams = new HashMap<>();
	    //dbParams.put(PostgisNGDataStoreFactory.DBTYPE.key, PostgisNGDataStoreFactory.DBTYPE.sample);
	    dbParams.put(PostgisNGDataStoreFactory.DBTYPE.key, "postgis");
	    dbParams.put(PostgisNGDataStoreFactory.USER.key, PooledDataSource.dbUser);
	    dbParams.put(PostgisNGDataStoreFactory.PASSWD.key, PooledDataSource.dbPassword);
	    dbParams.put(PostgisNGDataStoreFactory.HOST.key, PooledDataSource.dbHost);
	    dbParams.put(PostgisNGDataStoreFactory.PORT.key, PooledDataSource.dbPort);
	    dbParams.put(PostgisNGDataStoreFactory.DATABASE.key, PooledDataSource.dbName);
	    dbParams.put(PostgisNGDataStoreFactory.SCHEMA.key, "grids");

	    log.debug("dbParams: " + dbParams.toString());
	    // Read the shapefile
	    DataStore inputDataStore = DataStoreFinder.getDataStore(Collections.singletonMap("url", URLs.fileToUrl(inFile)));

	    String inputTypeName = inputDataStore.getTypeNames()[0];
	    SimpleFeatureType inputType = inputDataStore.getSchema(inputTypeName);

	    FeatureSource<SimpleFeatureType, SimpleFeature> source = inputDataStore.getFeatureSource(inputTypeName);

	    FeatureCollection<SimpleFeatureType, SimpleFeature> inputFeatureCollection = source.getFeatures();

	    // Write to database
	    DataStore dbDataStore = DataStoreFinder.getDataStore(dbParams);    
	    
	    log.debug("dbDataStore: " + dbDataStore);
	    
	    Iterator it = DataStoreFinder.getAvailableDataStores();
	    while(it.hasNext()){
	      log.debug("Available data store: " + it.next());
	    }
	    
	    // Prepend the "g_" on the unique table name to avoid the need to quote it in SQL if it starts with a number
	    // Also, tables that begin with "g_" will be excluded during jOOQ code generation
	    String gridTableName = "g_" + UUID.randomUUID().toString().replace("-", "");
	    
	    SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
	    
	    //Inspect the shapefile and make note of the names of the geom, col, and row fields
	    // Sometimes the geom column might be named something else (e.g. the_geom or geometry) and col/row might be proper or upper case (e.g. Col or COL)
	    // We currently use SQL alter table commands below to fix the key columns after the table is created
	    String geomColumnName = null;
	    String colColumnName = null;
	    String rowColumnName = null;
	    AttributeTypeBuilder attributeBuilder = new AttributeTypeBuilder();
	    
	    for (AttributeDescriptor att : inputType.getAttributeDescriptors()) {
	    	String name = att.getLocalName();
	    	AttributeType type = att.getType();
	    	if (type instanceof GeometryType) {
	    		// Remember this as the geom column name
	    		geomColumnName = name;

	    	    if (inputType.getCoordinateReferenceSystem() == null) {
	    	        // The geometry column doesn't have a CRS so let's hope it's NAD83 and try it
	    	        CoordinateReferenceSystem crs = null;

						// NAD83 PRJ from PostGIS
						// TODO: Find more elegant way to handle this
						String wkt = "GEOGCS[\"NAD83\",DATUM[\"North_American_Datum_1983\",SPHEROID[\"GRS 1980\",6378137,298.257222101,AUTHORITY[\"EPSG\",\"7019\"]],TOWGS84[0,0,0,0,0,0,0],AUTHORITY[\"EPSG\",\"6269\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"degree\",0.0174532925199433,AUTHORITY[\"EPSG\",\"9122\"]],AUTHORITY[\"EPSG\",\"4269\"]]";
						try {
							crs = CRS.parseWKT(wkt);
						} catch (FactoryException e) {
							// TODO Throw error: Unable to determine projection of shapefile.
							e.printStackTrace();
						}

	    	        builder.setCRS(crs);
	    	        GeometryDescriptor g = inputType.getGeometryDescriptor();
	    	        attributeBuilder.init(g);
	    	        attributeBuilder.setCRS(crs);
	    	        GeometryDescriptor att2 = (GeometryDescriptor) attributeBuilder.buildDescriptor(g.getLocalName());
	    	        builder.add(att2);
	    	        builder.setDefaultGeometry(att2.getLocalName());
	    	    } else {
	    	        builder.add(att);
	    	        builder.setDefaultGeometry(att.getLocalName());
	    	      }
	    		
	    	} else {
	    		// See if this is the col or row column
	    		if(name.equalsIgnoreCase("col")) { 
	    			colColumnName = name;
	    		} else if(name.equalsIgnoreCase("row")) {
	    			rowColumnName = name;
	    		}
	    		builder.add(att);
	    	}

	    }
	    builder.setName(gridTableName);
	    builder.setSuperType((SimpleFeatureType) inputType.getSuper());
	    SimpleFeatureType dbSchema = builder.buildFeatureType();
	    dbDataStore.createSchema(dbSchema);
	    SimpleFeatureStore dbFeatureStore = (SimpleFeatureStore) dbDataStore.getFeatureSource(gridTableName);
	    dbFeatureStore.addFeatures(inputFeatureCollection);

	    inputDataStore.dispose();
	    dbDataStore.dispose();
	    
	    //Standardize the name of the geom, col, and row fields using SQL alter table
	    DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());
	    if(geomColumnName != null && !geomColumnName.equals("geom")) {
	    	create.execute("alter table grids." + gridTableName + " rename column \"" + geomColumnName + "\" to geom;");
	    }
	    if(colColumnName != null && !colColumnName.equals("col")) {
	    	create.execute("alter table grids." + gridTableName + " rename column \"" + colColumnName + "\" to col;");
	    }
	    if(rowColumnName != null && !rowColumnName.equals("row")) {
	    	create.execute("alter table grids." + gridTableName + " rename column \"" + rowColumnName + "\" to row;");
	    }
	    
	    //TODO: Analyze tables immediately after creation?
	    
	    return gridTableName;
	}
	
	
}

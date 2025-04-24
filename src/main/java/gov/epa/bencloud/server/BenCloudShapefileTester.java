package gov.epa.bencloud.server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.FeatureStore;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.ReferenceIdentifier;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.geotools.util.URLs;
import org.geotools.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.epa.bencloud.server.database.PooledDataSource;
import gov.epa.bencloud.server.tasks.local.GridImportTaskRunnable;
import gov.epa.bencloud.server.util.ApplicationUtil;

public class BenCloudShapefileTester {
	private static final Logger log = LoggerFactory.getLogger(BenCloudServer.class);
	
	public static void main(String[] args) {
		try {
			ApplicationUtil.loadProperties("bencloud-server.properties");
			ApplicationUtil.loadProperties("bencloud-local.properties", true);
		} catch (IOException e) {
			log.error("Unable to load application properties", e);
			System.exit(-1);
		}

		try {
			if (!ApplicationUtil.validateProperties()) {
				log.error("properties are not all valid, application exiting");
				System.exit(-1);
			}
		} catch (IOException e) {
			log.error("Unable to validate application properties", e);
			System.exit(-1);
		}
		File shapefile = new File("/Users/jimanderton/Documents/Clients/EPA/Mels_Grid_AQ_Incidence/Report_Regions.shp");
		//File shapefile = new File("/Users/jimanderton/Documents/Clients/EPA/Mels_Grid_AQ_Incidence/with_prj/4grid_cell/4grid_cell.shp");
		try {
			//GridImportTaskRunnable.loadShapefileIntoDefaultPostGIS(shapefile.toPath());
			GridImportTaskRunnable.importShapefile(shapefile.toPath());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Load a given shapefile into the default PostGIS database using the
	 * postgisTableName parameter as new name for the table creation. The management
	 * of the projection system is done: if there is a valid SRID (in regard with
	 * the EPSG references) in the PRJ file, this SRID is taken into account.
	 * Otherwise, a WGS84 (EPSG:4326) projection system is used.
	 *
	 * @param shapefileURL     URL of the shapefile
	 * @param postgisTableName name of the PostGIS table to create
	 * @return true if loaded, false otherwise
	 *
	 * @throws FactoryException
	 */
	public static boolean loadShapefileIntoDefaultPostGIS(Path filePath) throws FactoryException {

		try {

			// shapefile loader
			File inFile = filePath.toFile();
			Map<String, Object> shapeParams = new HashMap<>();
			shapeParams.put("url", URLs.fileToUrl(inFile));
			DataStore shapeDataStore = DataStoreFinder.getDataStore(shapeParams);

			// feature type
			String typeName = shapeDataStore.getTypeNames()[0];
			FeatureSource<SimpleFeatureType, SimpleFeature> featSource = shapeDataStore.getFeatureSource(typeName);
			FeatureCollection<SimpleFeatureType, SimpleFeature> featSrcCollection = featSource.getFeatures();
			SimpleFeatureType ft = shapeDataStore.getSchema(typeName);

			// feature type copy to set the new name
			String postgisTableName = "g_" + UUID.randomUUID().toString().replace("-", "");
			SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
			builder.setName(postgisTableName);
			builder.setAttributes(ft.getAttributeDescriptors());
			builder.setCRS(ft.getCoordinateReferenceSystem());

			SimpleFeatureType newSchema = builder.buildFeatureType();

			// management of the projection system
			CoordinateReferenceSystem crs = ft.getCoordinateReferenceSystem();

			// test of the CRS based on the .prj file
			Integer crsCode = CRS.lookupEpsgCode(crs, true);
			CoordinateReferenceSystem crsTest = CRS.parseWKT(crs.toWKT());
			
			String wkt = "GEOGCS[\"NAD83\",DATUM[\"North_American_Datum_1983\",SPHEROID[\"GRS 1980\",6378137,298.257222101,AUTHORITY[\"EPSG\",\"7019\"]],TOWGS84[0,0,0,0,0,0,0],AUTHORITY[\"EPSG\",\"6269\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"degree\",0.0174532925199433,AUTHORITY[\"EPSG\",\"9122\"]],AUTHORITY[\"EPSG\",\"4269\"]]";
			CoordinateReferenceSystem crsTest2 = CRS.parseWKT(wkt);
			
			Set<ReferenceIdentifier> refIds = ft.getCoordinateReferenceSystem().getIdentifiers();
			if (((refIds == null) || (refIds.isEmpty())) && (crsCode == null)) {
				Set<String> testx = CRS.getSupportedCodes("EPSG");
				Set<String> testy = CRS.getSupportedAuthorities(true);
				Version testv = CRS.getVersion("EPSG");
				CoordinateReferenceSystem crsEpsg = CRS.decode("EPSG:4326");
				newSchema = SimpleFeatureTypeBuilder.retype(newSchema, crsEpsg);
			}
		    Map<String, Object> dbParams = new HashMap<>();
		    dbParams.put(PostgisNGDataStoreFactory.DBTYPE.key, "postgis");
		    dbParams.put(PostgisNGDataStoreFactory.USER.key, PooledDataSource.dbUser);
		    dbParams.put(PostgisNGDataStoreFactory.PASSWD.key, PooledDataSource.dbPassword);
		    dbParams.put(PostgisNGDataStoreFactory.HOST.key, PooledDataSource.dbHost);
		    dbParams.put(PostgisNGDataStoreFactory.PORT.key, PooledDataSource.dbPort);
		    dbParams.put(PostgisNGDataStoreFactory.DATABASE.key, PooledDataSource.dbName);
		    dbParams.put(PostgisNGDataStoreFactory.SCHEMA.key, "grids");
		    

			// storage in PostGIS
			DataStore dataStore = DataStoreFinder.getDataStore(dbParams);

			if (dataStore == null) {
				return false;
			}

			dataStore.createSchema(newSchema);

			FeatureStore<SimpleFeatureType, SimpleFeature> featStore = (FeatureStore<SimpleFeatureType, SimpleFeature>) dataStore
					.getFeatureSource(postgisTableName);

			featStore.addFeatures(featSrcCollection);

		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}
}

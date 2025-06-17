/**
 * 
 */
package gov.epa.bencloud.api.util;

import static gov.epa.bencloud.server.database.jooq.data.Tables.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.epa.bencloud.Constants;
import gov.epa.bencloud.server.database.JooqUtil;
import gov.epa.bencloud.server.database.jooq.data.tables.records.FileRecord;
import gov.epa.bencloud.server.util.ApplicationUtil;

/**
 * @author jimanderton
 *
 */
public class FilestoreUtil {
	private static final Logger log = LoggerFactory.getLogger(FilestoreUtil.class);

	/*
	 * Return the local path to the file associated with id
	 * This method does not enforce user-level security. That is the responsibility of the caller.
	 * The caller must NOT delete the file directly and should instead use the deleteFile(id) method
	 * 
	 * @param id
	 */
	public static Path getFilePath(Integer id) {
		// Return the Path for the file in the filestore
		// This will allow the caller to read the file using the desired libraries
		
		String filestorePath = ApplicationUtil.getProperty("file.store.path");
		return Paths.get(filestorePath, id.toString());

	}
	
	/*
	 * Return the file associated with id
	 * This method does not enforce user-level security. That is the responsibility of the caller.
	 * @param id
	 */
	public static File getFile(Integer id) {
		return getFilePath(id).toFile();
	}

	/*
	 * Returns all paths directly in a path
	 * This method does not enforce user-level security. That is the responsibility of the caller.
	 */
	public static List<Path> getPaths(Path path) throws IOException {
		try (Stream<Path> stream = Files.list(path)) {			
			return stream.collect(Collectors.toList());
    }
	}

	/*
	 * Returns the path of the filestore
	 */
	public static Path getFilestorePath() {
		return Paths.get(ApplicationUtil.getProperty("file.store.path"));
	}

	/*
	 * Stores a file in the fileStore and adds a corresponding record to the file table.
	 * @param file
	 * @param fileName
	 * @param user_id
	 * @param metadata
	 */
//	public static Integer putFile(File file, String fileName, String user_id, String metadata) {		
//		String filestorePath = ApplicationUtil.getProperty("file.store.path");
//		//TODO: Get inputstream for file and call method below. Clean up file when done?
//		return null;
//	}

	public static Integer putFile(InputStream is, String filename, String fileType, String user_id, String metadata) {
		// Insert record into file table
		// Store file in filestore using id as the name
		// Return id of newly stored file
		FileRecord fileRecord = null;
		
		String filestorePath = ApplicationUtil.getProperty("file.store.path");
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());
		
		fileRecord = create.insertInto(FILE
				, FILE.FILENAME
				, FILE.FILE_TYPE
				, FILE.FILE_SIZE
				, FILE.USER_ID
				, FILE.SHARE_SCOPE
				, FILE.METADATA
				, FILE.CREATED_DATE)
		.values(filename, fileType, null, user_id, Constants.SHARING_NONE, metadata, null)
		.returning(FILE.ID)
		.fetchOne();
		
		try {
			long fileSize = Files.copy(is, Paths.get(filestorePath, fileRecord.getId().toString()), StandardCopyOption.REPLACE_EXISTING);

			//Store the file size
			create.update(FILE)
			.set(FILE.FILE_SIZE, fileSize)
			.where(FILE.ID.eq(fileRecord.getId()))
			.execute();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// If the copy command files, remove the record from the file table
			e.printStackTrace();
		}
		return fileRecord.getId();
		
	}
	
	/*
	 * Return the file associated with id
	 * This method does not enforce user-level security. That is the responsibility of the caller.
	 * @param id
	 * @
	 */
	public static Boolean deleteFile(Integer id) {
		// Query file table for fileName and delete file
		
		String filestorePath = ApplicationUtil.getProperty("file.store.path");
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());

		try {
			Files.delete(Paths.get(filestorePath, id.toString()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
		int ret = create.deleteFrom(FILE)
		.where(FILE.ID.eq(id))
		.execute();
		
		return true;
	}



}

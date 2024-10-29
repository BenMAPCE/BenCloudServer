/**
 * 
 */
package gov.epa.bencloud.api;

import static gov.epa.bencloud.server.database.jooq.data.Tables.*;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.epa.bencloud.server.util.ApplicationUtil;

/**
 * @author jimanderton
 *
 */
public class FilestoreApi {
	private static final Logger log = LoggerFactory.getLogger(FilestoreApi.class);

	/*
	 * Return the file associated with id
	 * This method does not enforce user-level security. That is the responsibility of the caller.
	 * @param id
	 */
	public static File getFile(Integer id) {
		// Query file table for fileName and return file
		String filestorePath = ApplicationUtil.getProperty("file.store.path");
		
		return null;
	}

	/*
	 * Stores a file in the fileStore and adds a corresponding record to the file table.
	 * @param file
	 * @param fileName
	 * @param user_id
	 * @param metadata
	 */
	public static Integer putFile(File file, String fileName, String user_id, String metadata) {
		// Insert record into file table
		// Store file in filestore using id as the name
		// Return id of newly stored file
		
		String filestorePath = ApplicationUtil.getProperty("file.store.path");
		return null;
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
		
		return null;
	}
}

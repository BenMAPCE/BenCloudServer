/**
 * 
 */
package gov.epa.bencloud.api;

import static gov.epa.bencloud.server.database.jooq.data.Tables.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import org.jooq.impl.DSL;
import org.pac4j.core.profile.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.epa.bencloud.Constants;
import gov.epa.bencloud.api.util.FilestoreUtil;
import gov.epa.bencloud.server.database.JooqUtil;
import gov.epa.bencloud.server.database.jooq.data.tables.records.FileRecord;
import gov.epa.bencloud.server.util.ApplicationUtil;
import spark.Request;
import spark.Response;

/**
 * @author jimanderton
 * Other Api classes generally interact with the FileStore directly via FilestoreApi
 * This class is primarly design to provide admin-level access for the purpose of troubleshooting and managing the filestore
 */
public class FilestoreApi {
	private static final Logger log = LoggerFactory.getLogger(FilestoreApi.class);

	/*
	 * Return the file associated with id
	 * Requires admin access
	 * @param id
	 */
	public static void getFile(Request request, Response response, Optional<UserProfile> userProfile) {
		// Query file table for fileName and return file
//		String filestorePath = ApplicationUtil.getProperty("file.store.path");
		
		Integer id;
		try {
			id = Integer.valueOf(request.params("id"));
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return;
		}
		
		if(!CoreApi.isAdmin(userProfile)) {
			
		}
		
		return;
	}
	
	/*
	 * Delete the file associated with id
	 * Requires admin access
	 * @param id
	 */
	public static Object deleteFile(Request request, Response response, Optional<UserProfile> userProfile) {

		if(!CoreApi.isAdmin(userProfile)) {
			return CoreApi.getErrorResponseForbidden(request, response);
		}
				
		Integer id;
		try {
			id = Integer.valueOf(request.params("id"));
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return CoreApi.getErrorResponseNotFound(request, response);
		}
		
		FilestoreUtil.deleteFile(id);
		return CoreApi.getSuccessResponse(request, response, 200, "File deleted: " + id);
		
	}

	//TODO getFileListing()

}

/**
 * 
 */
package gov.epa.bencloud.api;

import java.util.Optional;

import org.pac4j.core.profile.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.epa.bencloud.api.util.FilestoreUtil;
import spark.Request;
import spark.Response;

/**
 * @author jimanderton
 * Other Api classes generally interact with the FileStore directly via FilestoreUtil
 * This class is primarily designed to provide admin-level access for the purpose of troubleshooting and managing the filestore
 */
public class FilestoreApi {
	private static final Logger log = LoggerFactory.getLogger(FilestoreApi.class);

	/*
	 * Return the file associated with id
	 * Requires admin access
	 * @param id
	 */
	public static void getFile(Request request, Response response, Optional<UserProfile> userProfile) {

		if(!CoreApi.isAdmin(userProfile)) {
			response.status(401);
			return; 
		}
		Integer id;
		try {
			id = Integer.valueOf(request.params("id"));
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return;
		}
		
		//TODO: Get and stream file to user
		
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

	//TODO getFileListing()?

}

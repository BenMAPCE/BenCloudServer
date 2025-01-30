/**
 * 
 */
package gov.epa.bencloud.api;

import static gov.epa.bencloud.server.database.jooq.data.Tables.FILE;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.jooq.DSLContext;
import org.jooq.Record2;
import org.jooq.impl.DSL;
import org.pac4j.core.profile.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.epa.bencloud.api.util.FilestoreUtil;
import gov.epa.bencloud.server.database.JooqUtil;
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
	 * Requires admin access or owning the file
	 * @param id
	 */
	public static Object getFile(Request request, Response response, Optional<UserProfile> userProfile) {

		Integer id;
		try {
			id = Integer.valueOf(request.params("id"));
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return CoreApi.getErrorResponseInvalidId(request,response);
		}

		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());

		Record2<String,String> record = create
				.select(FILE.FILENAME,FILE.USER_ID)
				.from(FILE)
				.where(FILE.ID.eq(id))
				.fetchOne();
		
		if (record == null) {
			return CoreApi.getErrorResponseNotFound(request,response);
		}

		if(!CoreApi.isAdmin(userProfile) && !userProfile.get().getId().equals(record.value2())) {
			return CoreApi.getErrorResponseForbidden(request,response);
		}

		File file = FilestoreUtil.getFile(id);

		if (!file.exists()) {
			return CoreApi.getErrorResponseNotFound(request,response);
		}

		response.type("application/octet-stream");
        response.header("Content-Disposition", "attachment; filename=\"" + record.value1() + "\"");
		response.header("Access-Control-Expose-Headers", "Content-Disposition");

		OutputStream responseOutputStream;
		try {
			responseOutputStream = response.raw().getOutputStream();
		} catch (IOException e) {
			log.error("Error getting output stream", e);
			return CoreApi.getErrorResponse(request,response,500,"Error getting output stream.");
		}

		FileInputStream fileInputStream;
		try {
			fileInputStream = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			log.error("File not found", e);
			return CoreApi.getErrorResponseNotFound(request,response);
		}

		try{
			IOUtils.copy(fileInputStream,responseOutputStream);
		} catch (IOException e) {
			log.error("Error copying file input to result output.",e);
			response.status(500);
			return CoreApi.getErrorResponse(request,response,500,"Error copying file input to result output.");
		}

		return CoreApi.getSuccessResponse(request,response,200,"File Found");
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

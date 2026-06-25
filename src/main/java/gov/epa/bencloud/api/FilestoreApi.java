/**
 * 
 */
package gov.epa.bencloud.api;

import static gov.epa.bencloud.server.database.jooq.data.Tables.FILE;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.jooq.DSLContext;
import org.jooq.Record2;
import org.jooq.impl.DSL;
import org.pac4j.core.profile.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
		// Set Content-Length so the response is sent without chunked transfer encoding.
		// This avoids per-chunk framing overhead and lets clients/proxies show download progress.
		response.header("Content-Length", String.valueOf(file.length()));

		// Stream the file using a 64KB buffer and buffered input to minimize read/write syscalls
		// on large downloads. try-with-resources guarantees the input stream is closed.
		try (InputStream in = new BufferedInputStream(new FileInputStream(file), 65536)) {
			OutputStream responseOutputStream = response.raw().getOutputStream();
			IOUtils.copyLarge(in, responseOutputStream, new byte[65536]);
			responseOutputStream.flush();
		} catch (FileNotFoundException e) {
			log.error("File not found", e);
			return CoreApi.getErrorResponseNotFound(request,response);
		} catch (IOException e) {
			log.error("Error copying file input to result output.",e);
			response.status(500);
			return CoreApi.getErrorResponse(request,response,500,"Error copying file input to result output.");
		}

		// The file bytes have already been written directly to the raw output stream.
		// Return the raw response so Spark does not serialize and append anything to the body.
		return response.raw();
	}

/*
	 * Return information on all files in the filestore
	 * Requires admin access
	 */
	public static Object getAllFiles(Request request, Response response, Optional<UserProfile> userProfile) { 

		if(!CoreApi.isAdmin(userProfile)) {
			return CoreApi.getErrorResponseForbidden(request, response);
		}

		ObjectMapper mapper = new ObjectMapper();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

		ObjectNode responseJSON = getPathJSON(FilestoreUtil.getFilestorePath(),mapper,formatter);

		response.type("application/json");
		response.status(200);
		return responseJSON;
	}

	private static ObjectNode getPathJSON(Path path, ObjectMapper mapper, DateTimeFormatter formatter) {

		ObjectNode pathData = mapper.createObjectNode();

		BasicFileAttributes attrs;
		try {
			attrs = Files.readAttributes(path, BasicFileAttributes.class);
		} catch(IOException e) {
			log.error("Error getting attributes for path: "+path, e);
			pathData.put("error","Error getting attributes");
			return pathData;
		}
		
		pathData.put("name",path.getFileName().toString());
		pathData.put("size",attrs.size());
		pathData.put("modified_date",attrs.lastModifiedTime().toInstant().atZone(ZoneId.systemDefault()).format(formatter));
		pathData.put("created_date",attrs.creationTime().toInstant().atZone(ZoneId.systemDefault()).format(formatter));
		pathData.put("is_directory",attrs.isDirectory());

		if (attrs.isDirectory()) {
			List<Path> paths;
			try {
				paths = FilestoreUtil.getPaths(path);
			} catch(IOException e) {
				log.error("Error getting paths in path: "+path, e);
				pathData.put("error","Error getting paths");
				return pathData;
			}
			ArrayNode contentsArray = pathData.putArray("contents");
			for (Path subPath : paths) {
				ObjectNode subData = getPathJSON(subPath,mapper,formatter);
				contentsArray.add(subData);
			}
		}

		return pathData;
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

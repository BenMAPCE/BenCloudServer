package gov.epa.bencloud.api;

import static gov.epa.bencloud.server.database.jooq.data.Tables.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import javax.servlet.MultipartConfigElement;

import org.jooq.DSLContext;
import org.jooq.JSON;
import org.jooq.JSONFormat;
import org.jooq.Result;
import org.jooq.JSONFormat.RecordFormat;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.impl.DSL;
import org.pac4j.core.profile.UserProfile;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import gov.epa.bencloud.Constants;
import gov.epa.bencloud.server.database.JooqUtil;
import gov.epa.bencloud.server.database.jooq.data.tables.records.SettingsRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.TaskConfigRecord;
import gov.epa.bencloud.api.model.ValidationMessage;
import gov.epa.bencloud.api.util.ApiUtil;

import spark.Request;
import spark.Response;

public class CoreApi {
	private static final Logger log = LoggerFactory.getLogger(CoreApi.class);
	private static final String BANNER_KEY = "banner";
	
	public static Object getErrorResponse(Request request, Response response, int statusCode, String msg) {
		response.type("application/json");
		response.status(statusCode);
		return "{\"message\":\"" + msg + "\"}";	
	}

	public static Object getErrorResponseNotFound(Request request, Response response) {
		return CoreApi.getErrorResponse(request, response, 404, "Not found");
	}
	
	public static Object getErrorResponseInvalidId(Request request, Response response) {
		return CoreApi.getErrorResponse(request, response, 400, "Invalid id");
	}
	
	public static Object getErrorResponseForbidden(Request request, Response response) {
		return CoreApi.getErrorResponse(request, response, 403, "Forbidden");
	}

	public static Object getErrorResponseBadRequest(Request request, Response response) {
		return CoreApi.getErrorResponse(request, response, 400, "Bad request");
	}
	
	public static Object getErrorResponseUnimplemented(Request request, Response response) {
		return CoreApi.getErrorResponse(request, response, 405, "Method not yet implemented");
	}
	
	public static Object getSuccessResponse(Request request, Response response, int statusCode, String msg) {
		response.type("application/json");
		response.status(statusCode);
		return "{\"message\":\"" + msg + "\"}";
	}

	public static Object getAirQualityLayerDeleteSuccessResponse(Request request, Response response) {
		return CoreApi.getSuccessResponse(request, response, 204, "Successfully deleted AQ layer");
	}



	/**
	 * 
	 * @param userOptionalProfile
	 * @return true if the current user's role is admin. If not, returns false.
	 */
	public static Boolean isAdmin(Optional<UserProfile> userOptionalProfile) {
		UserProfile userProfile = userOptionalProfile.get();
		if(userProfile == null) {
			return false;
		}

		for (String role : userProfile.getRoles()) {
			if(role.equalsIgnoreCase(Constants.ROLE_ADMIN)) {
				return true;
			}
		}

		return false;
	}


	/**
	 * 
	 * @param userOptionalProfile
	 * @return true if the current user's role is user. If not, returns false.
	 */
	public static Boolean isUser(Optional<UserProfile> userOptionalProfile) {
		UserProfile userProfile = userOptionalProfile.get();
		if(userProfile == null) {
			return false;
		}

		for (String role : userProfile.getRoles()) {
			if(role.equalsIgnoreCase(Constants.ROLE_USER)) {
				return true;
			}
		}

		return false;
	}


	/**
	 * 
	 * @param req
	 * @param res
	 * @param userOptionalProfile
	 * @return an ObjectNode representation of the current user's profile info.
	 */
	public static Object getUserInfo(Request req, Response res, Optional<UserProfile> userOptionalProfile) {

		UserProfile userProfile = userOptionalProfile.get();

		ObjectMapper mapper = new ObjectMapper();
		ObjectNode userNode = mapper.createObjectNode();

		userNode.put(Constants.HEADER_USER_ID, userProfile.getId());
		Object tmpDisplayName = userProfile.getAttribute(Constants.HEADER_DISPLAY_NAME);
		Object tmpMail = userProfile.getAttribute(Constants.HEADER_MAIL);

		userNode.put(Constants.HEADER_MAIL, tmpMail==null ? null : tmpMail.toString());

		// We want displayname to always contains something useful.
		// However, there is some inconsistency in the user object between EPA and login.gov logins that this code deals with
		if(tmpDisplayName == null || tmpDisplayName.toString().isEmpty()) {
			userNode.put(Constants.HEADER_DISPLAY_NAME, tmpMail==null ? null : tmpMail.toString());			
		} else {
			userNode.put(Constants.HEADER_DISPLAY_NAME, tmpDisplayName==null ? null : tmpDisplayName.toString());			
		}
			
		ArrayNode rolesNode = userNode.putArray(Constants.HEADER_GROUPS);
		for (String role : userProfile.getRoles()) {
			rolesNode.add(role);
		}
		userNode.put("isAdmin", isAdmin(userOptionalProfile));
		userNode.put("isUser", isUser(userOptionalProfile));

		return userNode;
	}
	
	/**
	 * 
	 * @param req
	 * @param res
	 * @param userOptionalProfile
	 * @return user's email or empty string
	 */
	public static String getUserEmail(Request req, Response res, Optional<UserProfile> userOptionalProfile) {

		UserProfile userProfile = userOptionalProfile.get();

		Object tmpMail = userProfile.getAttribute(Constants.HEADER_MAIL);

		return tmpMail==null ? "" : tmpMail.toString();

	}
	
	/**
	 * 
	 * @param req
	 * @param res
	 * @param userOptionalProfile
	 * @return an ObjectNode representation of the application and database version.
	 */
	public static Object getVersion(Request req, Response res, Optional<UserProfile> userOptionalProfile) {

		UserProfile userProfile = userOptionalProfile.get();

		ObjectMapper mapper = new ObjectMapper();
		ObjectNode responseNode = mapper.createObjectNode();

		responseNode.put("apiVersion", ApiUtil.appVersion);
		responseNode.put("dbVersion", ApiUtil.getDatabaseVersion());

		return responseNode;
	}

	/**
	 * 
	 * @param req
	 * @param res
	 * @param userOptionalProfile
	 * @return an ObjectNode representation of the application and database version.
	 */
	public static Object getBanner(Request req, Response res, Optional<UserProfile> userOptionalProfile) {

		SettingsRecord bannerRecord = DSL.using(JooqUtil.getJooqConfiguration())
			.selectFrom(SETTINGS)
			.where(SETTINGS.KEY.equalIgnoreCase(BANNER_KEY))
			.orderBy(SETTINGS.MODIFIED_DATE.desc())
			.limit(1)
			.fetchAny();

		if (bannerRecord == null) {
			return CoreApi.getErrorResponse(req, res, 404, "Banner data not found");
		}

		ObjectMapper mapper = new ObjectMapper();
		ObjectNode responseNode = mapper.createObjectNode();

		responseNode.put("message", bannerRecord.getValueText());
		responseNode.put("type", bannerRecord.getValueInt());
		responseNode.put("enabled", bannerRecord.getStatus() != 0);
		responseNode.put("modified_by", bannerRecord.getModifiedBy());

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssZ");
		ZoneId zoneId = ZoneId.systemDefault();
		try {
			responseNode.put("modified_date", bannerRecord.getModifiedDate().atZone(zoneId).format(formatter));
		} catch (Exception e) {
			responseNode.put("modified_date", "");
		}

		res.type("application/json");
		return responseNode;
	}

	/**
	 * 
	 * @param req
	 * @param res
	 * @param userOptionalProfile
	 * @return update the banner notification
	 */
	public static Object postBanner(Request req, Response res, Optional<UserProfile> userOptionalProfile) {

		if (!isAdmin(userOptionalProfile)) {
			return getErrorResponseForbidden(req,res);
		}

		String message;
		Integer type;
		Boolean enabled;
		
		try {
			message = req.raw().getParameter("message");
			type = Integer.parseInt(req.raw().getParameter("type"));
			enabled = Boolean.valueOf(req.raw().getParameter("enabled"));		
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return CoreApi.getErrorResponse(req, res, 400, "Invalid type " + req.raw().getParameter("type") +". Expected 1, 2, or 3.");
		}

		if (message == null || message.trim().isEmpty()) {
			return CoreApi.getErrorResponse(req, res, 400, "Invalid empty message.");
		}

		if (type < 1 || type > 3) {
			return CoreApi.getErrorResponse(req, res, 400, "Invalid type " + type +". Expected 1, 2, or 3.");
		}

		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());

		boolean bannerExists = create.fetchExists(
			create.selectFrom(SETTINGS)
				.where(SETTINGS.KEY.equalIgnoreCase(BANNER_KEY)));

		if (bannerExists) {
			create.update(SETTINGS)
				.set(SETTINGS.VALUE_TEXT,message.trim())
				.set(SETTINGS.VALUE_INT,type)
				.set(SETTINGS.STATUS,enabled ? 1 : 0)
				.set(SETTINGS.MODIFIED_BY,userOptionalProfile.get().getId())
				.set(SETTINGS.MODIFIED_DATE,LocalDateTime.now())
				.where(SETTINGS.KEY.equalIgnoreCase(BANNER_KEY))
				.execute();
		} else {
			create.insertInto(SETTINGS)
			  .set(SETTINGS.KEY,BANNER_KEY)
			  .set(SETTINGS.VALUE_TEXT,message.trim())
			  .set(SETTINGS.VALUE_INT,type)
			  .set(SETTINGS.STATUS,enabled ? 1 : 0)
			  .set(SETTINGS.MODIFIED_BY,userOptionalProfile.get().getId())
			  .set(SETTINGS.MODIFIED_DATE,LocalDateTime.now())
      	.execute();
		}

		return getSuccessResponse(req,res,200,"Banner successfully updated");
	}

	/**
	 * Transforms records into a JsonNode.
	 * @param records
	 * @return the trasformed records as a JsonNode.
	 */
	public static JsonNode transformRecordsToJSON(Record records) {
		
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode data = mapper.createObjectNode();

        JsonNode recordsJSON = null;
		try {
			JsonFactory factory = mapper.getFactory();
			JsonParser jp = factory.createParser(
					records.formatJSON(new JSONFormat().header(false).recordFormat(RecordFormat.OBJECT)));
			recordsJSON = mapper.readTree(jp);
		} catch (JsonParseException e) {
			log.error("Error parsing JSON",e);
		} catch (JsonProcessingException e) {
			log.error("Error processing JSON",e);
		} catch (IOException e) {
			log.error("IO Exception", e);
		}
		
		return recordsJSON;
		
	}
	
	/**
	 * Transforms a validation message into a JsonNode
	 * @param validationMessage
	 * @return the transformed validation message as a JsonNode.
	 */
	public static JsonNode transformValMsgToJSON(ValidationMessage validationMessage) {
		
		ObjectMapper mapper = new ObjectMapper();
		JsonNode recordsJSON = null;
		//ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		try {
			recordsJSON = mapper.valueToTree(validationMessage);
		} catch (Exception e) {
			log.error("Error converting validation message to JSON",e);
		} 
		
		return recordsJSON;
		
	}
}

package gov.epa.bencloud.api;

import static gov.epa.bencloud.server.database.jooq.data.Tables.*;

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import gov.epa.bencloud.Constants;
import gov.epa.bencloud.server.database.JooqUtil;
import gov.epa.bencloud.server.database.jooq.data.tables.records.TaskConfigRecord;
import gov.epa.bencloud.api.util.ApiUtil;

import spark.Request;
import spark.Response;

public class CoreApi {
	private static final Logger log = LoggerFactory.getLogger(CoreApi.class);
	
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
	 * @param req
	 * @param res
	 * @param userProfile
	 * @return
	 */
	public static Object getPurgeResults(Request req, Response res, Optional<UserProfile> userProfile) {
		if(! isAdmin(userProfile)) {
			return false;
		}

		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());

		create
		.deleteFrom(TASK_WORKER)
		.execute();

		create
		.deleteFrom(TASK_QUEUE)
		.execute();
		
		create
		.deleteFrom(TASK_COMPLETE)
		.execute();

		create
		.deleteFrom(HIF_RESULT)
		.execute();
		
		create
		.deleteFrom(HIF_RESULT_FUNCTION_CONFIG)
		.execute();
		
		create
		.deleteFrom(HIF_RESULT_DATASET)
		.execute();
		
		create
		.deleteFrom(VALUATION_RESULT)
		.execute();
		
		create
		.deleteFrom(VALUATION_RESULT_FUNCTION_CONFIG)
		.execute();
		
		create
		.deleteFrom(VALUATION_RESULT_DATASET)
		.execute();
		
		create
		.execute("vacuum analyze");
		
		return true;
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
	 * @param userProfile
	 * @return 
	 */
	public static Object getFixWeiFunction(Request req, Response res, Optional<UserProfile> userProfile) {
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());
		create.update(HEALTH_IMPACT_FUNCTION)
			.set(HEALTH_IMPACT_FUNCTION.START_AGE, 65)
			.where(HEALTH_IMPACT_FUNCTION.ID.eq(1018))
			.execute();	
		
		return "{'message': 'done'}";
	}

	public static boolean isValidTaskType(String type) {
		if(type.equalsIgnoreCase("HIF") || type.equalsIgnoreCase("Valuation")) {
			return true;
		}
		return false;
	}

}

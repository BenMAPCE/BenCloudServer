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
	
	public static Object getSuccessResponse(Request request, Response response, int statusCode, String msg) {
		response.type("application/json");
		return "{\"message\":\"" + msg + "\"}";
	}

	public static Object getAirQualityLayerDeleteSuccessResponse(Request request, Response response) {
		return CoreApi.getSuccessResponse(request, response, 204, "Successfully deleted AQ layer");
	}

	public static Object getTaskDeleteSuccessResponse(Request request, Response response) {
		return CoreApi.getSuccessResponse(request, response, 204, "Successfully deleted task");
	}	

	/**
	 * 
	 * @param request
	 * @param response
	 * @param userProfile
	 * @return a JSON representation of the current user's task configurations.
	 */
	public static Object getTaskConfigs(Request request, Response response, Optional<UserProfile> userProfile) {
		//TODO: Add type filter to select HIF or Valuation

		Result<Record> res = DSL.using(JooqUtil.getJooqConfiguration())
				.select(TASK_CONFIG.asterisk())
				.from(TASK_CONFIG)
				.where(TASK_CONFIG.USER_ID.eq(userProfile.get().getId()))
				.orderBy(TASK_CONFIG.NAME)
				.fetch();
		
		response.type("application/json");
		return res.formatJSON(new JSONFormat().header(false).recordFormat(RecordFormat.OBJECT));
	}
	

	
	public static Object deleteTaskConfig(Request request, Response response, Optional<UserProfile> userProfile) {
		Integer id;
		try {
			id = Integer.valueOf(request.params("id"));
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return CoreApi.getErrorResponseInvalidId(request, response);
		}
		
		
		//Users can only delete templates created by themselves 		
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());	
		Result<Record1<Integer>> res = create.select(TASK_CONFIG.ID)
				.from(TASK_CONFIG)
				.where(TASK_CONFIG.USER_ID.eq(userProfile.get().getId()))
				.and(TASK_CONFIG.ID.eq(id))
				.fetch();
		
		if(res==null) {
			return CoreApi.getErrorResponseForbidden(request, response);
		}		
		
		int configRows = create.deleteFrom(TASK_CONFIG).where(TASK_CONFIG.ID.eq(id)).execute();
		
		if(configRows == 0) {
			return CoreApi.getErrorResponse(request, response, 400, "Unknown error");
		} else {
			response.status(204);
			return response;
		}
		
	}
	
	public static Object renameTaskConfig(Request request, Response response, Optional<UserProfile> userProfile) {
		request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));
		Integer id;
		String newName;

		try {
			id = Integer.valueOf(request.params("id"));
			newName = ApiUtil.getMultipartFormParameterAsString(request, "newName");
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return CoreApi.getErrorResponseInvalidId(request, response);
		}		
		
		//make sure the new template name is unique among this user's templates
			List<String>taskNames = ApiUtil.getAllTemplateNamesByUser(userProfile.get().getId());
			if (taskNames.contains(newName)) {
				response.status(200);
				String errorMsg = "A task named " + newName + " already exists. Please enter a different name.";
				return "{\"message\": \"" + errorMsg + "\"}";
			}			
		
		//Users can only edit templates created by themselves 		
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());	
		Result<Record1<Integer>> res = create.select(TASK_CONFIG.ID)
				.from(TASK_CONFIG)
				.where(TASK_CONFIG.USER_ID.eq(userProfile.get().getId()))
				.and(TASK_CONFIG.ID.eq(id))
				.fetch();
		
		if(res==null) {
			response.status(400);
			return CoreApi.getErrorResponse(request, response, 400, "You can only rename tasks created by yourself.");
		}		
		
		int configRows = create.update(TASK_CONFIG)
				.set(TASK_CONFIG.NAME, newName)
				.where(TASK_CONFIG.ID.eq(id))
				.execute();
		
		if(configRows == 0) {
			response.status(400);
			return CoreApi.getErrorResponse(request, response, 400, "Unknown error");
		} else {
			response.status(204);
			return CoreApi.getSuccessResponse(request, response, 204, "Successfully renamed.");
		}
		
	}

	/**
	 * 
	 * @param request HTTP request body contains task configuration parameters
	 * @param response
	 * @param userProfile
	 * @return add a task configuration to the database.
	 */
	public static String postTaskConfig(Request request, Response response, Optional<UserProfile> userProfile) {
		ObjectMapper mapper = new ObjectMapper();
		String body = request.body();
		
		JsonNode jsonPost = null;
		
		try {
			jsonPost = mapper.readTree(body);
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			response.status(400);
			return null;
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			response.status(400);
			return null;
		} 
		
		String name;
		String type;
		JSON params;
		try {
			name = jsonPost.get("name").asText();
			type = jsonPost.get("type").asText();
			params = JSON.json(jsonPost.get("parameters").toString());
		} catch (NullPointerException e) {
			e.printStackTrace();
			response.status(400);
			return null;
		}
		

		//make sure the new template name is unique among this user's templates
		List<String>taskNames = ApiUtil.getAllTemplateNamesByUser(userProfile.get().getId());
		if (taskNames.contains(name)) {
			response.status(200);
			String errorMsg = "A task named " + name + " already exists. Please enter a different name.";
			return "{\"message\": \"" + errorMsg + "\"}";
		}	
		
		TaskConfigRecord rec = DSL.using(JooqUtil.getJooqConfiguration())
		.insertInto(TASK_CONFIG, TASK_CONFIG.NAME, TASK_CONFIG.TYPE, TASK_CONFIG.PARAMETERS, TASK_CONFIG.USER_ID)
		.values(name, type, params, userProfile.get().getId())
		.returning(TASK_CONFIG.asterisk())
		.fetchOne();
		
		return rec.formatJSON(new JSONFormat().header(false).recordFormat(RecordFormat.OBJECT));
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

	public static int getTotalTaskCountForUser(UserProfile profile) {
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());
		int pendingCount = create.fetchCount(TASK_QUEUE, TASK_QUEUE.USER_ID.eq(profile.getId()));
		int completedCount = create.fetchCount(TASK_COMPLETE, TASK_COMPLETE.USER_ID.eq(profile.getId()));
		return pendingCount + completedCount;
	}

	public static boolean isValidTaskType(String type) {
		if(type.equalsIgnoreCase("HIF") || type.equalsIgnoreCase("Valuation")) {
			return true;
		}
		return false;
	}

}

package gov.epa.bencloud.api;

import static gov.epa.bencloud.server.database.jooq.data.Tables.*;

import java.util.Set;

import org.jooq.DSLContext;
import org.jooq.JSON;
import org.jooq.JSONFormat;
import org.jooq.Result;
import org.jooq.JSONFormat.RecordFormat;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import gov.epa.bencloud.server.database.JooqUtil;
import gov.epa.bencloud.server.database.jooq.data.tables.records.TaskConfigRecord;

import spark.Request;
import spark.Response;

public class CoreApi {
	private static final Logger log = LoggerFactory.getLogger(CoreApi.class);
	
	public static Object getTaskConfigs(Request request, Response response) {
		//TODO: Add type filter to select HIF or Valuation
		Result<Record> res = DSL.using(JooqUtil.getJooqConfiguration())
				.select(TASK_CONFIG.asterisk())
				.from(TASK_CONFIG)
				.orderBy(TASK_CONFIG.NAME)
				.fetch();
		
		response.type("application/json");
		return res.formatJSON(new JSONFormat().header(false).recordFormat(RecordFormat.OBJECT));
	}
	
	//TODO: Add deleteTaskConfig, update?
	public static String postTaskConfig(Request request, Response response) {
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
		
		String name = jsonPost.get("name").asText();
		String type = jsonPost.get("type").asText();
		JSON params = JSON.json(jsonPost.get("parameters").toString());
		
		TaskConfigRecord rec = DSL.using(JooqUtil.getJooqConfiguration())
		.insertInto(TASK_CONFIG, TASK_CONFIG.NAME, TASK_CONFIG.TYPE, TASK_CONFIG.PARAMETERS)
		.values(name, type, params)
		.returning(TASK_CONFIG.asterisk())
		.fetchOne();
		
		return rec.formatJSON(new JSONFormat().header(false).recordFormat(RecordFormat.OBJECT));
	}

	public static Object getPurgeResults(Request req, Response res) {
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());

		create
		.truncate(TASK_WORKER)
		.execute();

		create
		.truncate(TASK_QUEUE)
		.execute();
		
		create
		.truncate(TASK_COMPLETE)
		.execute();
		
		create
		.truncate(HIF_RESULT)
		.execute();
		
		create
		.truncate(HIF_RESULT_FUNCTION_CONFIG)
		.execute();
		
		create
		.truncate(HIF_RESULT_DATASET)
		.execute();
		
		create
		.truncate(VALUATION_RESULT)
		.execute();
		
		create
		.truncate(VALUATION_RESULT_FUNCTION_CONFIG)
		.execute();
		
		create
		.truncate(VALUATION_RESULT_DATASET)
		.execute();
		
		create
		.execute("vacuum analyze");
		
		return true;
	}

	public static Object getUserInfo(Request req, Response res) {
		for(String header : req.headers()) {
			log.debug(header + ": " + req.headers(header));
		}
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode data = mapper.createObjectNode();
		String h = req.headers("USER");

		//Add other attributes once we figure out what's available
		
		data.put("userId", h);
		return data;
	}

	public static Object getFixHealthEffectGroupName(Request req, Response res) {
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());
		create.update(HEALTH_IMPACT_FUNCTION_GROUP)
			.set(HEALTH_IMPACT_FUNCTION_GROUP.NAME, "Results for Regulatory Analysis")
			.where(HEALTH_IMPACT_FUNCTION_GROUP.ID.eq(7))
			.execute();
		
		create.update(AIR_QUALITY_LAYER)
		.set(AIR_QUALITY_LAYER.NAME, "2023 Policy Baseline")
		.where(AIR_QUALITY_LAYER.ID.eq(6))
		.execute();	
		
		create.update(AIR_QUALITY_LAYER)
		.set(AIR_QUALITY_LAYER.NAME, "2023 Policy Implementation")
		.where(AIR_QUALITY_LAYER.ID.eq(7))
		.execute();			
		
		return "done";
	}
}

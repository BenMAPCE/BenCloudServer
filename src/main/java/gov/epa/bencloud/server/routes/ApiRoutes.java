package gov.epa.bencloud.server.routes;

import java.util.UUID;

import javax.servlet.MultipartConfigElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import freemarker.template.Configuration;
import gov.epa.bencloud.api.*;
import gov.epa.bencloud.api.util.ApiUtil;
import gov.epa.bencloud.server.tasks.TaskComplete;
import gov.epa.bencloud.server.tasks.TaskQueue;
import gov.epa.bencloud.server.tasks.model.Task;
import gov.epa.bencloud.server.util.ParameterUtil;
import spark.Service;

public class ApiRoutes extends RoutesBase {

	private static final Logger log = LoggerFactory.getLogger(ApiRoutes.class);
	private Service service = null;
	private final String apiPrefix = "/api";
	public ApiRoutes(Service service, Configuration freeMarkerConfiguration){
		this.service = service;
		addRoutes();
	}


	private void addRoutes() {

		/*
		 * GET array of all grid definitions
		 */
		service.get(apiPrefix + "/grid-definitions", (request, response) -> {
			return GridDefinitionApi.getAllGridDefinitions(response);
		});
		
		/*
		 * GET array of all pollutant definitions
		 */
		service.get(apiPrefix + "/pollutants", (request, response) -> {
			return PollutantApi.getAllPollutantDefinitions(response);
		});

		/*
		 * GET array of air quality surface definitions
		 * PARAMETERS:
		 *  pollutantId= (optional)
		 *  page=
		 *  rowsPerPage=
		 *  sortBy=
		 *  descending=
		 *  filter=
		 */
		service.get(apiPrefix + "/air-quality-data", (request, response) -> {
			return AirQualityApi.getAirQualityLayerDefinitions(request, response);
		});

		/*
		 * GET a single air quality surface definition
		 */
		service.get(apiPrefix + "/air-quality-data/:id/definition", (request, response) -> {
			return AirQualityApi.getAirQualityLayerDefinition(request, response);
		});

		/*
		 * GET the contents of a single air quality surface
		 * PARAMETERS:
		 *  :id
		 *  page=
		 *  rowsPerPage=
		 *  sortBy=
		 *  descending=
		 *  filter=
		 *  
		 *  REQUEST HEADER Accept=text/csv will produce a CSV file
		 *  else, application/json response
		 */
		service.get(apiPrefix + "/air-quality-data/:id/details", (request, response) -> {
			return AirQualityApi.getAirQualityLayerDetails(request, response);
		});

		/*
		 * GET array of all population dataset definitions
		 */
		service.get(apiPrefix + "/population", (request, response) -> {
			return PopulationApi.getAllPopulationDatasets(response);
		});
		
		/*
		 * GET array of all health impact function definitions
		 */
		service.get(apiPrefix + "/health-impact-functions", (request, response) -> {
			return HIFApi.getAllHealthImpactFunctions(request, response);
		});

		/*
		 * GET a health impact function definition
		 */
		service.get(apiPrefix + "/health-impact-function/:id", (request, response) -> {
			return HIFApi.getHealthImpactFunction(request, response);
		});
		
		/*
		 * GET array of health impact function groups
		 * PARAMETERS:
		 *  pollutantId=
		 *  
		 *  Response will include array of function ids within each group
		 */	
		service.get(apiPrefix + "/health-impact-function-groups", (request, response) -> {
			return HIFApi.getAllHifGroups(request, response);
		});
		
		/*
		 * GET definition of one or more health impact function groups
		 * PARAMETERS:
		 *  :ids (comma separated list of health impact function group ids
		 *  popYear=
		 *  incidencePrevalenceDataset=
		 *  pollutantId=
		 *  baselineId=
		 *  scenarioId=
		 *  
		 *  Parameters are used to filter the list of functions to only those relevant to the current analysis
		 *  
		 *  Response will include array of complete function definitions within each group
		 */	
		service.get(apiPrefix + "/health-impact-function-groups/:ids", (request, response) -> {
			return HIFApi.getSelectedHifGroups(request, response);
		});
		
		service.get(apiPrefix + "/incidence", (request, response) -> {
			return IncidenceApi.getAllIncidenceDatasets(response);
		});
		
		service.get(apiPrefix + "/prevalence", (request, response) -> {
			return IncidenceApi.getAllPrevalenceDatasets(response);
		});

		service.get(apiPrefix + "/valuation-functions", (request, response) -> {
			return ValuationApi.getAllValuationFunctions(request, response);
		});

		service.post(apiPrefix + "/air-quality-data", (request, response) -> {
			
			request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));
			String layerName = getPostParameterValue(request, "name");
			Integer pollutantId = Integer.valueOf(getPostParameterValue(request, "pollutantId"));
			Integer gridId = Integer.valueOf(getPostParameterValue(request, "gridId"));
			String layerType = getPostParameterValue(request, "type");
			
			return AirQualityApi.postAirQualityLayer(request, layerName, pollutantId, gridId, layerType, response);
		});

		service.delete(apiPrefix + "/air-quality-data/:id", (request, response) -> {

			if(AirQualityApi.deleteAirQualityLayerDefinition(request, response)) {
				response.status(204);
			} else {
				response.status(404);
			}
			
			return "";
		});

		/*
		 * GET array of all health impact function result datasets
		 */	
		service.get(apiPrefix + "/results/hif", (req, res) -> {
			
			String bcoUserIdentifier = getOrSetOrExtendCookie(req, res);
			
			return HIFApi.getHifResultDatasets(req, res);

		});
		
		/*
		 * GET array of all health impact function definitions that are part of a hif result dataset
		 */	
		service.get(apiPrefix + "/results/hif/:id/functions", (req, res) -> {
			
			String bcoUserIdentifier = getOrSetOrExtendCookie(req, res);
			
			return HIFApi.getHifResultDatasetFunctions(req, res);

		});
		
		/*
		 * GET health impact function results from an analysis
		 * PARAMETERS:
		 *  :id (health impact function results dataset id)
		 *  gridId= (aggregate the results to another grid definition)
		 *  hifId= (filter results to those from one or more functions via comma delimited list)
		 *  page=
		 *  rowsPerPage=
		 *  sortBy=
		 *  descending=
		 *  filter=
		 *  
		 *  REQUEST HEADER Accept=text/csv will produce a CSV file
		 *  else, application/json response
		 */	
		service.get(apiPrefix + "/results/hif/:id/details", (req, res) -> {
			
			String bcoUserIdentifier = getOrSetOrExtendCookie(req, res);
			
			//TODO: Implement a new version of this that supports filtering, etc
			HIFApi.getHifResultDetails2(req, res);
			
			return null;

		});
		
		/*
		 * GET array of all health impact function result datasets
		 */	
		service.get(apiPrefix + "/results/valuation", (req, res) -> {
			
			String bcoUserIdentifier = getOrSetOrExtendCookie(req, res);
			
			return ValuationApi.getValuationResultDatasets(req, res);

		});
		
		/*
		 * GET array of all health impact function definitions that are part of a hif result dataset
		 */	
		service.get(apiPrefix + "/results/valuation/:id/functions", (req, res) -> {
			
			String bcoUserIdentifier = getOrSetOrExtendCookie(req, res);
			
			return ValuationApi.getValuationResultDatasetFunctions(req, res);

		});
		
		
		/*
		 * GET valuation results from an analysis
		 * PARAMETERS:
		 *  :id (valuation results dataset id)
		 *  gridId= (aggregate the results to another grid definition)
		 *  hifId= (filter results to those from one or more functions via comma delimited list)
		 *  vfId= (filter results to those from one or more functions via comma delimited list)
		 *  page=
		 *  rowsPerPage=
		 *  sortBy=
		 *  descending=
		 *  filter=
		 *  
		 *  REQUEST HEADER Accept=text/csv will produce a CSV file
		 *  else, application/json response
		 */	
		service.get(apiPrefix + "/results/valuation/:id/details", (req, res) -> {
			
			String bcoUserIdentifier = getOrSetOrExtendCookie(req, res);
			
			//TODO: Implement a new version of this that supports filtering, etc
			ValuationApi.getValuationResultDetails2(req, res);
			
			return null;

		});
		
		service.get(apiPrefix + "/tasks/:uuid/results", (req, res) -> {
			
			String bcoUserIdentifier = getOrSetOrExtendCookie(req, res);
			
			ApiUtil.getTaskResultDetails(req, res);
			
			return null;

		});

		service.delete(apiPrefix + "/tasks/:uuid/results", (req, res) -> {
			
			String bcoUserIdentifier = getOrSetOrExtendCookie(req, res);
			
			return ApiUtil.deleteTaskResults(req, res);

		});
		
		service.get(apiPrefix + "/tasks/pending", (req, res) -> {
			
			String bcoUserIdentifier = getOrSetOrExtendCookie(req, res);
			
			ObjectNode data = TaskQueue.getPendingTasks(bcoUserIdentifier, getPostParametersAsMap(req));
			res.type("application/json");
			return data;

		});
		
		service.get(apiPrefix + "/tasks/completed", (req, res) -> {
			
			String bcoUserIdentifier = getOrSetOrExtendCookie(req, res);
			
			ObjectNode data = TaskComplete.getCompletedTasks(bcoUserIdentifier, getPostParametersAsMap(req));
			res.type("application/json");
			return data;

		});
		
		service.post(apiPrefix + "/tasks", (req, res) -> {

			String bcoUserIdentifier = getOrSetOrExtendCookie(req, res);
			String body = req.body();
			
			ObjectMapper mapper = new ObjectMapper();
			JsonNode params = mapper.readTree(body);
			
			
			Task task = new Task();
			task.setName(params.get("name").asText());
			
			task.setParameters(body);
			task.setUuid(UUID.randomUUID().toString());
			task.setUserIdentifier(bcoUserIdentifier);
			task.setType(params.get("type").asText());
			
			TaskQueue.writeTaskToQueue(task);

			ObjectNode ret = mapper.createObjectNode();
			ret.put("task_uuid", task.getUuid());
			res.type("application/json");
			return ret;

		});
		
	}

}

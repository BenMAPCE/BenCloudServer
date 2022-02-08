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
import gov.epa.bencloud.server.jobs.KubernetesUtil;
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
		 * GET array of air quality surface definition objects
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
		 * GET array of air quality surface definitions flattened for tabular display
		 * PARAMETERS:
		 *  pollutantId= (optional)
		 *  page=
		 *  rowsPerPage=
		 *  sortBy=
		 *  descending=
		 *  filter=
		 */
		service.get(apiPrefix + "/air-quality-data-by-metric", (request, response) -> {
			return AirQualityApi.getAirQualityLayerDefinitionsByMetric(request, response);
		});

		/*
		 * GET a single air quality surface definition
		 * PARAMETERS:
		 *  :id
		 */
		service.get(apiPrefix + "/air-quality-data/:id", (request, response) -> {
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
		service.get(apiPrefix + "/air-quality-data/:id/contents", (request, response) -> {
			return AirQualityApi.getAirQualityLayerDetails(request, response);
		});

		/*
		 * POST a single air quality surface
		 * PARAMETERS:
		 *  csv file
		 *  name=
		 *  pollutantId=
		 *  gridId=
		 *  type= (model or monitor)
		 */
		service.post(apiPrefix + "/air-quality-data", (request, response) -> {
			
			request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));
			String layerName = getPostParameterValue(request, "name");
			Integer pollutantId = Integer.valueOf(getPostParameterValue(request, "pollutantId"));
			Integer gridId = Integer.valueOf(getPostParameterValue(request, "gridId"));
			String layerType = getPostParameterValue(request, "type");
			
			return AirQualityApi.postAirQualityLayer(request, layerName, pollutantId, gridId, layerType, response);
		});

		/*
		 * DELETE a single air quality surface definition
		 * PARAMETERS:
		 *  :id
		 */
		service.delete(apiPrefix + "/air-quality-data/:id", (request, response) -> {

			if(AirQualityApi.deleteAirQualityLayerDefinition(request, response)) {
				response.status(204);
			} else {
				response.status(404);
			}
			
			return "";
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
		service.get(apiPrefix + "/health-impact-functions/:id", (request, response) -> {
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
		
		/*
		 * GET a list of incidence datasets
		 */
		service.get(apiPrefix + "/incidence", (request, response) -> {
			return IncidenceApi.getAllIncidenceDatasets(response);
		});
		
		/*
		 * GET a list of prevalence datasets
		 */
		service.get(apiPrefix + "/prevalence", (request, response) -> {
			return IncidenceApi.getAllPrevalenceDatasets(response);
		});

		service.get(apiPrefix + "/valuation-functions", (request, response) -> {
			return ValuationApi.getAllValuationFunctions(request, response);
		});


		/*
		 * GET array of all health impact function result datasets
		 */	
		service.get(apiPrefix + "/health-impact-result-datasets", (req, res) -> {
			
			String bcoUserIdentifier = getOrSetOrExtendCookie(req, res);
			
			return HIFApi.getHifResultDatasets(req, res);

		});
		
		/*
		 * GET array of all health impact function definitions that are part of a hif result dataset
		 * id can be the hifResultDataset.id OR the task UUID
		 */	
		service.get(apiPrefix + "/health-impact-result-datasets/:id", (req, res) -> {
			
			String bcoUserIdentifier = getOrSetOrExtendCookie(req, res);
			
			return HIFApi.getHifResultDatasetFunctions(req, res);

		});
		
		/*
		 * GET health impact function results from an analysis
		 * PARAMETERS:
		 *  :id (health impact function results dataset id or task UUID)
		 *  gridId= (aggregate the results to another grid definition)
		 *  hifId= (filter results to those from one or more functions via comma delimited list)
		 *  page=
		 *  rowsPerPage=
		 *  sortBy=
		 *  descending=
		 *  filter=
		 *  
		 *  application/json response
		 */	
		service.get(apiPrefix + "/health-impact-result-datasets/:id/contents", (req, res) -> {
			
			String bcoUserIdentifier = getOrSetOrExtendCookie(req, res);
			
			//TODO: Implement a new version of this that supports filtering, etc
			HIFApi.getHifResultContents(req, res);
			
			return null;

		});
		
		/*
		 * GET health impact function results as a zip file from an analysis
		 * PARAMETERS:
		 *  :id (health impact function results dataset id or task UUID)
		 *  gridId= (comma delimited list. aggregate the results to one or more grid definition)
		 *  
		 */	
		service.get(apiPrefix + "/health-impact-result-datasets/:id/export", (req, res) -> {
			
			String bcoUserIdentifier = getOrSetOrExtendCookie(req, res);
			
			//TODO: Implement a new version of this that supports filtering, etc
			HIFApi.getHifResultExport(req, res);
			
			return null;

		});
		
		/*
		 * GET array of all health impact function result datasets
		 */	
		service.get(apiPrefix + "/valuation-result-datasets", (req, res) -> {
			
			String bcoUserIdentifier = getOrSetOrExtendCookie(req, res);
			
			return ValuationApi.getValuationResultDatasets(req, res);

		});
		
		/*
		 * GET array of all health impact function definitions that are part of a hif result dataset
		 * id can be the valuationResultDataset.id OR the task UUID
		 */	
		service.get(apiPrefix + "/valuation-result-datasets/:id", (req, res) -> {
			
			String bcoUserIdentifier = getOrSetOrExtendCookie(req, res);
			
			return ValuationApi.getValuationResultDatasetFunctions(req, res);

		});
		
		
		/*
		 * GET valuation results from an analysis
		 * PARAMETERS:
		 *  :id (valuation results dataset id or task UUID)
		 *  gridId= (aggregate the results to another grid definition)
		 *  hifId= (filter results to those from one or more functions via comma delimited list)
		 *  vfId= (filter results to those from one or more functions via comma delimited list)
		 *  page=
		 *  rowsPerPage=
		 *  sortBy=
		 *  descending=
		 *  filter=
		 *  
		 *   application/json response
		 */	
		service.get(apiPrefix + "/valuation-result-datasets/:id/contents", (req, res) -> {
			
			String bcoUserIdentifier = getOrSetOrExtendCookie(req, res);
			
			ValuationApi.getValuationResultContents(req, res);
			
			return null;

		});

		/*
		 * GET valuation results from an analysis
		 * PARAMETERS:
		 *  :id (valuation results dataset id or task UUID)
		 *  gridId= (comma delimited list. aggregate the results to one or more grid definitions)
		 */	
		service.get(apiPrefix + "/valuation-result-datasets/:id/export", (req, res) -> {
			
			String bcoUserIdentifier = getOrSetOrExtendCookie(req, res);
			
			ValuationApi.getValuationResultExport(req, res);
			
			return null;

		});
		
		service.delete(apiPrefix + "/tasks/:uuid", (req, res) -> {
			
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
			JsonNode parentTaskUuid = params.get("parent_task_uuid");
			if(parentTaskUuid != null) {
				task.setParentUuid(params.get("parent_task_uuid").asText());
				
			}
			task.setUserIdentifier(bcoUserIdentifier);
			task.setType(params.get("type").asText());
			
			TaskQueue.writeTaskToQueue(task);

			ObjectNode ret = mapper.createObjectNode();
			ret.put("task_uuid", task.getUuid());
			res.type("application/json");
			return ret;

		});
		
		service.get(apiPrefix + "/task-configs", (req, res) -> {
			
			String bcoUserIdentifier = getOrSetOrExtendCookie(req, res);
			
			Object data = CoreApi.getTaskConfigs(req, res);
			res.type("application/json");
			return data;

		});
		
		service.post(apiPrefix + "/task-configs", (req, res) -> {

			String bcoUserIdentifier = getOrSetOrExtendCookie(req, res);
			
			String ret = CoreApi.postTaskConfig(req, res);

			res.type("application/json");
			return ret;

		});
		
		/*
		 * The following are temporary calls the facilitate testing. They will be removed in the future.
		 */
		service.get(apiPrefix + "/admin/purge-results", (req, res) -> {
			
			String bcoUserIdentifier = getOrSetOrExtendCookie(req, res);
			
			Object data = CoreApi.getPurgeResults(req, res);
			res.type("application/json");
			return data;

		});
		
		service.get(apiPrefix + "/admin/run-job", (req, res) -> {
			
			String bcoUserIdentifier = getOrSetOrExtendCookie(req, res);
			
			Object data = KubernetesUtil.runJob(req, res);
			res.type("application/json");
			return data;

		});
		
		service.get(apiPrefix + "/admin/pods", (req, res) -> {
			
			String bcoUserIdentifier = getOrSetOrExtendCookie(req, res);
			
			Object data = KubernetesUtil.listPods(req, res);
			res.type("application/json");
			return data;

		});
		
		service.get(apiPrefix + "/admin/job-logs", (req, res) -> {
			
			String bcoUserIdentifier = getOrSetOrExtendCookie(req, res);
			
			Object data = KubernetesUtil.listJobLogs(req, res);
			res.type("application/json");
			return data;

		});
		
		service.get(apiPrefix + "/admin/delete-jobs", (req, res) -> {
			
			String bcoUserIdentifier = getOrSetOrExtendCookie(req, res);
			
			Object data = KubernetesUtil.deleteJobs(req, res);
			res.type("application/json");
			return data;

		});
		
	}

}

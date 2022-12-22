package gov.epa.bencloud.server.routes;

import java.util.UUID;

import org.pac4j.core.profile.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import gov.epa.bencloud.api.*;
import gov.epa.bencloud.api.util.ApiUtil;
import gov.epa.bencloud.server.tasks.TaskComplete;
import gov.epa.bencloud.server.tasks.TaskQueue;
import gov.epa.bencloud.server.tasks.model.Task;
import gov.epa.bencloud.server.util.ApplicationUtil;
import gov.epa.bencloud.server.jobs.KubernetesUtil;
import spark.Service;

public class ApiRoutes extends RoutesBase {

	private static final Logger log = LoggerFactory.getLogger(ApiRoutes.class);
	private Service service = null;
	private final String apiPrefix = "/api";
	
	public ApiRoutes(Service service){
		this.service = service;
		addRoutes();
	}


	private void addRoutes() {

		service.notFound((request, response) -> {
			response.type("application/json");
			return "{\"message\":\"Not found\"}";
		});

		service.internalServerError((request, response) -> {
			response.type("application/json");
			return "{\"message\":\"Internal server error\"}";
		});
		
		/*
		 * GET array of all grid definitions
		 */
		service.get(apiPrefix + "/grid-definitions", (request, response) -> {
			return GridDefinitionApi.getAllGridDefinitions(request, response, getUserProfile(request, response));
		});
		
		/*
		 * GET array of all pollutant definitions
		 */
		service.get(apiPrefix + "/pollutants", (request, response) -> {
			return PollutantApi.getAllPollutantDefinitions(request, response, getUserProfile(request, response));
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
			return AirQualityApi.getAirQualityLayerDefinitions(request, response, getUserProfile(request, response));
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
			return AirQualityApi.getAirQualityLayerDefinitionsByMetric(request, response, getUserProfile(request, response));
		});

		/*
		 * GET a single air quality surface definition
		 * PARAMETERS:
		 *  :id
		 */
		service.get(apiPrefix + "/air-quality-data/:id", (request, response) -> {
			return AirQualityApi.getAirQualityLayerDefinition(request, response, getUserProfile(request, response));
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
			return AirQualityApi.getAirQualityLayerDetails(request, response, getUserProfile(request, response));
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
			return AirQualityApi.postAirQualityLayer(request, response, getUserProfile(request, response));
		});

		/*
		 * DELETE a single air quality surface definition
		 * PARAMETERS:
		 *  :id
		 */
		service.delete(apiPrefix + "/air-quality-data/:id", (request, response) -> {

			return AirQualityApi.deleteAirQualityLayerDefinition(request, response, getUserProfile(request, response));

		});
		
		/*
		 * GET array of all population dataset definitions
		 */
		service.get(apiPrefix + "/population", (request, response) -> {
			return PopulationApi.getAllPopulationDatasets(request, response, getUserProfile(request, response));
		});
		
		/*
		 * GET array of all health impact function definitions
		 */
		service.get(apiPrefix + "/health-impact-functions", (request, response) -> {
			return HIFApi.getAllHealthImpactFunctions(request, response, getUserProfile(request, response));
		});

		/*
		 * GET a health impact function definition
		 */
		service.get(apiPrefix + "/health-impact-functions/:id", (request, response) -> {
			return HIFApi.getHealthImpactFunction(request, response, getUserProfile(request, response));
		});
		
		/*
		 * GET array of health impact function groups
		 * PARAMETERS:
		 *  pollutantId=
		 *  
		 *  Response will include array of function ids within each group
		 */	
		service.get(apiPrefix + "/health-impact-function-groups", (request, response) -> {
			return HIFApi.getAllHifGroups(request, response, getUserProfile(request, response));
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
			return HIFApi.getSelectedHifGroups(request, response, getUserProfile(request, response));
		});
		
		/*
		 * GET a list of incidence datasets
		 */
		service.get(apiPrefix + "/incidence", (request, response) -> {
			return IncidenceApi.getAllIncidenceDatasets(response, getUserProfile(request, response));
		});
		
		/*
		 * GET a list of prevalence datasets
		 */
		service.get(apiPrefix + "/prevalence", (request, response) -> {
			return IncidenceApi.getAllPrevalenceDatasets(request, response, getUserProfile(request, response));
		});

		service.get(apiPrefix + "/valuation-functions", (request, response) -> {
			return ValuationApi.getAllValuationFunctions(request, response, getUserProfile(request, response));
		});


		/*
		 * GET array of all health impact function result datasets
		 */	
		service.get(apiPrefix + "/health-impact-result-datasets", (request, response) -> {
			return HIFApi.getHifResultDatasets(request, response, getUserProfile(request, response));

		});
		
		/*
		 * GET array of all health impact function definitions that are part of a hif result dataset
		 * id can be the hifResultDataset.id OR the task UUID
		 */	
		service.get(apiPrefix + "/health-impact-result-datasets/:id", (request, response) -> {	
			return HIFApi.getHifResultDatasetFunctions(request, response, getUserProfile(request, response));

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
		service.get(apiPrefix + "/health-impact-result-datasets/:id/contents", (request, response) -> {
			//TODO: Implement a new version of this that supports filtering, etc
			HIFApi.getHifResultContents(request, response, getUserProfile(request, response));
			
			if(response.status() == 400) {
				return CoreApi.getErrorResponseInvalidId(request, response);
			}

			return null;
		});
		
		/*
		 * GET health impact function results as a zip file from an analysis
		 * PARAMETERS:
		 *  :id (health impact function results dataset id or task UUID)
		 *  gridId= (comma delimited list. aggregate the results to one or more grid definition)
		 *  
		 */	
		service.get(apiPrefix + "/health-impact-result-datasets/:id/export", (request, response) -> {
			//TODO: Implement a new version of this that supports filtering, etc
			HIFApi.getHifResultExport(request, response, getUserProfile(request, response));
			
			if(response.status() == 400) {
				return CoreApi.getErrorResponseInvalidId(request, response);
			}

			return null;
		});
		
		/*
		 * GET array of all health impact function result datasets
		 */	
		service.get(apiPrefix + "/valuation-result-datasets", (request, response) -> {
			return ValuationApi.getValuationResultDatasets(request, response, getUserProfile(request, response));

		});
		
		/*
		 * GET array of all health impact function definitions that are part of a hif result dataset
		 * id can be the valuationResultDataset.id OR the task UUID
		 */	
		service.get(apiPrefix + "/valuation-result-datasets/:id", (request, response) -> {
			return ValuationApi.getValuationResultDatasetFunctions(request, response, getUserProfile(request, response));

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
		service.get(apiPrefix + "/valuation-result-datasets/:id/contents", (request, response) -> {
			ValuationApi.getValuationResultContents(request, response, getUserProfile(request, response));
			
			if(response.status() == 400) {
				return CoreApi.getErrorResponseInvalidId(request, response);
			}

			return null;

		});

		/*
		 * GET valuation results from an analysis
		 * PARAMETERS:
		 *  :id (valuation results dataset id or task UUID)
		 *  gridId= (comma delimited list. aggregate the results to one or more grid definitions)
		 */	
		service.get(apiPrefix + "/valuation-result-datasets/:id/export", (request, response) -> {
			ValuationApi.getValuationResultExport(request, response, getUserProfile(request, response));
			
			if(response.status() == 400) {
				return CoreApi.getErrorResponseInvalidId(request, response);
			}

			return null;
		});
		
		service.delete(apiPrefix + "/tasks/:uuid", (request, response) -> {
			return ApiUtil.deleteTaskResults(request, response, getUserProfile(request, response));

		});
		
		service.get(apiPrefix + "/tasks/pending", (request, response) -> {
			ObjectNode data = TaskQueue.getPendingTasks(request, response, getUserProfile(request, response), getPostParametersAsMap(request));
			response.type("application/json");
			return data;

		});
		
		service.get(apiPrefix + "/tasks/completed", (request, response) -> {

			ObjectNode data = TaskComplete.getCompletedTasks(request, response, getUserProfile(request, response), getPostParametersAsMap(request));
			response.type("application/json");
			return data;

		});
		
		service.post(apiPrefix + "/tasks", (request, response) -> {

			String body = request.body();
			
			ObjectMapper mapper = new ObjectMapper();
			JsonNode params = mapper.readTree(body);

			try {
				UserProfile profile = getUserProfile(request, response).get();
				// As an interim protection against overloading the system, users can only have a maximum of 10 pending or completed tasks total
				int taskCount = CoreApi.getTotalTaskCountForUser(profile);			
				int maxTasks = 20;
				
				String sMaxTaskPerUser = ApplicationUtil.getProperty("default.max.tasks.per.user"); 
				
				try {
					maxTasks = Integer.parseInt(sMaxTaskPerUser);
				} catch(NumberFormatException e) {
					//If this is no set in the properties, we will use the default of 20.
				}
						
				if(maxTasks != 0 && taskCount >= maxTasks) {
					return CoreApi.getErrorResponse(request, response, 401, "You have reached the maximum of " + maxTasks + " tasks allowed per user. Please delete existing task results before submitting new tasks.");
				}
				
				Task task = new Task();
				task.setUserIdentifier(profile.getId());
				task.setType(params.get("type").asText());

				if(CoreApi.isValidTaskType(task.getType()) == false) {
					return CoreApi.getErrorResponseBadRequest(request, response);
				}
				
				task.setName(params.get("name").asText());
				task.setParameters(body);
				task.setUuid(UUID.randomUUID().toString());
				JsonNode parentTaskUuid = params.get("parent_task_uuid");
				if(parentTaskUuid != null) {
					task.setParentUuid(params.get("parent_task_uuid").asText());
				}
			
				TaskQueue.writeTaskToQueue(task);
				
				ObjectNode ret = mapper.createObjectNode();
				ret.put("task_uuid", task.getUuid());
				response.type("application/json");
				return ret;
			} catch(NullPointerException e) {
				e.printStackTrace();
				return CoreApi.getErrorResponseBadRequest(request, response);
			}

		});
		
		service.get(apiPrefix + "/task-configs", (request, response) -> {
			Object data = CoreApi.getTaskConfigs(request, response, getUserProfile(request, response));
			response.type("application/json");
			return data;

		});
		
		service.post(apiPrefix + "/task-configs", (request, response) -> {
			String ret = CoreApi.postTaskConfig(request, response, getUserProfile(request, response));

			if(response.status() == 400) {
                return CoreApi.getErrorResponseInvalidId(request, response);
			}

			if(response.status() == 250) {
				return CoreApi.getErrorResponse(request, response, 250, ret);
			}

			response.type("application/json");
			return ret;

		});
		
		/*
		 * DELETE selected template
		 */
		service.delete(apiPrefix + "/task-configs/:id", (request, response) -> {
			return CoreApi.deleteTaskConfig(request, response, getUserProfile(request, response));

		});

		service.get(apiPrefix + "/user", (request, response) -> {
			Object ret = CoreApi.getUserInfo(request, response, getUserProfile(request, response));

			response.type("application/json");
			return ret;

		});
		
		/*
		 * The following are temporary calls the facilitate testing. They will be removed in the future.
		 */
//		service.get(apiPrefix + "/admin/fix-wei-function", (request, response) -> {
//			Object data = CoreApi.getFixWeiFunction(request, response, getUserProfile(request, response));
//			response.type("application/json");
//			return data;
//
//		});
		service.get(apiPrefix + "/admin/purge-results", (request, response) -> {
			Object data = CoreApi.getPurgeResults(request, response, getUserProfile(request, response));
			response.type("application/json");
			return data;

		});
		
		service.get(apiPrefix + "/admin/pods", (request, response) -> {
			Object data = KubernetesUtil.listPods(request, response, getUserProfile(request, response));
			response.type("application/json");
			return data;

		});
		
		service.get(apiPrefix + "/admin/top-pods", (request, response) -> {
			Object data = KubernetesUtil.getTopPods(request, response, getUserProfile(request, response));
			response.type("application/json");
			return data;

		});
		
		service.get(apiPrefix + "/admin/job-logs", (request, response) -> {
			Object data = KubernetesUtil.listJobLogs(request, response, getUserProfile(request, response));
			response.type("application/json");
			return data;

		});
		
		service.get(apiPrefix + "/admin/delete-jobs", (request, response) -> {
			Object data = KubernetesUtil.deleteJobs(request, response, getUserProfile(request, response));
			response.type("application/json");
			return data;

		});
		
	}

}

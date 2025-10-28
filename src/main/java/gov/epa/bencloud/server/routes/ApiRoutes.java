package gov.epa.bencloud.server.routes;

import java.util.UUID;

import org.pac4j.core.profile.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import gov.epa.bencloud.api.*;
import gov.epa.bencloud.api.model.BatchTaskConfig;
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
		 * GET grid definitions and their row and col counts
		 */
		service.get(apiPrefix + "/grid-definitions-info", (request, response) -> {
			return GridDefinitionApi.getAllGridDefinitionsInfo(request, response, getUserProfile(request, response));
		});
		
		/**
		 * GET grid definition table info- row, col, and geometry
		 */
		service.get(apiPrefix + "/grid-definitions/:id/contents", (request, response) -> {
			return GridDefinitionApi.getGridGeometries(request, response, getUserProfile(request, response));
		});
		
		/*
		 * POST a single grid definition
		 * PARAMETERS:
		 *  zip file
		 *  name=
		 */
		service.post(apiPrefix + "/grid-definitions", (request, response) -> {
			return GridDefinitionApi.postGridDefinitionShapefile(request, response, getUserProfile(request, response));
		});
		
		/*
		 * DELETE a single grid definition
		 * PARAMETERS:
		 *  :id
		 */
		service.delete(apiPrefix + "/grid-definitions/:id", (request, response) -> {
			return GridDefinitionApi.deleteGridDefinition(request, response, getUserProfile(request, response));
		});

		/*
		 * Rename a single grid definition
		 * PARAMETERS:
		 *  :id
		 *  newName=
		 */
		service.put(apiPrefix + "/grid-definitions/:id", (request, response) -> {
			return GridDefinitionApi.renameGridDefinition(request, response, getUserProfile(request, response));
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
			return AirQualityApi.getAirQualityLayerDefinitions(request, response, getUserProfile(request, response),"");
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
			return AirQualityApi.getAirQualityLayerDefinitionsByMetric(request, response, getUserProfile(request, response),"");
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
		 * GET array of all health effect group definitions
		 */
		service.get(apiPrefix + "/health-effect-groups", (request, response) -> {
			return ValuationApi.getAllHealthEffectGroups(request, response, getUserProfile(request, response));
		});
		
		/*
		 * GET array of all health impact function definitions
		 */
		service.get(apiPrefix + "/health-impact-functions", (request, response) -> {
			return HIFApi.getAllHealthImpactFunctions(request, response, getUserProfile(request, response));
		});


		/*
		 * Archive a health impact function dataset
		 */
		service.post(apiPrefix + "/health-impact-function/:id", (request, response) -> {
			return HIFApi.archiveHealthImpactFunction(request, response, getUserProfile(request, response));
		});

		/*
		 * POST a health impact function dataset
		 */
		service.post(apiPrefix + "/health-impact-function-data", (request, response) -> {
			return HIFApi.postHealthImpactFunctionData(request, response, getUserProfile(request, response));
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
		 * DELETE a health impact function group
		 * PARAMETERS:
		 *  :id
		 */
		service.delete(apiPrefix + "/health-impact-function-groups/:id", (request, response) -> {
			return HIFApi.deleteHealthImpactFunctionGroup(request, response, getUserProfile(request, response));
		});

		
		/*
		 * GET array of exposure function groups
		 * PARAMETERS:
		 *  
		 *  Response will include array of function ids within each group
		 */	
		service.get(apiPrefix + "/exposure-function-groups", (request, response) -> {
			return ExposureApi.getAllExposureGroups(request, response, getUserProfile(request, response));
		});
		
		/*
		 * GET selected exposure function groups
		 * PARAMETERS:
		 *  
		 *  Response will include a list of function groups with details of each function
		 */	
		service.get(apiPrefix + "/exposure-function-groups/:ids", (request, response) -> {
			return ExposureApi.getSelectedExposureGroups(request, response, getUserProfile(request, response));
		});
		
		/*
		 * GET a partially populated batch task config

		 * PARAMETERS:
		 *  ids= comma separated list of health impact function group ids
		 *  popYear=
		 *  incidencePrevalenceDataset=
		 *  pollutantId=
		 *  baselineId=
		 *  scenarioId[1-N] = Repeated for each post-policy scenario AQ surface selected
		 *  popYear[1-N] = comma separated lsit of population years selected for each post-policy scenario
		 *  Parameters are used to filter the list of functions to only those relevant to the current analysis
		 *  and also to configure the incidence and prevalence datasets and years
		 *  Response will include array of complete function definitions within each group
		 *  
		 *  Note that the array of valuation functions that are nested within each HIF will not be populated at this point.
		 */	
		service.get(apiPrefix + "/batch-task-config", (request, response) -> {
			Object b = TaskApi.getBatchTaskConfig(request, response, getUserProfile(request, response));
			response.type("application/json");
			return b;
		}, objectMapper::writeValueAsString);

		// service.get(apiPrefix + "/batch-task-config-example-hif", (request, response) -> {
		// 	Object b = TaskApi.getBatchTaskConfigExampleHIF(request, response, getUserProfile(request, response));
		// 	response.type("application/json");
		// 	return b;
		// }, objectMapper::writeValueAsString);

		// service.get(apiPrefix + "/batch-task-config-example-exposure", (request, response) -> {
		// 	Object b = TaskApi.getBatchTaskConfigExampleExposure(request, response, getUserProfile(request, response));
		// 	response.type("application/json");
		// 	return b;
		// }, objectMapper::writeValueAsString);
		
		/*
		 * GET a list of incidence datasets including prevalence
		 * ..Should incidence include prevalence?
		 */
		service.get(apiPrefix + "/incidence", (request, response) -> {
			return IncidenceApi.getAllIncidencePrevalenceDatasets(request, response, getUserProfile(request, response));
		});

		/*
		 * POST an incidence dataset
		 */
		service.post(apiPrefix + "/incidence-data", (request, response) -> {
			return IncidenceApi.postIncidenceData(request, response, getUserProfile(request, response));
		});

			/*
		 * DELETE a single incidence dataset definition
		 * PARAMETERS:
		 *  :id
		 */
		service.delete(apiPrefix + "/incidence/:id", (request, response) -> {

			return IncidenceApi.deleteIncidenceDataset(request, response, getUserProfile(request, response));

		});
		/*
		 * GET all the contents of an incidence dataset
		 */
		service.get(apiPrefix + "/incidence/:id/contents", (request, response) -> {
			return IncidenceApi.getIncidenceDatasetDetails(request, response, getUserProfile(request, response));
		});

		/*
		 * GET all the years of an incidence dataset
		 */
		service.get(apiPrefix + "/incidence-dataset-years", (request, response) -> {
			return IncidenceApi.getIncidenceDatasetYears(request, response, getUserProfile(request, response));
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

		service.get(apiPrefix + "/valuation-functions-by-health-effect", (request, response) -> {
			return ValuationApi.getAllValuationFunctionsByHealthEffect(request, response, getUserProfile(request, response));
		});


		/*
		 * GET array of all health impact function result datasets
		 */	
		service.get(apiPrefix + "/health-impact-result-datasets", (request, response) -> {
			return HIFApi.getHifResultDatasets(request, response, getUserProfile(request, response));

		});

		/*
		 * Archive a valuation function
		 */
		service.post(apiPrefix + "/valuation-function/:id", (request, response) -> {
			return ValuationApi.archiveValuationFunction(request, response, getUserProfile(request, response));
		});

		/*
		 * POST a valuation function dataset
		 */
		service.post(apiPrefix + "/valuation-function-data", (request, response) -> {
			return ValuationApi.postValuationFunctionData(request, response, getUserProfile(request, response));
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
		/*
		service.get(apiPrefix + "/health-impact-result-datasets/:id/export", (request, response) -> {
			//TODO: Implement a new version of this that supports filtering, etc
			HIFApi.getHifResultExport(request, response, getUserProfile(request, response));
			
			if(response.status() == 400) {
				return CoreApi.getErrorResponseInvalidId(request, response);
			}

			return null;
		});
		*/
		
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
		/*
		service.get(apiPrefix + "/valuation-result-datasets/:id/export", (request, response) -> {
			ValuationApi.getValuationResultExport(request, response, getUserProfile(request, response));
			
			if(response.status() == 400) {
				return CoreApi.getErrorResponseInvalidId(request, response);
			}

			return null;
		});
		*/
		
		/*
		 * GET exposure function results from an analysis
		 * PARAMETERS:
		 *  :id (exposure function results dataset id or task UUID)
		 *  gridId= (aggregate the results to another grid definition)
		 *  efId= (filter results to those from one or more functions via comma delimited list)
		 *  page=
		 *  rowsPerPage=
		 *  sortBy=
		 *  descending=
		 *  filter=
		 *  
		 *  application/json response
		 */	
		service.get(apiPrefix + "/exposure-result-datasets/:id/contents", (request, response) -> {
			//TODO: Implement a new version of this that supports filtering, etc
			ExposureApi.getExposureResultContents(request, response, getUserProfile(request, response));
			
			if(response.status() == 400) {
				return CoreApi.getErrorResponseInvalidId(request, response);
			}

			return null;
		});
		
		/*
		 * GET exposure function results as a zip file from an analysis
		 * PARAMETERS:
		 *  :id (exposure results dataset id or task UUID)
		 *  gridId= (comma delimited list. aggregate the results to one or more grid definition)
		 *  
		 */	
		
		/*
		service.get(apiPrefix + "/exposure-result-datasets/:id/export", (request, response) -> {
			//TODO: Implement a new version of this that supports filtering, etc
			ExposureApi.getExposureResultExport(request, response, getUserProfile(request, response));
			
			if(response.status() == 400) {
				return CoreApi.getErrorResponseInvalidId(request, response);
			}

			return null;
		});
		*/
		
			// TODO: validateStatus in JS for non-200 codes

		/*
		 * Cancel a pending task
		 */
		service.put(apiPrefix + "/tasks/:uuid", (request, response) -> {
			return ApiUtil.cancelTaskAndResults(request, response, getUserProfile(request, response));
		});
		
		/*
		 * Cancel a pending batch task
		 */
		service.put(apiPrefix + "/batch-tasks/:id", (request, response) -> {
			return ApiUtil.cancelBatchTaskAndResults(request, response, getUserProfile(request, response));
		});


		/*
		 * Accepts a BatchTaskConfig object in json format
		 * Returns 200 if object submission was successful
		 * Else, returns the appropriate http error code along with a {"message":"string"} json object
		 */
		service.post(apiPrefix + "/batch-tasks", (request, response) -> {
			return TaskApi.postBatchTask(request, response, getUserProfile(request, response));

		});
		
		service.delete(apiPrefix + "/batch-tasks/:id", (request, response) -> {
			return ApiUtil.deleteBatchTaskResults(request, response, getUserProfile(request, response));

		});

		service.get(apiPrefix + "/batch-tasks/pending", (request, response) -> {
			ObjectNode data = TaskQueue.getPendingBatchTasks(request, response, getUserProfile(request, response), getPostParametersAsMap(request));
			response.type("application/json");
            return data;
        });
        
        service.get(apiPrefix + "/batch-tasks/completed", (request, response) -> {
            ObjectNode data = TaskComplete.getCompletedBatchTasks(request, response, getUserProfile(request, response), getPostParametersAsMap(request));
			response.type("application/json");
			return data;
		});
		
		service.get(apiPrefix + "/batch-tasks/:id/scenarios", (request, response) -> {
            ObjectNode data = TaskApi.getBatchTaskScenarios(request, response, getUserProfile(request, response));
            response.type("application/json");
            return data;
        });
		
		/*
		 * GET results as a zip file from an analysis
		 * PARAMETERS:
		 *  :id (batch task id)
		 *  includeHealthImpact (boolean accepts values true/false and 1/0, default to 0)
		 *  includeValuation (boolean accepts values true/false and 1/0, default to 0)
		 *  includeExposure (boolean accepts values true/false and 1/0, default to 0)
		 *  gridId= (comma delimited list. aggregate the results to one or more grid definition)
		 *  
		 */	
		service.get(apiPrefix + "/batch-tasks/:id/export", (request, response) -> {
			TaskApi.getResultExport(request, response, getUserProfile(request, response));
			
			if(response.status() == 400) {
				return CoreApi.getErrorResponseInvalidId(request, response);
			}

			return null;
		});
		
		/*
		 * POST request to export results as a zip file from an analysis
		 * PARAMETERS:
		 *  :id (batch task id)
		 *  includeHealthImpact (boolean accepts values true/false and 1/0, default to 0)
		 *  includeValuation (boolean accepts values true/false and 1/0, default to 0)
		 *  includeExposure (boolean accepts values true/false and 1/0, default to 0)
		 *  gridId= (comma delimited list. aggregate the results to one or more grid definition)
		 *  
		 */	
		service.post(apiPrefix + "/batch-tasks/:id/export", (request, response) -> {
			return TaskApi.postResultExportTask(request, response, getUserProfile(request, response));
		});
		
		service.get(apiPrefix + "/task-configs", (request, response) -> {
			Object data = TaskApi.getTaskConfigs(request, response, getUserProfile(request, response));
			response.type("application/json");
			return data;

		});
		
		/*
		 * add task
		 */
		
		service.post(apiPrefix + "/task-configs", (request, response) -> {
			String ret = TaskApi.postTaskConfig(request, response, getUserProfile(request, response));

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
			return TaskApi.deleteTaskConfig(request, response, getUserProfile(request, response));

		});
		
		/*
		 * Rename selected template
		 */
		service.put(apiPrefix + "/task-configs/:id", (request, response) -> {
			return TaskApi.renameTaskConfig(request, response, getUserProfile(request, response));

		});

		service.get(apiPrefix + "/user", (request, response) -> {
			Object ret = CoreApi.getUserInfo(request, response, getUserProfile(request, response));

			response.type("application/json");
			return ret;

		});
		
		service.get(apiPrefix + "/version", (request, response) -> {
			Object ret = CoreApi.getVersion(request, response, getUserProfile(request, response));

			response.type("application/json");
			return ret;

		});

		service.get(apiPrefix + "/banner", (request, response) -> {
			return CoreApi.getBanner(request, response, getUserProfile(request, response));
		});

		/*
		 * POST Updates the banner notification
		 * PARAMETERS:
		 *  message (string the message to be shown)
		 *  type (integer notification type 1=Info, 2=Warning, 3=Error)
		 *  enabled (boolean whether the banner is active)
		 */	
		service.post(apiPrefix + "/banner", (request, response) -> {
			return CoreApi.postBanner(request, response, getUserProfile(request, response));
		});
		
		/*
		 * DELETE a single file from the file store
		 * PARAMETERS:
		 *  :id
		 */
		service.delete(apiPrefix + "/admin/files/:id", (request, response) -> {

			return FilestoreApi.deleteFile(request, response, getUserProfile(request, response));

		});

		/*
		 * GET info on all files in the file store
		 */
		service.get(apiPrefix + "/admin/files", (request, response) -> {
			return FilestoreApi.getAllFiles(request, response, getUserProfile(request, response));
		});

		/*
		 * GET a single file from the file store
		 * PARAMETERS:
		 *  :id
		 */
		service.get(apiPrefix + "/files/:id", (request, response) -> {
			return FilestoreApi.getFile(request, response, getUserProfile(request, response));
		});
		/*
		 * GET a file ID for a export task
		 * PARAMETERS:
		 *  :id
		 */
		service.get(apiPrefix + "/task-complete/:id", (request, response) -> {
			return TaskApi.getExportFileID(request, response, getUserProfile(request, response));
		});
		
		
		

	}

		/*
		 * The following are temporary calls the facilitate testing. They will be removed in the future.
		 */
//		service.get(apiPrefix + "/admin/fix-wei-function", (request, response) -> {
//			Object data = CoreApi.getFixWeiFunction(request, response, getUserProfile(request, response));
//			response.type("application/json");
//			return data;
//
//		});
		// service.get(apiPrefix + "/admin/purge-results", (request, response) -> {
		// 	Object data = CoreApi.getPurgeResults(request, response, getUserProfile(request, response));
		// 	response.type("application/json");
		// 	return data;

		// });
		
		// service.get(apiPrefix + "/admin/pods", (request, response) -> {
		// 	Object data = KubernetesUtil.listPods(request, response, getUserProfile(request, response));
		// 	response.type("application/json");
		// 	return data;

		// });
		
		// service.get(apiPrefix + "/admin/top-pods", (request, response) -> {
		// 	Object data = KubernetesUtil.getTopPods(request, response, getUserProfile(request, response));
		// 	response.type("application/json");
		// 	return data;

		// });
		
		// service.get(apiPrefix + "/admin/job-logs", (request, response) -> {
		// 	Object data = KubernetesUtil.listJobLogs(request, response, getUserProfile(request, response));
		// 	response.type("application/json");
		// 	return data;

		// });
		
		// service.get(apiPrefix + "/admin/delete-jobs", (request, response) -> {
		// 	Object data = KubernetesUtil.deleteJobs(request, response, getUserProfile(request, response));
		// 	response.type("application/json");
		// 	return data;

		// });
		
	
	

}

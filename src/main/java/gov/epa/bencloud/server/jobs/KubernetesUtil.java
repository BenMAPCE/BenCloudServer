package gov.epa.bencloud.server.jobs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1EnvVar;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1JobBuilder;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1Status;
import io.kubernetes.client.util.ClientBuilder;
import spark.Request;
import spark.Response;

/*
 * Creates a simple run to complete job that computes π to 2000 places and prints it out.
 */
public class KubernetesUtil {
    private static final Logger logger = LoggerFactory.getLogger(KubernetesUtil.class);

    public static Object runJob(Request req, Response res) {
    	try {
	    	ApiClient client = ClientBuilder.cluster().build();
	    	
	    	Configuration.setDefaultApiClient(client);
	    	
	    	client.setDebugging(true);

	    	BatchV1Api batchApi = new BatchV1Api(client);
	    	//CoreV1Api coreApi = new CoreV1Api(client);
	    	
	    	/*
	      - name: API_CI_JOB_ID
            value: "$CI_JOB_ID"
          - name: API_CI_COMMIT_SHA
            value: "$CI_COMMIT_SHA"
          - name: API_CI_COMMIT_SHORT_SHA
            value: "$CI_COMMIT_SHORT_SHA"
          - name: API_CI_PROJECT_PATH_SLUG
            value: "$CI_PROJECT_PATH_SLUG"
          - name: API_CI_ENVIRONMENT_SLUG
            value: "$CI_ENVIRONMENT_SLUG"
	    	 */
	    	
	    	Map<String, String> envMap = System.getenv();
	    	
	    	List<V1EnvVar> envVariables = new ArrayList<V1EnvVar>();
	    	V1EnvVar envVar = new V1EnvVar();
	    	envVar.setName("REDEPLOY_META");
	    	envVar.setValue(envMap.get("REDEPLOY_META"));
	    	envVariables.add(envVar);
	    	
	    	envVar = new V1EnvVar();
	    	envVar.setName("TASK_UUID");
	    	envVar.setValue("THIS IS A TEST UUID");
	    	envVariables.add(envVar);
	    	
	    	//Pass all the db variables through to the job
	    	for(String varKey : envMap.keySet()) {
	    		if(varKey.startsWith("DB_")) {
	    			envVar = new V1EnvVar();
	    	    	envVar.setName(varKey);
	    	    	envVar.setValue(envMap.get(varKey));
	    	    	envVariables.add(envVar);
	    		}
	    	}
	    	
	    	logger.debug("api: " + batchApi.toString());
	    	V1Job body = new V1JobBuilder()
	    	  //.withApiVersion("batch/v1")
	    	  //.withKind("Job")
	    	  .withNewMetadata()
	    	    .withNamespace("benmap-dev")
	    	    .withGenerateName("bencloud-job-")
	    	    .withAnnotations(
	    	    	Map.of(
	    	    		"app.gitlab.com/app", envMap.get("API_CI_PROJECT_PATH_SLUG"), 
	    	    		"app.gitlab.com/env", envMap.get("API_CI_ENVIRONMENT_SLUG")
	    	    	))
	    	    .endMetadata()
	    	  .withNewSpec()
	    	    .withNewTemplate()
	    	      .withNewMetadata()
	    	        .addToLabels("name", "bencloud-taskrunner")
		    	    .withAnnotations(
		    	    	Map.of(
		    	    		"app.gitlab.com/app", envMap.get("API_CI_PROJECT_PATH_SLUG"), 
		    	    		"app.gitlab.com/env", envMap.get("API_CI_ENVIRONMENT_SLUG")
		    	    	))
	    	        .endMetadata()
	    	      .editOrNewSpec()
	    	        .addNewContainer()
	    	          .withName("taskrunner")
	    	          .withImage("registry.epa.gov/benmap/bencloudserver/bencloudtaskrunner:" + envMap.get("API_CI_COMMIT_SHORT_SHA"))
	    	          .withImagePullPolicy("Always")
	    	          .withEnv(envVariables)
	    	        .endContainer()
	    	        .addNewImagePullSecret()
	    	        	.withName("glcr-auth")
	    	        .endImagePullSecret()
	    	        .withRestartPolicy("Never")
	    	        .endSpec()
	    	      .endTemplate()
	    	    .endSpec()
	    	  .build(); 
	    	
	    	logger.debug("body: " + body.toString());

	    	V1Job createdJob = batchApi.createNamespacedJob("benmap-dev", body, "true", null, null);

	    	logger.debug("Job status: " + createdJob.getStatus());

	    	return createdJob.toString();
	    	
    	} catch (ApiException e) {
    		logger.error("Failed running test", e);
    		logger.error("Response body: " + e.getResponseBody());
    		return false;
    	} catch (IOException e) {
    		logger.error("Failed running test", e);
    		return false;		
    	}
    	
    }

	public static boolean runTaskAsJob(String taskUuid, String taskRunnerUuid) {
		try {
			ApiClient client = ClientBuilder.cluster().build();

			Configuration.setDefaultApiClient(client);

			client.setDebugging(true);

			BatchV1Api batchApi = new BatchV1Api(client);

			Map<String, String> envMap = System.getenv();

			List<V1EnvVar> envVariables = new ArrayList<V1EnvVar>();
			V1EnvVar envVar = new V1EnvVar();
			envVar.setName("REDEPLOY_META");
			envVar.setValue(envMap.get("REDEPLOY_META"));
			envVariables.add(envVar);

			envVar = new V1EnvVar();
			envVar.setName("TASK_UUID");
			envVar.setValue(taskUuid);
			envVariables.add(envVar);

			envVar = new V1EnvVar();
			envVar.setName("TASK_RUNNER_UUID");
			envVar.setValue(taskRunnerUuid);
			envVariables.add(envVar);

			// Pass all the db variables through to the job
			for (String varKey : envMap.keySet()) {
				if (varKey.startsWith("DB_")) {
					envVar = new V1EnvVar();
					envVar.setName(varKey);
					envVar.setValue(envMap.get(varKey));
					envVariables.add(envVar);
				}
			}

			V1Job body = new V1JobBuilder()
					.withNewMetadata()
						.withNamespace("benmap-dev")
						.withGenerateName("bencloud-job-")
						.withAnnotations(
							Map.of("app.gitlab.com/app", envMap.get("API_CI_PROJECT_PATH_SLUG"),
							"app.gitlab.com/env", envMap.get("API_CI_ENVIRONMENT_SLUG")))
					.endMetadata()
					.withNewSpec()
						.withNewTemplate()
							.withNewMetadata()
								.addToLabels("app", "bencloud-taskrunner")
								.withAnnotations(
									Map.of("app.gitlab.com/app", envMap.get("API_CI_PROJECT_PATH_SLUG"),
									"app.gitlab.com/env", envMap.get("API_CI_ENVIRONMENT_SLUG")))
							.endMetadata()
							.editOrNewSpec()
								.addNewContainer()
									.withName("taskrunner")
									.withImage("registry.epa.gov/benmap/bencloudserver/bencloudtaskrunner:" + envMap.get("API_CI_COMMIT_SHORT_SHA"))
									.withImagePullPolicy("Always")
									.withNewResources()
									.withRequests(
										Map.of("memory", new Quantity("6G"),
										"cpu", new Quantity("4")))
									.endResources()
									.withEnv(envVariables)
								.endContainer()
								.addNewImagePullSecret()
									.withName("glcr-auth")
								.endImagePullSecret()
								.withRestartPolicy("Never")
							.endSpec()
						.endTemplate()
					.endSpec()
					.build();

			V1Job createdJob = batchApi.createNamespacedJob("benmap-dev", body, "true", null, null);

			logger.debug("Job status: " + createdJob.getStatus());

			return true;

		} catch (ApiException e) {
			logger.error("Failed running task " + taskUuid, e);
			logger.error("Response body: " + e.getResponseBody());
			return false;
		} catch (IOException e) {
			logger.error("Failed running task " + taskUuid, e);
			return false;
		}
	}

	public static Object listPods(Request req, Response res) {
		try {
			ApiClient client = ClientBuilder.cluster().build();

			Configuration.setDefaultApiClient(client);

			client.setDebugging(true);

			CoreV1Api coreApi = new CoreV1Api(client);

			V1PodList list = coreApi.listNamespacedPod("benmap-dev", "true", null, null, null, null, null, null, null,
					null, null);

			return list.toString();

		} catch (ApiException e) {
			logger.error("Failed running test", e);
			logger.error("Response body: " + e.getResponseBody());
			return false;
		} catch (IOException e) {
			logger.error("Failed running test", e);
			return false;
		}

	}

	public static Object listJobLogs(Request req, Response res) {
		try {
			ApiClient client = ClientBuilder.cluster().build();

			Configuration.setDefaultApiClient(client);

			client.setDebugging(true);

			CoreV1Api coreApi = new CoreV1Api(client);

			StringBuilder sb = new StringBuilder();

			V1PodList list = coreApi.listNamespacedPod("benmap-dev", "true", null, null, null, null, null, null, null,
					null, null);
			for (V1Pod item : list.getItems()) {
				if (item.getMetadata().getName().startsWith("bencloud-job-")) {
					sb.append("\nName: " + item.getMetadata().getName());
					sb.append("\nLog: " + coreApi.readNamespacedPodLog(item.getMetadata().getName(),
							item.getMetadata().getNamespace(), null, null, null, null, "true", null, null, null, null));
				}
			}

			return "no job found";

		} catch (ApiException e) {
			logger.error("Failed running test", e);
			logger.error("Response body: " + e.getResponseBody());
			return false;
		} catch (IOException e) {
			logger.error("Failed running test", e);
			return false;
		}

	}

	public static Object deleteJobs(Request req, Response res) {
		try {
	    	ApiClient client = ClientBuilder.cluster().build();
	    	
	    	Configuration.setDefaultApiClient(client);
	    	
	    	client.setDebugging(true);
	
	    	BatchV1Api batchApi = new BatchV1Api(client);
	    	CoreV1Api coreApi = new CoreV1Api(client);
	    	
	    	StringBuilder sb = new StringBuilder();
	    	int numDeleted = 0;
	    	
			V1PodList list = coreApi.listNamespacedPod("benmap-dev", "true", null, null, null, null, null, null, null, null, null);
			for (V1Pod item : list.getItems()) {
				
				if(item.getMetadata().getName().startsWith("bencloud-job-")) {
					V1Pod response = coreApi.deleteNamespacedPod(
							  item.getMetadata().getName(),
							  item.getMetadata().getNamespace(), 
							  null, 
							  null, 
							  null, 
							  null, 
							  null, 
							  null ) ;
				logger.debug("DELETED: " + response.toString());
//				if(item.getMetadata().getName().startsWith("bencloud-job-")) {
//					V1Status response = batchApi.deleteNamespacedJob(
//							  item.getMetadata().getLabels().get("job-name"),
//							  item.getMetadata().getNamespace(), 
//							  null, 
//							  null, 
//							  null, 
//							  null, 
//							  null, 
//							  null ) ;
					
					numDeleted++;
				}
			}
	
			// Don't forget to try the Kubectl object
			
	    	return "Deleted " + numDeleted + " jobs";
	    	
		} catch (ApiException e) {
			logger.error("Failed running test", e);
			logger.error("Response body: " + e.getResponseBody());
			return false;
		} catch (IOException e) {
			logger.error("Failed running test", e);
			return false;		
		}
		
	}
}
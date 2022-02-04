package gov.epa.bencloud.server.jobs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class K8sApiExample {
    private static final Logger logger = LoggerFactory.getLogger(K8sApiExample.class);

    public static Object runJob(Request req, Response res) {
    	try {
	    	ApiClient client = ClientBuilder.cluster().build();
	    	
	    	Configuration.setDefaultApiClient(client);
	    	
	    	client.setDebugging(true);

	    	logger.debug("k8s base path: " + client.getBasePath());

	    	BatchV1Api batchApi = new BatchV1Api(client);
	    	//CoreV1Api coreApi = new CoreV1Api(client);
	    	
	    	List<V1EnvVar> envVariables = new ArrayList<V1EnvVar>();
	    	V1EnvVar envVar = new V1EnvVar();
	    	envVar.setName("REDEPLOY_META");
	    	envVar.setValue("$CI_JOB_ID-$CI_COMMIT_SHA");
	    	envVariables.add(envVar);
	    	
	    	logger.debug("api: " + batchApi.toString());
	    	V1Job body = new V1JobBuilder()
	    	  //.withApiVersion("batch/v1")
	    	  //.withKind("Job")
	    	  .withNewMetadata()
	    	    .withNamespace("benmap-dev")
	    	    .withGenerateName("bencloud-job-")
	    	    .withAnnotations(
	    	    	Map.of(
	    	    		"app.gitlab.com/app", "${CI_PROJECT_PATH_SLUG}", 
	    	    		"app.gitlab.com/env", "${CI_ENVIRONMENT_SLUG}"
	    	    	))
	    	    .endMetadata()
	    	  .withNewSpec()
	    	    .withNewTemplate()
	    	      .withNewMetadata()
	    	        .addToLabels("name", "bencloud-taskrunner")
		    	    .withAnnotations(
		    	    	Map.of(
		    	    		"app.gitlab.com/app", "${CI_PROJECT_PATH_SLUG}", 
		    	    		"app.gitlab.com/env", "${CI_ENVIRONMENT_SLUG}"
		    	    	))
	    	        .endMetadata()
	    	      .editOrNewSpec()
	    	        .addNewContainer()
	    	          .withName("taskrunner")
	    	          .withImage("registry.epa.gov/benmap/bencloudserver/bencloudtaskrunner:$CI_COMMIT_SHORT_SHA")
	    	          .withImagePullPolicy("Always")
	    	          .withEnv(envVariables)
	    	          .addNewCommand("task")
	    	          .addNewArg("--id")
	    	          .addNewArg("ABC123")
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

	public static Object listPods(Request req, Response res) {
		try {
	    	ApiClient client = ClientBuilder.cluster().build();
	    	
	    	Configuration.setDefaultApiClient(client);
	    	
	    	client.setDebugging(true);
	
	    	CoreV1Api coreApi = new CoreV1Api(client);
	    	
	
			V1PodList list = coreApi.listNamespacedPod("benmap-dev", "true", null, null, null, null, null, null, null, null, null);
			//for (V1Pod item : list.getItems()) {
				//logger.debug("pod: " + item.);
			//}
	
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
	    	
			V1PodList list = coreApi.listNamespacedPod("benmap-dev", "true", null, null, null, null, null, null, null, null, null);
			for (V1Pod item : list.getItems()) {
				if(item.getMetadata().getName().startsWith("bencloud-job-")) {
					sb.append("\nName: " + item.getMetadata().getName());
					sb.append("\nLog: " + coreApi.readNamespacedPodLog(item.getMetadata().getName(), item.getMetadata().getNamespace(), null, null, null, null, "true", null, null, null, null));
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
					V1Status response = batchApi.deleteNamespacedJob(
							  item.getMetadata().getName(), 
							  item.getMetadata().getNamespace(), 
							  null, 
							  null, 
							  null, 
							  null, 
							  null, 
							  null ) ;
					
					numDeleted++;
				}
			}
	
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
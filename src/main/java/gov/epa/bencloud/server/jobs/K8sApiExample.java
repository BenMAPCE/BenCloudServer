package gov.epa.bencloud.server.jobs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.models.V1EnvVar;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1JobBuilder;
import io.kubernetes.client.util.ClientBuilder;
import spark.Request;
import spark.Response;

/*
 * Creates a simple run to complete job that computes π to 2000 places and prints it out.
 */
public class K8sApiExample {
    private static final Logger logger = LoggerFactory.getLogger(K8sApiExample.class);

    public static Object runTest(Request req, Response res) {
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
	    	  .withApiVersion("batch/v1")
	    	  .withKind("Job")
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

	    	return true;
	    	
    	} catch (ApiException e) {
    		logger.error("Failed running test", e);
    		logger.error("Response body: " + e.getResponseBody());
    		return false;
    	}
    	
    }
}
package gov.epa.bencloud.server.jobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.BatchApi;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Job;

import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.Config;
import spark.Request;
import spark.Response;

/*
 * Creates a simple run to complete job that computes π to 2000 places and prints it out.
 */
public class K8sApiExample {
    private static final Logger logger = LoggerFactory.getLogger(K8sApiExample.class);

    public static Object runTest(Request req, Response res) {
    	try {
	    	//ApiClient client  = Config.defaultClient();
	    	ApiClient client = ClientBuilder.cluster().build();
	    	
	    	Configuration.setDefaultApiClient(client);
	    	
	    	client.setDebugging(true);
	    	
	    	logger.debug("k8s base path: " + client.getBasePath());
	    	
	        CoreV1Api api = new CoreV1Api();

	        V1PodList list = api.listNamespacedPod("benmap-dev", "true", null, null, null, null, null, null, null, null, null);
	        for (V1Pod item : list.getItems()) {
	          logger.debug("pod: " + item.getMetadata().getName());
	        }
	    	
	    	/*
	    	BatchV1Api api = new BatchV1Api(client);
	    	
	    	logger.debug("api: " + api.toString());
	    	V1Job body = new V1JobBuilder()
	    	  .withApiVersion("apps/v1")
	    	  .withKind("Job")
	    	  .withNewMetadata()
	    	    .withNamespace("benmap-dev")
	    	    .withName("bencloud-job")
	    	    .endMetadata()
	    	  .withNewSpec()
	    	    .withNewTemplate()
	    	      .withNewMetadata()
	    	        .addToLabels("name", "hif-analysis")
	    	        .endMetadata()
	    	      .editOrNewSpec()
	    	        .addNewContainer()
	    	          .withName("main")
	    	          .withImage("task-runner")
	    	          .addNewCommand("task")
	    	          .addNewArg("--id")
	    	          .addNewArg("ABC123")
	    	          .endContainer()
	    	        .withRestartPolicy("Never")
	    	        .endSpec()
	    	      .endTemplate()
	    	    .endSpec()
	    	  .build(); 
	    	
	    	logger.debug("body: " + body.toString());
	    	
	    	V1Job createdJob = api.createNamespacedJob("benmap-dev", body, "true", null, null);
	    	
	    	logger.debug("Job status: " + createdJob.getStatus());
	    	*/
	    	return true;
	    	
    	} catch (Exception e) {
    		logger.error("Failed running test", e);
    		return false;
    	}
    	
    }
}
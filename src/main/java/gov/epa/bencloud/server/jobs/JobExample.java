package gov.epa.bencloud.server.jobs;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1JobBuilder;
import io.kubernetes.client.util.Config;
import spark.Request;
import spark.Response;

/*
 * Creates a simple run to complete job that computes π to 2000 places and prints it out.
 */
public class JobExample {
    private static final Logger logger = LoggerFactory.getLogger(JobExample.class);

    public static Object runJob(Request req, Response res) {
    	try {
	    	ApiClient client  = Config.defaultClient();
	    	client.setDebugging(true);
	    	
	    	logger.debug("k8s base path: " + client.getBasePath());
	    	
	    	BatchV1Api api = new BatchV1Api(client);
	    	logger.debug("api: " + api.toString());

	    	V1Job body = new V1JobBuilder()
	    	  .withNewMetadata()
	    	    .withNamespace("bencloud-jobs")
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
	    	logger.debug("getApiVersion: " + body.getApiVersion());
	    	logger.debug("getKind: " + body.getKind());
	    	logger.debug("body: " + body.toString());
	    	
	    	V1Job createdJob = api.createNamespacedJob("bencloud-jobs", body, "true", null, null);
	    	return true;
	    	
    	} catch (Exception e) {
    		logger.error("Failed creating job", e);
    		return false;
    	}
    	
    }
}
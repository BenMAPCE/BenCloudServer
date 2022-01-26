package gov.epa.bencloud.server.jobs;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1JobBuilder;
import io.kubernetes.client.util.Config;

/*
 * Creates a simple run to complete job that computes π to 2000 places and prints it out.
 */
public class JobExample {
    private static final Logger logger = LoggerFactory.getLogger(JobExample.class);

    public static void runJob() {
    	try {
	    	ApiClient client  = Config.defaultClient();
	    	BatchV1Api api = new BatchV1Api(client);
	    	V1Job body = new V1JobBuilder()
	    	  .withNewMetadata()
	    	    .withNamespace("report-jobs")
	    	    .withName("payroll-report-job")
	    	    .endMetadata()
	    	  .withNewSpec()
	    	    .withNewTemplate()
	    	      .withNewMetadata()
	    	        .addToLabels("name", "payroll-report")
	    	        .endMetadata()
	    	      .editOrNewSpec()
	    	        .addNewContainer()
	    	          .withName("main")
	    	          .withImage("report-runner")
	    	          .addNewCommand("payroll")
	    	          .addNewArg("--date")
	    	          .addNewArg("2021-05-01")
	    	          .endContainer()
	    	        .withRestartPolicy("Never")
	    	        .endSpec()
	    	      .endTemplate()
	    	    .endSpec()
	    	  .build(); 
	    	V1Job createdJob = api.createNamespacedJob("report-jobs", body, null, null, null);
    	} catch (Exception e) {
    		logger.error("Failed creating job", e);
    	}
    }
}
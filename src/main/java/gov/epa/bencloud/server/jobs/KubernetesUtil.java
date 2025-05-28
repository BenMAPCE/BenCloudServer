package gov.epa.bencloud.server.jobs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
import io.kubernetes.client.openapi.apis.CoreV1Api.APIcreateNamespacedPersistentVolumeClaimRequest;
import io.kubernetes.client.openapi.models.V1EnvVar;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1JobBuilder;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaim;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaimBuilder;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaimVolumeSource;
import io.kubernetes.client.openapi.models.V1VolumeMount;
import io.kubernetes.client.openapi.models.V1VolumeMountBuilder;
import io.kubernetes.client.proto.V1.PersistentVolumeClaim;
import io.kubernetes.client.util.ClientBuilder;

/*
 * Starts a K8s job to handle a specific task
 */
public class KubernetesUtil {
    private static final Logger logger = LoggerFactory.getLogger(KubernetesUtil.class);

	public static boolean runTaskAsJob(String taskUuid, String taskRunnerUuid) {
		try {
			ApiClient client = ClientBuilder.cluster().build();

			Configuration.setDefaultApiClient(client);

			client.setDebugging(true);

			BatchV1Api batchApi = new BatchV1Api(client);
			CoreV1Api coreApi = new CoreV1Api();
			
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
			
			envVar = new V1EnvVar();
			envVar.setName("DEFENDER_APP_ID");
			envVar.setValue("bencloud-taskrunner");
			envVariables.add(envVar);

			// Pass all the db and geoserver variables through to the job
			for (String varKey : envMap.keySet()) {
				if (varKey.startsWith("DB_") || varKey.startsWith("GEOSERVER_")) {
					envVar = new V1EnvVar();
					envVar.setName(varKey);
					envVar.setValue(envMap.get(varKey));
					envVariables.add(envVar);
				}
			}

			V1Job body = new V1JobBuilder()
					.withNewMetadata()
						.withNamespace(envMap.get("K8S_NAMESPACE"))
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
									.withImage("registry.epa.gov/benmap/bencloudserver/bencloudtaskrunner/app-defender:" + envMap.get("API_CI_COMMIT_SHORT_SHA"))
									.withImagePullPolicy("Always")
									.addNewVolumeMount()
										.withName("bencloud-server-pv")
										.withMountPath("/app-data")
									.endVolumeMount()
									.withNewResources()
										.withRequests(
												Map.of("memory", new Quantity("24G"),
														"cpu", new Quantity("8")))
									.endResources()
									.withEnv(envVariables)
								.endContainer()
								.addNewVolume()
									.withName("bencloud-server-pv")
									.withPersistentVolumeClaim(new V1PersistentVolumeClaimVolumeSource().claimName("bencloud-server-pv-claim"))
								.endVolume()
								.addNewImagePullSecret()
									.withName("glcr-auth")
								.endImagePullSecret()
								.withRestartPolicy("Never")
							.endSpec()
						.endTemplate()
						.withTtlSecondsAfterFinished(60*5) //Let the job hang around for 5 minutes so we can review the log. Can reduce this once we're capturing logs

					.endSpec()
					.build();
			
//			APIcreateNamespacedPersistentVolumeClaimRequest createdPvcRequest = coreApi.createNamespacedPersistentVolumeClaim( envMap.get("K8S_NAMESPACE"), persistentVolumeClaim);
//			V1PersistentVolumeClaim createdPvc = createdPvcRequest.execute();
//			logger.info("PVC created: " + createdPvc.getMetadata().getName());

			V1Job createdJob = batchApi.createNamespacedJob(envMap.get("K8S_NAMESPACE"), body).execute();
			logger.info("Job created: " + createdJob.getMetadata().getName());
			
			logger.debug("Starting job for " + taskUuid);
			
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

	/* Obsolete code
	 * 
	public static Object listPods(Request req, Response res, Optional<UserProfile> userProfile) {
		try {
			ApiClient client = ClientBuilder.cluster().build();

			Configuration.setDefaultApiClient(client);
	    	Map<String, String> envMap = System.getenv();
			client.setDebugging(true);

			CoreV1Api coreApi = new CoreV1Api(client);

			V1PodList list = coreApi.listNamespacedPod(envMap.get("K8S_NAMESPACE"), "true", null, null, null, null, null, null, null,
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

	public static Object listJobLogs(Request req, Response res, Optional<UserProfile> userProfile) {
		try {
			ApiClient client = ClientBuilder.cluster().build();

			Configuration.setDefaultApiClient(client);
	    	Map<String, String> envMap = System.getenv();
			client.setDebugging(true);

			CoreV1Api coreApi = new CoreV1Api(client);

			StringBuilder sb = new StringBuilder();

			V1PodList list = coreApi.listNamespacedPod(envMap.get("K8S_NAMESPACE"), "true", null, null, null, null, null, null, null,
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

	public static Object deleteJobs(Request req, Response res, Optional<UserProfile> userProfile) {
		try {
	    	ApiClient client = ClientBuilder.cluster().build();
	    	
	    	Configuration.setDefaultApiClient(client);
	    	Map<String, String> envMap = System.getenv();
	    	client.setDebugging(true);
	
	    	BatchV1Api batchApi = new BatchV1Api(client);
	    	CoreV1Api coreApi = new CoreV1Api(client);
	    	
	    	StringBuilder sb = new StringBuilder();
	    	int numDeleted = 0;
	    	
			V1PodList list = coreApi.listNamespacedPod(envMap.get("K8S_NAMESPACE"), "true", null, null, null, null, null, null, null, null, null);
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
	
	public static Object getTopPods(Request req, Response res, Optional<UserProfile> userProfile) {
		try {

			ObjectMapper mapper = new ObjectMapper();
			ArrayNode ret = mapper.createArrayNode();
	    	ApiClient client = ClientBuilder.cluster().build();
	    	
	    	Configuration.setDefaultApiClient(client);
	    	
			List<Pair<V1Node, NodeMetrics>> metrics = Kubectl.top(V1Node.class, NodeMetrics.class).metric("cpu")
					.execute();

			for (Pair<V1Node, NodeMetrics> metric : metrics) {
				ObjectNode node = ret.addObject();
				node.put("name", metric.getKey().getMetadata().getName());
				ArrayNode metricArray = node.putArray("metrics");

				for (String usageKey : metric.getValue().getUsage().keySet()) {
					ObjectNode metricEntry = metricArray.addObject();
					metricEntry.put(usageKey, metric.getValue().getUsage().get(usageKey).toString());
				}
			}
			return ret;
		} catch (KubectlException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "Error: " + e.getMessage();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "Error: " + e.getMessage();
		}
	}
	*/
}
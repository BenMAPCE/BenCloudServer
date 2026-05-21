package gov.epa.bencloud.server.jobs;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.epa.bencloud.Constants;
import gov.epa.bencloud.server.util.ApplicationUtil;

/**
 * Resolves the CPU and memory allocation for a Kubernetes task pod based on the
 * task type. Defaults are baked in here; deployments can override any value via
 * properties of the form:
 *
 *   task.resources.&lt;slug&gt;.cpu
 *   task.resources.&lt;slug&gt;.memory
 *   task.resources.&lt;slug&gt;.memory_limit
 *
 * where &lt;slug&gt; is the task type lowercased with spaces replaced by underscores
 * (e.g. "Result Export" → "result_export"). Unknown task types fall back to the
 * "default" spec, which mirrors the historical 32G / 8 CPU allocation.
 *
 * Memory units follow Kubernetes Quantity syntax (e.g. "8G", "32Gi").
 */
public final class TaskResources {

	private static final Logger log = LoggerFactory.getLogger(TaskResources.class);

	public static final class ResourceSpec {
		public final String cpu;
		public final String memory;
		public final String memoryLimit;

		public ResourceSpec(String cpu, String memory, String memoryLimit) {
			this.cpu = cpu;
			this.memory = memory;
			this.memoryLimit = memoryLimit;
		}
	}

	private static final String DEFAULT_KEY = "default";

	private static final Map<String, ResourceSpec> DEFAULTS = new HashMap<>();
	static {
		// Conservative fallback — matches the historical hard-coded allocation.
		DEFAULTS.put(DEFAULT_KEY,                    new ResourceSpec("8", "32G", "32G"));

		DEFAULTS.put(Constants.TASK_TYPE_HIF,        new ResourceSpec("8", "32G", "32G"));
		DEFAULTS.put(Constants.TASK_TYPE_EXPOSURE,   new ResourceSpec("8", "32G", "32G"));
		DEFAULTS.put(Constants.TASK_TYPE_VALUATION,  new ResourceSpec("4", "16G", "16G"));
		DEFAULTS.put(Constants.TASK_TYPE_GRID_IMPORT,new ResourceSpec("4", "16G", "16G"));
		DEFAULTS.put(Constants.TASK_TYPE_AQ_IMPORT,  new ResourceSpec("2", "8G",  "8G"));
		// Result Export streams via JOOQ cursor, but the container also has to fit the JVM
		// (-XX:MaxRAMPercentage=75 ⇒ ~12G heap at 16G), Defender tracer overhead, metaspace
		// (up to 1G), native GeoTools/PostGIS, and direct buffers. 8G turned out to be too
		// tight on large exports (OOMKill at peak); 16G leaves real headroom.
		DEFAULTS.put(Constants.TASK_TYPE_RESULT_EXPORT, new ResourceSpec("2", "16G", "16G"));
	}

	private TaskResources() {}

	/**
	 * Returns the resource spec for the given task type, after applying any
	 * property overrides. Never returns null.
	 */
	public static ResourceSpec forType(String taskType) {
		ResourceSpec base = DEFAULTS.getOrDefault(taskType, DEFAULTS.get(DEFAULT_KEY));
		ResourceSpec fallback = DEFAULTS.get(DEFAULT_KEY);

		String slug = slug(taskType);
		String cpu = override("task.resources." + slug + ".cpu", base.cpu);
		String memory = override("task.resources." + slug + ".memory", base.memory);
		String memoryLimit = override("task.resources." + slug + ".memory_limit", base.memoryLimit);

		if (base == fallback && !DEFAULTS.containsKey(taskType)) {
			log.warn("No resource spec defined for task type '{}'; using default {}/{}",
					taskType, cpu, memory);
		}

		return new ResourceSpec(cpu, memory, memoryLimit);
	}

	private static String override(String propertyKey, String fallback) {
		String value = ApplicationUtil.getProperty(propertyKey);
		return (value == null || value.isBlank()) ? fallback : value.trim();
	}

	private static String slug(String taskType) {
		if (taskType == null) {
			return DEFAULT_KEY;
		}
		return taskType.trim().toLowerCase().replace(' ', '_');
	}
}

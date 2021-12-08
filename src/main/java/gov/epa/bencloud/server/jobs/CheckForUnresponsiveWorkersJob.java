package gov.epa.bencloud.server.jobs;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.epa.bencloud.server.tasks.TaskWorker;

@DisallowConcurrentExecution
public class CheckForUnresponsiveWorkersJob implements Job {

	private static final Logger log = LoggerFactory.getLogger(CheckForUnresponsiveWorkersJob.class);
	
	public CheckForUnresponsiveWorkersJob() {
	}

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {

		TaskWorker.checkForUnresponsiveWorkers();

	}
}

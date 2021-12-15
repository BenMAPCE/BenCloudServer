package gov.epa.bencloud.server.jobs;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.epa.bencloud.server.tasks.TaskManager;
import gov.epa.bencloud.server.tasks.TaskQueue;

@DisallowConcurrentExecution
public class ReadFromQueueJob implements Job {

	private static final Logger log = LoggerFactory.getLogger(ReadFromQueueJob.class);
	
	public ReadFromQueueJob() {
	}

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {

		//System.out.println("ReadFromQueueJob execute");
		
		String uuid = TaskQueue.getTaskFromQueue();
		
		if (null != uuid) {
			TaskManager.processTask(uuid);
		}
	}
}

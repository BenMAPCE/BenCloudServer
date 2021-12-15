package gov.epa.bencloud.server.jobs;

import static org.quartz.JobBuilder.newJob;

import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.epa.bencloud.server.database.PooledDataSource;

public class JobsUtil {
	private static final Logger log = LoggerFactory.getLogger(JobsUtil.class);
	
	public static void startJobScheduler() {
		
      try {
			Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
			scheduler.start();

		    JobDetail readFromQueueJob = newJob(ReadFromQueueJob.class)
		    		.withIdentity("readQueueJob", "bencloud")
		    		.build();

		    Trigger readFromQueueJobTrigger = TriggerBuilder.newTrigger()
		    	    .withIdentity("readQueueTrigger", "bencloud")
		    	    .withSchedule(SimpleScheduleBuilder
		    	    		.simpleSchedule().withIntervalInSeconds(10).repeatForever()
		    	    		.withMisfireHandlingInstructionNextWithRemainingCount())
		    	    .build();
		    
		    scheduler.scheduleJob(readFromQueueJob, readFromQueueJobTrigger);

		    
		    JobDetail checkForUnresponsiveWorkersJob = newJob(CheckForUnresponsiveWorkersJob.class)
		    		.withIdentity("checkForUnresponsiveWorkersJob", "bencloud")
		    		.build();

		    Trigger checkForUnresponsiveWorkersJobTrigger = TriggerBuilder.newTrigger()
		    	    .withIdentity("checkForUnresponsiveWorkersJobTrigger", "bencloud")
		    	    .withSchedule(SimpleScheduleBuilder
		    	    		.simpleSchedule().withIntervalInSeconds(30).repeatForever()
		    	    		.withMisfireHandlingInstructionNextWithRemainingCount())
		    	    .build();

		    scheduler.scheduleJob(checkForUnresponsiveWorkersJob, checkForUnresponsiveWorkersJobTrigger);

		} catch (SchedulerException e1) {
			log.error("Error starting job scheduler", e1);
		}
	}
}

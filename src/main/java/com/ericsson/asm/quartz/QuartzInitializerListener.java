package com.ericsson.asm.quartz;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.jboss.logging.Logger;
import org.quartz.*;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.StdSchedulerFactory;

import com.ericsson.asm.common.CommonConstants;
import com.ericsson.asm.poll.PollForFiles;

public class QuartzInitializerListener implements ServletContextListener {

	private static final int erbsPollJobScheduleTime = 15;
	private static final Logger LOGGER = Logger
			.getLogger(QuartzInitializerListener.class);

	@Override
	public void contextInitialized(final ServletContextEvent sce) {
		try {
			final Scheduler scheduler = new StdSchedulerFactory()
					.getScheduler();
			LOGGER.info("Scheduler initialized");
			final JobDetail job = new JobDetailImpl();
			// using the same class i.e PollForFiles create different jobs for
			// different NE type
			((JobDetailImpl) job).setJobClass(PollForFiles.class);
			((JobDetailImpl) job).setName(CommonConstants.ERBS_POLL_JOB_NAME);
			((JobDetailImpl) job)
					.setDescription(CommonConstants.ERBS_POLL_JOB_NAME);

			final Trigger trigger = TriggerBuilder
					.newTrigger()
					.withIdentity("PollForFile", "group1")
					.withSchedule(
							SimpleScheduleBuilder
									.simpleSchedule()
									.withIntervalInMinutes(
											erbsPollJobScheduleTime)
									.repeatForever()).build();

			scheduler.start();
			LOGGER.info("Scheduling " + CommonConstants.ERBS_POLL_JOB_NAME);
			scheduler.scheduleJob(job, trigger);

		} catch (Exception e) {
			LOGGER.error("Exception occured while scheduling task(s)", e);
		}
	}

	@Override
	public void contextDestroyed(final ServletContextEvent sce) {
	}

}

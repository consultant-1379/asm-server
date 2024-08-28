/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2013
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.asm.poll;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.logging.Logger;
import org.quartz.*;

import com.ericsson.asm.common.CommonConstants;

/**
 * This class will poll for available pm file and place the parsing request on
 * JMS topic for the file
 * 
 * @author xshassi
 * 
 */
public class PollForFiles implements Job {

	// modifying access level to protected for testing
	protected File mainDirectory;
	final private Properties prop = new Properties();
	private String path;
	protected MessageWriter msgWriter;
	protected boolean isInitialized = false;

	private static final Logger LOGGER = Logger.getLogger(PollForFiles.class);

	@Override
	public void execute(final JobExecutionContext jobExecutionContext)
			throws JobExecutionException {
		long startTime = System.currentTimeMillis();
		try {
			LOGGER.info("PollForFiles invoked at " + new Date());
			if (!isInitialized) {
				initialize(jobExecutionContext);
			}
			executeJob();
		} catch (Exception e) {
			LOGGER.error(
					"Unexpected behaviour while executing PollForErbsFile.", e);
		}
		long timeTaken = System.currentTimeMillis() - startTime;
		LOGGER.debug("Time taken by PollForErbsFile is " + timeTaken);
	}

	/**
	 * Logic for the Poll
	 * 
	 * @throws JMSException
	 */
	protected void executeJob() throws JMSException {
		int noOfFilesFound = 0;
		if (mainDirectory.exists() && mainDirectory.isDirectory()) {
			// get the dir1, dir2 names
			final File[] dirNames = mainDirectory.listFiles();
			for (File subDir : dirNames) {
				// list all files
				final File[] symLinks = subDir.listFiles();
				for (File symLink : symLinks) {
					if (symLink.isFile()) {
						/*
						 * get the names of all files and send the path to
						 * PMServiceBean for processing
						 */

						/*
						 * since this task is for polling erbs file and the
						 * loaction is for erbs stats sym links we can use
						 * nodeType as erbs
						 */
						LOGGER.debug("Found file " + symLink.getName()
								+ ", JMS message will be published.");
						msgWriter.placeMessageOnJMS(symLink);
						noOfFilesFound++;
					}
				}
			}
		}
		if (noOfFilesFound == 0) {
			LOGGER.info("No file found for parsing");
		}
	}

	protected void initialize(final JobExecutionContext jobExecutionContext)
			throws IOException {
		// method signature changed to protected for testing
		prop.load(Thread.currentThread().getContextClassLoader()
				.getResourceAsStream("PollProperties.properties"));
		if (jobExecutionContext.getJobDetail().getDescription()
				.equalsIgnoreCase(CommonConstants.ERBS_POLL_JOB_NAME)) {
			path = prop.getProperty("ERBS_PATH");
		}
		mainDirectory = new File(path);
		msgWriter = new MessageWriter();
		isInitialized = true;
	}

	/**
	 * This inner class is responsible for managing activities related to
	 * processing mapMessage and placing the message on JMS Topic
	 * 
	 * @author xshassi
	 * 
	 */
	protected class MessageWriter {
		protected MessageWriter() {
		}

		/**
		 * This method will place message on JMS Topic for the file
		 * 
		 * @param pmFile
		 * @throws JMSException
		 */
		protected void placeMessageOnJMS(final File pmFile) throws JMSException {
			// method access set to protected for testing
			TopicConnection connection = null;
			TopicSession session = null;
			try {
				final Context context = new InitialContext();
				final TopicConnectionFactory factory = (TopicConnectionFactory) context
						.lookup("/ConnectionFactory");
				connection = factory.createTopicConnection();
				session = connection.createTopicSession(false,
						QueueSession.AUTO_ACKNOWLEDGE);
				final Topic topic = (Topic) context
						.lookup("topic/parseRequestTopic");
				final TopicPublisher sender = session.createPublisher(topic);

				final MapMessage message = session.createMapMessage();
				populateMapMessage(message, pmFile);
				sender.publish(message);
			} catch (Exception e) {
				LOGGER.error(
						"Unexpected behaviour while publishing message for available file "
								+ pmFile.getName(), e);
			} finally {
				if (connection != null) {
					connection.close();
				}
				if (session != null) {
					session.close();
				}
			}

		}

		/**
		 * This method populates MapMessage with the File details
		 * 
		 * @param message
		 * @param pmFile
		 * @throws JMSException
		 */
		private void populateMapMessage(final MapMessage message,
				final File pmFile) throws JMSException {
			message.setString(CommonConstants.MESSAGE_TYPE,
					CommonConstants.SUCCESS);
			message.setString(CommonConstants.FILE_PATH,
					pmFile.getAbsolutePath());
			message.setString(CommonConstants.SCANNER_TYPE,
					CommonConstants.DEFAULT_SCANNER_TYPE);
			message.setString(CommonConstants.NODE_NAME, "");
		}
	}
}

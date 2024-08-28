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
package com.ericsson.asm.service;

import java.io.IOException;
import java.util.*;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.logging.Logger;

import com.ericsson.asm.common.CommonConstants;
import com.ericsson.asm.parser.Parser;
import com.ericsson.asm.parser.XMLParser;

/**
 * This bean listens to topic - topic/parseRequestTopic, request for parsing for
 * the requested file, filter the result and publish the filtered parser as Map
 * on topic/parseResponseTopic Message-Driven Bean implementation class for:
 * PMServiceBean
 */
@MessageDriven(activationConfig = {
		@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
		@ActivationConfigProperty(propertyName = "destination", propertyValue = "topic/parseRequestTopic") }, mappedName = "topic/parseRequestTopic")
public class PMParserService implements MessageListener {

	private InitialContext initialContext;

	private Topic publishTopic;
	private TopicSession publishSession;
	private TopicConnection publishConnection;

	private Parser parser;
	private Map<String, Map<String, Object>> pmDataMap;

	private static final Logger LOGGER = Logger
			.getLogger(PMParserService.class);

	/**
	 * Default constructor.
	 */
	public PMParserService() {
		parser = new XMLParser();
		// Since all required files to process are in xml format, only
		// implementation is hard-coded this can be injected using CDI
	}

	public void setupPubSub() throws JMSException, NamingException {
		initialContext = new InitialContext();

		//String unUsedVariable = "";

		TopicConnectionFactory tcf = (TopicConnectionFactory) initialContext
				.lookup("/ConnectionFactory");
		publishConnection = tcf.createTopicConnection();
		publishTopic = (Topic) initialContext
				.lookup("topic/parseResponseTopic");
		publishSession = publishConnection.createTopicSession(false,
				TopicSession.AUTO_ACKNOWLEDGE);
		publishConnection.start();
	}

	public void stop() throws JMSException {
		LOGGER.info("stop method invoked on PMPArserService, topicConnection and topicSession will be closed now");
		publishConnection.close();
		publishSession.close();
	}

	/**
	 * @see MessageListener#onMessage(Message)
	 */
	@Override
	public void onMessage(final Message message) {
		try {
			LOGGER.info("Request received for Parsing file");
			MapMessage msg;
			String pathOfFile;
			if (message instanceof MapMessage) {
				msg = (MapMessage) message;

				// process file
				pathOfFile = msg.getString(CommonConstants.FILE_PATH);
				LOGGER.debug("Invoking parser for file " + pathOfFile);
				pmDataMap = parser.parseThisFile(pathOfFile,
						CommonConstants.ERBS_NODE_TYPE);
				if (pmDataMap != null && pmDataMap.size() > 0) {
					// non empty map, filter and publish result over JMS
					// Topic
					LOGGER.debug("Received non-empty map from parser, filtering map for relevant counters");
					// filter the map first
					filterPmDataMapForCounterName();
					// process filtered map
					processPmDataMap();
					if (pmDataMap.size() > 0) {
						LOGGER.debug("Filtered map, will publish map on JMS now");
						publishMessage();
					} else {
						LOGGER.info("After filtering, the pmDataMap is empty");
					}
				} else {
					// no need to publish empty map
					LOGGER.warn("Received empty map from parser, will not place response on JMS topic");
				}

			}
		} catch (Exception e) {
			LOGGER.error(
					"Unexpected Exception occured while processing parsing request",
					e);
		}

	}

	public void ejbCreate() {
		try {
			setupPubSub();
		} catch (JMSException e) {
			e.printStackTrace();
		} catch (NamingException e) {
			e.printStackTrace();
		}
		// TODO remove following catch block
		catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public void ejbRemove() {
		try {
			publishConnection.close();
			publishSession.close();
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param publishSession
	 *            the publishSession to set
	 */
	public void setPublishSession(final TopicSession inPublishSession) {
		this.publishSession = inPublishSession;
	}

	/**
	 * @param publishTopic
	 *            the publishTopic to set
	 */
	public void setPublishTopic(final Topic inPublishTopic) {
		this.publishTopic = inPublishTopic;
	}

	/**
	 * This method processes the values of counters(if required)
	 * 
	 * @throws IOException
	 * 
	 */
	private void processPmDataMap() throws IOException {
		final Properties counterInfoProp = new Properties();
		counterInfoProp.load(Thread.currentThread().getContextClassLoader()
				.getResourceAsStream(CommonConstants.COUNTER_INFO_PROPERTIES));
		final int noOfBins = Integer.parseInt((String) counterInfoProp
				.get("noOfBins"));

		final List<String> pdfCountersList = new ArrayList<String>();
		for (Object key : counterInfoProp.keySet()) {
			if (((String) counterInfoProp.get(key)).equalsIgnoreCase("pdf")) {
				pdfCountersList.add((String) key);
			}
		}
		for (String moid : pmDataMap.keySet()) {
			final Map<String, Object> counterDataMap = pmDataMap.get(moid);
			for (String counterName : counterDataMap.keySet()) {
				if (pdfCountersList.contains(counterName)) {
					final String[] pdfValueMap = processPdfCounterValue(
							(String) counterDataMap.get(counterName), noOfBins);
					// overwrite existing unprocessed value with processed value
					pmDataMap.get(moid).put(counterName, pdfValueMap);
				}
			}
		}
	}

	/**
	 * This method processes string value of pdfCounter to Map of binNumber to
	 * value format
	 * 
	 * @param pdfCounterValue
	 * @param noOfBins
	 * @return
	 */
	private String[] processPdfCounterValue(final String pdfCounterValue,
			final int noOfBins) {
		String[] pdfCounterValueArray = new String[noOfBins];
		final String[] pdfTokens = pdfCounterValue
				.split(CommonConstants.PDF_VALUE_SEPARATOR);
		final int noOfPasses = Integer.parseInt(pdfTokens[0]);
		if (pdfTokens.length % 2 == 0) {
			LOGGER.warn("PDF Counter value is not proper");
		}
		int pdfCounterIndex = 1;

		// populate the values from pdfCounterValue
		for (int pairCounter = 0; pairCounter < noOfPasses; pairCounter++) {
			if (pdfTokens.length > (pdfCounterIndex + 1)) {
				if (pdfTokens[pdfCounterIndex + 1] == null) {
					pdfCounterValueArray[pairCounter] = CommonConstants.ZERO_STRING;
				} else {
					pdfCounterValueArray[pairCounter] = pdfTokens[pdfCounterIndex + 1];
				}
				pdfCounterIndex = pdfCounterIndex + 2;
			} else {
				// skip iterations
				break;
			}

		}
		// populate other bins with 0 as value
		for (int i = 0; i < noOfBins; i = i + 1) {
			if (pdfCounterValueArray[i] == null) {
				// value for this bin is not in map
				pdfCounterValueArray[i] = CommonConstants.ZERO_STRING;
			}
		}
		return pdfCounterValueArray;
	}

	/**
	 * This method creates and publishes the MapMessage to topic
	 * topic/parseResponseTopic
	 * 
	 * @throws JMSException
	 */
	private void publishMessage() throws JMSException {
		final TopicPublisher sender = publishSession
				.createPublisher(publishTopic);
		// since map is not serialized , we need to convert moIdMap to hashmap

		final ObjectMessage objMessage = publishSession.createObjectMessage();
		final HashMap<String, HashMap<String, Object>> serializedPmDataMap = new HashMap<String, HashMap<String, Object>>();
		for (String moId : pmDataMap.keySet()) {
			final Map<String, Object> rawCounterDataMap = pmDataMap.get(moId);
			final HashMap<String, Object> counterDataMap = rawCounterDataMap instanceof HashMap ? (HashMap<String, Object>) rawCounterDataMap
					: new HashMap<String, Object>(rawCounterDataMap);
			serializedPmDataMap.put(moId, counterDataMap);
		}
		objMessage.setObject(serializedPmDataMap);
		objMessage.setStringProperty("subNetwork", parser.getSubNetwork());
		objMessage.setStringProperty("ropEndTime", parser.getRopEndTime());
		sender.publish(objMessage);
		sender.close();
	}

	/**
	 * This method filters moIdMap and retains only required counters
	 * 
	 * @throws IOException
	 */
	private void filterPmDataMapForCounterName() throws IOException {
		Map<String, Map<String, Object>> filteredMoIdMap = null;
		final Properties countersNameProperties = new Properties();
		final List<String> requiredCounterNames = loadCounterNames(countersNameProperties);
		for (String moId : pmDataMap.keySet()) {
			filteredMoIdMap = new HashMap<String, Map<String, Object>>();
			for (String counterName : pmDataMap.get(moId).keySet()) {
				final boolean isCurrentCounterRequired = requiredCounterNames
						.contains(counterName);
				if (isCurrentCounterRequired) {
					Map<String, Object> tempMap = filteredMoIdMap.get(moId);
					// if required add entry for moid in filteredMoIdMap
					if (tempMap == null) {
						// add moid and counter nam-value pair
						tempMap = new HashMap<String, Object>();
						tempMap.put(counterName,
								pmDataMap.get(moId).get(counterName));
						filteredMoIdMap.put(moId, tempMap);
					} else {
						// just add counter name-value pair to existing map
						tempMap.put(counterName,
								pmDataMap.get(moId).get(counterName));
						filteredMoIdMap.put(moId, tempMap);
					}
				}
			}
		}
		pmDataMap = filteredMoIdMap;
	}

	/**
	 * @param counterName
	 * @return
	 * @throws IOException
	 */
	private List<String> loadCounterNames(final Properties counterName)
			throws IOException {
		final List<String> counterNameList = new LinkedList<String>();
		counterName.load(Thread.currentThread().getContextClassLoader()
				.getResourceAsStream(CommonConstants.COUNTER_NAME_PROPERTIES));
		for (Object key : counterName.keySet()) {
			counterNameList.add((String) counterName.get(key));
		}
		return counterNameList;
	}

	/**
	 * @param inParser
	 */
	public void setParser(final Parser inParser) {
		this.parser = inParser;
	}
}

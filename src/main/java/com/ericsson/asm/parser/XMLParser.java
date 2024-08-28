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
package com.ericsson.asm.parser;

import java.io.File;
import java.io.IOException;
import java.util.*;

import javax.xml.parsers.*;

import org.jboss.logging.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.ericsson.asm.common.CommonConstants;

public class XMLParser extends DefaultHandler implements Parser {

	private final Map<String, Map<String, Object>> pmDataMap = new HashMap<String, Map<String, Object>>();
	private Map<String, Object> counterMap;
	private final List<String> counterNameList = new LinkedList<String>();
	private final List<String> counterValueList = new LinkedList<String>();

	private String moIdValue;
	private String nodeType;

	private String tempTagValue;
	private boolean isInSameTag;
	private boolean isSubNetworkNameRequired = true;
	private boolean isRopEndTimeRequired = true;

	private String pathOfFile;
	private String subNetwork;
	private String ropEndTime;

	private String COUNTER_NAME_TAG;// NOPMD
	private String COUNTER_VALUE_TAG;// NOPMD
	private String MOID_TAG;// NOPMD
	private String PARENT_TAG;// NOPMD
	private String VALUE_SET_TAG;// NOPMD
	private String SUBNETWORK_TAG;// NOPMD
	private String ROP_END_TIME_TAG;// NOPMD

	private static final Logger LOGGER = Logger.getLogger(XMLParser.class);

	/**
	 * This method parses the XML available at fileLocation, for the counterName
	 * and returns a Map, key is MO_ID and value is Map of counterName and
	 * counterValue
	 * 
	 * @param fileLocation
	 * @param inNodeType
	 * @return
	 */
	@Override
	public Map<String, Map<String, Object>> parseThisFile(
			final String fileLocation, final String inNodeType) {
		LOGGER.info("Request received for parsing XML file");
		pathOfFile = fileLocation;
		nodeType = inNodeType;
		try {
			populateTagValues();
			final SAXParserFactory parserfactory = SAXParserFactory
					.newInstance();
			final SAXParser parser = parserfactory.newSAXParser();
			parser.parse(new File(pathOfFile), this);
			if (pmDataMap.size() == 0) {
				// there is no COUNTER_VALUE_TAG and/or COUNTER_NAME_TAG
				LOGGER.error("Configured XML Tag(s) not found in file "
						+ pathOfFile);
			}
		} catch (ParserConfigurationException e) {
			// parser cannot be created which satisfies the requested
			// configuration.
			// LOGGER.
			LOGGER.error(
					"Unable to create parser with requested configuration", e);
		} catch (SAXException e) {
			// SAX error
			LOGGER.error("SAX error while creating parser", e);
		} catch (IOException e) {
			LOGGER.error("Unexpected IOException while parsing file.", e);
			// IO Error occurred
		} catch (Exception e) {
			LOGGER.error("Unexpected Exception while parsing file", e);
		}
		return pmDataMap;
	}

	@Override
	public void startElement(final String url, final String localName,
			final String elementName, final Attributes attributes) {
		if (elementName.equalsIgnoreCase(PARENT_TAG)) {
			isInSameTag = true;
		}
	}

	@Override
	public void endElement(final String uriName, final String localName,
			final String elementName) {
		if (isInSameTag || isSubNetworkNameRequired || isRopEndTimeRequired) {
			if (elementName.equalsIgnoreCase(COUNTER_NAME_TAG)) {
				counterNameList.add(tempTagValue);
			} else if (elementName.equalsIgnoreCase(COUNTER_VALUE_TAG)) {
				counterValueList.add(tempTagValue);
			} else if (elementName.equalsIgnoreCase(MOID_TAG)) {
				// store MO ID
				moIdValue = tempTagValue;
			} else if (elementName.equalsIgnoreCase(VALUE_SET_TAG)) {
				// there may be multiple moid/mv tags in a mi tag
				// prepare map for this moid
				prepareCounterMapForCurrentMoId();
				// clear counterValueList
				counterValueList.clear();
				// add counterMap for current mo id to moIdMap
				addCounterMapToMoIdMap();
			} else if (elementName.equalsIgnoreCase(PARENT_TAG)) {
				// Parent tag ends
				isInSameTag = false;
				counterNameList.clear();
			} else if (elementName.equalsIgnoreCase(SUBNETWORK_TAG)) {
				subNetwork = tempTagValue;
				isSubNetworkNameRequired = false;
			} else if (elementName.equalsIgnoreCase(ROP_END_TIME_TAG)) {
				ropEndTime = tempTagValue;
				isRopEndTimeRequired = false;
			}
		}
	}

	@Override
	public void characters(final char[] ch, final int start, final int length) {
		if (isInSameTag || isSubNetworkNameRequired || isRopEndTimeRequired) {
			tempTagValue = new String(ch, start, length);
		}
	}

	/**
	 * This method prepares a @Map<String, String> for counterName to
	 * counterValue
	 * 
	 */
	private void prepareCounterMapForCurrentMoId() {
		if (counterValueList.size() > 0) {
			// counterMap is cleared for every MO ID
			counterMap = new HashMap<String, Object>();
			for (int i = 0; i < counterNameList.size(); i++) {
				counterMap.put(counterNameList.get(i), counterValueList.get(i));
			}
		}
	}

	/**
	 * This method adds the counterMap corresponding to a MO id to MO id map
	 * 
	 */
	private void addCounterMapToMoIdMap() {
		if (counterMap != null && counterMap.size() > 0) {
			pmDataMap.put(moIdValue, counterMap);
		}
	}

	/**
	 * This method populates tag names based on node type
	 * 
	 * @throws IOException
	 */
	private void populateTagValues() throws IOException {
		String fileName = null;
		if (nodeType.equalsIgnoreCase(CommonConstants.ERBS_NODE_TYPE)) {
			fileName = CommonConstants.ERBS_PM_TAGS_FILE;
		}
		final Properties prop = new Properties();
		prop.load(Thread.currentThread().getContextClassLoader()
				.getResourceAsStream(fileName));
		COUNTER_NAME_TAG = prop.getProperty("COUNTER_NAME_TAG");
		COUNTER_VALUE_TAG = prop.getProperty("COUNTER_VALUE_TAG");
		MOID_TAG = prop.getProperty("MOID_TAG");
		PARENT_TAG = prop.getProperty("PARENT_TAG");
		VALUE_SET_TAG = prop.getProperty("VALUE_SET_TAG");
		SUBNETWORK_TAG = prop.getProperty("SUBNETWORK_TAG");
		ROP_END_TIME_TAG = prop.getProperty("ROP_END_TIME_TAG");
	}

	/**
	 * @return the subNetwork
	 */
	public String getSubNetwork() {
		return subNetwork;
	}

	/**
	 * @return the ropEndTime
	 */
	public String getRopEndTime() {
		return ropEndTime;
	}
}

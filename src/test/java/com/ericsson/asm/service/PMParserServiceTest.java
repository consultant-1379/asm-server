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

import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import javax.jms.*;

import org.hornetq.jms.client.HornetQTopic;
import org.jmock.Mockery;
import org.junit.*;

import com.ericsson.asm.common.CommonConstants;
import com.ericsson.asm.parser.Parser;

public class PMParserServiceTest {

	private PMParserService objUnderTest;

	private final Mockery mockery = new Mockery();
	private final Parser mockParser = mockery.mock(Parser.class);
	private final TopicSession mockTopicSession = mockery
			.mock(TopicSession.class);
	private final MapMessage messageMock = mockery.mock(MapMessage.class);
	private final TopicPublisher mockTopicPublisher = mockery
			.mock(TopicPublisher.class);
	private Topic sampleTopic;

	private Map<String, Map<String, String>> mockedMap;

	@Before
	public void setUp() {
		objUnderTest = new PMParserService();
		try {
			sampleTopic = new HornetQTopic("");
			prepareMockedMap();
			setExpectations();
			objUnderTest.setParser(mockParser);
			objUnderTest.setPublishSession(mockTopicSession);
			objUnderTest.setPublishTopic(sampleTopic);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void prepareMockedMap() {
		mockedMap = new HashMap<String, Map<String, String>>();
		final Map<String, String> tempCounterMap = new HashMap<String, String>();
		tempCounterMap.put("testCounterName1", "testCounterValue1");
		tempCounterMap.put("testCounterName2", "testCounterValue2");
		tempCounterMap.put("testCounterName3", "testCounterValue3");
		tempCounterMap.put("testCounterName4", "testCounterValue4");
		mockedMap.put("testMoId", tempCounterMap);
	}

	private void setExpectations() {
		try {
			mockery.checking(new org.jmock.Expectations()

			{
				{
					oneOf(mockParser).parseThisFile(with(any(String.class)),
							with(any(String.class)));
					will(returnValue((mockedMap)));
				}
				{
					oneOf(messageMock).getString(
							with(CommonConstants.MESSAGE_TYPE));
					will(returnValue(CommonConstants.SUCCESS));
				}
				{
					oneOf(messageMock).getString(
							with(CommonConstants.FILE_PATH));
					will(returnValue("/testPath"));
				}
				{
					oneOf(mockTopicPublisher).publish(
							with(any(ObjectMessage.class)));
				}
				{
					oneOf(mockTopicPublisher).close();
				}
				{
					oneOf(mockTopicSession).createPublisher(
							with(any(Topic.class)));
					will(returnValue(mockTopicPublisher));
				}
				{
					oneOf(mockTopicSession).createObjectMessage(
							with(any(HashMap.class))); // NOPMD
				}
			});
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@After
	public void tearDown() throws Exception {
		objUnderTest = null;
	}

	@Test
	public void testOnMessage() {
		try {
			objUnderTest.onMessage(messageMock);
		} catch (Exception e) {
			e.printStackTrace();
			fail("exception occured");
		}
	}

}

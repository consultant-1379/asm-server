package com.ericsson.asm.parser;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Map;

import org.junit.*;

import com.ericsson.asm.common.TestUtil;

public class XMLParserTest {

	private XMLParser objUnderTest;

	@Before
	public void setUp() throws Exception {
		objUnderTest = new XMLParser();
	}

	@After
	public void tearDown() throws Exception {
		objUnderTest = null;
	}

	@Test
	public void testParseXMLWithOneMoId() {
		try {
			final String filePath = TestUtil.copyFile(
					"testParserWithOneMoId.xml", null);
			final Map<String, Map<String, Object>> retVal = objUnderTest
					.parseThisFile(filePath, "erbs");
			assertTrue(retVal.size() == 1);
			Map<String, Object> counterNameValueMap = null;
			for (String moid : retVal.keySet()) {
				counterNameValueMap = retVal.get(moid);
				assertTrue(counterNameValueMap.size() == 11);
			}
			assertTrue(objUnderTest.getSubNetwork().equalsIgnoreCase(
					"SubNetwork=ONRM_ROOT_MO_R,MeContext=ERBS011"));
			assertTrue(objUnderTest.getRopEndTime().equalsIgnoreCase(
					"20130922093000Z"));
			final File createdFile = new File(filePath);
			createdFile.delete();
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception occured");
		}
	}

	@Test
	public void testParseXMLWithMultipleMoId() {
		try {
			final String filePath = TestUtil.copyFile(
					"testParserWithMultipleMoId.xml", null);
			final Map<String, Map<String, Object>> retVal = objUnderTest
					.parseThisFile(filePath, "erbs");
			assertTrue(retVal.size() == 2);
			Map<String, Object> counterNameValueMap = null;
			for (String moid : retVal.keySet()) {
				counterNameValueMap = retVal.get(moid);
				assertTrue((counterNameValueMap.size() == 11)
						|| (counterNameValueMap.size() == 9));
			}
			final File createdFile = new File(filePath);
			createdFile.delete();
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception occured");
		}
	}

	@Test
	public void testParseXMLWithOneMoIdWrongCounterValue() {
		try {
			final String filePath = TestUtil.copyFile(
					"testParserWithOneMoIdWrongCounterValue.xml", null);
			final Map<String, Map<String, Object>> retVal = objUnderTest
					.parseThisFile(filePath, "erbs");
			assertTrue(retVal.size() == 1);
			Map<String, Object> counterNameValueMap = null;
			for (final String moid : retVal.keySet()) {
				counterNameValueMap = retVal.get(moid);
				assertTrue(counterNameValueMap.size() == 11);
			}
			final File createdFile = new File(filePath);
			createdFile.delete();
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception occured");
		}

	}
}
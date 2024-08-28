package com.ericsson.asm.poll;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import javax.jms.JMSException;

import org.junit.*;
import org.quartz.JobExecutionContext;

import com.ericsson.asm.common.TestUtil;

public class PollForFilesTest {
	private PollForFiles objUnderTest;

	@Before
	public void setUp() throws Exception {
		objUnderTest = new StubbedPollForErbsFile();
	}

	@After
	public void tearDown() throws Exception {
		objUnderTest = null;
	}

	@Test
	public void testExecuteJob() {
		try {
			objUnderTest.initialize(null);
			objUnderTest.executeJob();
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception occured");
		}
	}

	private class StubbedPollForErbsFile extends PollForFiles {
		private File subDir;

		private String mainPath;

		@Override
		protected void initialize(final JobExecutionContext jobExecutionContext)
				throws IOException {
			msgWriter = new StubbedMessageWriter();
			final String homePath = System.getProperty("user.home");
			mainPath = homePath + "/test";
			mainDirectory = new File(mainPath);
			mainDirectory.mkdir();
			// create sub-directory dir1
			subDir = new File(mainPath + "/dir1");
			subDir.mkdir();
			TestUtil.copyFile("testParserWithMultipleMoId.xml", mainPath
					+ "/dir1/test.xml");
		}

		protected class StubbedMessageWriter extends MessageWriter {
			@Override
			protected void placeMessageOnJMS(final File pmFile)
					throws JMSException {
				// don't place message on JMS, delete files created in
				// initialize method
				final File xmlFile = new File(mainPath + "/dir1/test.xml");
				xmlFile.delete();
				subDir.delete();
				mainDirectory.delete();
			}

		}
	}
}
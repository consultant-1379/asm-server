/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2012
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.asm.common;

import java.io.*;

public abstract class TestUtil {

	public static String copyFile(final String testResourceXml,
			final String newFilePath) throws IOException, FileNotFoundException {
		final String osName = System.getProperty("os.name");
		File testXml = null;
		if (osName.contains("Windows")) {
			// any Windows OS
			if (newFilePath == null) {
				testXml = new File("C:\\test.xml");
			} else {
				testXml = new File(newFilePath);
			}
		} else if (osName.contains("Linux")) {
			// linux
			if (newFilePath == null) {
				testXml = new File("/tmp/test.xml");
			} else {
				testXml = new File(newFilePath);
			}
		}
		if (testXml != null) {
			testXml.createNewFile();
			final InputStream inStream = Thread.currentThread()
					.getContextClassLoader()
					.getResourceAsStream(testResourceXml);
			final OutputStream outStream = new FileOutputStream(testXml);
			final byte[] buffer = new byte[1024];
			int length;
			// copy the file content in bytes
			while ((length = inStream.read(buffer)) > 0) {
				outStream.write(buffer, 0, length);
			}
			inStream.close();
			outStream.close();
			return testXml.getAbsolutePath();
		} else {
			return null;
		}
	}

}

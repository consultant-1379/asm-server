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

import java.util.Map;

public interface Parser {

	Map<String, Map<String, Object>> parseThisFile(final String fileLocation,
			final String inNodeType);

	String getSubNetwork();

	String getRopEndTime();

}

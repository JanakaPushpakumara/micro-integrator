/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.esb.mediators.payloadFactory;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.engine.exceptions.AutomationFrameworkException;
import org.wso2.carbon.automation.test.utils.http.client.HttpURLConnectionClient;
import org.wso2.carbon.integration.common.utils.exceptions.AutomationUtilException;
import org.wso2.esb.integration.common.utils.ESBIntegrationTest;
import org.wso2.esb.integration.common.utils.common.ServerConfigurationManager;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * This class tests whether special chars are properly escaped in JSON payload with replace function scenarios.
 */
public class SpecialCharEscapeWithReplaceTest extends ESBIntegrationTest {
    private ServerConfigurationManager serverConfigurationManager;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init();
        serverConfigurationManager = new ServerConfigurationManager(context);
        serverConfigurationManager.applyMIConfigurationWithRestart(new File(getESBResourceLocation()
                + File.separator + "payloadFactory" + File.separator + "replace" + File.separator + "deployment.toml"));
        super.init();
        loadESBConfigurationFromClasspath(
                File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig"
                        + File.separator + "payloadmediatype" + File.separator
                        + "SpecialCharEscapeWithReplaceTest.xml");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        serverConfigurationManager.restoreToLastConfiguration();
        super.cleanup();
    }

    @Test(groups = "wso2.esb", description = "Payload with Replace function SpecialChar Escape Test")
    public void PayloadWithReplaceFunctionSpecialCharEscapeTest()
            throws IOException, AutomationFrameworkException, AutomationUtilException {

        //json request payload.
        String payload = "{\n\t\"CaseExchange\": \"workorder\"\n}";

        Reader data = new StringReader(payload);
        Writer writer = new StringWriter();

        String serviceURL = this.getApiInvocationURL("workOrderAPI");

        String response = HttpURLConnectionClient
                .sendPostRequestAndReadResponse(data, new URL(serviceURL), writer, "application/json");
        assertNotNull(response, "Response is null");
        //should return the response without throwing any errors.
        assertTrue(response.contains("\"{\\n\\t\\\\\\\"CaseExchange\\\\\\\": \\\\\\\"workorder\\\\\\\"\\n}\""));
    }
}


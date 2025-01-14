/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.esb.json;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.carbon.logging.view.stub.LogViewerLogViewerException;
import org.wso2.esb.integration.common.utils.CarbonLogReader;
import org.wso2.esb.integration.common.utils.ESBIntegrationTest;

import java.io.IOException;

public class ESBJAVA_3698_MessageBuildingWithDifferentPayloadAndContentTypeTestCase extends ESBIntegrationTest {
    private final DefaultHttpClient httpClient = new DefaultHttpClient();
    private CarbonLogReader carbonLogReader;

    @Test(groups = { "wso2.esb" }, description = "Check for Axis Fault when xml payload is sent with application/json"
            + " content type", enabled = true)
    public void testAxisFaultWithXmlPayloadAndJSONContentType()
            throws ClientProtocolException, IOException, InterruptedException, LogViewerLogViewerException {
        carbonLogReader.start();
        final HttpPost post = new HttpPost("http://localhost:8480/ESBJAVA3698jsonstockquote/test");
        post.addHeader("Content-Type", "application/json");
        post.addHeader("SOAPAction", "urn:getQuote");
        StringEntity se = new StringEntity(getPayload());
        post.setEntity(se);

        httpClient.execute(post);

        boolean errorLogFound = carbonLogReader.checkForLog(
                "Error occurred while processing document for application/json", 60);
        carbonLogReader.stop();
        Assert.assertEquals(errorLogFound, true, "Expected SOAP Response was NOT found in the LOG stream.");
    }

    private String getPayload() {
        final String payload =
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://services.samples/xsd\" xmlns:ser=\"http://services.samples\">"
                        + "<soapenv:Header/>" + "<soapenv:Body>" + "<ser:getQuote>" + "<ser:request>"
                        + "<xsd:symbol>IBM</xsd:symbol>" + "</ser:request>" + "</ser:getQuote>" + "</soapenv:Body>"
                        + "</soapenv:Envelope>";
        return payload;
    }
}

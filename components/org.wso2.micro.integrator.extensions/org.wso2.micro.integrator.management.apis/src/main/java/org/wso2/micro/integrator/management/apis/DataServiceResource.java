/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.micro.integrator.management.apis;

import com.google.gson.Gson;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.json.JSONObject;
import org.wso2.micro.integrator.dataservices.common.DBConstants;
import org.wso2.micro.integrator.dataservices.core.DBUtils;
import org.wso2.micro.integrator.dataservices.core.description.query.Query;
import org.wso2.micro.integrator.dataservices.core.engine.DataService;
import org.wso2.micro.integrator.inbound.endpoint.internal.http.api.APIResource;
import org.wso2.micro.integrator.management.apis.models.dataServices.DataServiceInfo;
import org.wso2.micro.integrator.management.apis.models.dataServices.DataServiceSummary;
import org.wso2.micro.integrator.management.apis.models.dataServices.DataServicesList;
import org.wso2.micro.integrator.management.apis.models.dataServices.QuerySummary;
import org.wso2.micro.service.mgt.ServiceAdmin;
import org.wso2.micro.service.mgt.ServiceMetaData;



import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DataServiceResource extends APIResource {

    private static final Log log = LogFactory.getLog(DataServiceResource.class);

    private static ServiceAdmin serviceAdmin = null;

    public DataServiceResource(String urlTemplate) {
        super(urlTemplate);
    }

    @Override
    public Set<String> getMethods() {
        Set<String> methods = new HashSet<>();
        methods.add(Constants.HTTP_GET);
        return methods;
    }

    @Override
    public boolean invoke(MessageContext msgCtx) {
        buildMessage(msgCtx);
        if (serviceAdmin == null) {
            serviceAdmin = Utils.getServiceAdmin(msgCtx);
        }
        String param = Utils.getQueryParameter(msgCtx, "dataServiceName");

        try {
            if (param != null) {
                // data-service specified by name
                populateDataServiceByName(msgCtx, param);
            } else {
                // list of all data-services
                populateDataServiceList(msgCtx);
            }
        } catch (Exception exception) {
            log.error("Error while populating service: ", exception);
            msgCtx.setProperty(Constants.HTTP_STATUS_CODE, Constants.INTERNAL_SERVER_ERROR);
        }

        org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) msgCtx)
                .getAxis2MessageContext();

        axis2MessageContext.removeProperty(Constants.NO_ENTITY_BODY);
        return true;
    }

    private void populateDataServiceList(MessageContext msgCtx) throws Exception {
        SynapseConfiguration configuration = msgCtx.getConfiguration();
        AxisConfiguration axisConfiguration = configuration.getAxisConfiguration();
        String[] dataServicesNames = DBUtils.getAvailableDS(axisConfiguration);

        // initiate list model
        DataServicesList dataServicesList = new DataServicesList(dataServicesNames.length);

        for (String dataServiceName : dataServicesNames) {
            DataService dataService = getDataServiceByName(msgCtx, dataServiceName);
            ServiceMetaData serviceMetaData = getServiceMetaData(dataService);

            // initiate summary model
            DataServiceSummary summary = null;
            if (serviceMetaData != null) {
                summary = new DataServiceSummary(serviceMetaData.getName(), serviceMetaData.getWsdlURLs());
            }
            dataServicesList.addServiceSummary(summary);
        }

        org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) msgCtx)
                .getAxis2MessageContext();

        String stringPayload = new Gson().toJson(dataServicesList);
        Utils.setJsonPayLoad(axis2MessageContext, new JSONObject(stringPayload));
    }

    private void populateDataServiceByName(MessageContext msgCtx, String serviceName) throws Exception {
        DataService dataService = getDataServiceByName(msgCtx, serviceName);

        org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) msgCtx)
                .getAxis2MessageContext();

        if (dataService == null) {
            axis2MessageContext.setProperty(Constants.HTTP_STATUS_CODE, Constants.NOT_FOUND);
        } else {
            ServiceMetaData serviceMetaData = getServiceMetaData(dataService);
            DataServiceInfo dataServiceInfo = null;

            if (serviceMetaData != null) {
                dataServiceInfo = new DataServiceInfo(serviceMetaData.getName(), serviceMetaData.getDescription(),
                                                      serviceMetaData.getServiceGroupName(),
                                                      serviceMetaData.getWsdlURLs());

                Map<String, Query> queries = dataService.getQueries();
                for (Map.Entry<String, Query> stringQuery : queries.entrySet()) {
                    QuerySummary querySummary = new QuerySummary(stringQuery.getKey(),
                                                                 stringQuery.getValue().getNamespace());
                    dataServiceInfo.addQuery(querySummary);
                }
            }
            String stringPayload = new Gson().toJson(dataServiceInfo);
            Utils.setJsonPayLoad(axis2MessageContext, new JSONObject(stringPayload));
        }
    }

    private DataService getDataServiceByName(MessageContext msgCtx, String serviceName) {
        AxisService axisService = msgCtx.getConfiguration().
                getAxisConfiguration().getServiceForActivation(serviceName);
        DataService dataService = null;
        if (axisService != null) {
            dataService = (DataService) axisService.getParameter(DBConstants.DATA_SERVICE_OBJECT).getValue();
        } else {
            log.debug("DataService {} is null.");
        }
        return dataService;
    }

    private ServiceMetaData getServiceMetaData(DataService dataService) throws Exception {
        if (dataService != null) {
            return serviceAdmin.getServiceData(dataService.getName());
        } else {
            return null;
        }
    }
}
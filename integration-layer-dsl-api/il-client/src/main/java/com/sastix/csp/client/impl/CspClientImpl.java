package com.sastix.csp.client.impl;

import com.sastix.csp.client.CspClient;
import com.sastix.csp.commons.client.ApiVersionClient;
import com.sastix.csp.commons.client.RetryRestTemplate;
import com.sastix.csp.commons.model.IntegrationData;
import com.sastix.csp.commons.model.VersionDTO;
import com.sastix.csp.commons.routes.ContextUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

/**
 * Created by iskitsas on 5/3/17.
 */
public class CspClientImpl implements CspClient, ContextUrl {
    private Logger LOG = (Logger) LoggerFactory.getLogger(CspClientImpl.class);

    @Autowired
    ApiVersionClient apiVersionClient;


    @Autowired
    @Qualifier("CspRestTemplate")
    RetryRestTemplate retryRestTemplate;

    @Override
    public VersionDTO getApiVersion() {
        return apiVersionClient.getApiVersion();
    }

    @Override
    public ResponseEntity<String> postIntegrationData(IntegrationData integrationData, String context) {
        final String url = apiVersionClient.getApiUrl() + context;
        LOG.debug("API call [post]: " + url);
        ResponseEntity<String> response = retryRestTemplate.exchange(url, HttpMethod.POST, new HttpEntity<Object>(integrationData), String.class);
        return response;
    }

    @Override
    public ResponseEntity<String> updateIntegrationData(IntegrationData integrationData, String context) {
        final String url = apiVersionClient.getApiUrl() + context;
        LOG.debug("API call [put]: " + url);
        ResponseEntity<String> response = retryRestTemplate.exchange(url, HttpMethod.PUT,new HttpEntity<Object>(integrationData), String.class);
        LOG.debug("status code: "+response.getStatusCode());
        return response;
    }

    @Override
    public ResponseEntity<String> deleteIntegrationData(IntegrationData integrationData, String context) {
        final String url = apiVersionClient.getApiUrl() + context;
        LOG.debug("API call [delete]: " + url);
        ResponseEntity<String> response = retryRestTemplate.exchange(url, HttpMethod.DELETE,new HttpEntity<Object>(integrationData), String.class);
        LOG.debug("status code: "+response.getStatusCode());
        return response;
    }

}

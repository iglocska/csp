package com.intrasoft.csp.integration.sandbox.server.internal;

import com.intrasoft.csp.client.CspClient;
import com.intrasoft.csp.commons.apiHttpStatusResponse.HttpStatusResponseType;
import com.intrasoft.csp.commons.exceptions.InvalidDataTypeException;
import com.intrasoft.csp.commons.model.DataParams;
import com.intrasoft.csp.commons.model.IntegrationData;
import com.intrasoft.csp.commons.model.IntegrationDataType;
import com.intrasoft.csp.commons.model.SharingParams;
import com.intrasoft.csp.commons.routes.CamelRoutes;
import com.intrasoft.csp.commons.routes.ContextUrl;
import com.intrasoft.csp.server.CspApp;
import com.intrasoft.csp.server.utils.TestUtil;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.apache.camel.test.spring.MockEndpointsAndSkip;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * Created by iskitsas on 6/9/17.
 */
@RunWith(CamelSpringBootRunner.class)
@SpringBootTest(classes = {CspApp.class},
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "spring.datasource.url:jdbc:h2:mem:csp_policy",
                "flyway.enabled:false",
                "server.port: 8089",
                "csp.server.protocol: http",
                "csp.server.host: localhost",
                "csp.server.port: 8089",
                "api.version: 1",
                "csp.retry.backOffPeriod:10",
                "csp.retry.maxAttempts:1",
                "embedded.activemq.start:false",
                "apache.camel.use.activemq:false",
                "external.use.ssl:false",
                "internal.use.ssl:false",
                "misp.protocol:http",
                "taranis.protocol:http",
        })
@MockEndpointsAndSkip("http:*")
public class CspServerVariousIntegrationDataTest implements CamelRoutes, ContextUrl{
    private MockMvc mvc;

    private HttpMessageConverter mappingJackson2HttpMessageConverter;


    URL data_artefact_with_team_id = getClass().getClassLoader().getResource("json/data_artefact_with_team_id.json");
    URL data_artefact_with_teamid_arr = getClass().getClassLoader().getResource("json/data_artefact_with_teamid_arr.json");
    URL tc_cmm_sample = getClass().getClassLoader().getResource("json/tc_cmm_sample.json");

    @Autowired
    void setConverters(HttpMessageConverter<?>[] converters) {
        this.mappingJackson2HttpMessageConverter = Arrays.asList(converters)
                .stream().filter(hmc -> hmc instanceof MappingJackson2HttpMessageConverter)
                .findAny().get();
        Assert.assertNotNull("The JSON message converter must not be null", this.mappingJackson2HttpMessageConverter);
    }

    @Autowired
    CspClient cspClient;


    @Autowired
    private WebApplicationContext webApplicationContext;

    @Before
    public void init() throws Exception {
        mvc = webAppContextSetup(webApplicationContext).build();

    }

    @Test
    public void InvalidIntegrationDataTest() throws Exception {
        IntegrationData integrationData = new IntegrationData();
        DataParams dataParams = new DataParams();
        integrationData.setDataParams(dataParams);

        try {
            cspClient.postIntegrationData(integrationData);
            fail("Expected InvalidDataTypeException exception");
        }catch (InvalidDataTypeException e){
            assertThat(e.getMessage(),containsString("Field error in object 'integrationData'"));
        }

    }

    @Test
    public void InvalidSharingParamsTest() throws Exception {
        IntegrationData integrationData = new IntegrationData();
        integrationData.setDataType(IntegrationDataType.CHAT);

        DataParams dataParams = new DataParams();
        dataParams.setApplicationId("appid");
        dataParams.setCspId("cspid");
        dataParams.setRecordId("recordId");
        dataParams.setDateTime(DateTime.now());
        dataParams.setOriginApplicationId("origin");
        dataParams.setOriginCspId("origin");
        dataParams.setOriginRecordId("origin");
        integrationData.setDataParams(dataParams);
        SharingParams sharingParams = new SharingParams();
        sharingParams.setIsExternal(false);
        sharingParams.setToShare(false);
        List<Boolean> teams= new ArrayList<>();
        teams.add(true);
        teams.add(false);
        sharingParams.setTeamId(teams);
        integrationData.setSharingParams(sharingParams);

        integrationData.setDataObject(sharingParams);


        try {
            cspClient.postIntegrationData(integrationData);
            fail("Expected InvalidDataTypeException exception");
        }catch (InvalidDataTypeException e){
            assertThat(e.getMessage(),containsString("teamId should be an array of strings"));
        }

    }

    @Test
    public void InvalidJsonDataObjectTest(){
        IntegrationData integrationData = new IntegrationData();
        integrationData.setDataType(IntegrationDataType.VULNERABILITY);
        SharingParams sharingParams = new SharingParams();
        sharingParams.setIsExternal(true);
        sharingParams.setToShare(false);
        integrationData.setSharingParams(sharingParams);
        DataParams dataParams = new DataParams();
        dataParams.setRecordId("222");
        dataParams.setApplicationId("appId");
        dataParams.setDateTime(DateTime.now());
        dataParams.setCspId("cspId");
        integrationData.setDataParams(dataParams);
        integrationData.setDataObject(null);
        try {
        cspClient.postIntegrationData(integrationData);
            fail("Expected InvalidDataTypeException exception");
        }catch (InvalidDataTypeException e){
            assertThat(e.getMessage(),containsString(HttpStatusResponseType.MALFORMED_INTEGRATION_DATA_STRUCTURE.getReasonPhrase()));
        }
    }

    @Test
    public void integrationDataJsonTest() throws Exception {

        IntegrationData integrationData = new IntegrationData();
        integrationData.setDataType(IntegrationDataType.VULNERABILITY);
        SharingParams sharingParams = new SharingParams();
        sharingParams.setIsExternal(true);
        sharingParams.setToShare(false);
        integrationData.setSharingParams(sharingParams);
        DataParams dataParams = new DataParams();
        dataParams.setRecordId("222");
        dataParams.setApplicationId("appId");
        dataParams.setDateTime(DateTime.now());
        dataParams.setCspId("cspId");
        dataParams.setOriginCspId("origin-testCspId");
        dataParams.setOriginApplicationId("origin-test1");
        dataParams.setOriginRecordId("origin-recordId");
        integrationData.setDataParams(dataParams);
        integrationData.setDataObject(25d);
        String jsonStr = json(integrationData);
        mvc.perform(post("/v"+REST_API_V1+"/"+DSL_INTEGRATION_DATA).accept(MediaType.TEXT_PLAIN)
                .content(jsonStr)
                .contentType(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(content().string(HttpStatusResponseType.SUCCESSFUL_OPERATION.getReasonPhrase()));

    }

    @Ignore
    @Test
    public void teamsTest() throws Exception {
        String jsonStr = FileUtils.readFileToString(new File(data_artefact_with_team_id.toURI()), Charset.forName("UTF-8"));
        mvc.perform(post("/v"+REST_API_V1+"/"+DSL_INTEGRATION_DATA).accept(MediaType.TEXT_PLAIN)
                .content(jsonStr)
                .contentType(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(content().string(HttpStatusResponseType.SUCCESSFUL_OPERATION.getReasonPhrase()));
    }

    @Ignore
    @Test
    public void teamsArrTest() throws Exception {
        String jsonStr = FileUtils.readFileToString(new File(data_artefact_with_teamid_arr.toURI()), Charset.forName("UTF-8"));
        mvc.perform(post("/v"+REST_API_V1+"/"+DSL_INTEGRATION_DATA).accept(MediaType.TEXT_PLAIN)
                .content(jsonStr)
                .contentType(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(content().string(HttpStatusResponseType.SUCCESSFUL_OPERATION.getReasonPhrase()));
    }

    @Test
    public void tcCMMTest() throws Exception {
        String jsonStr = FileUtils.readFileToString(new File(tc_cmm_sample.toURI()), Charset.forName("UTF-8"));
        mvc.perform(post("/v"+REST_API_V1+"/"+DSL_INTEGRATION_DATA).accept(MediaType.TEXT_PLAIN)
                .content(jsonStr)
                .contentType(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(content().string(HttpStatusResponseType.SUCCESSFUL_OPERATION.getReasonPhrase()));
    }

    protected String json(Object object) throws IOException {
        MockHttpOutputMessage mockHttpOutputMessage = new MockHttpOutputMessage();
        this.mappingJackson2HttpMessageConverter.write(object, MediaType.APPLICATION_JSON, mockHttpOutputMessage);
        return mockHttpOutputMessage.getBodyAsString();
    }
}

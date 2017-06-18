package com.intrasoft.csp.integration.business.server.internal;


import com.intrasoft.csp.commons.model.IntegrationData;
import com.intrasoft.csp.commons.model.IntegrationDataType;
import com.intrasoft.csp.commons.routes.CamelRoutes;
import com.intrasoft.csp.integration.MockUtils;
import com.intrasoft.csp.server.CspApp;
import com.intrasoft.csp.server.routes.RouteUtils;
import com.intrasoft.csp.server.service.ErrorMessageHandler;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.component.http.HttpMethods;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.SpringCamelContext;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.apache.camel.test.spring.MockEndpointsAndSkip;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(CamelSpringBootRunner.class)
@SpringBootTest(classes = {CspApp.class, MockUtils.class},
        properties = {
                "csp.retry.backOffPeriod:10",
                "csp.retry.maxAttempts:1",
                "embedded.activemq.start:false",
                "apache.camel.use.activemq:false",
                "internal.use.ssl: false",
                "internal.ssl.keystore.resource: sslcert/csp-internal.jks",
                "internal.ssl.keystore.passphrase: 123456",
                "external.use.ssl: false",
                "external.ssl.keystore.resource: sslcert/csp-internal.jks",
                "external.ssl.keystore.passphrase: 123456",

                "misp.protocol: http",
                "misp.host: csp2.dangerduck.gr",
                "misp.port: 8082",
                "misp.path: /adapter/misp",

                "tc.protocol: http",
                "tc.host: tc.csp2.dangerduck.gr",
                "tc.port: 8000",
                "tc.path.circles: /api/v1/circles",
                "tc.path.teams: /api/v1/teams",

                "elastic.protocol: http",
                "elastic.host: csp2.dangerduck.gr",
                "elastic.port: 9200",
                "elastic.path: /cspdata"
        })
//@MockEndpointsAndSkip("^https4-in://localhost.*adapter.*|https4-in://csp.*|https4-ex://ex.*") // by removing this any http requests will be sent as expected.
//@MockEndpointsAndSkip("http://external.csp*") // by removing this any http requests will be sent as expected.
@MockEndpointsAndSkip("^http://csp2.dangerduck.gr:8081/v1/dcl/integrationData")
// In this test we mock all other http requests except for tc. TC dummy server is expected on 3001 port.
// To start the TC dummy server:
// $ APP_NAME=tc SSL=true PORT=8081 node server.js
public class CspServerInternalBusinessTestFlow2 implements CamelRoutes {

    private static final Logger LOG = LoggerFactory.getLogger(CspServerInternalBusinessTestFlow2.class);

    private MockMvc mvc;
    @Autowired
    private WebApplicationContext webApplicationContext;

    @EndpointInject(uri = CamelRoutes.MOCK_PREFIX + ":" + DIRECT + ":" + DSL)
    private MockEndpoint mockedDsl;

    @EndpointInject(uri = CamelRoutes.MOCK_PREFIX + ":" + DIRECT + ":" + APP)
    private MockEndpoint mockedApp;

    @EndpointInject(uri = CamelRoutes.MOCK_PREFIX + ":" + DIRECT + ":" + EDCL)
    private MockEndpoint mockedEDcl;

    @EndpointInject(uri = CamelRoutes.MOCK_PREFIX + ":" + DIRECT + ":" + TC)
    private MockEndpoint mockedTC;

    @Autowired
    MockUtils mockUtils;

    @Autowired
    RouteUtils routes;

    @Autowired
    SpringCamelContext springCamelContext;

    @Autowired
    ErrorMessageHandler errorMessageHandler;

    @Autowired
    Environment env;

    private Integer numOfCspsToTest = 3;
    private Integer currentCspId = 0;
    private final IntegrationDataType dataTypeToTest = IntegrationDataType.VULNERABILITY;
    private final String applicationId = "taranis";
    private final String cspId = "CERT-GR";

    @Before
    public void init() throws Exception {
        mvc = webAppContextSetup(webApplicationContext).build();
        mockUtils.setSpringCamelContext(springCamelContext);

        mockUtils.mockRoute(CamelRoutes.MOCK_PREFIX, routes.apply(DSL), mockedDsl.getEndpointUri());
        mockUtils.mockRoute(CamelRoutes.MOCK_PREFIX, routes.apply(TC), mockedTC.getEndpointUri());
        mockUtils.mockRoute(CamelRoutes.MOCK_PREFIX, routes.apply(APP), mockedApp.getEndpointUri());
        mockUtils.mockRoute(CamelRoutes.MOCK_PREFIX, routes.apply(DCL), mockedEDcl.getEndpointUri());
    }

    // Use @DirtiesContext on each test method to force Spring Testing to automatically reload the CamelContext after
    // each test method - this ensures that the tests don't clash with each other, e.g., one test method sending to an
    // endpoint that is then reused in another test method.
    @DirtiesContext
    @Test
    public void testDslFlow2PostAuthorized() throws Exception {
        /*
        isExternal will be false in Flow 2, and TcProcessor will set it to true
        toShare will be true in Flow 2
         */
        mockUtils.sendFlow2Data(mvc, applicationId, false, true, this.cspId, this.dataTypeToTest, HttpMethods.POST.name());

        _authorizedFlowImpl();

        //Thread.sleep(10*1000); //to avoid "Rejecting received message because of the listener container having been stopped in the meantime"
        //be careful when debugging, you might miss breakpoints if the time is not enough
    }

    @DirtiesContext
    @Test
    public void testDslFlow2PutAuthorized() throws Exception {
        /*
        isExternal will be false in Flow 2, and TcProcessor will set it to true
        toShare will be true in Flow 2
         */
        mockUtils.sendFlow2Data(mvc, applicationId, false, true, this.cspId, this.dataTypeToTest, HttpMethods.PUT.name());

        _authorizedFlowImpl();

        //Thread.sleep(10*1000); //to avoid "Rejecting received message because of the listener container having been stopped in the meantime"
        //be careful when debugging, you might miss breakpoints if the time is not enough
    }

    @DirtiesContext
    @Test
    public void testDslFlow2PostNotAuthorized() throws Exception {
        /*
        isExternal will be false in Flow 2, and TcProcessor will set it to true
        toShare will be true in Flow 2
         */
        //For NOT authorized flow, set cspId to have a value different than the one initialized in Class (CERT-GR)
        mockUtils.sendFlow2Data(mvc, applicationId, false, true, "CERT-DUMMY-GR", this.dataTypeToTest, HttpMethods.POST.name());

        _notAuthorizedFlowImpl();

        //Thread.sleep(10*1000); //to avoid "Rejecting received message because of the listener container having been stopped in the meantime"
        //be careful when debugging, you might miss breakpoints if the time is not enough
    }

    @DirtiesContext
    @Test
    public void testDslFlow2PutNotAuthorized() throws Exception {
        /*
        isExternal will be false in Flow 2, and TcProcessor will set it to true
        toShare will be true in Flow 2
         */
        //For NOT authorized flow, set cspId to have a value different than the ones initialized in Class (CERT-GR)
        mockUtils.sendFlow2Data(mvc, applicationId, false, true, "CERT-DUMMY-GR", this.dataTypeToTest, HttpMethods.PUT.name());

        _notAuthorizedFlowImpl();

        //Thread.sleep(10*1000); //to avoid "Rejecting received message because of the listener container having been stopped in the meantime"
        //be careful when debugging, you might miss breakpoints if the time is not enough
    }


    private void _authorizedFlowImpl() throws Exception {
        /*
        External DCL: expect 0-messages for VULNERABILITY, according to csp2.dangerduck.gr TC configuration
         */
        mockedEDcl.expectedMessageCount(0);
        mockedEDcl.assertIsSatisfied();

        //assert datatype, isExternal (?), toShare
        List<Exchange> list = mockedEDcl.getReceivedExchanges();
        for (Exchange exchange : list) {
            Message in = exchange.getIn();
            IntegrationData data = in.getBody(IntegrationData.class);
            assertThat(data.getDataType(), is(this.dataTypeToTest));
            //assertThat(data.getSharingParams().getIsExternal(), is(false));
            assertThat(data.getSharingParams().getToShare(), is(true));
        }


        /*
        TC: expect 1-message for authorized calls
         */
        mockedTC.expectedMessageCount(1);
        mockedTC.assertIsSatisfied();

        //assert datatype, isExternal (is changed for authorized), toShare
        list = mockedTC.getReceivedExchanges();
        for (Exchange exchange : list) {
            Message in = exchange.getIn();
            IntegrationData data = in.getBody(IntegrationData.class);
            assertThat(data.getDataType(), is(this.dataTypeToTest));
            assertThat(data.getSharingParams().getIsExternal(), is(true));
            assertThat(data.getSharingParams().getToShare(), is(true));
        }

       /*
        DSL: expect 1-message
         */
        mockedDsl.expectedMessageCount(1);
        mockedDsl.assertIsSatisfied();


        list = mockedDsl.getReceivedExchanges();
        for (Exchange exchange : list) {
            Message in = exchange.getIn();
            IntegrationData data = in.getBody(IntegrationData.class);
            assertThat(data.getDataType(), is(this.dataTypeToTest));
            assertThat(data.getSharingParams().getIsExternal(), is(true));
            assertThat(data.getSharingParams().getToShare(), is(true));
        }

        /*
        APP
         */
        // The data type to test is defined in class -> private IntegrationDataType dataTypeToTest = IntegrationDataType.VULNERABILITY;
        // The application id is "taranis"
        // Expect 1-messages according to application.properties (external.vulnerability.apps:taranis)
        mockedApp.expectedMessageCount(1);
        mockedApp.assertIsSatisfied();

        list = mockedApp.getReceivedExchanges();
        for (Exchange exchange : list) {
            Message in = exchange.getIn();
            IntegrationData data = in.getBody(IntegrationData.class);
            assertThat(data.getDataType(), is(this.dataTypeToTest));
            assertThat(data.getSharingParams().getIsExternal(), is(true));
            assertThat(data.getSharingParams().getToShare(), is(true));
        }
    }

    private void _notAuthorizedFlowImpl() throws Exception {
        /*
        External DCL: expect 0-messages for VULNERABILITY, according to csp2.dangerduck.gr TC configuration
         */
        mockedEDcl.expectedMessageCount(0);
        mockedEDcl.assertIsSatisfied();

        //assert datatype, isExternal (?), toShare
        List<Exchange> list = mockedEDcl.getReceivedExchanges();
        for (Exchange exchange : list) {
            Message in = exchange.getIn();
            IntegrationData data = in.getBody(IntegrationData.class);
            assertThat(data.getDataType(), is(this.dataTypeToTest));
            //assertThat(data.getSharingParams().getIsExternal(), is(false));
            assertThat(data.getSharingParams().getToShare(), is(true));
        }


        /*
        TC: expect 1-message for non-authorized calls
         */
        mockedTC.expectedMessageCount(1);
        mockedTC.assertIsSatisfied();

        //assert datatype, isExternal (not changed for NOT authorized), toShare
        list = mockedTC.getReceivedExchanges();
        for (Exchange exchange : list) {
            Message in = exchange.getIn();
            IntegrationData data = in.getBody(IntegrationData.class);
            assertThat(data.getDataType(), is(this.dataTypeToTest));
            assertThat(data.getSharingParams().getIsExternal(), is(false));
            assertThat(data.getSharingParams().getToShare(), is(true));
        }


       /*
        DSL: expect no message, flow has ended
         */
        mockedDsl.expectedMessageCount(0);
        mockedDsl.assertIsSatisfied();


        /*
        APP: expect no message, flow has ended
         */
        mockedApp.expectedMessageCount(0);
        mockedApp.assertIsSatisfied();
    }
}

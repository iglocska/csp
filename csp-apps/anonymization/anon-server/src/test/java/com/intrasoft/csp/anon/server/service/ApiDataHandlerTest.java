package com.intrasoft.csp.anon.server.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.intrasoft.csp.anon.commons.model.*;
import com.intrasoft.csp.anon.server.AnonApp;
import com.intrasoft.csp.anon.server.model.Rules;
import com.intrasoft.csp.anon.server.utils.CryptoPAN;
import com.intrasoft.csp.commons.model.IntegrationData;
import com.intrasoft.csp.commons.model.IntegrationDataType;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {AnonApp.class},
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "spring.datasource.url:jdbc:h2:mem:anon",
                "server.port: 8585",
                "anon.server.protocol: http",
                "anon.server.host: localhost",
                "anon.server.port: 8585",
                "api.version: 1",
                "csp.retry.backOffPeriod:10",
                "csp.retry.maxAttempts:1",
                "key.update=10000",
                "enable.oam:false",
                "logging.level.com.intrasoft.csp.anon=TRACE"
        })
public class ApiDataHandlerTest {

    private static final Logger LOG = LoggerFactory.getLogger(ApiDataHandlerTest.class);

    @Autowired
    ApiDataHandler apiDataHandler;

    URL data_incident = getClass().getClassLoader().getResource("data_incident.json");
    URL rules_incident = getClass().getClassLoader().getResource("rules_incident.json");

    URL data_artefact = getClass().getClassLoader().getResource("data_artefact.json");
    URL rules_artefact = getClass().getClassLoader().getResource("rules_artefact.json");

    URL data_threat = getClass().getClassLoader().getResource("data_threat.json");
    URL rules_threat = getClass().getClassLoader().getResource("rules_threat.json");

    URL data_vulnerability = getClass().getClassLoader().getResource("data_vulnerability.json");
    URL rules_vulnerability = getClass().getClassLoader().getResource("rules_vulnerability.json");

    URL data_event = getClass().getClassLoader().getResource("data_event.json");
    URL rules_event = getClass().getClassLoader().getResource("rules_event.json");
    URL rules_event_2 = getClass().getClassLoader().getResource("rules_event_2.json");


    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    AnonService anonService;

    @Autowired
    RulesService rulesService;

    @DirtiesContext
    @Test
    public void handleAnonIncidentTest() throws URISyntaxException, IOException, InvalidKeyException, NoSuchAlgorithmException {
        String json = FileUtils.readFileToString(new File(data_incident.toURI()), Charset.forName("UTF-8"));
        IntegrationData integrationData = objectMapper.readValue(json,IntegrationData.class);

        String cspId = integrationData.getDataParams().getCspId();
        String applicationId = integrationData.getDataParams().getApplicationId();
        //insert mapping and ruleset
        RuleSetDTO ruleSetDTO = new RuleSetDTO();
        ruleSetDTO.setFilename(new File(rules_incident.getFile()).getName());
        ruleSetDTO.setFile(FileUtils.readFileToByteArray(new File(rules_incident.toURI())));
        ruleSetDTO.setDescription("incident ruleset");
        RuleSetDTO savedRuleSet = anonService.saveRuleSet(ruleSetDTO);

        MappingDTO mappingDTO = new MappingDTO(cspId,savedRuleSet,integrationData.getDataType(), ApplicationId.INTELMQ);
        MappingDTO savedMapping = anonService.saveMapping(mappingDTO);

        IntegrationAnonData integrationAnonData = new IntegrationAnonData();
        integrationAnonData.setCspId(cspId);
        integrationAnonData.setDataType(integrationData.getDataType());
        integrationAnonData.setDataObject(integrationData.getDataObject());
        integrationAnonData.setApplicationId(applicationId);

        IntegrationAnonData anonData = apiDataHandler.handleAnonIntegrationData(integrationAnonData);

        String jsonOut = objectMapper.writeValueAsString(anonData.getDataObject());
        assertThat(jsonOut, containsString("\"classification.taxonomy\":\""));//cannot really test the randomness of anon here
        assertThat(jsonOut, containsString("\"classification.identifier\":\""));//cannot really test the randomness of anon here
        assertThat(jsonOut, containsString("\"destination.local_hostname\":\"hostname\""));
        assertThat(jsonOut, containsString("\"destination.local_ip\":\"***.***.***.***\""));
        assertThat(jsonOut, containsString("\"destination.account\":\"***@******.**\""));
        assertThat(jsonOut, containsString("\"destination.asn\":\"00000000\""));
    }

    @DirtiesContext
    @Test
    public void handleAnonThreatTest() throws URISyntaxException, IOException, InvalidKeyException, NoSuchAlgorithmException {
        String json = FileUtils.readFileToString(new File(data_threat.toURI()), Charset.forName("UTF-8"));
        IntegrationData integrationData = objectMapper.readValue(json,IntegrationData.class);

        String cspId = integrationData.getDataParams().getCspId();
        String applicationId = integrationData.getDataParams().getApplicationId();
        //insert mapping and ruleset
        RuleSetDTO ruleSetDTO = new RuleSetDTO();
        ruleSetDTO.setFilename(new File(rules_threat.getFile()).getName());
        ruleSetDTO.setFile(FileUtils.readFileToByteArray(new File(rules_threat.toURI())));
        ruleSetDTO.setDescription("threat ruleset");
        RuleSetDTO savedRuleSet = anonService.saveRuleSet(ruleSetDTO);

        MappingDTO mappingDTO = new MappingDTO(cspId,savedRuleSet,integrationData.getDataType(), ApplicationId.RT);
        MappingDTO savedMapping = anonService.saveMapping(mappingDTO);

        IntegrationAnonData integrationAnonData = new IntegrationAnonData();
        integrationAnonData.setCspId(cspId);
        integrationAnonData.setDataType(integrationData.getDataType());
        integrationAnonData.setDataObject(integrationData.getDataObject());
        integrationAnonData.setApplicationId(applicationId);

        IntegrationAnonData anonData = apiDataHandler.handleAnonIntegrationData(integrationAnonData);

        String jsonOut = objectMapper.writeValueAsString(anonData.getDataObject());
//        assertThat(jsonOut, containsString("\"classification.taxonomy\":\""));//cannot really test the randomness of anon here
        assertThat(jsonOut, containsString("\"id\":\"00000000\""));
        assertThat(jsonOut, containsString("\"info\":\"*******\""));
        assertThat(jsonOut, containsString("\"uuid\":\"*******\""));
        assertThat(jsonOut, containsString("\"disable_correlation\":\"*******\""));
    }

    @DirtiesContext
    @Test
    public void handleAnonArtefactTest() throws URISyntaxException, IOException, InvalidKeyException, NoSuchAlgorithmException {
        String json = FileUtils.readFileToString(new File(data_artefact.toURI()), Charset.forName("UTF-8"));
        IntegrationData integrationData = objectMapper.readValue(json,IntegrationData.class);

        String cspId = integrationData.getDataParams().getCspId();
        String applicationId = integrationData.getDataParams().getApplicationId();
        //insert mapping and ruleset
        RuleSetDTO ruleSetDTO = new RuleSetDTO();
        ruleSetDTO.setFilename(new File(rules_artefact.getFile()).getName());
        ruleSetDTO.setFile(FileUtils.readFileToByteArray(new File(rules_artefact.toURI())));
        ruleSetDTO.setDescription("artefact ruleset");
        RuleSetDTO savedRuleSet = anonService.saveRuleSet(ruleSetDTO);

        MappingDTO mappingDTO = new MappingDTO(cspId,savedRuleSet,integrationData.getDataType(), ApplicationId.MISP);
        MappingDTO savedMapping = anonService.saveMapping(mappingDTO);

        IntegrationAnonData integrationAnonData = new IntegrationAnonData();
        integrationAnonData.setCspId(cspId);
        integrationAnonData.setDataType(integrationData.getDataType());
        integrationAnonData.setDataObject(integrationData.getDataObject());
        integrationAnonData.setApplicationId(applicationId);

        IntegrationAnonData anonData = apiDataHandler.handleAnonIntegrationData(integrationAnonData);

        String jsonOut = objectMapper.writeValueAsString(anonData.getDataObject());
        assertThat(jsonOut, containsString("\"type\":\""));//cannot really test the randomness of anon here
        assertThat(jsonOut, containsString("\"id\":\"00000000\""));
        assertThat(jsonOut, containsString("\"size\":\"00000000\""));
        assertThat(jsonOut, containsString("\"name\":\"*******\""));
    }

    @DirtiesContext
    @Test
    public void handleAnonVulnerabilityTest() throws URISyntaxException, IOException, InvalidKeyException, NoSuchAlgorithmException {
        String json = FileUtils.readFileToString(new File(data_vulnerability.toURI()), Charset.forName("UTF-8"));
        IntegrationData integrationData = objectMapper.readValue(json,IntegrationData.class);

        String cspId = integrationData.getDataParams().getCspId();
        String applicationId = integrationData.getDataParams().getApplicationId();
        //insert mapping and ruleset
        RuleSetDTO ruleSetDTO = new RuleSetDTO();
        ruleSetDTO.setFilename(new File(rules_vulnerability.getFile()).getName());
        ruleSetDTO.setFile(FileUtils.readFileToByteArray(new File(rules_vulnerability.toURI())));
        ruleSetDTO.setDescription("vulnerability ruleset");
        RuleSetDTO savedRuleSet = anonService.saveRuleSet(ruleSetDTO);

        MappingDTO mappingDTO = new MappingDTO(cspId,savedRuleSet,integrationData.getDataType(), ApplicationId.INTELMQ);
        MappingDTO savedMapping = anonService.saveMapping(mappingDTO);

        IntegrationAnonData integrationAnonData = new IntegrationAnonData();
        integrationAnonData.setCspId(cspId);
        integrationAnonData.setDataType(integrationData.getDataType());
        integrationAnonData.setDataObject(integrationData.getDataObject());
        integrationAnonData.setApplicationId(applicationId);


        IntegrationAnonData anonData = apiDataHandler.handleAnonIntegrationData(integrationAnonData);

        String jsonOut = objectMapper.writeValueAsString(anonData.getDataObject());
        LOG.info(jsonOut.toString());
//        assertThat(jsonOut, containsString("\"classification.taxonomy\":\""));//cannot really test the randomness of anon here
        assertThat(jsonOut, containsString("\"affected_products_text\":\""));//cannot really test the randomness of anon here
//        assertThat(jsonOut, containsString("\"version\":\"00000000\""));
//        assertThat(jsonOut, containsString("\"producer\":\"*******\""));
    }

    @DirtiesContext
    @Test
    public void handleAnonEventsTest() throws URISyntaxException, IOException, InvalidKeyException, NoSuchAlgorithmException {
        String json = FileUtils.readFileToString(new File(data_event.toURI()), Charset.forName("UTF-8"));
        IntegrationData integrationData = objectMapper.readValue(json,IntegrationData.class);

        String cspId = integrationData.getDataParams().getCspId();
        String applicationId = integrationData.getDataParams().getApplicationId();
        //insert mapping and ruleset
        RuleSetDTO ruleSetDTO = new RuleSetDTO();
        ruleSetDTO.setFilename(new File(rules_event_2.getFile()).getName());
        ruleSetDTO.setFile(FileUtils.readFileToByteArray(new File(rules_event_2.toURI())));
        ruleSetDTO.setDescription("event ruleset");
        RuleSetDTO savedRuleSet = anonService.saveRuleSet(ruleSetDTO);

        MappingDTO mappingDTO = new MappingDTO(cspId,savedRuleSet,integrationData.getDataType(), ApplicationId.MISP);
        MappingDTO defaultmappingDTO = new MappingDTO("demo4-csp",savedRuleSet,integrationData.getDataType(), ApplicationId.MISP);
        anonService.saveMapping(mappingDTO);
        anonService.saveMapping(defaultmappingDTO);

        IntegrationAnonData integrationAnonData = new IntegrationAnonData();
        integrationAnonData.setCspId(cspId);
        integrationAnonData.setDataType(integrationData.getDataType());
        integrationAnonData.setDataObject(integrationData.getDataObject());
        integrationAnonData.setApplicationId(applicationId);

        IntegrationAnonData anonData = apiDataHandler.handleAnonIntegrationData(integrationAnonData);

        String jsonOut = objectMapper.writeValueAsString(anonData.getDataObject());
        LOG.info(jsonOut.toString());
        assertThat(jsonOut, containsString("\"event_creator_email\":\"\""));
        /*assertThat(jsonOut, containsString("\"value\":\"10.37.235.254\""));
        assertThat(jsonOut, containsString("\"value\":\"10.37.235.127\""));*/
    }

    @DirtiesContext
    @Test
    public void rulesServiceTest() throws IOException, URISyntaxException {
        String json = FileUtils.readFileToString(new File(data_incident.toURI()), Charset.forName("UTF-8"));
        String json2 = FileUtils.readFileToString(new File(data_incident.toURI()), Charset.forName("UTF-8"));
        IntegrationData integrationData = objectMapper.readValue(json,IntegrationData.class);
        IntegrationData integrationData2 = objectMapper.readValue(json2,IntegrationData.class);

        String cspId = integrationData.getDataParams().getCspId();
        //insert mapping and ruleset
        RuleSetDTO ruleSetDTO = new RuleSetDTO();
        ruleSetDTO.setFilename(new File(rules_incident.getFile()).getName());
        ruleSetDTO.setFile(FileUtils.readFileToByteArray(new File(rules_incident.toURI())));
        ruleSetDTO.setDescription("incident ruleset");
        RuleSetDTO savedRuleSet = anonService.saveRuleSet(ruleSetDTO);

        MappingDTO mappingDTO = new MappingDTO(cspId,savedRuleSet,integrationData.getDataType(), ApplicationId.INTELMQ);
        MappingDTO savedMapping = anonService.saveMapping(mappingDTO);

        mappingDTO = new MappingDTO(cspId,savedRuleSet,integrationData2.getDataType(), ApplicationId.INTELMQ);
        MappingDTO savedMapping2 = anonService.saveMapping(mappingDTO);

        Rules rules = rulesService.getRule(IntegrationDataType.INCIDENT,"demo1-csp ");//test if the value is trimmed

        assertThat(rules.getRules().size(),greaterThan(0));
    }

    @DirtiesContext
    @Test
    public void testCryptoPAN() throws IOException, URISyntaxException, NoSuchAlgorithmException, InterruptedException {

            String key = UUID.randomUUID().toString();
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(key.getBytes());

            String digest = new BigInteger(1, md.digest()).toString(16);
            String digest2 = String.format("%032x", new BigInteger(1, md.digest()));
            LOG.trace("CryptoPAN mask1: " + digest);
            LOG.trace("CryptoPAN mask2: " + digest2);

            StringBuffer hexString = new StringBuffer();
            for (int i=0;i<md.digest().length;i++) {
                String hex=Integer.toHexString(0xFF & md.digest()[i]);
                if(hex.length()==1)
                    hexString.append('0');

                hexString.append(hex);
            }
            LOG.trace("CryptoPAN mask3: " + hexString.toString());

            CryptoPAN cryptoPAN = new CryptoPAN(digest);
            String newVal = cryptoPAN.anonymize("10.10.20.254");
            LOG.info(newVal);
            Thread.sleep(1000);

    }

}

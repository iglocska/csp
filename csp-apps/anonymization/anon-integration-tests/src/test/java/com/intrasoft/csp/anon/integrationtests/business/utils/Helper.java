package com.intrasoft.csp.anon.integrationtests.business.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intrasoft.csp.anon.commons.model.MappingDTO;
import com.intrasoft.csp.anon.commons.model.RuleSetDTO;
import com.intrasoft.csp.anon.server.model.Mapping;
import com.intrasoft.csp.anon.server.model.Rule;
import com.intrasoft.csp.anon.server.model.Rules;
import com.intrasoft.csp.commons.model.IntegrationDataType;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

public class Helper {

    public static RuleSetDTO createRuleset() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        Rules rules = new Rules();
        List<Rule> ruleList = new ArrayList<>();
        ruleList.add(new Rule("$.trustcircle.short_name", "anon", "string"));
        ruleList.add(new Rule("$.trustcircle.created", "anon", "numeric"));
        ruleList.add(new Rule("$.trustcircle.teams[*].description", "pseudo", "string"));
        ruleList.add(new Rule("$.trustcircle.teams[*].theip", "anon", "ip"));
        ruleList.add(new Rule("$.trustcircle.teams[*].theemail", "anon", "email"));
        rules.setRules(ruleList);
        RuleSetDTO ruleSetDTO = new RuleSetDTO();
        ruleSetDTO.setFilename("TrustCircleRulesFile");
        ruleSetDTO.setDescription("Rules to anonymize a trustcircle data object");
        ruleSetDTO.setFile(objectMapper.writeValueAsBytes(rules));
        return ruleSetDTO;
    }

    public static RuleSetDTO createRuleset2() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        Rules rules = new Rules();
        List<Rule> ruleList = new ArrayList<>();
        ruleList.add(new Rule("xml_advisory.meta_info.system_information.systemdetail.affected_products_text", "anon", "string"));
        rules.setRules(ruleList);
        RuleSetDTO ruleSetDTO = new RuleSetDTO();
        ruleSetDTO.setFilename("VulnerabilityRulesFile");
        ruleSetDTO.setDescription("Rules to anonymize a vulnerability data object");
        ruleSetDTO.setFile(objectMapper.writeValueAsBytes(rules));
        return ruleSetDTO;
    }

    public static MappingDTO createMapping(RuleSetDTO ruleSetDTO){
        MappingDTO mappingDTO = new MappingDTO();
        mappingDTO.setCspId("CERT-BUND");
        mappingDTO.setDataType(IntegrationDataType.TRUSTCIRCLE);
        mappingDTO.setRuleSetDTO(ruleSetDTO);
        return mappingDTO;
    }

    public static MappingDTO createMapping2(RuleSetDTO ruleSetDTO){
        MappingDTO mappingDTO = new MappingDTO();
        mappingDTO.setCspId("demo1-csp");
        mappingDTO.setDataType(IntegrationDataType.VULNERABILITY);
        mappingDTO.setRuleSetDTO(ruleSetDTO);
        return mappingDTO;
    }
}

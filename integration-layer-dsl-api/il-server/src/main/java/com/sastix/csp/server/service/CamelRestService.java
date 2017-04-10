package com.sastix.csp.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Created by iskitsas on 4/8/17.
 */
@Service
public class CamelRestService {
    private static final Logger LOG = LoggerFactory.getLogger(CamelRestService.class);
    @Autowired
    ObjectMapper objectMapper;

    @Produce
    private ProducerTemplate producerTemplate;

    public <T> T send(String uri, Object obj ,Class<T> tClass) throws IOException {
        byte[] b = objectMapper.writeValueAsBytes(obj);
        Exchange exchange = producerTemplate.send(uri, new Processor() {
            public void process(Exchange exchange) throws Exception {
                exchange.getIn().setHeader(Exchange.HTTP_METHOD,"POST");
                exchange.getIn().setBody(b);
            }
        });

        String out = exchange.getOut().getBody(String.class);
        return (T)objectMapper.readValue(out, tClass);
    }
}
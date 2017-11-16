package com.intrasoft.csp.misp.client.config;

import com.intrasoft.csp.libraries.restclient.config.RestTemplateConfiguration;
import com.intrasoft.csp.libraries.restclient.exceptions.CspBusinessException;
import com.intrasoft.csp.libraries.restclient.handlers.ExceptionHandler;
import com.intrasoft.csp.libraries.restclient.service.RetryRestTemplate;
import com.intrasoft.csp.misp.client.MispAppClient;
import com.intrasoft.csp.misp.client.impl.MispAppClientImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class MispAppClientConfig {

    @Value("${misp.app.protocol}")
    private String protocol;

    @Value("${misp.app.host}")
    private String host;

    @Value("${misp.app.port}")
    private String port;

    @Value("${misp.app.authorization.key}")
    private String authorizationKey;

    @Value("${retry.backOffPeriod:5000}")
    private String backOffPeriod;

    @Value("${retry.maxAttempts:3}")
    private String maxAttempts;

    @Value("${client.ssl.enabled:false}")
    Boolean cspClientSslEnabled;

    @Value("${client.ssl.jks.keystore:path}")
    String cspClientSslJksKeystore;

    @Value("${client.ssl.jks.keystore.password:securedPass}")
    String cspClientSslJksKeystorePassword;

    @Autowired
    ResourcePatternResolver resourcePatternResolver;

    private static final ConcurrentHashMap<String, ExceptionHandler> SUPPORTED_EXCEPTIONS = new ConcurrentHashMap<>();

    static {
        SUPPORTED_EXCEPTIONS.put(CspBusinessException.class.getName(), CspBusinessException::new);
    }

    @Autowired
    @Qualifier("MispAppRestTemplate")
    RetryRestTemplate retryRestTemplate;

    @Bean(name="MispAppRestTemplate")
    public RetryRestTemplate getRetryRestTemplate() throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {
        RestTemplateConfiguration restTemplateConfiguration = new RestTemplateConfiguration(backOffPeriod, maxAttempts, cspClientSslEnabled, cspClientSslJksKeystore, cspClientSslJksKeystorePassword, resourcePatternResolver);
        return restTemplateConfiguration.getRestTemplateWithSupportedExceptions(SUPPORTED_EXCEPTIONS);
    }

    @Bean(name = "MispAppClient")
    public MispAppClient addMispEvent(){
        MispAppClient client = new MispAppClientImpl();
        client.setProtocolHostPortHeaders(protocol,host,port, authorizationKey);
        return client;
    }

}

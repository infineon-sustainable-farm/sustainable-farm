package com.infineonbit.sustainablefarm.modules.producttransformation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate Configuration
 * Provides RestTemplate bean for HTTP-based inter-module integration
 *
 * <p>The bean is used only by the Product Transformation integration adapters
 * (Plants, Crop Storage). Both call modules that run in the same application
 * today, but the contract is HTTP: without timeouts a slow or unreachable peer
 * blocks the calling thread for as long as the operating system keeps the socket
 * open, which is unbounded. The values below bound that wait: 2 s to establish a
 * connection, 5 s to read the response.
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.1.0
 */
@Configuration
public class RestTemplateConfig {

    private static final int CONNECT_TIMEOUT_MILLIS = 2_000;
    private static final int READ_TIMEOUT_MILLIS = 5_000;

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        requestFactory.setReadTimeout(READ_TIMEOUT_MILLIS);
        return new RestTemplate(requestFactory);
    }
}

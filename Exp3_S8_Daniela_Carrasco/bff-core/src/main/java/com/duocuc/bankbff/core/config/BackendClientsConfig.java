package com.duocuc.bankbff.core.config;

import com.duocuc.bankbff.core.security.ServiceTokenInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class BackendClientsConfig {

    @Bean
    public ClientHttpRequestFactory backendRequestFactory(
            @Value("${backend.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${backend.read-timeout-ms:3000}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        return factory;
    }

    @Bean
    public ServiceTokenInterceptor serviceTokenInterceptor(
            @Value("${service-auth.token-uri:http://localhost:9000/oauth2/token}") String tokenUri,
            @Value("${service-auth.client-id}") String clientId,
            @Value("${service-auth.client-secret}") String clientSecret,
            @Value("${service-auth.scope:ms.read ms.write}") String scope,
            ClientHttpRequestFactory backendRequestFactory) {
        return new ServiceTokenInterceptor(tokenUri, clientId, clientSecret, scope, backendRequestFactory);
    }

    @Bean
    public RestClient cuentasRestClient(@Value("${backend.ms-cuentas.base-url}") String baseUrl,
                                         ClientHttpRequestFactory backendRequestFactory,
                                         ServiceTokenInterceptor serviceTokenInterceptor) {
        return RestClient.builder().baseUrl(baseUrl).requestFactory(backendRequestFactory)
                .requestInterceptor(serviceTokenInterceptor).build();
    }

    @Bean
    public RestClient movimientosRestClient(@Value("${backend.ms-movimientos.base-url}") String baseUrl,
                                             ClientHttpRequestFactory backendRequestFactory,
                                             ServiceTokenInterceptor serviceTokenInterceptor) {
        return RestClient.builder().baseUrl(baseUrl).requestFactory(backendRequestFactory)
                .requestInterceptor(serviceTokenInterceptor).build();
    }

    @Bean
    public RestClient transaccionesRestClient(@Value("${backend.ms-transacciones.base-url}") String baseUrl,
                                                ClientHttpRequestFactory backendRequestFactory,
                                                ServiceTokenInterceptor serviceTokenInterceptor) {
        return RestClient.builder().baseUrl(baseUrl).requestFactory(backendRequestFactory)
                .requestInterceptor(serviceTokenInterceptor).build();
    }
}

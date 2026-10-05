package com.duocuc.bankbff.core.security;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

public class ServiceTokenInterceptor implements ClientHttpRequestInterceptor {

    private final RestClient tokenClient;
    private final String tokenUri;
    private final String clientId;
    private final String clientSecret;
    private final String scope;

    private String token;
    private Instant expiresAt = Instant.EPOCH;

    public ServiceTokenInterceptor(String tokenUri, String clientId, String clientSecret,
                                    String scope, ClientHttpRequestFactory requestFactory) {
        this.tokenUri = tokenUri;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.scope = scope;
        this.tokenClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                         ClientHttpRequestExecution execution) throws IOException {
        request.getHeaders().setBearerAuth(currentToken());
        return execution.execute(request, body);
    }

    private synchronized String currentToken() {
        if (token == null || Instant.now().isAfter(expiresAt.minusSeconds(30))) {
            refresh();
        }
        return token;
    }

    private void refresh() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("scope", scope);
        try {
            Map<String, Object> response = tokenClient.post()
                    .uri(tokenUri)
                    .headers(h -> h.setBasicAuth(clientId, clientSecret))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            if (response == null || response.get("access_token") == null) {
                throw new IllegalStateException("Respuesta sin access_token");
            }
            this.token = String.valueOf(response.get("access_token"));
            long expiresIn = response.get("expires_in") instanceof Number n ? n.longValue() : 300L;
            this.expiresAt = Instant.now().plusSeconds(expiresIn);
        } catch (Exception e) {
            throw new ResourceAccessException("No se pudo obtener el token de servicio del auth-server",
                    new IOException(e));
        }
    }
}

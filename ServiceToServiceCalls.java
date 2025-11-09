package com.example.microservice.security;

import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class ServiceToServiceCalls {

    // Using RestTemplate - traditional approach
    public void callDownstreamServiceWithRestTemplate() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String token = null;
        
        if (auth != null && auth.getCredentials() instanceof String) {
            token = (String) auth.getCredentials();
        }
        
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        
        // Use headers with RestTemplate exchange methods
    }

    // Using WebClient - modern reactive approach
    public WebClient downstreamServiceWebClient() {
        return WebClient.builder()
            .filter((request, next) -> {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getCredentials() instanceof String) {
                    String token = (String) auth.getCredentials();
                    return next.exchange(
                        ClientRequest.from(request)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build()
                    );
                }
                return next.exchange(request);
            })
            .baseUrl("http://downstream-service/api")
            .build();
    }

    // Using Feign Client - for service-to-service communication
}
package com.chhavi.busbuddy_backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI busBuddyOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("BusBuddy Backend API")
                        .version("v1")
                        .description("Production-grade backend APIs for BusBuddy bus, route, stop, company, and authentication operations."));
    }
}

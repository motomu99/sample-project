package com.example.boxwrapper.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger) 設定
 */
@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI boxApiOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Box SDK Wrapper REST API")
                .description("Box Java SDKをラップしたREST API")
                .version("v1.0.0")
                .contact(new Contact()
                    .name("Box Wrapper API")
                    .email("support@example.com")))
            .addSecurityItem(new SecurityRequirement().addList("API Key"))
            .components(new Components()
                .addSecuritySchemes("API Key",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("X-API-Key")));
    }
}

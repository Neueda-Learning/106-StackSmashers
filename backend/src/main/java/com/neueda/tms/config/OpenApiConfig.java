package com.neueda.tms.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger documentation configuration.
 *
 * Swagger UI is served at {@code /api/swagger-ui.html} and the raw OpenAPI
 * spec at {@code /api/v3/api-docs} (context-path {@code /api} applies, see
 * application.properties). Both paths are whitelisted in SecurityConfig so
 * they are reachable without authentication.
 *
 * A "bearerAuth" security scheme is registered so the Swagger UI "Authorize"
 * button can be used to attach a JWT (obtained via POST /auth/login) to
 * subsequent try-it-out requests.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI tmsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Transaction Monitoring System API")
                        .description("Financial Transaction Monitoring and Alerting System")
                        .version("1.0.0")
                        .contact(new Contact().name("Neueda - StackSmashers")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}

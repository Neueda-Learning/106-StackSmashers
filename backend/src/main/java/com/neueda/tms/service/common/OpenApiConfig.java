package com.neueda.tms.service.common;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI tmsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Transaction Monitoring System API")
                        .version("v1")
                        .description("REST API documentation for the Transaction Monitoring System backend")
                        .contact(new Contact()
                                .name("TMS Team")
                                .email("support@tms.local")));
    }
}

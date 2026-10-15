package com.mael.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI: http://localhost:8080/swagger-ui.html
 * Bấm Authorize, dán token lấy từ /api/auth/login.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI maelOpenApi() {
        return new OpenAPI()
                .info(new Info().title("MAEL API").version("v1")
                        .description("Multi-Agent Edtech Learning - học Toán THCS cùng 3 AI"))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }
}

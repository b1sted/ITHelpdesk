package ru.basted.ithelpdesk.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {
    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI itHelpdeskOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("IT Helpdesk API")
                        .description("API системы корпоративной технической поддержки. "
                                + "Сервис обрабатывает заявки сотрудников, распределяет их между специалистами "
                                + "и ведёт переписку в режиме диалога.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Служба технической поддержки")
                                .email("support@company.ru"))
                        .license(new License()
                                .name("Внутреннее использование")
                                .url("https://company.ru")))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Вставьте JWT-токен, полученный через /api/v1/auth/login")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }
}
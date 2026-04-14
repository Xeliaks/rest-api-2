package com.example.tasks.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI tasksOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Tasks API")
                        .version("1.0.0")
                        .description(
                                "Example REST API for tasks. Data is stored in memory and resets when the app stops. "
                                        + "Paginated HAL resources live under /api/hypermedia/tasks.")
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}

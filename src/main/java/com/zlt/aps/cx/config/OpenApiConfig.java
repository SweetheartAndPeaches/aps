package com.zlt.aps.cx.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI配置类
 *
 * @author APS Team
 * @since 2.0.0
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("金宇轮胎APS成型排程系统API")
                        .version("2.0.0")
                        .description("金宇轮胎APS成型排程系统RESTful API文档")
                        .contact(new Contact()
                                .name("APS Team")
                                .email("aps@zlt.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}

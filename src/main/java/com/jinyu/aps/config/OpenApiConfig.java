package com.jinyu.aps.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI配置类 (使用SpringDoc替代SpringFox)
 *
 * @author APS Team
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("金宇轮胎APS系统-成型排程模块 API文档")
                        .description("成型排程系统RESTful API接口文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("APS Team")
                                .email(""))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}

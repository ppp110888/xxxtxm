package com.xxxtxm.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI 配置
 *
 * <p>Knife4j 提供增强版 Swagger UI，访问地址：http://localhost:8080/doc.html</p>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI codeMateOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("CodeMate API 文档")
                        .description("AI 伴学编程平台 — RESTful API 接口文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("CodeMate Team")));
    }
}

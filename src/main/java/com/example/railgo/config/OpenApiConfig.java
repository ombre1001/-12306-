package com.example.railgo.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI railGoOpenAPI() {

        Server localServer = new Server()
                .url("http://localhost:8080")
                .description("本地开发环境");

        SecurityScheme bearerAuth = new SecurityScheme()
                .name("bearerAuth")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("登录后返回的 accessToken");

        Info info = new Info()
                .title("RailGo 火车票售票系统接口文档")
                .description("数据库课程设计后端 REST API")
                .version("1.0.0")
                .contact(new Contact()
                        .name("RailGo 项目组"))
                .license(new License()
                        .name("仅用于课程设计"));

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer))
                .components(new Components()
                        .addSecuritySchemes(
                                "bearerAuth",
                                bearerAuth
                        ));
    }
}
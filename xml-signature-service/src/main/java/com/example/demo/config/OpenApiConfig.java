package com.example.demo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ISO 20022 XML Signature Service API")
                        .version("1.0.0")
                        .description("""
                                API documentation for the ISO 20022 XML Signature Service.

                                This service provides secure XML Digital Signature (XMLDSig)
                                and XAdES signing and verification capabilities for
                                ISO 20022 financial messages.

                                Features include:
                                • XML Digital Signature (XMLDSig)
                                • XAdES-BES compliant electronic signatures
                                • XML signature verification
                                • Certificate and keystore management
                                • Canonicalization (Exclusive XML C14N)
                                • SHA-256 digest generation
                                • RSA-based digital signatures
                                • Signature validation and integrity verification

                                This API is designed for payment gateways, financial
                                institutions, banking systems, and payment switches
                                exchanging ISO 20022 messages securely.
                                """)
                        .contact(new Contact()
                                .name("API Support")
                                .email("support@yourcompany.com")
                                .url("https://yourcompany.com"))
                        .license(new License()
                                .name("Proprietary License")
                                .url("https://yourcompany.com/license")))

                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))

                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
package so.cb.adapter.shared.config;

import io.swagger.v3.oas.models.Components;
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
                        .title("Somali Payment Switch (SPS) Bank Payment Adapter API")
                        .version("1.0.0")
                        .description("""
                                API documentation for the Somali Payment Switch (SPS) Bank Payment Adapter Service.

                                Educational & Practice Lab Project:
                                This service is built strictly for educational, research, and practice purposes
                                to demonstrate real-world implementation of ISO 20022 XML Digital Signatures,
                                Central Bank PKI Certificate Trust Validation, and Payment Switch Adapters.

                                Core Functional Responsibilities:
                                • Outgoing Workflow (Core Banking -> Adapter -> SPS): Validates JSON payment requests, maps JSON to ISO 20022 XML (e.g. pacs.008), digitally signs XML using bank's private key (private.pem), and transmits to SPS.
                                • Incoming Workflow (SPS -> Adapter -> Core Banking): Validates Central Bank Root CA trust anchor (chain.pem), verifies W3C XML Digital Signatures, validates ISO 20022 XML payload, transforms XML to bank JSON format, and forwards requests to Core Banking.
                                • Startup Security Validation: Validates local presence, X.509 validity dates, Central Bank CA trust, and RSA keypair matching for chain.pem, certificate.pem, and private.pem on application boot.
                                """)
                        .contact(new Contact()
                                .name("SPS Integration Team")
                                .email("adapter.support@cb.so")
                                .url("https://cb.so"))
                        .license(new License()
                                .name("Proprietary License")
                                .url("https://cb.so/license")))

                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .name("bearerAuth")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("""
                                                JWT Authorization header using Bearer scheme.

                                                Format:
                                                Authorization: Bearer <your_token>
                                                """)))

                .addSecurityItem(
                        new SecurityRequirement()
                                .addList("bearerAuth")
                );
    }
}
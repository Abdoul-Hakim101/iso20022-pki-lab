package so.cb.pki.shared.config;

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
                        .title("Central Bank PKI Certificate Management Service API")
                        .version("1.0.0")
                        .description("""
                                API documentation for the Central Bank PKI Certificate Management Service.

                                This service acts as the Certificate Authority (CA) and Certificate Management System (CMS)
                                within the ISO 20022 lab network environment. It manages the complete production lifecycle
                                of PKI certificates, including Root and Intermediate CAs, CSR approval, certificate issuance,
                                renewal, and revocation.

                                Core Architecture Modules & Responsibilities:
                                • Certificate Signing Request (CSR) Enrollment: Processes incoming CSR uploads (containing public keys and organization metadata) for bank validation.
                                • Certificate Issuance: Generates cryptographically signed leaf certificates (certificate.pem) and chains (chain.pem) using CA private keys.
                                • Certificate Lifecycle Management: Exposes endpoints for certificate retrieval, renewal, and revocation.
                                • Validation Services: Publishes Certificate Revocation Lists (CRL) and exposes certificate status queries (OCSP).

                                Key Endpoint Operations:
                                • POST /csr - Upload Certificate Signing Request (CSR)
                                • POST /renew - Renew an existing certificate before expiry
                                • GET /certificate/{id} - Retrieve issued certificate by ID
                                • GET /chain - Download the Intermediate/Root CA certificate chain
                                • GET /crl - Download the Certificate Revocation List
                                • GET /status - Query the real-time status of a certificate
                                """)
                        .contact(new Contact()
                                .name("PKI Engineering Team")
                                .email("pki.support@cb.so")
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

                                                This token is strictly required for authenticated operator, 
                                                supervisor, and confidential-case handler endpoints.
                                                """)))

                .addSecurityItem(
                        new SecurityRequirement()
                                .addList("bearerAuth")
                );
    }
}
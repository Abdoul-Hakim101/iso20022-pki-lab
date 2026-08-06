package so.cb.adapter.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "adapter.certs")
public class AdapterProperties {

    private String dir = "./certs";
    private String caCertFile = "chain.pem";
    private String bankCertFile = "certificate.pem";
    private String privateKeyFile = "private.pem";
    private boolean strictStartupCheck = true;
}

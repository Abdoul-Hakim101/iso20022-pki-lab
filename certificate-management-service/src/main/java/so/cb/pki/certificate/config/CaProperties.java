package so.cb.pki.certificate.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "pki.ca")
public class CaProperties {

    private String dir;
    private String privateKeyFile;
    private String certificateFile;
    private String issuerDn;
    private int validityYears;
}

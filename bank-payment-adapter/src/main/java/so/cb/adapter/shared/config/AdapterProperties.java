package so.cb.adapter.shared.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "adapter")
public class AdapterProperties {

    private String bankBic = "PMRBSOMM";

    private Certs certs = new Certs();

    @Getter
    @Setter
    public static class Certs {
        private String dir = "./certs";
        private String caCertFile = "chain.pem";
        private String bankCertFile = "certificate.pem";
        private String privateKeyFile = "private.pem";
        private boolean strictStartupCheck = true;
    }

    public String getDir() {
        return certs != null ? certs.getDir() : "./certs";
    }

    public void setDir(String dir) {
        if (certs == null) certs = new Certs();
        certs.setDir(dir);
    }

    public String getCaCertFile() {
        return certs != null ? certs.getCaCertFile() : "chain.pem";
    }

    public void setCaCertFile(String caCertFile) {
        if (certs == null) certs = new Certs();
        certs.setCaCertFile(caCertFile);
    }

    public String getBankCertFile() {
        return certs != null ? certs.getBankCertFile() : "certificate.pem";
    }

    public void setBankCertFile(String bankCertFile) {
        if (certs == null) certs = new Certs();
        certs.setBankCertFile(bankCertFile);
    }

    public String getPrivateKeyFile() {
        return certs != null ? certs.getPrivateKeyFile() : "private.pem";
    }

    public void setPrivateKeyFile(String privateKeyFile) {
        if (certs == null) certs = new Certs();
        certs.setPrivateKeyFile(privateKeyFile);
    }

    public boolean isStrictStartupCheck() {
        return certs == null || certs.isStrictStartupCheck();
    }

    public void setStrictStartupCheck(boolean strictStartupCheck) {
        if (certs == null) certs = new Certs();
        certs.setStrictStartupCheck(strictStartupCheck);
    }
}

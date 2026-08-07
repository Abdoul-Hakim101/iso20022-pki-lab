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
    private Signature signature = new Signature();

    @Getter
    @Setter
    public static class Certs {
        private String dir = "./certs";
        private String caCertFile = "chain.pem";
        private String bankCertFile = "certificate.pem";
        private String privateKeyFile = "private.pem";
        private String privateKeyPassphrase = "";
        private boolean strictStartupCheck = true;
    }

    @Getter
    @Setter
    public static class Signature {
        private String algorithm = "SHA256withRSA";
        private String canonicalization = "EXCLUSIVE";
        private boolean xadesEnabled = true;
        private String xadesNamespace = "http://uri.etsi.org/01903/v1.3.2#";
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

    public String getPrivateKeyPassphrase() {
        return certs != null ? certs.getPrivateKeyPassphrase() : "";
    }

    public void setPrivateKeyPassphrase(String privateKeyPassphrase) {
        if (certs == null) certs = new Certs();
        certs.setPrivateKeyPassphrase(privateKeyPassphrase);
    }

    public boolean isStrictStartupCheck() {
        return certs == null || certs.isStrictStartupCheck();
    }

    public void setStrictStartupCheck(boolean strictStartupCheck) {
        if (certs == null) certs = new Certs();
        certs.setStrictStartupCheck(strictStartupCheck);
    }
}

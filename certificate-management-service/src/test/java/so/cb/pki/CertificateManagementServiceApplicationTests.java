package so.cb.pki;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import so.cb.pki.csr.service.CsrService;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = Application.class)
public class CertificateManagementServiceApplicationTests {

	@Autowired
	private CsrService csrService;

	@Test
	void contextLoads() {
		assertNotNull(csrService, "CsrService bean should be loaded into Spring Context");
	}

}

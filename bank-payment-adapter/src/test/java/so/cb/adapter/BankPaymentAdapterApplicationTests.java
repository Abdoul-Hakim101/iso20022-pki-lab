package so.cb.adapter;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "adapter.certs.strict-startup-check=false")
class BankPaymentAdapterApplicationTests {

	@Test
	void contextLoads() {
	}

}

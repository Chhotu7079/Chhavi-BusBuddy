package com.chhavi.busbuddy_backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
		"APP_SECURITY_JWT_SECRET=12345678901234567890123456789012",
		"APP_SECURITY_JWT_EXPIRATION_MS=60000",
		"FIREBASE_SERVICE_ACCOUNT_PATH=E:/Bus/BusBuddy/backend/secrets/busbuddy-729d4-8a30dc491cc5.json",
		"FIREBASE_DATABASE_URL=https://busbuddy-729d4-default-rtdb.firebaseio.com/"
})
class BusBuddyBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}

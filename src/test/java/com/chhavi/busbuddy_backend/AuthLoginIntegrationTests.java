package com.chhavi.busbuddy_backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.security.jwt.secret=12345678901234567890123456789012",
        "app.security.jwt.expiration-ms=60000",
        "firebase.service-account-path=E:/Bus/BusBuddy/backend/secrets/busbuddy-729d4-8a30dc491cc5.json",
        "firebase.database-url=https://busbuddy-729d4-default-rtdb.firebaseio.com/"
})
class AuthLoginIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void authLoginShouldRejectMissingBody() throws Exception {
        mockMvc.perform(post("/auth/company/login"))
                .andExpect(status().isBadRequest());
    }


    @Test
    void authLoginShouldRejectBlankFirebaseIdToken() throws Exception {
        mockMvc.perform(post("/auth/company/login")
                        .contentType("application/json")
                        .content("{\"firebaseIdToken\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"));
    }

    @Test
    void authLoginShouldRejectInvalidFirebaseIdToken() throws Exception {
        mockMvc.perform(post("/auth/company/login")
                        .contentType("application/json")
                        .content("{\"firebaseIdToken\":\"not-a-valid-token\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid Firebase ID token"));
    }
}

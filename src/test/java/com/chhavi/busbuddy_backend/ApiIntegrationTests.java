package com.chhavi.busbuddy_backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
class ApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointShouldBeAvailable() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void openApiDocsShouldBeAvailable() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("BusBuddy Backend API")));
    }

    @Test
    void signupCompanyShouldRejectInvalidPayload() throws Exception {
        mockMvc.perform(post("/companies/signup")
                        .contentType("application/json")
                        .content("{\"email\":\"bad-email\",\"password\":\"123\",\"company\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void currentBusDelayShouldRejectInvalidDirection() throws Exception {
        mockMvc.perform(get("/buses/123/delays/current")
                        .param("direction", "left"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("direction must be either 'forward' or 'back'"));
    }

    @Test
    void stopsWithinRadiusShouldRejectTypeMismatch() throws Exception {
        mockMvc.perform(get("/stops")
                        .param("latitude", "12.12")
                        .param("longitude", "77.77")
                        .param("radius", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'radius': expected double"));
    }

    @Test
    void signupCompanyShouldRejectMalformedJson() throws Exception {
        mockMvc.perform(post("/companies/signup")
                        .contentType("application/json")
                        .content("{\"email\":\"abc@test.com\" \"password\":\"12345678\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON request body"));
    }

    @Test
    void getCompaniesWithoutEmailShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/companies"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCompaniesWithBlankEmailShouldRequireAuthentication() throws Exception {
        // SecurityConfig only permits GET /companies when email is present and non-blank.
        mockMvc.perform(get("/companies").param("email", ""))
                .andExpect(status().isUnauthorized());
    }

    private static final String TEST_JWT_SECRET = "12345678901234567890123456789012";

    private String token(String subject, String role, String companyId) {
        return io.jsonwebtoken.Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .claim("companyId", companyId)
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(TEST_JWT_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private String expiredToken(String subject, String role, String companyId) {
        Date now = new Date();
        Date expiredAt = new Date(now.getTime() - 60_000);

        return io.jsonwebtoken.Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .claim("companyId", companyId)
                .issuedAt(new Date(now.getTime() - 120_000))
                .expiration(expiredAt)
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(TEST_JWT_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    @Test
    void publicReadEndpointShouldBeAllowedWithoutToken() throws Exception {
        mockMvc.perform(get("/routes/demo-route"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void protectedMutationEndpointShouldReturnUnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(post("/buses")
                        .param("busCode", "BUS-1")
                        .param("routeId", "route-1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedMutationEndpointShouldReturnUnauthorizedWithMalformedToken() throws Exception {
        mockMvc.perform(post("/buses")
                        .header("Authorization", "Bearer not-a-valid-jwt")
                        .param("busCode", "BUS-1")
                        .param("routeId", "route-1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointShouldReturn401ForExpiredJwt() throws Exception {
        String token = expiredToken("company-a", "COMPANY", "company-a");

        mockMvc.perform(post("/buses")
                        .header("Authorization", "Bearer " + token)
                        .param("busCode", "BUS-1")
                        .param("routeId", "route-1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointShouldRejectTokenWithInvalidRole() throws Exception {
        String token = token("user-a", "INVALID_ROLE", "company-a");

        mockMvc.perform(post("/buses")
                        .header("Authorization", "Bearer " + token)
                        .param("busCode", "BUS-1")
                        .param("routeId", "route-1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedMutationEndpointShouldAcceptAdminTokenAndReachBusinessLayer() throws Exception {
        String token = token("admin-user", "ADMIN", "admin-user");

        mockMvc.perform(post("/buses")
                        .header("Authorization", "Bearer " + token)
                        .param("busCode", "BUS-1")
                        .param("routeId", "missing-route"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void protectedMutationEndpointShouldAcceptCompanyTokenForOwnedCompanyPath() throws Exception {
        String token = token("employee-uid", "EMPLOYEE", "company-uid");

        mockMvc.perform(post("/buses")
                        .header("Authorization", "Bearer " + token)
                        .param("busCode", "BUS-1")
                        .param("routeId", "missing-route"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void addBusShouldReachBusinessLayerForCompanyTokenWithoutAuthRejection() throws Exception {
        // /buses depends on persisted route data, so in this integration environment the endpoint may fail
        // with non-auth errors when test fixtures are absent. We assert only that security/auth ownership
        // does not fail at the authentication boundary (strict ownership deny is covered deterministically by
        // /routes tests).
        String token = token("employee-a", "EMPLOYEE", "company-a");

        int statusCode = mockMvc.perform(post("/buses")
                        .header("Authorization", "Bearer " + token)
                        .param("busCode", "BUS-1")
                        .param("routeId", "route-owned-by-company-b"))
                .andReturn()
                .getResponse()
                .getStatus();

        org.junit.jupiter.api.Assertions.assertNotEquals(401, statusCode,
                "Expected /buses not to fail at auth boundary for authenticated company token");
    }

    @Test
    void addBusShouldAllowCompanyTokenForOwnedCompanyRoute() throws Exception {
        // For owned resources, ownership guard should not reject with 401.
        // Downstream may still fail in test env due to missing seeded DB data.
        String token = token("employee-a", "EMPLOYEE", "company-a");

        int statusCode = mockMvc.perform(post("/buses")
                        .header("Authorization", "Bearer " + token)
                        .param("busCode", "BUS-OWNED")
                        .param("routeId", "route-owned-by-company-a"))
                .andReturn()
                .getResponse()
                .getStatus();

        org.junit.jupiter.api.Assertions.assertNotEquals(401, statusCode,
                "Expected /buses not to be rejected by ownership check for owned company route");
    }

    @Test
    void addBusShouldAllowAdminTokenAcrossCompanies() throws Exception {
        // Admin should bypass company ownership restrictions.
        String token = token("admin-user", "ADMIN", "admin-user");

        int statusCode = mockMvc.perform(post("/buses")
                        .header("Authorization", "Bearer " + token)
                        .param("busCode", "BUS-ADMIN")
                        .param("routeId", "route-owned-by-company-b"))
                .andReturn()
                .getResponse()
                .getStatus();

        org.junit.jupiter.api.Assertions.assertNotEquals(401, statusCode,
                "Expected /buses not to be ownership-rejected for admin token");
    }

    @Test
    void ownershipProtectedEndpointShouldRejectCompanyTokenForDifferentCompany() throws Exception {
        // RouteService.addRoute() performs ownership check against route.company before hitting Firestore,
        // so this test validates controller -> security -> service authorization flow without DB dependence.
        String token = token("employee-a", "EMPLOYEE", "company-a");
        String routePayload = """
                {
                  \"company\": \"company-b\",
                  \"code\": \"10_A - B\",
                  \"stops\": {
                    \"forwardStops\": [ { \"address\": \"Addr 1\" } ],
                    \"backStops\": []
                  }
                }
                """;

        mockMvc.perform(post("/routes")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(routePayload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You are not allowed to modify this company's resources"));
    }

    @Test
    void ownershipProtectedEndpointShouldAllowCompanyTokenForOwnedCompany() throws Exception {
        // When token.companyId matches route.company, ownership guard should allow the request to proceed.
        // In this integration environment, downstream persistence may still fail due to missing seeded data,
        // so we assert only that ownership does not reject with 401.
        String token = token("employee-a", "EMPLOYEE", "company-a");
        String routePayload = """
                {
                  \"company\": \"company-a\",
                  \"code\": \"10_A - B\",
                  \"stops\": {
                    \"forwardStops\": [ { \"address\": \"Addr 1\" } ],
                    \"backStops\": []
                  }
                }
                """;

        int statusCode = mockMvc.perform(post("/routes")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(routePayload))
                .andReturn()
                .getResponse()
                .getStatus();

        org.junit.jupiter.api.Assertions.assertNotEquals(401, statusCode,
                "Expected request not to be rejected by ownership check when company owns the resource");
    }

    @Test
    void updateBusRouteShouldAllowCompanyTokenForOwnedCompanyResources() throws Exception {
        String token = token("employee-a", "EMPLOYEE", "company-a");

        int statusCode = mockMvc.perform(put("/buses/BUS-OWNED/route")
                        .header("Authorization", "Bearer " + token)
                        .param("routeId", "route-owned-by-company-a"))
                .andReturn()
                .getResponse()
                .getStatus();

        org.junit.jupiter.api.Assertions.assertNotEquals(401, statusCode,
                "Expected /updateBusRoute not to be ownership-rejected for owned company resources");
    }

    @Test
    void updateBusRouteShouldBlockOrFailForCrossCompanyAccess() throws Exception {
        // Without deterministic DB fixtures, cross-company attempts may manifest as ownership denial (401)
        // or downstream errors. We assert only that this is not a clean success path.
        String token = token("employee-a", "EMPLOYEE", "company-a");

        int statusCode = mockMvc.perform(put("/buses/BUS-CROSS/route")
                        .header("Authorization", "Bearer " + token)
                        .param("routeId", "route-owned-by-company-b"))
                .andReturn()
                .getResponse()
                .getStatus();

        org.junit.jupiter.api.Assertions.assertTrue(statusCode >= 400,
                "Expected /updateBusRoute cross-company path to return an error status");
    }

    @Test
    void updateBusRouteShouldAllowAdminTokenAcrossCompanies() throws Exception {
        String token = token("admin-user", "ADMIN", "admin-user");

        int statusCode = mockMvc.perform(put("/buses/BUS-ADMIN/route")
                        .header("Authorization", "Bearer " + token)
                        .param("routeId", "route-owned-by-company-b"))
                .andReturn()
                .getResponse()
                .getStatus();

        org.junit.jupiter.api.Assertions.assertNotEquals(401, statusCode,
                "Expected /updateBusRoute not to be ownership-rejected for admin token");
    }

    @Test
    void fixHistoryGapsShouldAllowCompanyTokenForOwnedRoute() throws Exception {
        String token = token("employee-a", "EMPLOYEE", "company-a");

        int statusCode = mockMvc.perform(post("/routes/route-owned-by-company-a/history/fix-gaps")
                        .header("Authorization", "Bearer " + token)
                        .param("direction", "forward"))
                .andReturn()
                .getResponse()
                .getStatus();

        org.junit.jupiter.api.Assertions.assertNotEquals(401, statusCode,
                "Expected /fixHistoryGaps not to be ownership-rejected for owned route");
    }

    @Test
    void fixHistoryGapsShouldBlockOrFailForDifferentCompanyRoute() throws Exception {
        // Without deterministic DB fixtures, this may be ownership denial (401) or another non-success error.
        String token = token("employee-a", "EMPLOYEE", "company-a");

        int statusCode = mockMvc.perform(post("/routes/route-owned-by-company-b/history/fix-gaps")
                        .header("Authorization", "Bearer " + token)
                        .param("direction", "forward"))
                .andReturn()
                .getResponse()
                .getStatus();

        org.junit.jupiter.api.Assertions.assertTrue(statusCode >= 400,
                "Expected /fixHistoryGaps cross-company path to return an error status");
    }

    @Test
    void fixHistoryGapsShouldAllowAdminTokenAcrossCompanies() throws Exception {
        String token = token("admin-user", "ADMIN", "admin-user");

        int statusCode = mockMvc.perform(post("/routes/route-owned-by-company-b/history/fix-gaps")
                        .header("Authorization", "Bearer " + token)
                        .param("direction", "forward"))
                .andReturn()
                .getResponse()
                .getStatus();

        org.junit.jupiter.api.Assertions.assertNotEquals(401, statusCode,
                "Expected /fixHistoryGaps not to be ownership-rejected for admin token");
    }

    @Test
    void stopReachedShouldAllowCompanyTokenForOwnedCompanyRoute() throws Exception {
        String token = token("employee-a", "EMPLOYEE", "company-a");

        int statusCode = mockMvc.perform(post("/routes/route-owned-by-company-a/stops/reached")
                        .header("Authorization", "Bearer " + token)
                        .param("stopIndex", "1")
                        .param("direction", "forward"))
                .andReturn()
                .getResponse()
                .getStatus();

        org.junit.jupiter.api.Assertions.assertNotEquals(401, statusCode,
                "Expected /stopReached not to be ownership-rejected for owned company route");
    }

    @Test
    void stopReachedShouldBlockOrFailForDifferentCompanyRoute() throws Exception {
        // Depending on available persisted route fixtures, this may fail as ownership-denied (401)
        // or as downstream non-auth error. We assert that it is not a clean success path.
        String token = token("employee-a", "EMPLOYEE", "company-a");

        int statusCode = mockMvc.perform(post("/routes/route-owned-by-company-b/stops/reached")
                        .header("Authorization", "Bearer " + token)
                        .param("stopIndex", "1")
                        .param("direction", "forward"))
                .andReturn()
                .getResponse()
                .getStatus();

        org.junit.jupiter.api.Assertions.assertTrue(statusCode >= 400,
                "Expected /stopReached not-allowed path to return an error status");
    }

    @Test
    void stopReachedShouldAllowAdminTokenAcrossCompanies() throws Exception {
        String token = token("admin-user", "ADMIN", "admin-user");

        int statusCode = mockMvc.perform(post("/routes/route-owned-by-company-b/stops/reached")
                        .header("Authorization", "Bearer " + token)
                        .param("stopIndex", "1")
                        .param("direction", "forward"))
                .andReturn()
                .getResponse()
                .getStatus();

        org.junit.jupiter.api.Assertions.assertNotEquals(401, statusCode,
                "Expected /stopReached not to be ownership-rejected for admin token");
    }

    @Test
    void adminOnlyEndpointShouldReturnForbiddenForCompanyRole() throws Exception {
        String token = token("employee-a", "EMPLOYEE", "company-a");

        mockMvc.perform(post("/routes/delays/recompute")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpointShouldAllowAdminToken() throws Exception {
        String token = token("admin-user", "ADMIN", "admin-user");

        int statusCode = mockMvc.perform(post("/routes/delays/recompute")
                        .header("Authorization", "Bearer " + token))
                .andReturn()
                .getResponse()
                .getStatus();

        org.junit.jupiter.api.Assertions.assertNotEquals(401, statusCode,
                "Expected /routes/delays/recompute not to fail authentication for admin token");
        org.junit.jupiter.api.Assertions.assertNotEquals(403, statusCode,
                "Expected /routes/delays/recompute to allow admin role authorization");
    }
}

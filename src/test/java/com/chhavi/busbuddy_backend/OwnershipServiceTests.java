package com.chhavi.busbuddy_backend;

import com.chhavi.busbuddy_backend.exception.ApplicationException;
import com.chhavi.busbuddy_backend.exception.ResourceNotFoundException;
import com.chhavi.busbuddy_backend.security.OwnershipService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OwnershipServiceTests {

    @Test
    void shouldReturnOwnerForKnownRoute() {
        OwnershipService service = new StubOwnershipService(
                Map.of("route-1", "company-a"),
                Map.of("bus-1", "company-a"));

        assertEquals("company-a", service.getRouteOwnerCompanyId("route-1"));
    }

    @Test
    void shouldThrowWhenRouteOwnerMissing() {
        OwnershipService service = new StubOwnershipService(Map.of(), Map.of());

        assertThrows(ResourceNotFoundException.class, () -> service.getRouteOwnerCompanyId("missing-route"));
    }

    @Test
    void shouldThrowWhenBusOwnerMissing() {
        OwnershipService service = new StubOwnershipService(Map.of(), Map.of());

        assertThrows(ResourceNotFoundException.class, () -> service.getBusOwnerCompanyId("missing-bus"));
    }

    private static class StubOwnershipService implements OwnershipService {
        private final Map<String, String> routeOwners;
        private final Map<String, String> busOwners;

        private StubOwnershipService(Map<String, String> routeOwners, Map<String, String> busOwners) {
            this.routeOwners = routeOwners;
            this.busOwners = busOwners;
        }

        @Override
        public String getRouteOwnerCompanyId(String routeId) {
            String owner = routeOwners.get(routeId);
            if (owner == null) {
                throw new ResourceNotFoundException("Route not found with id: " + routeId);
            }
            return owner;
        }

        @Override
        public String getBusOwnerCompanyId(String busId) {
            String owner = busOwners.get(busId);
            if (owner == null) {
                throw new ResourceNotFoundException("Bus not found with id: " + busId);
            }
            return owner;
        }
    }
}

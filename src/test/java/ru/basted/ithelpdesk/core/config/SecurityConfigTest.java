package ru.basted.ithelpdesk.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import ru.basted.ithelpdesk.auth.security.JwtAuthenticationFilter;
import ru.basted.ithelpdesk.core.security.RestAuthenticationHandler;

class SecurityConfigTest {
    @Test
    void jwtFilterIsNotAutoRegisteredInServletContainer() {
        JwtAuthenticationFilter filter = mock(JwtAuthenticationFilter.class);
        SecurityConfig config = new SecurityConfig(
                filter,
                mock(RestAuthenticationHandler.class)
        );

        var registration = config.jwtAuthenticationFilterRegistration(filter);

        assertFalse(registration.isEnabled());
        assertEquals(filter, registration.getFilter());
    }
}

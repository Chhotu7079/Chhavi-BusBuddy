package com.chhavi.busbuddy_backend.security;

import com.chhavi.busbuddy_backend.constant.UserRole;
import com.chhavi.busbuddy_backend.exception.ForbiddenException;
import com.chhavi.busbuddy_backend.exception.UnauthorizedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthorizationService {

    public AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new UnauthorizedException("Authentication is required");
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof AuthenticatedUser user)) {
            throw new UnauthorizedException("Invalid authenticated principal");
        }
        return user;
    }

    public void requireAdminOrCompanyOwnership(String companyId) {
        AuthenticatedUser user = currentUser();
        if (user.getRole() == UserRole.ADMIN) {
            return;
        }
        if ((user.getRole() == UserRole.OWNER || user.getRole() == UserRole.EMPLOYEE)
                && companyId != null && companyId.equals(user.getCompanyId())) {
            return;
        }
        throw new ForbiddenException("You are not allowed to modify this company's resources");
    }

    public void requireAdmin() {
        AuthenticatedUser user = currentUser();
        if (user.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Admin access is required");
        }
    }
}

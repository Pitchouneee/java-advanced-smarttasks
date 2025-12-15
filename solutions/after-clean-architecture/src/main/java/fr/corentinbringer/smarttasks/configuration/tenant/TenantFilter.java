package fr.corentinbringer.smarttasks.configuration.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TenantFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        String path = req.getServletPath();

        return path.startsWith("/swagger-ui/")
                || path.equals("/swagger-ui.html")
                || path.equals("/v3/api-docs")
                || path.startsWith("/v3/api-docs/")
                || path.equals("/error");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String tenantId = null;

        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            tenantId = jwt.getSubject();
        }

        if (tenantId == null) {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required or JWT invalid.");
            return;
        }

        TenantContext.setTenant(tenantId);
        try {
            chain.doFilter(req, res);
        } finally {
            TenantContext.clear();
        }
    }
}

package dev.ayoubelb25.mapex.resources;

import java.util.List;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

import dev.ayoubelb25.mapex.resources.error.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST endpoints for checking authentication state and logging out.
 */
@Path("auth")
public class AuthResource {

    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    public Response me() {
        Authentication auth = currentAuthentication();
        if (auth == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new ApiError(401, "Unauthorized",
                            "No authenticated session. POST /mapex/login with 'username' and 'password' first."))
                    .build();
        }
        return Response.ok(new Identity(auth.getName(), authorities(auth))).build();
    }

    @POST
    @Path("logout")
    @Produces(MediaType.APPLICATION_JSON)
    public Response logout(@Context HttpServletRequest request,
            @Context HttpServletResponse response) {

        Authentication auth = currentAuthentication();
        String username = (auth != null) ? auth.getName() : null;

        new SecurityContextLogoutHandler().logout(request, response, auth);

        String message = (username != null)
                ? "Logged out '" + username + "'. The session is no longer valid."
                : "No authenticated session to log out; nothing changed.";
        return Response.ok(new LogoutOutcome(username != null, username, message)).build();
    }

    /**
     * Returns the current authentication object, or null for anonymous callers.
     */
    private Authentication currentAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return auth;
    }

    private List<String> authorities(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }

    public static class Identity {

        private final boolean authenticated = true;
        private final String username;
        private final List<String> roles;

        Identity(String username, List<String> roles) {
            this.username = username;
            this.roles = roles;
        }

        public boolean isAuthenticated() {
            return authenticated;
        }

        public String getUsername() {
            return username;
        }

        public List<String> getRoles() {
            return roles;
        }
    }

    public static class LogoutOutcome {

        private final boolean wasAuthenticated;
        private final String username;
        private final String message;

        LogoutOutcome(boolean wasAuthenticated, String username, String message) {
            this.wasAuthenticated = wasAuthenticated;
            this.username = username;
            this.message = message;
        }

        public boolean isWasAuthenticated() {
            return wasAuthenticated;
        }

        public String getUsername() {
            return username;
        }

        public String getMessage() {
            return message;
        }
    }
}

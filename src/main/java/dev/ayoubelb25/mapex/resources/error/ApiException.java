package dev.ayoubelb25.mapex.resources.error;

import jakarta.ws.rs.core.Response;

/**
 * REST exception with a known HTTP status.
 */
public class ApiException extends RuntimeException {

    private final Response.Status status;

    public ApiException(Response.Status status, String message) {
        super(message);
        this.status = status;
    }

    public static ApiException badRequest(String message) {
        return new ApiException(Response.Status.BAD_REQUEST, message);
    }

    public static ApiException notFound(String message) {
        return new ApiException(Response.Status.NOT_FOUND, message);
    }

    public Response.Status getStatus() {
        return status;
    }
}

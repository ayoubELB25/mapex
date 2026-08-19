package dev.ayoubelb25.mapex.resources.error;

import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Fallback mapper for uncaught REST exceptions.
 */
@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(GenericExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable e) {
        LOG.log(Level.SEVERE, "Unhandled exception in REST layer", e);
        ApiError body = new ApiError(500, "Internal Server Error", "An unexpected error occurred.");
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}

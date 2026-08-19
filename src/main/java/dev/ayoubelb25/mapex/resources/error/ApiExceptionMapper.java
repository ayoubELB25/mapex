package dev.ayoubelb25.mapex.resources.error;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ApiExceptionMapper implements ExceptionMapper<ApiException> {

    @Override
    public Response toResponse(ApiException e) {
        ApiError body = new ApiError(
                e.getStatus().getStatusCode(), e.getStatus().getReasonPhrase(), e.getMessage());
        return Response.status(e.getStatus())
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}

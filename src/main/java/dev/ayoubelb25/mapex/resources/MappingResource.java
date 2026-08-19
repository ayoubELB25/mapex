package dev.ayoubelb25.mapex.resources;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;

import dev.ayoubelb25.mapex.model.MappingResult;
import dev.ayoubelb25.mapex.model.mapping.EntityMappingContext;
import dev.ayoubelb25.mapex.resources.error.ApiException;
import dev.ayoubelb25.mapex.service.MappingContextResolver;
import dev.ayoubelb25.mapex.service.MappingService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("mapping")
public class MappingResource {

    private WebApplicationContext ctx() {
        return ContextLoader.getCurrentWebApplicationContext();
    }

    private MappingContextResolver mappingContextResolver() {
        return ctx().getBean(MappingContextResolver.class);
    }

    private MappingService mappingService() {
        return ctx().getBean(MappingService.class);
    }

    @GET
    @Path("columns")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getColumns(@QueryParam("entity") String entity) {
        EntityMappingContext context = resolveEntity(entity);
        return Response.ok(context).build();
    }

    @POST
    @Path("pdf")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response mapPdf(List<EntityPart> parts) throws IOException {
        String entity = requireText(parts, "entity", "The 'entity' field (target entity or table name) is required.");
        EntityMappingContext context = resolveEntity(entity);

        EntityPart filePart = findPart(parts, "file")
                .orElseThrow(() -> ApiException.badRequest("A 'file' part containing the PDF is required."));
        String model = requireText(parts, "model", "The 'model' field is required.");

        String provider = readText(parts, "provider");
        provider = (provider == null || provider.isBlank()) ? "ollama" : provider;
        String apiKey = readText(parts, "apiKey");
        String apiUrl = readText(parts, "apiUrl");

        String userPrompt = readText(parts, "prompt");

        byte[] pdfBytes;
        try (InputStream in = filePart.getContent(InputStream.class)) {
            pdfBytes = in.readAllBytes();
        }

        MappingResult result = mappingService().mapPdf(
                pdfBytes, context.getColumns(), provider, model, apiKey, apiUrl, userPrompt);

        return Response.ok(result).build();
    }

    private EntityMappingContext resolveEntity(String entity) {
        if (entity == null || entity.isBlank()) {
            throw ApiException.badRequest("The 'entity' field (target entity or table name) is required.");
        }
        return mappingContextResolver().resolve(entity)
                .orElseThrow(() -> ApiException.notFound("No entity or table found matching '" + entity + "'"));
    }

    private Optional<EntityPart> findPart(List<EntityPart> parts, String name) {
        return parts.stream().filter(p -> p.getName().trim().equalsIgnoreCase(name)).findFirst();
    }

    private String readText(List<EntityPart> parts, String name) throws IOException {
        Optional<EntityPart> part = findPart(parts, name);
        return part.isPresent() ? part.get().getContent(String.class).trim() : null;
    }

    private String requireText(List<EntityPart> parts, String name, String errorMessage) throws IOException {
        String value = readText(parts, name);
        if (value == null || value.isBlank()) {
            throw ApiException.badRequest(errorMessage);
        }
        return value;
    }
}

package dev.ayoubelb25.mapex.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dev.ayoubelb25.mapex.model.mapping.EntityInfo;
import dev.ayoubelb25.mapex.model.mapping.EntityMappingContext;

/**
 * Resolves a page identifier to the matching JPA entity metadata.
 */
@Component
public class MappingContextResolver {

    @Autowired
    private SchemaDiscoveryService schemaDiscoveryService;

    public Optional<EntityMappingContext> resolve(String pageIdentifier) {
        if (pageIdentifier == null || pageIdentifier.isBlank()) {
            return Optional.empty();
        }

        EntityInfo match = schemaDiscoveryService.listEntities().stream()
                .filter(e -> e.getName().equalsIgnoreCase(pageIdentifier)
                        || e.getTableName().equalsIgnoreCase(pageIdentifier))
                .findFirst()
                .orElse(null);

        if (match == null) {
            return Optional.empty();
        }

        return Optional.of(new EntityMappingContext(
                match.getName(),
                match.getTableName(),
                match.getDisplayLabel(),
                schemaDiscoveryService.getMappableColumns(match.getName())));
    }
}

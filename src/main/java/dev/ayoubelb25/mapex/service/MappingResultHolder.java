package dev.ayoubelb25.mapex.service;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

/**
 * Session-scoped storage for pending mapping rows, keyed by page identifier.
 */
@Component("mappingResultHolder")
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class MappingResultHolder implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Map<String, List<Map<String, String>>> rowsByPageKey = new LinkedHashMap<>();

    public void store(String pageKey, List<Map<String, String>> rows) {
        rowsByPageKey.put(pageKey, rows);
    }

    public List<Map<String, String>> retrieve(String pageKey) {
        return rowsByPageKey.getOrDefault(pageKey, Collections.emptyList());
    }

    public void clear(String pageKey) {
        rowsByPageKey.remove(pageKey);
    }

    public boolean hasPending(String pageKey) {
        List<Map<String, String>> rows = rowsByPageKey.get(pageKey);
        return rows != null && !rows.isEmpty();
    }
}

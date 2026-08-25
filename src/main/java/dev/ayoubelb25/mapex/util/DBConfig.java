package dev.ayoubelb25.mapex.util;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
//import jakarta.enterprise.context.ApplicationScoped;
import org.springframework.stereotype.Component;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Component
public class DBConfig {

    private EntityManagerFactory emf;

    @PostConstruct
    public void init() {
        Properties props = loadProperties();

        Map<String, String> overrides = new HashMap<>();
        overrides.put("jakarta.persistence.jdbc.url",      props.getProperty("db.url"));
        overrides.put("jakarta.persistence.jdbc.user",     props.getProperty("db.user"));
        overrides.put("jakarta.persistence.jdbc.password", props.getProperty("db.password"));
        overrides.put("jakarta.persistence.jdbc.driver", "org.postgresql.Driver");

        emf = Persistence.createEntityManagerFactory("mapex-pu", overrides);
    }

    
    public EntityManager createEntityManager() {
        return emf.createEntityManager();
    }

    @PreDestroy
    public void destroy() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    private Properties loadProperties() {
        Properties props = new Properties();
        String externalPath = System.getProperty("DB_CONFIG_PATH");
        if (externalPath == null || externalPath.isBlank()) {
            externalPath = System.getenv("DB_CONFIG_PATH");
        }

        if (externalPath != null && !externalPath.isBlank()) {
            Path path = Path.of(externalPath);
            if (Files.exists(path)) {
                try (InputStream is = Files.newInputStream(path)) {
                    props.load(is);
                    return props;
                } catch (IOException e) {
                    throw new RuntimeException("Failed to load db.properties", e);
                }
            }
        }

        try (InputStream is = getClass()
                .getClassLoader()
                .getResourceAsStream("db.properties")) {

            if (is == null) {
                throw new IllegalStateException(
                    "db.properties not found in classpath. " +
                    "Copy db.properties.example → db.properties and fill in your credentials."
                );
            }
            props.load(is);

        } catch (IOException e) {
            throw new RuntimeException("Failed to load db.properties", e);
        }
        return props;
    }
}

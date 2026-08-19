# Mapex

## What This Is
Mapex is a portable, REST-based mapping service that maps incoming PDF, CSV, and JSON data to a target schema. It uses schema metadata, database column comments, and LLM-assisted prompt generation to produce field mappings, and for PDFs it can also extract rows from the document text.

The project is packaged as a WAR and can run standalone in a Jakarta EE 10 compatible application server or be copied into another Java application that already provides the same runtime pieces.

## Requirements
- Java 21
- Maven 3.x
- Jakarta EE 10 runtime, because the project depends on `jakarta.jakartaee-api:10.0.0` as `provided`
- A WAR-capable application server; no specific server version is pinned in `pom.xml`
- PostgreSQL
- Optional Ollama runtime if you use the default local Ollama client
- Optional Gemini API access if you use the Gemini client

Versioned dependencies from `pom.xml`:
- `org.primefaces:primefaces:14.0.0` with the `jakarta` classifier
- `org.hibernate.orm:hibernate-core:6.5.2.Final`
- `org.postgresql:postgresql:42.7.3`
- `org.apache.commons:commons-csv:1.11.0`
- `org.apache.pdfbox:pdfbox:3.0.3`
- `org.springframework:spring-context:6.1.14`
- `org.springframework:spring-web:6.1.14`
- `org.springframework.security:spring-security-web:6.3.4`
- `org.springframework.security:spring-security-config:6.3.4`

## Quick Start (Standalone)
1. Clone the repository.
2. Copy `src/main/resources/db.example.properties` to `src/main/resources/db.properties`.
3. Fill in the required keys in `db.properties`:
   - `db.url`
   - `db.user`
   - `db.password`
   - `gemini.api.key` if you plan to use Gemini
4. Build the WAR with Maven:
   ```bash
   mvn clean package
   ```
5. Deploy `target/mapex.war` to your Jakarta EE 10 application server.
6. Confirm the app is up by calling the health-check endpoint:
   - `GET <context-path>/resources/jakartaee10`
   - The method returns `200 OK` with the plain text body `ping Jakarta EE`.

## Plugging Into An Existing Project
If you want to copy Mapex into another Java/Jakarta EE + Spring project, the portable code lives under:
- `src/main/java/dev/ayoubelb25/mapex/`
- `src/main/resources/META-INF/persistence.xml`
- `src/main/resources/prompts/`
- `src/main/resources/db.example.properties` if you want the sample config file

The Java packages that make up the mapping service are:
- `dev.ayoubelb25.mapex`
- `dev.ayoubelb25.mapex.config`
- `dev.ayoubelb25.mapex.dao`
- `dev.ayoubelb25.mapex.model`
- `dev.ayoubelb25.mapex.model.mapping`
- `dev.ayoubelb25.mapex.resources`
- `dev.ayoubelb25.mapex.resources.error`
- `dev.ayoubelb25.mapex.service`
- `dev.ayoubelb25.mapex.util`

What the host project must provide:
- A `persistence.xml` persistence unit named `mapex-pu`
- The JPA entities listed in that persistence unit
- PostgreSQL connectivity
- A Spring application context, because the resources and services use Spring `@Component` and bean lookup
- Spring Security configuration if you need the same login/logout behavior
- Jakarta REST activation with `@ApplicationPath("resources")`

Hardcoded package and deployment assumptions to be aware of:
- The code is rooted in the `dev.ayoubelb25.mapex` package, and `AppConfig` scans that base package
- `MappingResource` and `AuthResource` look up Spring beans from the current web application context
- `MappingContextResolver` and `SchemaDiscoveryService` resolve entities by JPA metamodel name or table name
- `persistence.xml` must list each entity class explicitly
- The REST application path is `resources`
- The login page and JSF resource paths in `SecurityConfig` are hardcoded for the current app layout

## API Reference
All REST endpoints currently defined in the code:

### `GET /resources/jakartaee10`
- Controller: `JakartaEE10Resource`
- Purpose: simple health check
- Parameters: none
- Response:
  - `200 OK`
  - Plain text body: `ping Jakarta EE`

### `GET /resources/auth/me`
- Controller: `AuthResource`
- Purpose: returns the current authenticated identity, or reports that no authenticated session exists
- Parameters: none
- Response:
  - `200 OK` with JSON body:
    - `authenticated: true`
    - `username: string`
    - `roles: string[]`
  - `401 Unauthorized` with JSON body containing:
    - `status: 401`
    - `error: "Unauthorized"`
    - `message: "No authenticated session. POST /mapex/login with 'username' and 'password' first."`

### `POST /resources/auth/logout`
- Controller: `AuthResource`
- Purpose: clears the Spring Security context and invalidates the current HTTP session
- Parameters:
  - `HttpServletRequest` and `HttpServletResponse` from the container
- Response:
  - `200 OK` with JSON body:
    - `wasAuthenticated: boolean`
    - `username: string | null`
    - `message: string`

### `GET /resources/mapping/columns`
- Controller: `MappingResource`
- Purpose: resolves an entity or table name to mapping metadata
- Query parameters:
  - `entity` required
- Response:
  - `200 OK` with JSON body representing `EntityMappingContext`
  - `400 Bad Request` if `entity` is missing or blank
  - `404 Not Found` if no entity or table matches the supplied name

### `POST /resources/mapping/pdf`
- Controller: `MappingResource`
- Purpose: maps a PDF to the target schema and extracts rows from the PDF text
- Content type: `multipart/form-data`
- Required multipart fields:
  - `entity`: target entity or table name
  - `file`: the PDF file
  - `model`: model name/tag
- Optional multipart fields:
  - `provider`: defaults to `ollama`
  - `apiKey`
  - `apiUrl`
  - `prompt`
- Response:
  - `200 OK` with JSON body of `MappingResult`
  - `400 Bad Request` if required multipart fields are missing or blank
  - `404 Not Found` if the entity or table cannot be resolved

## Configuration Reference
Properties and runtime values the app reads:

### `db.url`
- Read from: `src/main/resources/db.properties`
- Used by: `DBConfig`
- Required: yes
- Purpose: JDBC URL passed into `jakarta.persistence.jdbc.url`

### `db.user`
- Read from: `src/main/resources/db.properties`
- Used by: `DBConfig`
- Required: yes
- Purpose: JDBC username passed into `jakarta.persistence.jdbc.user`

### `db.password`
- Read from: `src/main/resources/db.properties`
- Used by: `DBConfig`
- Required: yes
- Purpose: JDBC password passed into `jakarta.persistence.jdbc.password`

### `gemini.api.key`
- Read from: `src/main/resources/db.properties`
- Used by: `GeminiClient`
- Required: optional overall, but required if you call the Gemini client without an override key
- Purpose: API key for `https://generativelanguage.googleapis.com/v1beta/models/`

### Multipart `provider`
- Read from: `MappingResource.mapPdf`
- Required: no
- Default: `ollama`
- Purpose: selects the model provider

### Multipart `apiKey`
- Read from: `MappingResource.mapPdf`
- Required: no for Ollama, optional override for Gemini
- Purpose: overrides the Gemini API key when provided

### Multipart `apiUrl`
- Read from: `MappingResource.mapPdf`
- Required: no
- Purpose: overrides the Gemini base URL when provided

### Multipart `prompt`
- Read from: `MappingResource.mapPdf`
- Required: no
- Purpose: optional free-text instruction passed into PDF mapping

### Multipart `entity`
- Read from: `MappingResource.getColumns` and `MappingResource.mapPdf`
- Required: yes
- Purpose: identifies the target entity or table name to resolve

### Multipart `file`
- Read from: `MappingResource.mapPdf`
- Required: yes
- Purpose: the PDF document to extract and map

### Multipart `model`
- Read from: `MappingResource.mapPdf`
- Required: yes
- Purpose: model tag forwarded to the selected LLM client

## Known Limitations
- The app server version is not pinned in `pom.xml`; it needs a Jakarta EE 10 compatible container.
- `GeminiClient` reads its default API key from `db.properties`, so the database config file also carries LLM configuration.
- `OllamaClient` targets a hardcoded local base URL: `http://localhost:11434/api/generate`.
- `GeminiClient` targets a hardcoded base URL unless `apiUrl` is supplied in the PDF request.
- `MappingResource` depends on the Spring web application context being available at runtime.
- `SecurityConfig` uses session-based Spring Security with form login and logout.
- `persistence.xml` lists entity classes explicitly, so adding a new entity requires updating that file.
- `SchemaMapperService` still contains a `throw new UnsupportedOperationException("Not supported yet.")` method stub.
- `Employee` is not a JPA entity, so it will not resolve through schema discovery.
- `hibernate.hbm2ddl.auto` is set to `update` in `persistence.xml`.

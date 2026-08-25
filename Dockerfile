# Stage 1: build the Jakarta EE WAR with the same Java release configured in pom.xml.
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

# Copy the POM first so dependency resolution can be cached between source edits.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# Stage 2: run the WAR on WildFly with Jakarta EE 10 support and JDK 21.
FROM quay.io/wildfly/wildfly:33.0.0.Final-jdk21 AS runtime

# Database config is not baked into this image. At runtime, mount a
# db.properties file somewhere in the container and point DB_CONFIG_PATH
# at it, e.g.:
#   -e DB_CONFIG_PATH=/opt/db-config/db.properties
#   -v /path/to/your/db.docker.properties:/opt/db-config/db.properties
# If DB_CONFIG_PATH is not set, the app falls back to looking for
# db.properties on the classpath (which won't exist in this image).
COPY --from=build /workspace/target/mapex.war /opt/jboss/wildfly/standalone/deployments/mapex.war

EXPOSE 8080

CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0"]

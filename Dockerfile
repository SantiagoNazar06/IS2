# ============================================================
# Stage 1: Build — compile and package the fat JAR
# ============================================================
FROM maven:3-eclipse-temurin-21 AS build
WORKDIR /app

# Layer caching: copy pom.xml first and resolve dependencies
# (source changes won't invalidate the dependency layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build (includes ActiveJDBC instrumentation at process-classes)
COPY src ./src
RUN mvn package -DskipTests -B

# ============================================================
# Stage 2: Runtime — minimal JRE image
# ============================================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Ensure db/ directory exists for SQLite (ActiveJDBC creates tables on startup)
RUN mkdir -p /app/db

# Copy fat JAR from build stage
COPY --from=build /app/target/proye-is-*.jar /app/

# Spark HTTP port
EXPOSE 8080

# Entrypoint script: transforms env vars to JVM -D properties
# Uses exec form for proper signal handling (no sh as PID 1)
COPY docker-entrypoint.sh /
RUN chmod +x /docker-entrypoint.sh
ENTRYPOINT ["/docker-entrypoint.sh"]

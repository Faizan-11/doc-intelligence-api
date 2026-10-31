
# ==========================================
# Stage 1: Build the Spring Boot application
# ==========================================
FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

# Copy Maven wrapper and project configuration
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Ensure Maven wrapper is executable
RUN chmod +x mvnw

# Download dependencies
RUN ./mvnw -B dependency:go-offline

# Copy application source
COPY src/ src/

# Package the application
RUN ./mvnw -B -DskipTests package


# ==========================================
# Stage 2: Run the application
# ==========================================
FROM eclipse-temurin:21-jre

WORKDIR /app

# Create a non-root user and storage directory
RUN useradd --system --uid 10001 spring \
    && mkdir -p /app/storage \
    && chown -R spring:spring /app

# Copy the packaged Spring Boot application
COPY --from=build --chown=spring:spring \
    /workspace/target/*.jar /app/app.jar

# Run as a non-root user
USER spring

# Application port
EXPOSE 8080

# Persistent document storage location
ENV DOCUMIND_STORAGE_DIRECTORY=/app/storage

# Start Spring Boot
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

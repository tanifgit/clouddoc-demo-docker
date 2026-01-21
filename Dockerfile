# Multi-stage build: first build the shaded JAR, then run it in a slim JDK image

# --- Build stage ---
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /build

# Copy Maven descriptor and source
COPY pom.xml .
COPY src ./src

# Build the shaded JAR
RUN mvn -q -DskipTests package

# --- Runtime stage ---
FROM eclipse-temurin:17-jdk
WORKDIR /app

# Copy the runnable shaded JAR
COPY --from=build /build/target/clouddoc-demo-app.jar /app/clouddoc-demo-app.jar

# SSL configuration file (must be named SSLConfig.properties)
COPY SSLConfig.properties /app/SSLConfig.properties

# Entrypoint script to create keystore from mounted certificate and run the app
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh

ENTRYPOINT ["/app/docker-entrypoint.sh"]

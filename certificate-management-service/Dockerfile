# ===================================================================
# Multi-Stage Dockerfile for Certificate Management Service
# ===================================================================

# 1. Build Stage
FROM maven:3.9.9-eclipse-temurin-24-alpine AS builder
WORKDIR /app

# Copy pom.xml first to cache Maven dependencies in a separate layer
COPY pom.xml .

# Download dependencies offline (cached layer unless pom.xml changes)
RUN mvn dependency:go-offline -B

# Copy source code after dependencies are cached
COPY src ./src

# Build production jar using cached dependencies
RUN mvn package -DskipTests

# 2. Runtime Stage
FROM eclipse-temurin:24-jre-alpine
WORKDIR /app

# Create enterprise PKI CA directory
RUN mkdir -p /etc/pki/ca && chown -R 10001:10001 /etc/pki/ca /app

# Copy built application jar from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Run as non-root user for security compliance
USER 10001:10001

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

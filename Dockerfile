# ===== Stage 1: Build =====
FROM eclipse-temurin:22-jdk-alpine as build

WORKDIR /app

# Copy the JAR built by Maven
COPY target/*.jar app.jar

# ===== Stage 2: Run =====
FROM eclipse-temurin:22-jdk-alpine

WORKDIR /app

# Copy the fat JAR from build stage
COPY --from=build /app/app.jar app.jar

# Expose app port (overridden by .env)
EXPOSE 8284

# Run Spring Boot app
ENTRYPOINT ["java", "-jar", "app.jar"]
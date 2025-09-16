# Stage 1: Build service using Maven
FROM maven:3.9.5-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -q clean package -DskipTests

# Stage 2: Build lightweight final image
FROM bellsoft/liberica-runtime-container:jre-17-slim-glibc
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]

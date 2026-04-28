# Build stage (requires repository root as Docker build context)
FROM maven:3.9.5-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn clean package -pl c4e-outbox-worker -am -DskipTests

# Run stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/c4e-outbox-worker/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]


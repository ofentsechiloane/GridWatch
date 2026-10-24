# ---- Build stage: compile and package the jar ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# ---- Runtime stage: small image that only runs the jar ----
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --create-home gridwatch
COPY --from=build /build/target/*.jar app.jar
USER gridwatch
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
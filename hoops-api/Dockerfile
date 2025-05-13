# -------- Stage 1: Build the app --------
FROM gradle:8.6-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle :hoops-api:bootJar --no-daemon

# -------- Stage 2: Run the app --------
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY --from=build /app/hoops-api/build/libs/*.jar app.jar
CMD ["java", "-jar", "app.jar"]
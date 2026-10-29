FROM node:24-alpine AS frontend
WORKDIR /build/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /build/backend
COPY backend/pom.xml ./
RUN mvn -B dependency:go-offline
COPY backend/src ./src
COPY --from=frontend /build/frontend/dist/city-weather/browser/ ./src/main/resources/static/
RUN mvn -B verify

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S weather && adduser -S weather -G weather
COPY --from=backend --chown=weather:weather /build/backend/target/city-weather-0.1.0.jar app.jar
USER weather
ENV PORT=8080 JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]

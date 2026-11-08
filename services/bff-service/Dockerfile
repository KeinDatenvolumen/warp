# [KI-Log Nr. KI-003] (39894d5) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: initialize Deployment and Health Endpoints for Services. Frontend to examine health status of services (WARP-001)
# [KI-Log Nr. KI-035] (2ccc959) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix: add graphql schemas to container (WARP-003)
# [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
FROM gradle:9.8.0-jdk21 AS build

WORKDIR /workspace

COPY gradlew gradlew
COPY gradle gradle
COPY settings.gradle settings.gradle
COPY build.gradle build.gradle
COPY gradle.properties gradle.properties

COPY contracts/graphql contracts/graphql

COPY services/bff-service services/bff-service
COPY services/identity-service/build.gradle services/identity-service/build.gradle
COPY services/routing-service/build.gradle services/routing-service/build.gradle
COPY services/weather-service/build.gradle services/weather-service/build.gradle
COPY services/storage-service/build.gradle services/storage-service/build.gradle

RUN chmod +x gradlew
RUN ./gradlew :services:bff-service:bootJar --no-daemon

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN useradd --system --uid 10001 warp

COPY --from=build \
    /workspace/services/bff-service/build/libs/*.jar \
    /app/application.jar

USER warp

ENTRYPOINT ["java", "-jar", "/app/application.jar"]
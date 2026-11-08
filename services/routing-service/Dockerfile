# [KI-Log Nr. KI-003] (39894d5) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: initialize Deployment and Health Endpoints for Services. Frontend to examine health status of services (WARP-001)
# [KI-Log Nr. KI-016] (4fd56ee) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add local databases aswell as heroku databases (WARP-002)
# [KI-Log Nr. KI-018] (73ce21c) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix: update Dockerfiles, unpriviliged User (WARP-002)
# [KI-Log Nr. KI-033] (7bdabfa) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add graphql to backend and frontend. Initialize dict for SPA (WARP-003)
# [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
FROM gradle:9.8.0-jdk21 AS build

WORKDIR /workspace

COPY gradlew gradlew
COPY gradle gradle
COPY settings.gradle settings.gradle
COPY build.gradle build.gradle
COPY gradle.properties gradle.properties

COPY services/bff-service/build.gradle services/bff-service/build.gradle
COPY services/identity-service/build.gradle services/identity-service/build.gradle
COPY services/weather-service/build.gradle services/weather-service/build.gradle
COPY services/storage-service/build.gradle services/storage-service/build.gradle

COPY services/routing-service services/routing-service

RUN chmod +x gradlew

RUN ./gradlew :services:routing-service:bootJar \
    --no-daemon \
    --stacktrace


FROM eclipse-temurin:21-jre

WORKDIR /app

RUN useradd --system --uid 10001 warp

COPY --from=build \
    /workspace/services/routing-service/build/libs/*.jar \
    /app/application.jar

COPY --chmod=755 \
    infrastructure/docker/production/spring-entrypoint.sh \
    /usr/local/bin/spring-entrypoint

USER warp

ENTRYPOINT ["/usr/local/bin/spring-entrypoint"]
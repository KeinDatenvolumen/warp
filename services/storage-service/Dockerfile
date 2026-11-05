# [KI-Log Nr. KI-13] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Persistenz gespeicherter Routen (WARP-020/021)
FROM gradle:9.8.0-jdk21 AS build

WORKDIR /workspace

COPY gradlew gradlew
COPY gradle gradle
COPY settings.gradle settings.gradle
COPY build.gradle build.gradle
COPY gradle.properties gradle.properties

COPY services/bff-service/build.gradle services/bff-service/build.gradle
COPY services/identity-service/build.gradle services/identity-service/build.gradle
COPY services/routing-service/build.gradle services/routing-service/build.gradle
COPY services/weather-service/build.gradle services/weather-service/build.gradle

COPY services/storage-service services/storage-service

RUN chmod +x gradlew

RUN ./gradlew :services:storage-service:bootJar \
    --no-daemon \
    --stacktrace


FROM eclipse-temurin:21-jre

WORKDIR /app

RUN useradd --system --uid 10001 warp

COPY --from=build \
    /workspace/services/storage-service/build/libs/*.jar \
    /app/application.jar

COPY --chmod=755 \
    infrastructure/docker/production/spring-entrypoint.sh \
    /usr/local/bin/spring-entrypoint

USER warp

ENTRYPOINT ["/usr/local/bin/spring-entrypoint"]
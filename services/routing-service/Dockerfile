# [KI-Log Nr. KI-04] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): GraphQL BFF & Apollo Client (WARP-003/004)
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
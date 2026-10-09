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
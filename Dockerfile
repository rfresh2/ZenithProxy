FROM gradle:8.13.0-jdk21 as builder

WORKDIR /build/zenithproxy

COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY gradle.properties .
COPY settings.gradle.kts .

COPY src/main src/main
COPY src/test src/test

RUN chmod +x gradlew && ./gradlew jarBuild --no-daemon

# ----

FROM openjdk:21

WORKDIR /app

COPY --from=builder /build/zenithproxy/build/libs/ZenithProxy.jar ZenithProxy.jar

ENTRYPOINT ["java", "-jar", "ZenithProxy.jar"]

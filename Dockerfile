# ---- Build stage: compile the backend with Gradle ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY gradle/wrapper/ ./gradle/wrapper/
COPY gradlew settings.gradle build.gradle ./
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies >/dev/null

COPY src ./src
RUN ./gradlew --no-daemon bootJar -x test \
    && cp "$(find build/libs -maxdepth 1 -name '*.jar' ! -name '*-plain.jar' | head -n 1)" /app/app.jar

# ---- Runtime stage: minimal JRE, non-root user ----
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

LABEL org.opencontainers.image.title="it-helpdesk-backend" \
      org.opencontainers.image.description="IT Helpdesk REST API"

RUN addgroup -S -g 10001 app \
    && adduser -S -u 10001 -G app -h /app app

COPY --from=build --chown=app:app /app/app.jar ./app.jar

USER 10001:10001
EXPOSE 8080

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -q --spider http://127.0.0.1:8080/v3/api-docs || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]

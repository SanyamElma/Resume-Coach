# ---- Build stage: compile and package with Maven (no local Maven needed) ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Cache dependencies first for faster incremental builds
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- Runtime stage: slim JRE ----
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
# Run as a non-root user
RUN groupadd -r app && useradd -r -g app app
COPY --from=build /app/target/resume-analyzer-api.jar app.jar
RUN mkdir -p /app/storage && chown -R app:app /app
USER app
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q UP || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

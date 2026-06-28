# Multi-stage build for Worker Service
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install FFmpeg
RUN apk add --no-cache ffmpeg

RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

RUN mkdir -p /data/blobs/originals /data/blobs/transcoded /data/blobs/thumbnails && \
    chown -R appuser:appgroup /data/blobs

COPY --from=build /app/target/*.jar app.jar

USER appuser
EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD wget -qO- http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]

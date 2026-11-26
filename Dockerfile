# Multi-stage Dockerfile for Online Feedback Form (Java + Maven)
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build
COPY pom.xml index.html ./
COPY src ./src

RUN mvn -B clean package

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=builder /build/target/online-feedback-form-1.0.0.jar /app/app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

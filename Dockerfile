# Simple runtime Dockerfile using the Maven-packaged JAR artifact
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY target/online-feedback-form-1.0.0.jar /app/app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# Online Feedback Form — Java/Maven DevOps Application

A lightweight Java & Maven web application serving an HTML Online Feedback Form (`Name`, `Email`, `Feedback Category`, `Rating`, `Comments`, and `Submit` button) backed by an in-memory JDK `HttpServer` handler (no database), complete with CI/CD pipelines for **GitHub Actions**, **Jenkins**, and **Docker** (`codexashu08/online-feedback-form`).

## Project Structure

```text
online-feedback-form/
├── .github/
│   └── workflows/
│       └── ci.yml                  # GitHub Actions Maven & Docker CI workflow
├── src/
│   ├── main/java/com/codexashu/feedback/
│   │   └── FeedbackServer.java     # Minimal embedded Java HTTP server & form handler
│   └── test/java/com/codexashu/feedback/
│       └── FeedbackServerTest.java # JUnit 5 tests for HTML form & server endpoints
├── index.html                      # Single-page Online Feedback HTML form
├── pom.xml                         # Maven configuration & executable JAR packaging
├── Dockerfile                      # Multi-stage Maven + Temurin JRE container build
├── .dockerignore
├── .gitignore
├── Jenkinsfile                     # Declarative Jenkins CI pipeline (test, package, Docker)
└── README.md
```

## Quick Start

### 1. Build & Test with Maven
```bash
mvn package
```
This runs the JUnit 5 test suite and builds `target/online-feedback-form-1.0.0.jar`.

### 2. Run Locally with Java
```bash
PORT=8081 java -jar target/online-feedback-form-1.0.0.jar
```
Open `http://localhost:8081` in your browser.

### 3. Build & Run with Docker
```bash
docker build -t codexashu08/online-feedback-form:v1 -t codexashu08/online-feedback-form:latest .
docker run -d -p 8081:8080 --name online-feedback-app codexashu08/online-feedback-form:latest
```
Open `http://localhost:8081` in your browser.

### 4. Push to Docker Hub
```bash
docker push codexashu08/online-feedback-form:v1
docker push codexashu08/online-feedback-form:latest
```

## Endpoints
- `GET /` — Serves the Online Feedback HTML form (`index.html`)
- `POST /submit` — Accepts and validates feedback submissions in memory
- `GET /api/feedback` — Lists in-memory feedback submissions as JSON
- `GET /health` — Health check (`{"status":"UP","service":"online-feedback-form"}`)

## Repository & Registry
- **GitHub:** https://github.com/codexashu/online-feedback-form
- **Docker Hub:** https://hub.docker.com/r/codexashu08/online-feedback-form

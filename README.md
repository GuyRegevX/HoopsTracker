# Hoops Tracker

A basketball statistics tracking system consisting of two main services:
- Hoops API: REST API for accessing basketball data
- Hoops Ingest: Service for ingesting and processing basketball data

## Prerequisites

- Java 21
- Docker Desktop
- Gradle 8.6+ (via wrapper)

## Development Setup

1. Configure Git line endings for Windows:
```bash
git config --global core.autocrlf true
```

## Project Structure

# To build the project
gradlew.bat build

# To run the API service
gradlew.bat :hoops-api:bootRun

# To run the Ingest service
gradlew.bat :hoops-ingest:bootRun

# To clean the build
gradlew.bat clean

# To see all available tasks
gradlew.bat tasks

## Building

```bash
# Clean build directories
./gradlew clean

# Build all projects
./gradlew build

# Build specific project
./gradlew :hoops-api:build
./gradlew :hoops-ingest:build
```

## Running

```bash
# Run API service (port 8080)
./gradlew :hoops-api:bootRun

# Run Ingest service (port 8081)
./gradlew :hoops-ingest:bootRun

```

## API Documentation

- API Service Swagger UI: http://localhost:8080/swagger-ui.html
- Ingest Service Swagger UI: http://localhost:8081/swagger-ui.html

## Development

- Java 24 features enabled including preview features
- Spring Boot 3.2.3
- OpenAPI documentation via SpringDoc

## Access Points & Clients

### Main Services
- Hoops API: http://localhost:8080
  - Swagger UI: http://localhost:8080/swagger-ui.html
- Hoops Ingest: http://localhost:8082
  - Swagger UI: http://localhost:8082/swagger-ui.html

### Database Clients
- TimescaleDB (pgweb):
  - URL: http://localhost:8084
  - Connection Details:
    - Host: localhost
    - Port: 5433
    - Database: hoopsdb
    - Username: hoops
    - Password: hoopspass
    - Connection string: postgres://hoops:hoopspass@localhost:5433/hoopsdb

- Redis Commander:
  - URL: http://localhost:8083
  - Connection Details:
    - Host: localhost
    - Port: 6379
    - No authentication required

### Direct Database Connections
- TimescaleDB:
  - Host: localhost
  - Port: 5433
  - Database: hoopsdb
  - Username: hoops
  - Password: hoopspass
  - JDBC URL: jdbc:postgresql://localhost:5433/hoopsdb

- Redis:
  - Host: localhost
  - Port: 6379

## Quick Access
1. TimescaleDB Management: [pgweb](http://localhost:8084)
2. Redis Management: [Redis Commander](http://localhost:8083)
3. API Documentation: [Swagger UI - API](http://localhost:8080/swagger-ui.html)
4. Ingest Documentation: [Swagger UI - Ingest](http://localhost:8082/swagger-ui.html)

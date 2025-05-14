# Hoops Tracker

A basketball statistics tracking system consisting of two main services:
- Hoops API: REST API for accessing basketball data
- Hoops Ingest: Service for ingesting and processing basketball data

## Prerequisites

- Java 21
- Docker Desktop
- Gradle 8.6+ (via wrapper)

## Docker Setup

### Docker Requirements
- Docker Desktop must be installed and running
  - Windows/Mac: Install [Docker Desktop](https://www.docker.com/products/docker-desktop)
  - Linux: Install Docker Engine and Docker Compose
- Minimum Docker requirements:
  - Docker Engine: 20.10.0 or higher
  - Docker Compose: v2.0.0 or higher

### Running with Docker
```bash
# Start all services
docker-compose up -d

# Stop all services
docker-compose down

# View logs
docker-compose logs -f
```

The following services will be available:
- Hoops API: http://localhost:8080
- Hoops Ingest: http://localhost:8082
- Database UI (pgweb): http://localhost:8084
- Redis Commander: http://localhost:8083

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

## Data Models

### Team Data Models

#### TeamMetaDTO
- `teamId`: String - Unique identifier for the team
- `name`: String - Team name
- `leagueId`: String - Associated league identifier
- `leagueName`: String - Name of the league
- `country`: String - Team's country
- `lastUpdated`: OffsetDateTime - Last update timestamp

#### TeamStatsDTO
- `teamId`: String - Team identifier
- `games`: Integer - Number of games played
- `ppg`: Double - Points per game
- `apg`: Double - Assists per game
- `rpg`: Double - Rebounds per game
- `spg`: Double - Steals per game
- `bpg`: Double - Blocks per game
- `topg`: Double - Turnovers per game
- `mpg`: Double - Minutes per game

### Player Data Models

#### PlayerMetaDTO
- `playerId`: String - Unique identifier for the player
- `name`: String - Player's name
- `teamId`: String - Associated team identifier
- `teamName`: String - Name of the team
- `jerseyNumber`: Integer - Player's jersey number
- `position`: String - Player's position
- `active`: Boolean - Player's active status
- `lastUpdated`: OffsetDateTime - Last update timestamp

#### PlayerStatsDTO
- `playerId`: String - Player identifier
- `games`: Integer - Number of games played
- `ppg`: Double - Points per game
- `apg`: Double - Assists per game
- `rpg`: Double - Rebounds per game
- `spg`: Double - Steals per game
- `bpg`: Double - Blocks per game
- `topg`: Double - Turnovers per game
- `mpg`: Double - Minutes per game

## Troubleshooting

### Database Connection Issues

1. First, make sure all Docker containers are running:
```bash
docker-compose ps
```

2. If services are not running, start them:
```bash
docker-compose up -d
```

3. Check if TimescaleDB is ready:
```bash
docker-compose logs timescaledb
```

4. To view the database through pgweb:
   - Open http://localhost:8084 in your browser
   - The connection should be automatic
   - If not, use these connection details:
     - Host: timescaledb (NOT localhost)
     - Port: 5432 (NOT 5433)
     - Database: hoopsdb
     - Username: hoops
     - Password: hoopspass

5. If still having issues:
   - Try restarting the services:
     ```bash
     docker-compose down
     docker-compose up -d
     ```
   - Wait about 30 seconds for TimescaleDB to fully initialize
   - Then try accessing pgweb again at http://localhost:8084

Note: When using Docker, use 'timescaledb' as the host name instead of 'localhost' when connecting from within the Docker network.

6. DB Map

+----------------+       +----------------+
|    leagues     |<------|     teams      |
|  league_id PK  |       |  team_id PK    |
|  name          |       |  name          |
|  country       |       |  league_id FK  |
|  created_at    |       |  country       |
+----------------+       |  city          |
                        |  division      |
                        |  conference    |
                        |  arena         |
                        |  founded_year  |
                        |  created_at    |
                        |  last_updated  |
                        +----------------+
                               ^
                               |
                               |
                        +----------------+
                        |    players     |
                        |  player_id PK  |
                        |  name          |
                        |  team_id FK    |
                        |  jersey_number |
                        |  position      |
                        |  active        |
                        |  created_at    |
                        +----------------+
                               ^
                               |
                               |
                        +----------------+       +----------------+
                        |     games      |<------|    seasons     |
                        |  game_id PK    |       |  season_id PK  |
                        |  game_date     |       |  name          |
                        |  season_id FK  |       |  start_date    |
                        |  league_id FK  |       |  end_date      |
                        |  home_team_id FK|       |  active        |
                        |  away_team_id FK|       |  created_at    |
                        |  start_time    |       +----------------+
                        |  created_at    |
                        |  state         |
                        +----------------+
                               ^
                               |
                               |
                        +----------------+
                        | player_stat_events |
                        |  event_id, timestamp PK |
                        |  player_id FK   |
                        |  game_id FK     |
                        |  team_id FK     |
                        |  stat_type      |
                        |  stat_value     |
                        |  game_state     |
                        |  created_at     |
                        +----------------+
                               ^
                               |
                               |
                        +----------------+
                        |   team_stats    |
                        |  team_id, season_id, time PK |
                        |  games          |
                        |  ppg, apg, rpg  |
                        |  spg, bpg, topg |
                        |  mpg            |
                        |  created_at     |
                        +----------------+

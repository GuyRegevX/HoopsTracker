# HoopsTracker

A real-time basketball statistics tracking system with WebSocket-based event ingestion and Redis Streams processing.

## System Overview

- **Hoops API**: REST API for accessing basketball data
- **Hoops Ingest**: WebSocket service for real-time game event ingestion
- **Data Processing**: Redis Streams for real-time event processing
- **Storage**: TimescaleDB for historical data

## Prerequisites

- Java 21
- Docker Desktop
- Gradle 8.6+ (via wrapper)

## Quick Start

1. Start all services:
```bash
docker-compose up -d
```

2. Access points:
- WebSocket Test Interface: http://localhost:8082/
- API Service: http://localhost:8080
- Database UI (pgweb): http://localhost:8084
- Redis Commander: http://localhost:8083

## Design & Implementation

### WebSocket Test Client

The system includes a built-in test client (`/src/main/resources/static/index.html`) that provides:
- Real-time WebSocket connection testing
- Event sending functionality
- Connection status monitoring
- Event logging display

#### Event Format
```json
{
  "gameId": "1001",           // Keep under 10 chars for Redis
  "teamId": "BOS",
  "playerId": "jt0",
  "playerName": "Jayson Tatum",
  "event": "points",          // See supported events below
  "value": 3,
  "timestamp": "2024-03-10T21:00:15.321Z"
}
```

#### Supported Events

| Event Type      | Value Type | Valid Range    |
|----------------|------------|----------------|
| points         | integer    | 1-3           |
| rebounds       | integer    | ≥ 1           |
| assists        | integer    | ≥ 1           |
| steals         | integer    | ≥ 1           |
| blocks         | integer    | ≥ 1           |
| fouls          | integer    | 0-6           |
| minutes_played | float      | 0-48          |

#### Validation Rules
- All fields are required
- GameID must be a string under 10 characters
- Values must be within specified ranges
- Timestamps must be in ISO-8601 format and not in future
- Event types must match the supported list

## Testing Guide

### 1. Start Required Services

```bash
# Start Redis and other dependencies
docker-compose up -d

# Start the Ingest service
./gradlew :hoops-ingest:bootRun
```

### 2. Testing Steps

1. Open `http://localhost:8082/` in your browser
2. Click "Connect" to establish WebSocket connection
3. Use test buttons to send events:
   - "Send Points Event" - Valid 3-point score
   - "Send Invalid Event" - Invalid 4-point score

### 3. Monitor Results

1. Watch the test client's log window for:
   - Connection status
   - Event sending confirmation
   - Error messages

2. Check Redis Commander (http://localhost:8083) to view:
   - Stream entries in 'game-events-stream'
   - Event processing status

### 4. Troubleshooting

1. Connection Issues:
   - Ensure the page is accessed via `http://localhost:8082/`
   - Check that the Ingest service is running
   - Verify Redis is running (`docker-compose ps`)

2. Event Processing Issues:
   - Check the gameId length (keep under 10 chars)
   - Verify event type and value range
   - Check application logs for validation errors

3. Redis Issues:
   - Open Redis Commander to verify stream contents
   - Check Redis connection in application logs
   - Restart Redis if needed: `docker-compose restart redis`

## Development Setup

1. Configure Git line endings for Windows:
```bash
git config --global core.autocrlf true
```

2. Build the project:
```bash
./gradlew build
```

3. Run services:
```bash
# Run API service
./gradlew :hoops-api:bootRun

# Run Ingest service
./gradlew :hoops-ingest:bootRun
```

## Database Access

### TimescaleDB
- URL: http://localhost:8084 (pgweb)
- Connection Details:
  - Host: localhost
  - Port: 5433
  - Database: hoopsdb
  - Username: hoops
  - Password: hoopspass
  - JDBC URL: jdbc:postgresql://localhost:5433/hoopsdb

### Redis
- Commander UI: http://localhost:8083
- Connection Details:
  - Host: localhost
  - Port: 6379
  - No authentication required

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

## Contributing

1. Fork the repository
2. Create your feature branch
3. Commit your changes
4. Push to the branch
5. Create a new Pull Request

## Services

- **hoops-api** (port 8080): REST API for basketball data
- **hoops-ingest** (port 8082): WebSocket service for real-time event ingestion
- **hoops-processor** (port 8084): Event processing and enrichment service

## Development Tools

- **pgweb** (port 9080): Web-based PostgreSQL database viewer
- **redis-commander** (port 9081): Web-based Redis database management
- **TimescaleDB** (port 5433): Time-series database for statistics
- **Redis** (port 6379): In-memory database for caching and streams

## Getting Started

1. Start the services:
   ```bash
   docker-compose up
   ```

2. Access the tools:
   - Database UI: http://localhost:9080
   - Redis UI: http://localhost:9081
   - API Docs: http://localhost:8080/swagger-ui.html
   - WebSocket Test UI: http://localhost:8082

## Development Guidelines

1. Code Structure:
   - Follow existing package naming conventions
   - Use common library for shared code
   - Implement proper error handling and logging

2. Data Storage:
   - Use Redis for caching (24-hour TTL)
   - TimescaleDB for persistent storage
   - Follow existing schema conventions

3. Testing:
   - Write unit tests for new functionality
   - Use test containers for integration tests
   - Follow existing test naming conventions

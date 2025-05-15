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

## Testing Guide

### 1. Start Required Services

```bash
# Start Redis and other dependencies
docker-compose up -d

# Start the Ingest service
./gradlew :hoops-ingest:bootRun
```

### 2. Access the WebSocket Test Client

1. Open `http://localhost:8082/` in your browser
2. Click the "Connect" button to establish WebSocket connection
3. Use the test buttons to send events

### 3. Send Test Events

The client provides two test buttons:
- "Send Points Event" - Sends a valid 3-point score
- "Send Invalid Event" - Sends an invalid 4-point score (for testing validation)

Example of a valid event:
```json
{
  "gameId": "1001",           // Keep under 10 chars for Redis
  "teamId": "BOS",
  "playerId": "jt0",
  "playerName": "Jayson Tatum",
  "event": "points",
  "value": 3,
  "timestamp": "2024-03-10T21:00:15.321Z"
}
```

### 4. Monitor Results

1. Watch the test client's log window for:
   - Connection status
   - Event sending confirmation
   - Error messages

2. Check Redis Commander (http://localhost:8083) to view:
   - Stream entries in 'game-events-stream'
   - Event processing status

### 5. Supported Events

| Event Type      | Value Type | Valid Range    |
|----------------|------------|----------------|
| points         | integer    | 1-3           |
| rebounds       | integer    | ≥ 1           |
| assists        | integer    | ≥ 1           |
| steals         | integer    | ≥ 1           |
| blocks         | integer    | ≥ 1           |
| fouls          | integer    | 0-6           |
| minutes_played | float      | 0-48          |

### 6. Validation Rules

- All fields are required
- GameID must be a string under 10 characters
- Values must be within specified ranges
- Timestamps must be in ISO-8601 format and not in future
- Event types must match the supported list

### 7. Troubleshooting

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

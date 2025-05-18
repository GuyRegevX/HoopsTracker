# Basketball Stats Tracking API

## Overview
This API provides real-time basketball statistics tracking for teams and players. It supports both live game stats and historical season averages.

## Base URL
```
http://localhost:8080/api/v1
```

## Authentication
Currently, the API does not require authentication.

## Endpoints

### Teams

#### Get Team Stats
```http
GET /teams/{teamId}/stats?seasonId={seasonId}
```

Retrieves average statistics for a specific team in a given season.

**Parameters:**
- `teamId` (path) - The unique identifier of the team
- `seasonId` (query) - The season identifier (required)

**Response Format:**
```json
{
    "teamId": "1",
    "seasonId": "1",
    "games": 10,
    "ppg": 105.5,    // Points per game
    "apg": 24.3,     // Assists per game
    "rpg": 42.1,     // Rebounds per game
    "spg": 8.7,      // Steals per game
    "bpg": 5.2,      // Blocks per game
    "topg": 13.8,    // Turnovers per game
    "mpg": 240.0     // Minutes per game
}
```

**Status Codes:**
- 200: Success
- 404: Team not found
- 400: Invalid request (missing seasonId)
- 500: Server error

### Players

#### Get Player Stats
```http
GET /players/{playerId}/stats?seasonId={seasonId}
```

Retrieves average statistics for a specific player in a given season.

**Parameters:**
- `playerId` (path) - The unique identifier of the player
- `seasonId` (query) - The season identifier (required)

**Response Format:**
```json
{
    "playerId": "1",
    "seasonId": "1",
    "teamId": "1",
    "games": 10,
    "ppg": 27.5,     // Points per game
    "apg": 9.0,      // Assists per game
    "rpg": 7.0,      // Rebounds per game
    "spg": 1.5,      // Steals per game
    "bpg": 1.5,      // Blocks per game
    "topg": 2.5,     // Turnovers per game
    "mpg": 31.5      // Minutes per game
}
```

**Status Codes:**
- 200: Success
- 404: Player not found
- 400: Invalid request (missing seasonId)
- 500: Server error

### WebSocket Events

The API also supports real-time updates via WebSocket connection.

#### Connection URL
```
ws://localhost:8080/ws
```

#### Event Types

##### Stat Update Event
```json
{
    "type": "statUpdate",
    "playerId": "1",
    "field": "points",
    "value": 2,
    "isHome": true,
    "stats": {
        "points": 10,
        "assists": 5,
        "rebounds": 3,
        "steals": 1,
        "blocks": 0,
        "turnovers": 2
    }
}
```

## Data Models

### Team Stats
| Field    | Type    | Description           |
|----------|---------|-----------------------|
| teamId   | string  | Team identifier       |
| seasonId | string  | Season identifier     |
| games    | integer | Games played          |
| ppg      | double  | Points per game       |
| apg      | double  | Assists per game      |
| rpg      | double  | Rebounds per game     |
| spg      | double  | Steals per game       |
| bpg      | double  | Blocks per game       |
| topg     | double  | Turnovers per game    |
| mpg      | double  | Minutes per game      |

### Player Stats
| Field    | Type    | Description           |
|----------|---------|-----------------------|
| playerId | string  | Player identifier     |
| teamId   | string  | Team identifier       |
| seasonId | string  | Season identifier     |
| games    | integer | Games played          |
| ppg      | double  | Points per game       |
| apg      | double  | Assists per game      |
| rpg      | double  | Rebounds per game     |
| spg      | double  | Steals per game       |
| bpg      | double  | Blocks per game       |
| topg     | double  | Turnovers per game    |
| mpg      | double  | Minutes per game      |

## Error Responses

When an error occurs, the API will return a JSON response with an error message:

```json
{
    "error": "Error message here",
    "status": 404,
    "timestamp": "2024-01-20T12:34:56.789Z"
}
```

## Rate Limiting
Currently, there are no rate limits implemented.

## Caching
The API uses Redis caching with a TTL of 1 hour for stats endpoints.

## Database Schema

### Materialized Views

#### team_avg_stats_view
Continuous materialized view that updates every 10 minutes with team statistics:
```sql
CREATE MATERIALIZED VIEW team_avg_stats_view
WITH (timescaledb.continuous, timescaledb.materialized_only=false) AS
SELECT
    team_id,
    season_id,
    time_bucket('1 day', created_at) AS bucket_time,
    COUNT(DISTINCT game_id) AS games,
    AVG(CASE WHEN stat_type = 'point' THEN stat_value ELSE 0 END) AS ppg,
    AVG(CASE WHEN stat_type = 'assist' THEN stat_value ELSE 0 END) AS apg,
    AVG(CASE WHEN stat_type = 'rebound' THEN stat_value ELSE 0 END) AS rpg,
    AVG(CASE WHEN stat_type = 'steal' THEN stat_value ELSE 0 END) AS spg,
    AVG(CASE WHEN stat_type = 'block' THEN stat_value ELSE 0 END) AS bpg,
    AVG(CASE WHEN stat_type = 'turnover' THEN stat_value ELSE 0 END) AS topg,
    AVG(CASE WHEN stat_type = 'minutes_played' THEN stat_value ELSE 0 END) AS mpg,
    MAX(created_at) AS last_updated
FROM player_stat_events
GROUP BY team_id, season_id, time_bucket('1 day', created_at);
```

#### player_avg_stats_view
Continuous materialized view that updates every 10 minutes with player statistics:
```sql
CREATE MATERIALIZED VIEW player_avg_stats_view
WITH (timescaledb.continuous, timescaledb.materialized_only=false) AS
SELECT
    player_id,
    team_id,
    season_id,
    time_bucket('1 day', created_at) AS bucket_time,
    COUNT(DISTINCT game_id) AS games,
    AVG(CASE WHEN stat_type = 'point' THEN stat_value ELSE 0 END) AS ppg,
    AVG(CASE WHEN stat_type = 'assist' THEN stat_value ELSE 0 END) AS apg,
    AVG(CASE WHEN stat_type = 'rebound' THEN stat_value ELSE 0 END) AS rpg,
    AVG(CASE WHEN stat_type = 'steal' THEN stat_value ELSE 0 END) AS spg,
    AVG(CASE WHEN stat_type = 'block' THEN stat_value ELSE 0 END) AS bpg,
    AVG(CASE WHEN stat_type = 'turnover' THEN stat_value ELSE 0 END) AS topg,
    AVG(CASE WHEN stat_type = 'minutes_played' THEN stat_value ELSE 0 END) AS mpg,
    MAX(created_at) AS last_updated
FROM player_stat_events
GROUP BY player_id, team_id, season_id, time_bucket('1 day', created_at);
```

## Development Setup

### Prerequisites
- Java 17 or higher
- Docker and Docker Compose
- PostgreSQL with TimescaleDB extension
- Redis

### Running the Application
1. Start the database and Redis:
```bash
docker-compose up -d
```

2. Run the application:
```bash
./mvnw spring-boot:run
```

### Testing
Run the test suite:
```bash
./mvnw test
```

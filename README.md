# Basketball Stats Tracking API

## Overview
This API provides real-time basketball statistics tracking for teams and players. It supports both live game stats and historical season averages.

## Development Setup

### Prerequisites
- Java 24 or higher
- Docker and Docker Compose
- Git installed
- Gardle installed(if you want to run from IDE)

### Running the Application
1. Start the database and Redis:
```bash
docker-compose up -d
```

2. Run the application:
```bash
./gradlew spring-boot:run
```

### Testing
Run the test suite:
```bash
./gradlew test
```

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
ws://localhost:8082/ws/game_live_update
```

#### Message Format
All messages should follow this general structure:
```json
{
    "gameId": "2024030100",    // Format: YYYYMMDD00
    "teamId": "BOS",           // 3-letter team code
    "playerId": "jt0",         // Player's unique ID
    "event": "point",          // Event type
    "value": 3                 // Event value
}
```

Available events:
- `point` (value: 2 or 3)
- `assist` (value: 1)
- `rebound` (value: 1)
- `steal` (value: 1)
- `block` (value: 1)
- `turnover` (value: 1)
- `minutes` (value: 0-48)



#### Response Messages
The server will respond with success/error messages in this format:
```json
{
    "status": "success",
    "message": "Event recorded successfully",
    "timestamp": "2024-03-01T19:30:00Z"
}
```

or for errors:
```json
{
    "status": "error",
    "message": "Invalid event type",
    "timestamp": "2024-03-01T19:30:00Z"
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
The API uses Redis caching

## Database Schema

The application uses TimescaleDB (PostgreSQL extension) for time-series data management. Here's the database schema:

### Core Tables

#### leagues
| Column     | Type      | Description                |
|------------|-----------|----------------------------|
| league_id  | TEXT      | Primary Key               |
| name       | TEXT      | League name               |
| country    | TEXT      | Country of the league     |
| created_at | TIMESTAMP | Creation timestamp        |

#### seasons
| Column     | Type      | Description                |
|------------|-----------|----------------------------|
| season_id  | TEXT      | Primary Key               |
| name       | TEXT      | Season name               |
| start_date | DATE      | Season start date         |
| end_date   | DATE      | Season end date           |
| active     | BOOLEAN   | Is current season         |
| created_at | TIMESTAMP | Creation timestamp        |

#### teams
| Column        | Type      | Description                |
|---------------|-----------|----------------------------|
| team_id       | TEXT      | Primary Key               |
| name          | TEXT      | Team name                 |
| league_id     | TEXT      | Foreign Key to leagues    |
| country       | TEXT      | Team's country            |
| city          | TEXT      | Team's city               |
| division      | TEXT      | Division                  |
| conference    | TEXT      | Conference                |
| arena         | TEXT      | Home arena                |
| founded_year  | INTEGER   | Year founded              |
| created_at    | TIMESTAMP | Creation timestamp        |
| last_updated  | TIMESTAMP | Last update timestamp     |

#### players
| Column        | Type      | Description                |
|---------------|-----------|----------------------------|
| player_id     | TEXT      | Primary Key               |
| name          | TEXT      | Player name               |
| team_id       | TEXT      | Foreign Key to teams      |
| jersey_number | TEXT      | Jersey number             |
| position      | TEXT      | Playing position          |
| active        | BOOLEAN   | Is active player          |
| created_at    | TIMESTAMP | Creation timestamp        |
| last_updated  | TIMESTAMP | Last update timestamp     |

#### games
| Column        | Type      | Description                |
|---------------|-----------|----------------------------|
| game_id       | TEXT      | Primary Key               |
| game_date     | DATE      | Game date                 |
| season_id     | TEXT      | Foreign Key to seasons    |
| league_id     | TEXT      | Foreign Key to leagues    |
| home_team_id  | TEXT      | Foreign Key to teams      |
| away_team_id  | TEXT      | Foreign Key to teams      |
| start_time    | TIME      | Game start time           |
| state         | TEXT      | Game state                |
| created_at    | TIMESTAMP | Creation timestamp        |
| last_updated  | TIMESTAMP | Last update timestamp     |

### Time-Series Tables

#### player_stat_events (Hypertable)
| Column      | Type      | Description                |
|-------------|-----------|----------------------------|
| event_id    | SERIAL    | Event identifier          |
| player_id   | TEXT      | Foreign Key to players    |
| game_id     | TEXT      | Foreign Key to games      |
| team_id     | TEXT      | Foreign Key to teams      |
| season_id   | TEXT      | Season identifier         |
| stat_type   | TEXT      | Type of stat              |
| stat_value  | NUMERIC   | Stat value                |
| version     | BIGINT    | Event version             |
| created_at  | TIMESTAMP | Event timestamp (partition)|

Valid stat types: point, assist, rebound, steal, block, foul, turnover, minutes_played

### Materialized Views

#### team_avg_stats_view_per_bucket
Continuous aggregation of team statistics per day, including:
- Games played
- Total points, assists, rebounds, steals, blocks, turnovers
- Minutes played
- Updates every 10 minutes

#### player_avg_stats_view_per_bucket
Continuous aggregation of player statistics per day, similar to team stats
- Updates every 10 minutes

### Regular Views

#### team_avg_stats_view
Calculates per-game averages for teams from the bucketed view:
- Points per game (ppg)
- Assists per game (apg)
- Rebounds per game (rpg)
- Other per-game statistics

#### player_avg_stats_view
Calculates per-game averages for players from the bucketed view:
- Points per game (ppg)
- Assists per game (apg)
- Rebounds per game (rpg)
- Other per-game statistics

### Relationships
```mermaid
erDiagram
    leagues ||--o{ teams : has
    leagues ||--o{ games : hosts
    seasons ||--o{ games : contains
    teams ||--o{ players : rosters
    teams ||--o{ player_stat_events : records
    players ||--o{ player_stat_events : performs
    games ||--o{ player_stat_events : generates
```

### Indexes
- player_stat_events: game_id, player_id, stat_type
- games: season_id, league_id
- players: team_id
- teams: league_id


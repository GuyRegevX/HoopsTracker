# Hoops API Documentation

## Base URL
```
http://localhost:8080/api/v1
```

## Endpoints

### 1. Teams API

#### Get All Teams
```http
GET /teams
```

**Description:** Retrieves metadata for all teams

**Response (200 OK):**
```json
[
    {
        "teamId": "string",
        "name": "string",
        "leagueId": "string",
        "leagueName": "string",
        "country": "string",
        "city": "string",
        "division": "string",
        "conference": "string"
    }
]
```

**Error Responses:**
- 500: Internal server error

#### Get Team Stats
```http
GET /teams/{teamId}/stats?seasonId={seasonId}
```

**Description:** Retrieves statistics for a specific team in a given season

**Parameters:**
- `teamId` (path) - Team ID
- `seasonId` (query, required) - Season ID

**Response (200 OK):**
```json
{
    "teamId": "string",
    "seasonId": "string",
    "games": 0,
    "ppg": 0.0,
    "apg": 0.0,
    "rpg": 0.0,
    "spg": 0.0,
    "bpg": 0.0,
    "topg": 0.0,
    "mpg": 0.0
}
```

**Error Responses:**
- 404: Team not found
- 500: Internal server error

### 2. Players API

#### Get All Players
```http
GET /players
```

**Description:** Retrieves a list of all players with their metadata information

**Response (200 OK):**
```json
[
    {
        "playerId": "string",
        "name": "string",
        "teamId": "string",
        "teamName": "string",
        "jerseyNumber": "string",
        "position": "string",
        "active": true
    }
]
```

**Error Responses:**
- 500: Internal server error

#### Get Player Stats
```http
GET /players/{playerId}/stats?seasonId={seasonId}
```

**Description:** Retrieves statistics for a specific player in a given season

**Parameters:**
- `playerId` (path, required) - ID of the player
- `seasonId` (query, required) - ID of the season

**Response (200 OK):**
```json
{
    "playerId": "string",
    "seasonId": "string",
    "teamId": "string",
    "games": 0,
    "ppg": 0.0,
    "apg": 0.0,
    "rpg": 0.0,
    "spg": 0.0,
    "bpg": 0.0,
    "topg": 0.0,
    "mpg": 0.0
}
```

**Error Responses:**
- 400: Invalid player ID or season ID
- 404: Player or season not found

### 3. Seasons API


## Running

```bash
../gradlew bootRun
```

Service will start on port 8080.

## API Documentation

Access Swagger UI at http://localhost:8080/swagger-ui.html 
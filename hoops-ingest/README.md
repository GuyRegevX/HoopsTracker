# Hoops Ingest Service

Real-time basketball statistics ingestion service using WebSocket and Redis Streams.

## Features

- WebSocket endpoint for real-time game events
- Event validation and processing
- Redis Streams integration for event publishing
- Built-in test client interface

## Design

### WebSocket Interface to recive events

- Endpoint: `ws://localhost:8082/ws/game_live_update`
- Test Client: `http://localhost:8082/`

### Event Format
Events

```json
{
  "gameId": "1001",           // Keep under 10 chars for Redis
  "teamId": "BOS",
  "playerId": "jt0",
  "playerName": "Jayson Tatum",
  "event": "point",
  "value": 3,
  "timestamp": "2024-03-10T21:00:15.321Z"
}
```

### Supported Events

| Event Type      | Value Type | Valid Range    |
|----------------|------------|----------------|
| points         | integer    | 1-3           |
| rebounds       | integer    | ≥ 1           |
| assists        | integer    | ≥ 1           |
| steals         | integer    | ≥ 1           |
| blocks         | integer    | ≥ 1           |
| fouls          | integer    | 0-6           |
| minutes_played | float      | 0-48          |

## Running

```bash
# Start Redis first
docker-compose up redis -d

# Start the service
../gradlew bootRun
```

Service will start on port 8082.

## Testing

1. Start the service and Redis
2. Open `http://localhost:8082/` in your browser
3. Use the test interface to:
   - Connect/disconnect WebSocket
   - Send test events
   - Monitor connection status
   - View event logs

### Important Notes

- GameIDs must be under 10 characters for Redis compatibility
- Events are validated before processing
- Failed validations are logged but not stored

### Troubleshooting

1. Connection Issues:
   - Verify Redis is running
   - Check service logs for errors
   - Ensure correct port (8082) is used

2. Event Processing Issues:
   - Verify event format matches specification
   - Validate event type and value range

## API Documentation

- WebSocket endpoint: `/ws/game_live_update`
- Test interface: `/index.html`
- Swagger UI: `http://localhost:8082/swagger-ui.html` 
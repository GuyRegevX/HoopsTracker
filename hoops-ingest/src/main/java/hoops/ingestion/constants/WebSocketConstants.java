package hoops.ingestion.constants;

public final class WebSocketConstants {
    private WebSocketConstants() {
        // Prevent instantiation
    }

    public static final String GAME_EVENTS_ENDPOINT = "/ws/game_live_update";
    public static final String GAME_EVENTS_ENDPOINT_PATH = "/ws/**";  // For WebSocket configuration
} 
package hoops.ingestion.config;

import hoops.ingestion.constants.WebSocketConstants;
import hoops.ingestion.websocket.GameEventWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final GameEventWebSocketHandler gameEventWebSocketHandler;

    public WebSocketConfig(GameEventWebSocketHandler gameEventWebSocketHandler) {
        this.gameEventWebSocketHandler = gameEventWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(gameEventWebSocketHandler, WebSocketConstants.GAME_EVENTS_ENDPOINT)
               .setAllowedOrigins("*");
    }
} 
package hoops.ingestion.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import hoops.common.constants.StreamConstants;
import hoops.ingestion.models.events.GameEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.StreamEntryID;

import java.util.HashMap;
import java.util.Map;

@Service
public class GameEventServiceImpl implements GameEventService {
    private static final Logger logger = LoggerFactory.getLogger(GameEventServiceImpl.class);
    
    private final JedisPool jedisPool;
    
    @Autowired
    public GameEventServiceImpl(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }
    
    @Override
    public void processGameEvent(GameEvent event) {
        try {
            // Convert event to map for Redis stream
            Map<String, String> eventMap = new HashMap<>();
            eventMap.put("gameId", event.getGameId());
            eventMap.put("teamId", event.getTeamId());
            eventMap.put("playerId", event.getPlayerId());
            eventMap.put("playerName", event.getPlayerName());
            eventMap.put("event", event.getEvent());
            eventMap.put("value", String.valueOf(event.getValue()));
            eventMap.put("timestamp", event.getTimestamp().toString());
            
            // Add event to Redis stream
            try (var jedis = jedisPool.getResource()) {
                StreamEntryID entryId = jedis.xadd(
                    StreamConstants.GAME_EVENTS_STREAM,
                    StreamEntryID.NEW_ENTRY,
                    eventMap
                );
                logger.info("Successfully published event to Redis stream with ID: {}", entryId);
            }
        } catch (Exception e) {
            logger.error("Error publishing event to Redis stream", e);
            throw new RuntimeException("Failed to process game event", e);
        }
    }
} 
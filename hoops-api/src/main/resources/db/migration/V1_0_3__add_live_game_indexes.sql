-- Index for quickly finding live games
CREATE INDEX idx_live_games ON games (state) WHERE state = 'IN_PROGRESS';

-- Index for player statistics that will be frequently queried during live games
CREATE INDEX idx_player_stat_events ON player_stat_events (game_id, stat_type, stat_value);

-- Create a composite index that includes timestamp for chronological queries
CREATE INDEX idx_player_stat_events_time ON player_stat_events (game_id, created_at DESC, stat_type, stat_value);

COMMENT ON INDEX idx_live_games IS 'Optimizes queries for finding currently active games';
COMMENT ON INDEX idx_player_stat_events IS 'Optimizes queries for retrieving player statistics';
COMMENT ON INDEX idx_player_stat_events_time IS 'Optimizes time-based queries for player statistics'; 
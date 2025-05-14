-- Enable TimescaleDB extension
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- Create base tables
CREATE TABLE leagues (
    league_id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    country TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE seasons (
    season_id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    active BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE teams (
    team_id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    league_id UUID REFERENCES leagues(league_id),
    country TEXT NOT NULL,
    city TEXT NOT NULL,
    division TEXT NOT NULL,
    conference TEXT NOT NULL,
    arena TEXT NOT NULL,
    founded_year INTEGER NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE players (
    player_id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    team_id UUID REFERENCES teams(team_id),
    jersey_number TEXT,
    position TEXT,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE games (
    game_id UUID PRIMARY KEY,
    game_date DATE NOT NULL,
    season_id UUID REFERENCES seasons(season_id),
    league_id UUID REFERENCES leagues(league_id),
    home_team_id UUID REFERENCES teams(team_id),
    away_team_id UUID REFERENCES teams(team_id),
    start_time TIME NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    state TEXT NOT NULL
);

-- Create player_stat_events table
CREATE TABLE player_stat_events (
    event_id UUID,
    player_id UUID NOT NULL,
    game_id UUID NOT NULL,
    team_id UUID,
    timestamp TIMESTAMPTZ NOT NULL,
    stat_type TEXT NOT NULL,
    stat_value INTEGER NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (event_id, timestamp),
    FOREIGN KEY (player_id) REFERENCES players(player_id),
    FOREIGN KEY (game_id) REFERENCES games(game_id),
    FOREIGN KEY (team_id) REFERENCES teams(team_id),
    CONSTRAINT valid_stat_type CHECK (LOWER(stat_type) IN ('point', 'assist', 'rebound', 'steal', 'block', 'foul', 'turnover', 'minutes_played'))
);

-- Convert to hypertable
SELECT create_hypertable('player_stat_events', 'timestamp');

-- Create indexes
CREATE INDEX idx_stat_player_game ON player_stat_events (player_id, game_id);
CREATE INDEX idx_stat_team_game ON player_stat_events (team_id, game_id);
CREATE INDEX idx_stat_type ON player_stat_events (LOWER(stat_type));

-- Function to refresh materialized views
CREATE OR REPLACE FUNCTION refresh_stat_views()
RETURNS TRIGGER AS $$
BEGIN
    -- Refresh team stats
    REFRESH MATERIALIZED VIEW CONCURRENTLY team_session_avg;
    -- Refresh player stats
    REFRESH MATERIALIZED VIEW CONCURRENTLY player_session_avg;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Create materialized view for player stats from completed games
CREATE MATERIALIZED VIEW player_session_avg AS
WITH game_stats AS (
    -- First aggregate stats per game
    SELECT 
        e.player_id,
        e.game_id,
        g.season_id,  -- Include season_id from games table
        SUM(CASE WHEN LOWER(e.stat_type) = 'point' THEN e.stat_value ELSE 0 END) AS game_points,
        SUM(CASE WHEN LOWER(e.stat_type) = 'assist' THEN e.stat_value ELSE 0 END) AS game_assists,
        SUM(CASE WHEN LOWER(e.stat_type) = 'rebound' THEN e.stat_value ELSE 0 END) AS game_rebounds,
        SUM(CASE WHEN LOWER(e.stat_type) = 'steal' THEN e.stat_value ELSE 0 END) AS game_steals,
        SUM(CASE WHEN LOWER(e.stat_type) = 'block' THEN e.stat_value ELSE 0 END) AS game_blocks,
        SUM(CASE WHEN LOWER(e.stat_type) = 'turnover' THEN e.stat_value ELSE 0 END) AS game_turnovers,
        SUM(CASE WHEN LOWER(e.stat_type) = 'minutes_played' THEN e.stat_value ELSE 0 END) AS game_minutes_played,
        MAX(e.created_at) as last_event_time
    FROM player_stat_events e
    JOIN games g ON e.game_id = g.game_id  -- Join with games to get season_id
    WHERE g.state = 'COMPLETED'
    GROUP BY e.player_id, e.game_id, g.season_id
)
SELECT 
    gs.player_id,
    gs.season_id,
    p.name as player_name,  -- Include player name
    t.team_id,             -- Include team info
    t.name as team_name,
    t.league_id,
    l.name as league_name,
    AVG(gs.game_points) AS ppg,
    AVG(gs.game_assists) AS apg,
    AVG(gs.game_rebounds) AS rpg,
    AVG(gs.game_steals) AS spg,
    AVG(gs.game_blocks) AS bpg,
    AVG(gs.game_turnovers) AS topg,
    AVG(gs.game_minutes_played) AS mpg,
    COUNT(DISTINCT gs.game_id) AS games_played,
    MAX(gs.last_event_time) AS last_updated
FROM game_stats gs
JOIN games g ON gs.game_id = g.game_id
JOIN players p ON gs.player_id = p.player_id  -- Join with players to get player info
JOIN teams t ON p.team_id = t.team_id        -- Join with teams to get team info
JOIN leagues l ON t.league_id = l.league_id   -- Join with leagues to get league name
GROUP BY gs.player_id, gs.season_id, p.name, t.team_id, t.name, t.league_id, l.name;

CREATE UNIQUE INDEX idx_player_session_avg ON player_session_avg (player_id, season_id);

-- Initial refresh of player materialized view
REFRESH MATERIALIZED VIEW player_session_avg;

-- Create view for live player stats
CREATE VIEW player_live_stats AS
WITH live_stats AS (
    SELECT 
        e.player_id,
        e.game_id,
        g.season_id,
        SUM(CASE WHEN LOWER(e.stat_type) = 'point' THEN e.stat_value ELSE 0 END) AS points,
        SUM(CASE WHEN LOWER(e.stat_type) = 'assist' THEN e.stat_value ELSE 0 END) AS assists,
        SUM(CASE WHEN LOWER(e.stat_type) = 'rebound' THEN e.stat_value ELSE 0 END) AS rebounds,
        SUM(CASE WHEN LOWER(e.stat_type) = 'steal' THEN e.stat_value ELSE 0 END) AS steals,
        SUM(CASE WHEN LOWER(e.stat_type) = 'block' THEN e.stat_value ELSE 0 END) AS blocks,
        SUM(CASE WHEN LOWER(e.stat_type) = 'turnover' THEN e.stat_value ELSE 0 END) AS turnovers,
        SUM(CASE WHEN LOWER(e.stat_type) = 'minutes_played' THEN e.stat_value ELSE 0 END) AS minutes_played,
        MAX(e.created_at) as last_event_time
    FROM player_stat_events e
    JOIN games g ON e.game_id = g.game_id
    WHERE g.state = 'IN_PROGRESS'
    GROUP BY e.player_id, e.game_id, g.season_id
)
SELECT 
    ls.player_id,
    ls.season_id,
    p.name as player_name,
    t.team_id,
    t.name as team_name,
    t.league_id,
    l.name as league_name,
    ls.points AS ppg,
    ls.assists AS apg,
    ls.rebounds AS rpg,
    ls.steals AS spg,
    ls.blocks AS bpg,
    ls.turnovers AS topg,
    ls.minutes_played AS mpg,
    1 AS games_played,
    ls.last_event_time as last_updated
FROM live_stats ls
JOIN games g ON ls.game_id = g.game_id
JOIN players p ON ls.player_id = p.player_id
JOIN teams t ON p.team_id = t.team_id
JOIN leagues l ON t.league_id = l.league_id;

-- Create view for combined player stats
CREATE VIEW player_combined_stats AS
WITH combined_stats AS (
    -- Get completed game stats
    SELECT 
        player_id,
        season_id,
        player_name,
        team_id,
        team_name,
        league_id,
        league_name,
        ppg * games_played as total_points,
        apg * games_played as total_assists,
        rpg * games_played as total_rebounds,
        spg * games_played as total_steals,
        bpg * games_played as total_blocks,
        topg * games_played as total_turnovers,
        mpg * games_played as total_minutes,
        games_played,
        last_updated
    FROM player_session_avg
    
    UNION ALL
    
    -- Get live game stats
    SELECT 
        player_id,
        season_id,
        player_name,
        team_id,
        team_name,
        league_id,
        league_name,
        ppg as total_points,
        apg as total_assists,
        rpg as total_rebounds,
        spg as total_steals,
        bpg as total_blocks,
        topg as total_turnovers,
        mpg as total_minutes,
        games_played,
        last_updated
    FROM player_live_stats
)
SELECT 
    player_id,
    season_id,
    player_name,
    team_id,
    team_name,
    league_id,
    league_name,
    SUM(total_points) / SUM(games_played) as ppg,
    SUM(total_assists) / SUM(games_played) as apg,
    SUM(total_rebounds) / SUM(games_played) as rpg,
    SUM(total_steals) / SUM(games_played) as spg,
    SUM(total_blocks) / SUM(games_played) as bpg,
    SUM(total_turnovers) / SUM(games_played) as topg,
    SUM(total_minutes) / SUM(games_played) as mpg,
    SUM(games_played) as games_played,
    MAX(last_updated) as last_updated
FROM combined_stats
GROUP BY player_id, season_id, player_name, team_id, team_name, league_id, league_name;

-- Create materialized view for team stats from completed games
CREATE MATERIALIZED VIEW team_session_avg AS
WITH game_stats AS (
    -- First aggregate stats per team per game
    SELECT 
        e.team_id,
        e.game_id,
        g.season_id,  -- Include season_id from games table
        SUM(CASE WHEN LOWER(e.stat_type) = 'point' THEN e.stat_value ELSE 0 END) AS game_points,
        SUM(CASE WHEN LOWER(e.stat_type) = 'assist' THEN e.stat_value ELSE 0 END) AS game_assists,
        SUM(CASE WHEN LOWER(e.stat_type) = 'rebound' THEN e.stat_value ELSE 0 END) AS game_rebounds,
        SUM(CASE WHEN LOWER(e.stat_type) = 'steal' THEN e.stat_value ELSE 0 END) AS game_steals,
        SUM(CASE WHEN LOWER(e.stat_type) = 'block' THEN e.stat_value ELSE 0 END) AS game_blocks,
        SUM(CASE WHEN LOWER(e.stat_type) = 'turnover' THEN e.stat_value ELSE 0 END) AS game_turnovers,
        SUM(CASE WHEN LOWER(e.stat_type) = 'minutes_played' THEN e.stat_value ELSE 0 END) AS game_minutes_played,
        MAX(e.created_at) as last_event_time
    FROM player_stat_events e
    JOIN games g ON e.game_id = g.game_id  -- Join with games to get season_id
    WHERE g.state = 'COMPLETED'
    GROUP BY e.team_id, e.game_id, g.season_id
)
SELECT 
    gs.team_id,
    gs.season_id,
    t.name as team_name,  -- Include team name
    t.league_id,          -- Include league ID
    l.name as league_name, -- Include league name
    AVG(gs.game_points) AS ppg,
    AVG(gs.game_assists) AS apg,
    AVG(gs.game_rebounds) AS rpg,
    AVG(gs.game_steals) AS spg,
    AVG(gs.game_blocks) AS bpg,
    AVG(gs.game_turnovers) AS topg,
    AVG(gs.game_minutes_played) AS mpg,
    COUNT(DISTINCT gs.game_id) AS games_played,
    MAX(gs.last_event_time) AS last_updated
FROM game_stats gs
JOIN games g ON gs.game_id = g.game_id
JOIN teams t ON gs.team_id = t.team_id  -- Join with teams to get team info
JOIN leagues l ON t.league_id = l.league_id  -- Join with leagues to get league name
GROUP BY gs.team_id, gs.season_id, t.name, t.league_id, l.name;

CREATE UNIQUE INDEX idx_team_session_avg ON team_session_avg (team_id, season_id);

-- Initial refresh of materialized views
REFRESH MATERIALIZED VIEW team_session_avg;

-- Create view for live team stats (current/in-progress games)
CREATE VIEW team_live_stats AS
WITH live_stats AS (
    SELECT 
        e.team_id,
        e.game_id,
        g.season_id,
        SUM(CASE WHEN LOWER(e.stat_type) = 'point' THEN e.stat_value ELSE 0 END) AS points,
        SUM(CASE WHEN LOWER(e.stat_type) = 'assist' THEN e.stat_value ELSE 0 END) AS assists,
        SUM(CASE WHEN LOWER(e.stat_type) = 'rebound' THEN e.stat_value ELSE 0 END) AS rebounds,
        SUM(CASE WHEN LOWER(e.stat_type) = 'steal' THEN e.stat_value ELSE 0 END) AS steals,
        SUM(CASE WHEN LOWER(e.stat_type) = 'block' THEN e.stat_value ELSE 0 END) AS blocks,
        SUM(CASE WHEN LOWER(e.stat_type) = 'turnover' THEN e.stat_value ELSE 0 END) AS turnovers,
        SUM(CASE WHEN LOWER(e.stat_type) = 'minutes_played' THEN e.stat_value ELSE 0 END) AS minutes_played,
        MAX(e.created_at) as last_event_time
    FROM player_stat_events e
    JOIN games g ON e.game_id = g.game_id
    WHERE g.state = 'IN_PROGRESS'
    GROUP BY e.team_id, e.game_id, g.season_id
)
SELECT 
    ls.team_id,
    ls.season_id,
    t.name as team_name,
    t.league_id,
    l.name as league_name,
    ls.points AS ppg,
    ls.assists AS apg,
    ls.rebounds AS rpg,
    ls.steals AS spg,
    ls.blocks AS bpg,
    ls.turnovers AS topg,
    ls.minutes_played AS mpg,
    1 AS games_played,
    ls.last_event_time as last_updated
FROM live_stats ls
JOIN games g ON ls.game_id = g.game_id
JOIN teams t ON ls.team_id = t.team_id
JOIN leagues l ON t.league_id = l.league_id;

-- Create view for combined team stats (both completed and live games)
CREATE VIEW team_combined_stats AS
WITH combined_stats AS (
    -- Get completed game stats
    SELECT 
        team_id,
        season_id,
        team_name,
        league_id,
        league_name,
        ppg * games_played as total_points,
        apg * games_played as total_assists,
        rpg * games_played as total_rebounds,
        spg * games_played as total_steals,
        bpg * games_played as total_blocks,
        topg * games_played as total_turnovers,
        mpg * games_played as total_minutes,
        games_played,
        last_updated
    FROM team_session_avg
    
    UNION ALL
    
    -- Get live game stats
    SELECT 
        team_id,
        season_id,
        team_name,
        league_id,
        league_name,
        ppg as total_points,
        apg as total_assists,
        rpg as total_rebounds,
        spg as total_steals,
        bpg as total_blocks,
        topg as total_turnovers,
        mpg as total_minutes,
        games_played,
        last_updated
    FROM team_live_stats
)
SELECT 
    team_id,
    season_id,
    team_name,
    league_id,
    league_name,
    SUM(total_points) / SUM(games_played) as ppg,
    SUM(total_assists) / SUM(games_played) as apg,
    SUM(total_rebounds) / SUM(games_played) as rpg,
    SUM(total_steals) / SUM(games_played) as spg,
    SUM(total_blocks) / SUM(games_played) as bpg,
    SUM(total_turnovers) / SUM(games_played) as topg,
    SUM(total_minutes) / SUM(games_played) as mpg,
    SUM(games_played) as games_played,
    MAX(last_updated) as last_updated
FROM combined_stats
GROUP BY team_id, season_id, team_name, league_id, league_name;

-- Create trigger to refresh views when stats are updated
CREATE TRIGGER refresh_stats_trigger
    AFTER INSERT OR UPDATE OR DELETE ON player_stat_events
    FOR EACH STATEMENT
    EXECUTE FUNCTION refresh_stat_views();

-- Create trigger to refresh views when game state changes
CREATE TRIGGER refresh_stats_on_game_state_change
    AFTER UPDATE OF state ON games
    FOR EACH ROW
    WHEN (OLD.state != NEW.state)
    EXECUTE FUNCTION refresh_stat_views();

-- Create indexes for the underlying tables to help view performance
CREATE INDEX idx_stat_player_season ON player_stat_events (player_id, game_id);
CREATE INDEX idx_stat_team_season ON player_stat_events (team_id, game_id);
CREATE INDEX idx_game_season ON games (game_id, season_id);

-- Create continuous aggregate for player stats
CREATE MATERIALIZED VIEW player_stats_cagg
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 hour', timestamp) as bucket,
    player_id,
    game_id,
    stat_type,
    SUM(stat_value) as total_value,
    COUNT(*) as event_count
FROM player_stat_events
GROUP BY bucket, player_id, game_id, stat_type
WITH NO DATA;

-- Create continuous aggregate for team stats
CREATE MATERIALIZED VIEW team_stats_cagg
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 hour', timestamp) as bucket,
    team_id,
    game_id,
    stat_type,
    SUM(stat_value) as total_value,
    COUNT(*) as event_count
FROM player_stat_events
GROUP BY bucket, team_id, game_id, stat_type
WITH NO DATA;

-- Create indexes
CREATE INDEX idx_game_state ON games (state, game_id);
CREATE INDEX idx_game_season_state ON games (season_id, state);
CREATE INDEX idx_game_team ON games (home_team_id, away_team_id);

-- Composite indexes for stat queries
CREATE INDEX idx_player_stats_game ON player_stat_events (player_id, game_id, stat_type);
CREATE INDEX idx_team_stats_game ON player_stat_events (team_id, game_id, stat_type);
CREATE INDEX idx_stats_timestamp ON player_stat_events (timestamp, game_id);

-- Partial indexes for live games (smaller, faster)
CREATE INDEX idx_live_game_stats ON player_stat_events (game_id, stat_type, stat_value) 
WHERE game_id IN (SELECT game_id FROM games WHERE state = 'IN_PROGRESS');

CREATE INDEX idx_live_game_events ON player_stat_events (game_id, player_id, stat_type) 
WHERE game_id IN (SELECT game_id FROM games WHERE state = 'IN_PROGRESS');

-- Explain index benefits
COMMENT ON INDEX idx_game_state IS 'Fast filtering of live vs completed games';
COMMENT ON INDEX idx_game_season_state IS 'Efficient season-based queries with game state';
COMMENT ON INDEX idx_game_team IS 'Quick team game lookups';
COMMENT ON INDEX idx_player_stats_game IS 'Optimizes player game stat retrieval';
COMMENT ON INDEX idx_team_stats_game IS 'Optimizes team game stat retrieval';
COMMENT ON INDEX idx_stats_timestamp IS 'Helps TimescaleDB chunk navigation';
COMMENT ON INDEX idx_live_game_stats IS 'Small index only for active games';
COMMENT ON INDEX idx_live_game_events IS 'Fast live game stat queries';

-- Create player_stats view for overall player averages
CREATE VIEW player_stats AS
WITH game_totals AS (
    SELECT 
        e.player_id,
        e.game_id,
        SUM(CASE WHEN stat_type = 'point' THEN stat_value ELSE 0 END) as points,
        SUM(CASE WHEN stat_type = 'assist' THEN stat_value ELSE 0 END) as assists,
        SUM(CASE WHEN stat_type = 'rebound' THEN stat_value ELSE 0 END) as rebounds,
        SUM(CASE WHEN stat_type = 'steal' THEN stat_value ELSE 0 END) as steals,
        SUM(CASE WHEN stat_type = 'block' THEN stat_value ELSE 0 END) as blocks,
        SUM(CASE WHEN stat_type = 'turnover' THEN stat_value ELSE 0 END) as turnovers,
        SUM(CASE WHEN stat_type = 'minutes_played' THEN stat_value ELSE 0 END) as minutes
    FROM player_stat_events e
    GROUP BY e.player_id, e.game_id
)
SELECT 
    p.name,
    p.player_id,
    COUNT(DISTINCT gt.game_id) as games_played,
    ROUND(AVG(gt.points), 1) as ppg,
    ROUND(AVG(gt.assists), 1) as apg,
    ROUND(AVG(gt.rebounds), 1) as rpg,
    ROUND(AVG(gt.steals), 1) as spg,
    ROUND(AVG(gt.blocks), 1) as bpg,
    ROUND(AVG(gt.turnovers), 1) as topg,
    ROUND(AVG(gt.minutes), 1) as mpg
FROM players p
LEFT JOIN game_totals gt ON p.player_id = gt.player_id
GROUP BY p.name, p.player_id;

-- Create team_stats view for overall team averages
CREATE VIEW team_stats AS
WITH game_totals AS (
    SELECT 
        e.team_id,
        e.game_id,
        SUM(CASE WHEN stat_type = 'point' THEN stat_value ELSE 0 END) as points,
        SUM(CASE WHEN stat_type = 'assist' THEN stat_value ELSE 0 END) as assists,
        SUM(CASE WHEN stat_type = 'rebound' THEN stat_value ELSE 0 END) as rebounds,
        SUM(CASE WHEN stat_type = 'steal' THEN stat_value ELSE 0 END) as steals,
        SUM(CASE WHEN stat_type = 'block' THEN stat_value ELSE 0 END) as blocks,
        SUM(CASE WHEN stat_type = 'turnover' THEN stat_value ELSE 0 END) as turnovers,
        SUM(CASE WHEN stat_type = 'minutes_played' THEN stat_value ELSE 0 END) as minutes
    FROM player_stat_events e
    GROUP BY e.team_id, e.game_id
)
SELECT 
    t.name,
    t.team_id,
    COUNT(DISTINCT gt.game_id) as games_played,
    ROUND(AVG(gt.points), 1) as ppg,
    ROUND(AVG(gt.assists), 1) as apg,
    ROUND(AVG(gt.rebounds), 1) as rpg,
    ROUND(AVG(gt.steals), 1) as spg,
    ROUND(AVG(gt.blocks), 1) as bpg,
    ROUND(AVG(gt.turnovers), 1) as topg,
    ROUND(AVG(gt.minutes), 1) as mpg
FROM teams t
LEFT JOIN game_totals gt ON t.team_id = gt.team_id
GROUP BY t.name, t.team_id;

-- Create indexes for performance
CREATE INDEX idx_player_stats_game ON player_stat_events (player_id, game_id, stat_type);
CREATE INDEX idx_game_state ON games (game_id, state, season_id);
CREATE INDEX idx_team_stats_game ON player_stat_events (team_id, game_id, stat_type);

-- Create view for live team stats (current/in-progress games)
CREATE VIEW team_live_stats AS
WITH live_stats AS (
    SELECT 
        e.team_id,
        e.game_id,
        g.season_id,
        SUM(CASE WHEN LOWER(e.stat_type) = 'point' THEN e.stat_value ELSE 0 END) AS points,
        SUM(CASE WHEN LOWER(e.stat_type) = 'assist' THEN e.stat_value ELSE 0 END) AS assists,
        SUM(CASE WHEN LOWER(e.stat_type) = 'rebound' THEN e.stat_value ELSE 0 END) AS rebounds,
        SUM(CASE WHEN LOWER(e.stat_type) = 'steal' THEN e.stat_value ELSE 0 END) AS steals,
        SUM(CASE WHEN LOWER(e.stat_type) = 'block' THEN e.stat_value ELSE 0 END) AS blocks,
        SUM(CASE WHEN LOWER(e.stat_type) = 'turnover' THEN e.stat_value ELSE 0 END) AS turnovers,
        SUM(CASE WHEN LOWER(e.stat_type) = 'minutes_played' THEN e.stat_value ELSE 0 END) AS minutes_played,
        MAX(e.created_at) as last_event_time
    FROM player_stat_events e
    JOIN games g ON e.game_id = g.game_id
    WHERE g.state = 'IN_PROGRESS'
    GROUP BY e.team_id, e.game_id, g.season_id
)
SELECT 
    ls.team_id,
    ls.season_id,
    t.name as team_name,
    t.league_id,
    l.name as league_name,
    ls.points AS ppg,
    ls.assists AS apg,
    ls.rebounds AS rpg,
    ls.steals AS spg,
    ls.blocks AS bpg,
    ls.turnovers AS topg,
    ls.minutes_played AS mpg,
    1 AS games_played,
    ls.last_event_time as last_updated
FROM live_stats ls
JOIN games g ON ls.game_id = g.game_id
JOIN teams t ON ls.team_id = t.team_id
JOIN leagues l ON t.league_id = l.league_id;

-- Create view for combined team stats (both completed and live games)
CREATE VIEW team_combined_stats AS
WITH combined_stats AS (
    -- Get completed game stats
    SELECT 
        team_id,
        season_id,
        team_name,
        league_id,
        league_name,
        ppg * games_played as total_points,
        apg * games_played as total_assists,
        rpg * games_played as total_rebounds,
        spg * games_played as total_steals,
        bpg * games_played as total_blocks,
        topg * games_played as total_turnovers,
        mpg * games_played as total_minutes,
        games_played,
        last_updated
    FROM team_session_avg
    
    UNION ALL
    
    -- Get live game stats
    SELECT 
        team_id,
        season_id,
        team_name,
        league_id,
        league_name,
        ppg as total_points,
        apg as total_assists,
        rpg as total_rebounds,
        spg as total_steals,
        bpg as total_blocks,
        topg as total_turnovers,
        mpg as total_minutes,
        games_played,
        last_updated
    FROM team_live_stats
)
SELECT 
    team_id,
    season_id,
    team_name,
    league_id,
    league_name,
    SUM(total_points) / SUM(games_played) as ppg,
    SUM(total_assists) / SUM(games_played) as apg,
    SUM(total_rebounds) / SUM(games_played) as rpg,
    SUM(total_steals) / SUM(games_played) as spg,
    SUM(total_blocks) / SUM(games_played) as bpg,
    SUM(total_turnovers) / SUM(games_played) as topg,
    SUM(total_minutes) / SUM(games_played) as mpg,
    SUM(games_played) as games_played,
    MAX(last_updated) as last_updated
FROM combined_stats
GROUP BY team_id, season_id, team_name, league_id, league_name;

-- Create trigger to refresh views when stats are updated
CREATE TRIGGER refresh_stats_trigger
    AFTER INSERT OR UPDATE OR DELETE ON player_stat_events
    FOR EACH STATEMENT
    EXECUTE FUNCTION refresh_stat_views();

-- Create trigger to refresh views when game state changes
CREATE TRIGGER refresh_stats_on_game_state_change
    AFTER UPDATE OF state ON games
    FOR EACH ROW
    WHEN (OLD.state != NEW.state)
    EXECUTE FUNCTION refresh_stat_views();

-- Create indexes for the underlying tables to help view performance
CREATE INDEX idx_stat_player_season ON player_stat_events (player_id, game_id);
CREATE INDEX idx_stat_team_season ON player_stat_events (team_id, game_id);
CREATE INDEX idx_game_season ON games (game_id, season_id);

-- Create continuous aggregate for player stats
CREATE MATERIALIZED VIEW player_stats_cagg
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 hour', timestamp) as bucket,
    player_id,
    game_id,
    stat_type,
    SUM(stat_value) as total_value,
    COUNT(*) as event_count
FROM player_stat_events
GROUP BY bucket, player_id, game_id, stat_type
WITH NO DATA;

-- Create continuous aggregate for team stats
CREATE MATERIALIZED VIEW team_stats_cagg
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 hour', timestamp) as bucket,
    team_id,
    game_id,
    stat_type,
    SUM(stat_value) as total_value,
    COUNT(*) as event_count
FROM player_stat_events
GROUP BY bucket, team_id, game_id, stat_type
WITH NO DATA;

-- Create indexes
CREATE INDEX idx_game_state ON games (state, game_id);
CREATE INDEX idx_game_season_state ON games (season_id, state);
CREATE INDEX idx_game_team ON games (home_team_id, away_team_id);

-- Composite indexes for stat queries
CREATE INDEX idx_player_stats_game ON player_stat_events (player_id, game_id, stat_type);
CREATE INDEX idx_team_stats_game ON player_stat_events (team_id, game_id, stat_type);
CREATE INDEX idx_stats_timestamp ON player_stat_events (timestamp, game_id);

-- Partial indexes for live games (smaller, faster)
CREATE INDEX idx_live_game_stats ON player_stat_events (game_id, stat_type, stat_value) 
WHERE game_id IN (SELECT game_id FROM games WHERE state = 'IN_PROGRESS');

CREATE INDEX idx_live_game_events ON player_stat_events (game_id, player_id, stat_type) 
WHERE game_id IN (SELECT game_id FROM games WHERE state = 'IN_PROGRESS');

-- Explain index benefits
COMMENT ON INDEX idx_game_state IS 'Fast filtering of live vs completed games';
COMMENT ON INDEX idx_game_season_state IS 'Efficient season-based queries with game state';
COMMENT ON INDEX idx_game_team IS 'Quick team game lookups';
COMMENT ON INDEX idx_player_stats_game IS 'Optimizes player game stat retrieval';
COMMENT ON INDEX idx_team_stats_game IS 'Optimizes team game stat retrieval';
COMMENT ON INDEX idx_stats_timestamp IS 'Helps TimescaleDB chunk navigation';
COMMENT ON INDEX idx_live_game_stats IS 'Small index only for active games';
COMMENT ON INDEX idx_live_game_events IS 'Fast live game stat queries';

-- Create player_stats view for overall player averages
CREATE VIEW player_stats AS
WITH game_totals AS (
    SELECT 
        e.player_id,
        e.game_id,
        SUM(CASE WHEN stat_type = 'point' THEN stat_value ELSE 0 END) as points,
        SUM(CASE WHEN stat_type = 'assist' THEN stat_value ELSE 0 END) as assists,
        SUM(CASE WHEN stat_type = 'rebound' THEN stat_value ELSE 0 END) as rebounds,
        SUM(CASE WHEN stat_type = 'steal' THEN stat_value ELSE 0 END) as steals,
        SUM(CASE WHEN stat_type = 'block' THEN stat_value ELSE 0 END) as blocks,
        SUM(CASE WHEN stat_type = 'turnover' THEN stat_value ELSE 0 END) as turnovers,
        SUM(CASE WHEN stat_type = 'minutes_played' THEN stat_value ELSE 0 END) as minutes
    FROM player_stat_events e
    GROUP BY e.player_id, e.game_id
)
SELECT 
    p.name,
    p.player_id,
    COUNT(DISTINCT gt.game_id) as games_played,
    ROUND(AVG(gt.points), 1) as ppg,
    ROUND(AVG(gt.assists), 1) as apg,
    ROUND(AVG(gt.rebounds), 1) as rpg,
    ROUND(AVG(gt.steals), 1) as spg,
    ROUND(AVG(gt.blocks), 1) as bpg,
    ROUND(AVG(gt.turnovers), 1) as topg,
    ROUND(AVG(gt.minutes), 1) as mpg
FROM players p
LEFT JOIN game_totals gt ON p.player_id = gt.player_id
GROUP BY p.name, p.player_id;

-- Create team_stats view for overall team averages
CREATE VIEW team_stats AS
WITH game_totals AS (
    SELECT 
        e.team_id,
        e.game_id,
        SUM(CASE WHEN stat_type = 'point' THEN stat_value ELSE 0 END) as points,
        SUM(CASE WHEN stat_type = 'assist' THEN stat_value ELSE 0 END) as assists,
        SUM(CASE WHEN stat_type = 'rebound' THEN stat_value ELSE 0 END) as rebounds,
        SUM(CASE WHEN stat_type = 'steal' THEN stat_value ELSE 0 END) as steals,
        SUM(CASE WHEN stat_type = 'block' THEN stat_value ELSE 0 END) as blocks,
        SUM(CASE WHEN stat_type = 'turnover' THEN stat_value ELSE 0 END) as turnovers,
        SUM(CASE WHEN stat_type = 'minutes_played' THEN stat_value ELSE 0 END) as minutes
    FROM player_stat_events e
    GROUP BY e.team_id, e.game_id
)
SELECT 
    t.name,
    t.team_id,
    COUNT(DISTINCT gt.game_id) as games_played,
    ROUND(AVG(gt.points), 1) as ppg,
    ROUND(AVG(gt.assists), 1) as apg,
    ROUND(AVG(gt.rebounds), 1) as rpg,
    ROUND(AVG(gt.steals), 1) as spg,
    ROUND(AVG(gt.blocks), 1) as bpg,
    ROUND(AVG(gt.turnovers), 1) as topg,
    ROUND(AVG(gt.minutes), 1) as mpg
FROM teams t
LEFT JOIN game_totals gt ON t.team_id = gt.team_id
GROUP BY t.name, t.team_id;

-- Create indexes for performance
CREATE INDEX idx_player_stats_game ON player_stat_events (player_id, game_id, stat_type);
CREATE INDEX idx_game_state ON games (game_id, state, season_id);
CREATE INDEX idx_team_stats_game ON player_stat_events (team_id, game_id, stat_type);

-- Create view for live team stats (current/in-progress games)
CREATE VIEW team_live_stats AS
WITH live_stats AS (
    SELECT 
        e.team_id,
        e.game_id,
        g.season_id,
        SUM(CASE WHEN LOWER(e.stat_type) = 'point' THEN e.stat_value ELSE 0 END) AS points,
        SUM(CASE WHEN LOWER(e.stat_type) = 'assist' THEN e.stat_value ELSE 0 END) AS assists,
        SUM(CASE WHEN LOWER(e.stat_type) = 'rebound' THEN e.stat_value ELSE 0 END) AS rebounds,
        SUM(CASE WHEN LOWER(e.stat_type) = 'steal' THEN e.stat_value ELSE 0 END) AS steals,
        SUM(CASE WHEN LOWER(e.stat_type) = 'block' THEN e.stat_value ELSE 0 END) AS blocks,
        SUM(CASE WHEN LOWER(e.stat_type) = 'turnover' THEN e.stat_value ELSE 0 END) AS turnovers,
        SUM(CASE WHEN LOWER(e.stat_type) = 'minutes_played' THEN e.stat_value ELSE 0 END) AS minutes_played,
        MAX(e.created_at) as last_event_time
    FROM player_stat_events e
    JOIN games g ON e.game_id = g.game_id
    WHERE g.state = 'IN_PROGRESS'
    GROUP BY e.team_id, e.game_id, g.season_id
)
SELECT 
    ls.team_id,
    ls.season_id,
    t.name as team_name,
    t.league_id,
    l.name as league_name,
    ls.points AS ppg,
    ls.assists AS apg,
    ls.rebounds AS rpg,
    ls.steals AS spg,
    ls.blocks AS bpg,
    ls.turnovers AS topg,
    ls.minutes_played AS mpg,
    1 AS games_played,
    ls.last_event_time as last_updated
FROM live_stats ls
JOIN games g ON ls.game_id = g.game_id
JOIN teams t ON ls.team_id = t.team_id
JOIN leagues l ON t.league_id = l.league_id;

-- Create view for combined team stats (both completed and live games)
CREATE VIEW team_combined_stats AS
WITH combined_stats AS (
    -- Get completed game stats
    SELECT 
        team_id,
        season_id,
        team_name,
        league_id,
        league_name,
        ppg * games_played as total_points,
        apg * games_played as total_assists,
        rpg * games_played as total_rebounds,
        spg * games_played as total_steals,
        bpg * games_played as total_blocks,
        topg * games_played as total_turnovers,
        mpg * games_played as total_minutes,
        games_played,
        last_updated
    FROM team_session_avg
    
    UNION ALL
    
    -- Get live game stats
    SELECT 
        team_id,
        season_id,
        team_name,
        league_id,
        league_name,
        ppg as total_points,
        apg as total_assists,
        rpg as total_rebounds,
        spg as total_steals,
        bpg as total_blocks,
        topg as total_turnovers,
        mpg as total_minutes,
        games_played,
        last_updated
    FROM team_live_stats
)
SELECT 
    team_id,
    season_id,
    team_name,
    league_id,
    league_name,
    SUM(total_points) / SUM(games_played) as ppg,
    SUM(total_assists) / SUM(games_played) as apg,
    SUM(total_rebounds) / SUM(games_played) as rpg,
    SUM(total_steals) / SUM(games_played) as spg,
    SUM(total_blocks) / SUM(games_played) as bpg,
    SUM(total_turnovers) / SUM(games_played) as topg,
    SUM(total_minutes) / SUM(games_played) as mpg,
    SUM(games_played) as games_played,
    MAX(last_updated) as last_updated
FROM combined_stats
GROUP BY team_id, season_id, team_name, league_id, league_name;

-- Create trigger to refresh views when stats are updated
CREATE TRIGGER refresh_stats_trigger
    AFTER INSERT OR UPDATE OR DELETE ON player_stat_events
    FOR EACH STATEMENT
    EXECUTE FUNCTION refresh_stat_views();

-- Create trigger to refresh views when game state changes
CREATE TRIGGER refresh_stats_on_game_state_change
    AFTER UPDATE OF state ON games
    FOR EACH ROW
    WHEN (OLD.state != NEW.state)
    EXECUTE FUNCTION refresh_stat_views();

-- Create indexes for the underlying tables to help view performance
CREATE INDEX idx_stat_player_season ON player_stat_events (player_id, game_id);
CREATE INDEX idx_stat_team_season ON player_stat_events (team_id, game_id);
CREATE INDEX idx_game_season ON games (game_id, season_id);

-- Create continuous aggregate for player stats
CREATE MATERIALIZED VIEW player_stats_cagg
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 hour', timestamp) as bucket,
    player_id,
    game_id,
    stat_type,
    SUM(stat_value) as total_value,
    COUNT(*) as event_count
FROM player_stat_events
GROUP BY bucket, player_id, game_id, stat_type
WITH NO DATA;

-- Create continuous aggregate for team stats
CREATE MATERIALIZED VIEW team_stats_cagg
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 hour', timestamp) as bucket,
    team_id,
    game_id,
    stat_type,
    SUM(stat_value) as total_value,
    COUNT(*) as event_count
FROM player_stat_events
GROUP BY bucket, team_id, game_id, stat_type
WITH NO DATA;

-- Create indexes
CREATE INDEX idx_game_state ON games (state, game_id);
CREATE INDEX idx_game_season_state ON games (season_id, state);
CREATE INDEX idx_game_team ON games (home_team_id, away_team_id);

-- Composite indexes for stat queries
CREATE INDEX idx_player_stats_game ON player_stat_events (player_id, game_id, stat_type);
CREATE INDEX idx_team_stats_game ON player_stat_events (team_id, game_id, stat_type);
CREATE INDEX idx_stats_timestamp ON player_stat_events (timestamp, game_id);

-- Partial indexes for live games (smaller, faster)
CREATE INDEX idx_live_game_stats ON player_stat_events (game_id, stat_type, stat_value) 
WHERE game_id IN (SELECT game_id FROM games WHERE state = 'IN_PROGRESS');

CREATE INDEX idx_live_game_events ON player_stat_events (game_id, player_id, stat_type) 
WHERE game_id IN (SELECT game_id FROM games WHERE state = 'IN_PROGRESS');

-- Explain index benefits
COMMENT ON INDEX idx_game_state IS 'Fast filtering of live vs completed games';
COMMENT ON INDEX idx_game_season_state IS 'Efficient season-based queries with game state';
COMMENT ON INDEX idx_game_team IS 'Quick team game lookups';
COMMENT ON INDEX idx_player_stats_game IS 'Optimizes player game stat retrieval';
COMMENT ON INDEX idx_team_stats_game IS 'Optimizes team game stat retrieval';
COMMENT ON INDEX idx_stats_timestamp IS 'Helps TimescaleDB chunk navigation';
COMMENT ON INDEX idx_live_game_stats IS 'Small index only for active games';
COMMENT ON INDEX idx_live_game_events IS 'Fast live game stat queries';

-- Create player_stats view for overall player averages
CREATE VIEW player_stats AS
WITH game_totals AS (
    SELECT 
        e.player_id,
        e.game_id,
        SUM(CASE WHEN stat_type = 'point' THEN stat_value ELSE 0 END) as points,
        SUM(CASE WHEN stat_type = 'assist' THEN stat_value ELSE 0 END) as assists,
        SUM(CASE WHEN stat_type = 'rebound' THEN stat_value ELSE 0 END) as rebounds,
        SUM(CASE WHEN stat_type = 'steal' THEN stat_value ELSE 0 END) as steals,
        SUM(CASE WHEN stat_type = 'block' THEN stat_value ELSE 0 END) as blocks,
        SUM(CASE WHEN stat_type = 'turnover' THEN stat_value ELSE 0 END) as turnovers,
        SUM(CASE WHEN stat_type = 'minutes_played' THEN stat_value ELSE 0 END) as minutes
    FROM player_stat_events e
    GROUP BY e.player_id, e.game_id
)
SELECT 
    p.name,
    p.player_id,
    COUNT(DISTINCT gt.game_id) as games_played,
    ROUND(AVG(gt.points), 1) as ppg,
    ROUND(AVG(gt.assists), 1) as apg,
    ROUND(AVG(gt.rebounds), 1) as rpg,
    ROUND(AVG(gt.steals), 1) as spg,
    ROUND(AVG(gt.blocks), 1) as bpg,
    ROUND(AVG(gt.turnovers), 1) as topg,
    ROUND(AVG(gt.minutes), 1) as mpg
FROM players p
LEFT JOIN game_totals gt ON p.player_id = gt.player_id
GROUP BY p.name, p.player_id;

-- Create team_stats view for overall team averages
CREATE VIEW team_stats AS
WITH game_totals AS (
    SELECT 
        e.team_id,
        e.game_id,
        SUM(CASE WHEN stat_type = 'point' THEN stat_value ELSE 0 END) as points,
        SUM(CASE WHEN stat_type = 'assist' THEN stat_value ELSE 0 END) as assists,
        SUM(CASE WHEN stat_type = 'rebound' THEN stat_value ELSE 0 END) as rebounds,
        SUM(CASE WHEN stat_type = 'steal' THEN stat_value ELSE 0 END) as steals,
        SUM(CASE WHEN stat_type = 'block' THEN stat_value ELSE 0 END) as blocks,
        SUM(CASE WHEN stat_type = 'turnover' THEN stat_value ELSE 0 END) as turnovers,
        SUM(CASE WHEN stat_type = 'minutes_played' THEN stat_value ELSE 0 END) as minutes
    FROM player_stat_events e
    GROUP BY e.team_id, e.game_id
)
SELECT 
    t.name,
    t.team_id,
    COUNT(DISTINCT gt.game_id) as games_played,
    ROUND(AVG(gt.points), 1) as ppg,
    ROUND(AVG(gt.assists), 1) as apg,
    ROUND(AVG(gt.rebounds), 1) as rpg,
    ROUND(AVG(gt.steals), 1) as spg,
    ROUND(AVG(gt.blocks), 1) as bpg,
    ROUND(AVG(gt.turnovers), 1) as topg,
    ROUND(AVG(gt.minutes), 1) as mpg
FROM teams t
LEFT JOIN game_totals gt ON t.team_id = gt.team_id
GROUP BY t.name, t.team_id;

-- Create indexes for performance
CREATE INDEX idx_player_stats_game ON player_stat_events (player_id, game_id, stat_type);
CREATE INDEX idx_game_state ON games (game_id, state, season_id);
CREATE INDEX idx_team_stats_game ON player_stat_events (team_id, game_id, stat_type);

-- Create view for live team stats (current/in-progress games)
CREATE VIEW team_live_stats AS
WITH live_stats AS (
    SELECT 
        e.team_id,
        e.game_id,
        g.season_id,
        SUM(CASE WHEN LOWER(e.stat_type) = 'point' THEN e.stat_value ELSE 0 END) AS points,
        SUM(CASE WHEN LOWER(e.stat_type) = 'assist' THEN e.stat_value ELSE 0 END) AS assists,
        SUM(CASE WHEN LOWER(e.stat_type) = 'rebound' THEN e.stat_value ELSE 0 END) AS rebounds,
        SUM(CASE WHEN LOWER(e.stat_type) = 'steal' THEN e.stat_value ELSE 0 END) AS steals,
        SUM(CASE WHEN LOWER(e.stat_type) = 'block' THEN e.stat_value ELSE 0 END) AS blocks,
        SUM(CASE WHEN LOWER(e.stat_type) = 'turnover' THEN e.stat_value ELSE 0 END) AS turnovers,
        SUM(CASE WHEN LOWER(e.stat_type) = 'minutes_played' THEN e.stat_value ELSE 0 END) AS minutes_played,
        MAX(e.created_at) as last_event_time
    FROM player_stat_events e
    JOIN games g ON e.game_id = g.game_id
    WHERE g.state = 'IN_PROGRESS'
    GROUP BY e.team_id, e.game_id, g.season_id
)
SELECT 
    ls.team_id,
    ls.season_id,
    t.name as team_name,
    t.league_id,
    l.name as league_name,
    ls.points AS ppg,
    ls.assists AS apg,
    ls.rebounds AS rpg,
    ls.steals AS spg,
    ls.blocks AS bpg,
    ls.turnovers AS topg,
    ls.minutes_played AS mpg,
    1 AS games_played,
    ls.last_event_time as last_updated
FROM live_stats ls
JOIN games g ON ls.game_id = g.game_id
JOIN teams t ON ls.team_id = t.team_id
JOIN leagues l ON t.league_id = l.league_id;

-- Create view for combined team stats (both completed and live games)
CREATE VIEW team_combined_stats AS
WITH combined_stats AS (
    -- Get completed game stats
    SELECT 
        team_id,
        season_id,
        team_name,
        league_id,
        league_name,
        ppg * games_played as total_points,
        apg * games_played as total_assists,
        rpg * games_played as total_rebounds,
        spg * games_played as total_steals,
        bpg * games_played as total_blocks,
        topg * games_played as total_turnovers,
        mpg * games_played as total_minutes,
        games_played,
        last_updated
    FROM team_session_avg
    
    UNION ALL
    
    -- Get live game stats
    SELECT 
        team_id,
        season_id,
        team_name,
        league_id,
        league_name,
        ppg as total_points,
        apg as total_assists,
        rpg as total_rebounds,
        spg as total_steals,
        bpg as total_blocks,
        topg as total_turnovers,
        mpg as total_minutes,
        games_played,
        last_updated
    FROM team_live_stats
)
SELECT 
    team_id,
    season_id,
    team_name,
    league_id,
    league_name,
    SUM(total_points) / SUM(games_played) as ppg,
    SUM(total_assists) / SUM(games_played) as apg,
    SUM(total_rebounds) / SUM(games_played) as rpg,
    SUM(total_steals) / SUM(games_played) as spg,
    SUM(total_blocks) / SUM(games_played) as bpg,
    SUM(total_turnovers) / SUM(games_played) as topg,
    SUM(total_minutes) / SUM(games_played) as mpg,
    SUM(games_played) as games_played,
    MAX(last_updated) as last_updated
FROM combined_stats
GROUP BY team_id, season_id, team_name, league_id, league_name;

-- Create trigger to refresh views when stats are updated
CREATE TRIGGER refresh_stats_trigger
    AFTER INSERT OR UPDATE OR DELETE ON player_stat_events
    FOR EACH STATEMENT
    EXECUTE FUNCTION refresh_stat_views();

-- Create trigger to refresh views when game state changes
CREATE TRIGGER refresh_stats_on_game_state_change
    AFTER UPDATE OF state ON games
    FOR EACH ROW
    WHEN (OLD.state != NEW.state)
    EXECUTE FUNCTION refresh_stat_views();

-- Create indexes for the underlying tables to help view performance
CREATE INDEX idx_stat_player_season ON player_stat_events (player_id, game_id);
CREATE INDEX idx_stat_team_season ON player_stat_events (team_id, game_id);
CREATE INDEX idx_game_season ON games (game_id, season_id);

-- Create continuous aggregate for player stats
CREATE MATERIALIZED VIEW player_stats_cagg
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 hour', timestamp) as bucket,
    player_id,
    game_id,
    stat_type,
    SUM(stat_value) as total_value,
    COUNT(*) as event_count
FROM player_stat_events
GROUP BY bucket, player_id, game_id, stat_type
WITH NO DATA;

-- Create continuous aggregate for team stats
CREATE MATERIALIZED VIEW team_stats_cagg
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 hour', timestamp) as bucket,
    team_id,
    game_id,
    stat_type,
    SUM(stat_value) as total_value,
    COUNT(*) as event_count
FROM player_stat_events
GROUP BY bucket, team_id, game_id, stat_type
WITH NO DATA;

-- Create indexes
CREATE INDEX idx_game_state ON games (state, game_id);
CREATE INDEX idx_game_season_state ON games (season_id, state);
CREATE INDEX idx_game_team ON games (home_team_id, away_team_id);

-- Composite indexes for stat queries
CREATE INDEX idx_player_stats_game ON player_stat_events (player_id, game_id, stat_type);
CREATE INDEX idx_team_stats_game ON player_stat_events (team_id, game_id, stat_type);
CREATE INDEX idx_stats_timestamp ON player_stat_events (timestamp, game_id);

-- Partial indexes for live games (smaller, faster)
CREATE INDEX idx_live_game_stats ON player_stat_events (game_id, stat_type, stat_value) 
WHERE game_id IN (SELECT game_id FROM games WHERE state = 'IN_PROGRESS');

CREATE INDEX idx_live_game_events ON player_stat_events (game_id, player_id, stat_type) 
WHERE game_id IN (SELECT game_id FROM games WHERE state = 'IN_PROGRESS');

-- Explain index benefits
COMMENT ON INDEX idx_game_state IS 'Fast filtering of live vs completed games';
COMMENT ON INDEX idx_game_season_state IS 'Efficient season-based queries with game state';
COMMENT ON INDEX idx_game_team IS 'Quick team game lookups';
COMMENT ON INDEX idx_player_stats_game IS 'Optimizes player game stat retrieval';
COMMENT ON INDEX idx_team_stats_game IS 'Optimizes team game stat retrieval';
COMMENT ON INDEX idx_stats_timestamp IS 'Helps TimescaleDB chunk navigation';
COMMENT ON INDEX idx_live_game_stats IS 'Small index only for active games';
COMMENT ON INDEX idx_live_game_events IS 'Fast live game stat queries';

-- Create player_stats view for overall player averages
CREATE VIEW player_stats AS
WITH game_totals AS (
    SELECT 
        e.player_id,
        e.game_id,
        SUM(CASE WHEN stat_type = 'point' THEN stat_value ELSE 0 END) as points,
        SUM(CASE WHEN stat_type = 'assist' THEN stat_value ELSE 0 END) as assists,
        SUM(CASE WHEN stat_type = 'rebound' THEN stat_value ELSE 0 END) as rebounds,
        SUM(CASE WHEN stat_type = 'steal' THEN stat_value ELSE 0 END) as steals,
        SUM(CASE WHEN stat_type = 'block' THEN stat_value ELSE 0 END) as blocks,
        SUM(CASE WHEN stat_type = 'turnover' THEN stat_value ELSE 0 END) as turnovers,
        SUM(CASE WHEN stat_type = 'minutes_played' THEN stat_value ELSE 0 END) as minutes
    FROM player_stat_events e
    GROUP BY e.player_id, e.game_id
)
SELECT 
    p.name,
    p.player_id,
    COUNT(DISTINCT gt.game_id) as games_played,
    ROUND(AVG(gt.points), 1) as ppg,
    ROUND(AVG(gt.assists), 1) as apg,
    ROUND(AVG(gt.rebounds), 1) as rpg,
    ROUND(AVG(gt.steals), 1) as spg,
    ROUND(AVG(gt.blocks), 1) as bpg,
    ROUND(AVG(gt.turnovers), 1) as topg,
    ROUND(AVG(gt.minutes), 1) as mpg
FROM players p
LEFT JOIN game_totals gt ON p.player_id = gt.player_id
GROUP BY p.name, p.player_id;

-- Create team_stats view for overall team averages
CREATE VIEW team_stats AS
WITH game_totals AS (
    SELECT 
        e.team_id,
        e.game_id,
        SUM(CASE WHEN stat_type = 'point' THEN stat_value ELSE 0 END) as points,
        SUM(CASE WHEN stat_type = 'assist' THEN stat_value ELSE 0 END) as assists,
        SUM(CASE WHEN stat_type = 'rebound' THEN stat_value ELSE 0 END) as rebounds,
        SUM(CASE WHEN stat_type = 'steal' THEN stat_value ELSE 0 END) as steals,
        SUM(CASE WHEN stat_type = 'block' THEN stat_value ELSE 0 END) as blocks,
        SUM(CASE WHEN stat_type = 'turnover' THEN stat_value ELSE 0 END) as turnovers,
        SUM(CASE WHEN stat_type = 'minutes_played' THEN stat_value ELSE 0 END) as minutes
    FROM player_stat_events e
    GROUP BY e.team_id, e.game_id
)
SELECT 
    t.name,
    t.team_id,
    COUNT(DISTINCT gt.game_id) as games_played,
    ROUND(AVG(gt.points), 1) as ppg,
    ROUND(AVG(gt.assists), 1) as apg,
    ROUND(AVG(gt.rebounds), 1) as rpg,
    ROUND(AVG(gt.steals), 1) as spg,
    ROUND(AVG(gt.blocks), 1) as bpg,
    ROUND(AVG(gt.turnovers), 1) as topg,
    ROUND(AVG(gt.minutes), 1) as mpg
FROM teams t
LEFT JOIN game_totals gt ON t.team_id = gt.team_id
GROUP BY t.name, t.team_id;

-- Create indexes for performance
CREATE INDEX idx_player_stats_game ON player_stat_events (player_id, game_id, stat_type);
CREATE INDEX idx_game_state ON games (game_id, state, season_id);
CREATE INDEX idx_team_stats_game ON player_stat_events (team_id, game_id, stat_type);

-- Create view for live team stats (current/in-progress games)
CREATE VIEW team_live_stats AS
WITH live_stats AS (
    SELECT 
        e.team_id,
        e.game_id,
        g.season_id,
        SUM(CASE WHEN LOWER(e.stat_type) = 'point' THEN e.stat_value ELSE 0 END) AS points,
        SUM(CASE WHEN LOWER(e.stat_type) = 'assist' THEN e.stat_value ELSE 0 END) AS assists,
        SUM(CASE WHEN LOWER(e.stat_type) = 'rebound' THEN e.stat_value ELSE 0 END) AS rebounds,
        SUM(CASE WHEN LOWER(e.stat_type) = 'steal' THEN e.stat_value ELSE 0 END) AS steals,
        SUM(CASE WHEN LOWER(e.stat_type) = 'block' THEN e.stat_value ELSE 0 END) AS blocks,
        SUM(CASE WHEN LOWER(e.stat_type) = 'turnover' THEN e.stat_value ELSE 0 END) AS turnovers,
        SUM(CASE WHEN LOWER(e.stat_type) = 'minutes_played' THEN e.stat_value ELSE 0 END) AS minutes_played,
        MAX(e.created_at) as last_event_time
    FROM player_stat_events e
    JOIN games g ON e.game_id = g.game_id
    WHERE g.state = 'IN_PROGRESS'
    GROUP BY e.team_id, e.game_id, g.season_id
)
SELECT 
    ls.team_id,
    ls.season_id,
    t.name as team_name,
    t.league_id,
    l.name as league_name,
    ls.points AS ppg,
    ls.assists AS apg,
    ls.rebounds AS rpg,
    ls.steals AS spg,
    ls.blocks AS bpg,
    ls.turnovers AS topg,
    ls.minutes_played AS mpg,
    1 AS games_played,
    ls.last_event_time as last_updated
FROM live_stats ls
JOIN games g ON ls.game_id = g.game_id
JOIN teams t ON ls.team_id = t.team_id
JOIN leagues l ON t.league_id = l.league_id;

-- Create view for combined team stats (both completed and live games)
CREATE VIEW team_combined_stats AS
WITH combined_stats AS (
    -- Get completed game stats
    SELECT 
        team_id,
        season_id,
        team_name,
        league_id,
        league_name,
        ppg * games_played as total_points,
        apg * games_played as total_assists,
        rpg * games_played as total_rebounds,
        spg * games_played as total_steals,
        bpg * games_played as total_blocks,
        topg * games_played as total_turnovers,
        mpg * games_played as total_minutes,
        games_played,
        last_updated
    FROM team_session_avg
    
    UNION ALL
    
    -- Get live game stats
    SELECT 
        team_id,
        season_id,
        team_name,
        league_id,
        league_name,
        ppg as total_points,
        apg as total_assists,
        rpg as total_rebounds,
        spg as total_steals,
        bpg as total_blocks,
        topg as total_turnovers,
        mpg as total_minutes,
        games_played,
        last_updated
    FROM team_live_stats
)
SELECT 
    team_id,
    season_id,
    team_name,
    league_id,
    league_name,
    SUM(total_points) / SUM(games_played) as ppg,
    SUM(total_assists) / SUM(games_played) as apg,
    SUM(total_rebounds) / SUM(games_played) as rpg,
    SUM(total_steals) / SUM(games_played) as spg,
    SUM(total_blocks) / SUM(games_played) as bpg,
    SUM(total_turnovers) / SUM(games_played) as topg,
    SUM(total_minutes) / SUM(games_played) as mpg,
    SUM(games_played) as games_played,
    MAX(last_updated) as last_updated
FROM combined_stats
GROUP BY team_id, season_id, team_name, league_id, league_name;

-- Create trigger to refresh views when stats are updated
CREATE TRIGGER refresh_stats_trigger
    AFTER INSERT OR UPDATE OR DELETE ON player_stat_events
    FOR EACH STATEMENT
    EXECUTE FUNCTION refresh_stat_views();

-- Create trigger to refresh views when game state changes
CREATE TRIGGER refresh_stats_on_game_state_change
    AFTER UPDATE OF state ON games
    FOR EACH ROW
    WHEN (OLD.state != NEW.state)
    EXECUTE FUNCTION refresh_stat_views();

-- Create indexes for the underlying tables to help view performance
CREATE INDEX idx_stat_player_season ON player_stat_events (player_id, game_id);
CREATE INDEX idx_stat_team_season ON player_stat_events (team_id, game_id);
CREATE INDEX idx_game_season ON games (game_id, season_id);

-- Create continuous aggregate for player stats
CREATE MATERIALIZED VIEW player_stats_cagg
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 hour', timestamp) as bucket,
    player_id,
    game_id,
    stat_type,
    SUM(stat_value) as total_value,
    COUNT(*) as event_count
FROM player_stat_events
GROUP BY bucket, player_id, game_id, stat_type
WITH NO DATA;

-- Create continuous aggregate for team stats
CREATE MATERIALIZED VIEW team_stats_cagg
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 hour', timestamp) as bucket,
    team_id,
    game_id,
    stat_type,
    SUM(stat_value) as total_value,
    COUNT(*) as event_count
FROM player_stat_events
GROUP BY bucket, team_id, game_id, stat_type
WITH NO DATA;

-- Create indexes
CREATE INDEX idx_game_state ON games (state, game_id);
CREATE INDEX idx_game_season_state ON games (season_id, state);
CREATE INDEX idx_game_team ON games (home_team_id, away_team_id);

-- Composite indexes for stat queries
CREATE INDEX idx_player_stats_game ON player_stat_events (player_id, game_id, stat_type);
CREATE INDEX idx_team_stats_game ON player_stat_events (team_id, game_id, stat_type);
CREATE INDEX idx_stats_timestamp ON player_stat_events (timestamp, game_id);

-- Partial indexes for live games (smaller, faster)
CREATE INDEX idx_live_game_stats ON player_stat_events (game_id, stat_type, stat_value) 
WHERE game_id IN (SELECT game_id FROM games WHERE state = 'IN_PROGRESS');

CREATE INDEX idx_live_game_events ON player_stat_events (game_id, player_id, stat_type) 
WHERE game_id IN (SELECT game_id FROM games WHERE state = 'IN_PROGRESS');

-- Explain index benefits
COMMENT ON INDEX idx_game_state IS 'Fast filtering of live vs completed games';
COMMENT ON INDEX idx_game_season_state IS 'Efficient season-based queries with game state';
COMMENT ON INDEX idx_game_team IS 'Quick team game lookups';
COMMENT ON INDEX idx_player_stats_game IS 'Optimizes player game stat retrieval';
COMMENT ON INDEX idx_team_stats_game IS 'Optimizes team game stat retrieval';
COMMENT ON INDEX idx_stats_timestamp IS 'Helps TimescaleDB chunk navigation';
COMMENT ON INDEX idx_live_game_stats IS 'Small index only for active games';
COMMENT ON INDEX idx_live_game_events IS 'Fast live game stat queries';

-- Create player_stats view for overall player averages
CREATE VIEW player_stats AS
WITH game_totals AS (
    SELECT 
        e.player_id,
        e.game_id,
        SUM(CASE WHEN stat_type = 'point' THEN stat_value ELSE 0 END) as points,
        SUM(CASE WHEN stat_type = 'assist' THEN stat_value ELSE 0 END) as assists,
        SUM(CASE WHEN stat_type = 'rebound' THEN stat_value ELSE 0 END) as rebounds,
        SUM(CASE WHEN stat_type = 'steal' THEN stat_value ELSE 0 END) as steals,
        SUM(CASE WHEN stat_type = 'block' THEN stat_value ELSE 0 END) as blocks,
        SUM(CASE WHEN stat_type = 'turnover' THEN stat_value ELSE 0 END) as turnovers,
        SUM(CASE WHEN stat_type = 'minutes_played' THEN stat_value ELSE 0 END) as minutes
    FROM player_stat_events e
    GROUP BY e.player_id, e.game_id
)
SELECT 
    p.name,
    p.player_id,
    COUNT(DISTINCT gt.game_id) as games_played,
    ROUND(AVG(gt.points), 1) as ppg,
    ROUND(AVG(gt.assists), 1) as apg,
    ROUND(AVG(gt.rebounds), 1) as rpg,
    ROUND(AVG(gt.steals), 1) as spg,
    ROUND(AVG(gt.blocks), 1) as bpg,
    ROUND(AVG(gt.turnovers), 1) as topg,
    ROUND(AVG(gt.minutes), 1) as mpg
FROM players p
LEFT JOIN game_totals gt ON p.player_id = gt.player_id
GROUP BY p.name, p.player_id;

-- Create team_stats view for overall team averages
CREATE VIEW team_stats AS
WITH game_totals AS (
    SELECT 
        e.team_id,
        e.game_id,
        SUM(CASE WHEN stat_type = 'point' THEN stat_value ELSE 0 END) as points,
        SUM(CASE WHEN stat_type = 'assist' THEN stat_value ELSE 0 END) as assists,
        SUM(CASE WHEN stat_type = 'rebound' THEN stat_value ELSE 0 END) as rebounds,
        SUM(CASE WHEN stat_type = 'steal' THEN stat_value ELSE 0 END) as steals,
        SUM(CASE WHEN stat_type = 'block' THEN stat_value ELSE 0 END) as blocks,
        SUM(CASE WHEN stat_type = 'turnover' THEN stat_value ELSE 0 END) as turnovers,
        SUM(CASE WHEN stat_type = 'minutes_played' THEN stat_value ELSE 0 END) as minutes
    FROM player_stat_events e
    GROUP BY e.team_id, e.game_id
)
SELECT 
    t.name,
    t.team_id,
    COUNT(DISTINCT gt.game_id) as games_played,
    ROUND(AVG(gt.points), 1) as ppg,
    ROUND(AVG(gt.assists), 1) as apg,
    ROUND(AVG(gt.rebounds), 1) as rpg,
    ROUND(AVG(gt.steals), 1) as spg,
    ROUND(AVG(gt.blocks), 1) as bpg,
    ROUND(AVG(gt.turnovers), 1) as topg,
    ROUND(AVG(gt.minutes), 1) as mpg
FROM teams t
LEFT JOIN game_totals gt ON t.team_id = gt.team_id
GROUP BY t.name, t.team_id;

-- Create indexes for performance
CREATE INDEX idx_player_stats_game ON player_stat_events (player_id, game_id, stat_type);
CREATE INDEX idx_game_state ON games (game_id, state, season_id);
CREATE INDEX idx_team_stats_game ON player_stat_events (team_id, game_id, stat_type);

-- Create view for live team stats (current/in-progress games)
CREATE VIEW team_live_stats AS
WITH live_stats AS (
    SELECT 
        e.team_id,
        e.game_id,
        g.season_id,
        SUM(CASE WHEN LOWER(e.stat_type) = 'point' THEN e.stat_value ELSE 0 END) AS points,
        SUM(CASE WHEN LOWER(e.stat_type) = 'assist' THEN e.stat_value ELSE 0 END) AS assists,
        SUM(CASE WHEN LOWER(e.stat_type) = 'rebound' THEN e.stat_value ELSE 0 END) AS rebounds,
        SUM(CASE WHEN LOWER(e.stat_type) = 'steal' THEN e.stat_value ELSE 0 END) AS steals,
        SUM(CASE WHEN LOWER(e.stat_type) = 'block' THEN e.stat_value ELSE 0 END) AS blocks,
        SUM(CASE WHEN LOWER(e.stat_type) = 'turnover' THEN e.stat_value ELSE 0 END) AS turnovers,
        SUM(CASE WHEN LOWER(e.stat_type) = 'minutes_played' THEN e.stat_value ELSE 0 END) AS minutes_played,
        MAX(e.created_at) as last_event_time
    FROM player_stat_events e
    JOIN games g ON e.game_id = g.game_id
    WHERE g.state = 'IN_PROGRESS'
    GROUP BY e.team_id, e.game_id, g.season_id
)
SELECT 
    ls.team_id,
    ls.season_id,
    t.name as team_name,
    t.league_id,
    l.name as league_name,
    ls.points AS ppg,
    ls.assists AS apg,
    ls.rebounds AS rpg,
    ls.steals AS spg,
    ls.blocks AS bpg,
    ls.turnovers AS topg,
    ls.minutes_played AS mpg,
    1 AS games_played,
    ls.last_event_time as last_updated
FROM live_stats ls
JOIN games g ON ls.game_id = g.game_id
JOIN teams t ON ls.team_id = t.team_id
JOIN leagues l ON t.league_id = l.league_id;

-- Create view for combined team stats (both completed and live games)
CREATE VIEW team_combined_stats AS
WITH combined_stats AS (
    -- Get completed game stats
    SELECT 
        team_id,
        season_id,
        team_name,
        league_id,
        league_name,
        ppg * games_played as total_points,
        apg * games_played as total_assists,
        rpg * games_played as total_rebounds,
        spg * games_played as total_steals,
        bpg * games_played as total_blocks,
        topg * games_played as total_turnovers,
        mpg * games_played as total_minutes,
        games_played,
        last_updated
    FROM team_session_avg
    
    UNION ALL
    
    -- Get live game stats
    SELECT 
        team_id,
        season_id,
        team_name,
        league_id,
        league_name,
        ppg as total_points,
        apg as total_assists,
        rpg as total_rebounds,
        spg as total_steals,
        bpg as total_blocks,
        topg as total_turnovers,
        mpg as total_minutes,
        games_played,
        last_updated
    FROM team_live_stats
)
SELECT 
    team_id,
    season_id,
    team_name,
    league_id,
    league_name,
    SUM(total_points) / SUM(games_played) as ppg,
    SUM(total_assists) / SUM(games_played) as apg,
    SUM(total_rebounds) / SUM(games_played) as rpg,
    SUM(total_steals) / SUM(games_played) as spg,
    SUM(total_blocks) / SUM(games_played) as bpg,
    SUM(total_turnovers) / SUM(games_played) as topg,
    SUM(total_minutes) / SUM(games_played) as mpg,
    SUM(games_played) as games_played,
    MAX(last_updated) as last_updated
FROM combined_stats
GROUP BY team_id, season_id, team_name, league_id, league_name;

-- Create trigger to refresh views when stats are updated
CREATE TRIGGER refresh_stats_trigger
    AFTER INSERT OR UPDATE OR DELETE ON player_stat_events
    FOR EACH STATEMENT
    EXECUTE FUNCTION refresh_stat_views();

-- Create trigger to refresh views when game state changes
CREATE TRIGGER refresh_stats_on_game_state_change
    AFTER UPDATE OF state ON games
    FOR EACH ROW
    WHEN (OLD.state != NEW.state)
    EXECUTE FUNCTION refresh_stat_views();

-- Create indexes for the underlying tables to help view performance
CREATE INDEX idx_stat_player_season ON player_stat_events (player_id, game_id);
CREATE INDEX idx_stat_team_season ON player_stat_events (team_id, game_id);
CREATE INDEX idx_game_season ON games (game_id, season_id);

-- Create continuous aggregate for player stats
CREATE MATERIALIZED VIEW player_stats_cagg
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 hour', timestamp) as bucket,
    player_id,
    game_id,
    stat_type,
    SUM(stat_value) as total_value,
    COUNT(*) as event_count
FROM player_stat_events
GROUP BY bucket, player_id, game_id, stat_type
WITH NO DATA;

-- Create continuous aggregate for team stats
CREATE MATERIALIZED VIEW team_stats_cagg
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 hour', timestamp) as bucket,
    team_id,
    game_id,
    stat_type,
    SUM(stat_value) as total_value,
    COUNT(*) as event_count
FROM player_stat_events
GROUP BY bucket, team_id, game_id, stat_type
WITH NO DATA;

-- Create indexes
CREATE INDEX idx_game_state ON games (state, game_id);
CREATE INDEX idx_game_season_state ON games (season_id, state);
CREATE INDEX idx_game_team ON games (home_team_id, away_team_id);

-- Composite indexes for stat queries
CREATE INDEX idx_player_stats_game ON player_stat_events (player_id, game_id, stat_type);
CREATE INDEX idx_team_stats_game ON player_stat_events (team_id, game_id, stat_type);
CREATE INDEX idx_stats_timestamp ON player_stat_events (timestamp, game_id);

-- Partial indexes for live games (smaller, faster)
CREATE INDEX idx_live_game_stats ON player_stat_events (game_id, stat_type, stat_value) 
WHERE game_id IN (SELECT game_id FROM games WHERE state = 'IN_PROGRESS');

CREATE INDEX idx_live_game_events ON player_stat_events (game_id, player_id, stat_type) 
WHERE game_id IN (SELECT game_id FROM games WHERE state = 'IN_PROGRESS');

-- Explain index benefits
COMMENT ON INDEX idx_game_state IS 'Fast filtering of live vs completed games';
COMMENT ON INDEX idx_game_season_state IS 'Efficient season-based queries with game state';
COMMENT ON INDEX idx_game_team IS 'Quick team game lookups';
COMMENT ON INDEX idx_player_stats_game IS 'Optimizes player game stat retrieval';
COMMENT ON INDEX idx_team_stats_game IS 'Optimizes team game stat retrieval';
COMMENT ON INDEX idx_stats_timestamp IS 'Helps TimescaleDB chunk navigation';
COMMENT ON INDEX idx_live_game_stats IS 'Small index only for active games';
COMMENT ON INDEX idx_live_game_events IS 'Fast live game stat queries';

-- Create player_stats view for overall player averages
CREATE VIEW player_stats AS
WITH game_totals AS (
    SELECT 
        e.player_id,
        e.game_id,
        SUM(CASE WHEN stat_type = 'point' THEN stat_value ELSE 0 END) as points,
        SUM(CASE WHEN stat_type = 'assist' THEN stat_value ELSE 0 END) as assists,
        SUM(CASE WHEN stat_type = 'rebound' THEN stat_value ELSE 0 END) as rebounds,
        SUM(CASE WHEN stat_type = 'steal' THEN stat_value ELSE 0 END) as steals,
        SUM(CASE WHEN stat_type = 'block' THEN stat_value ELSE 0 END) as blocks,
        SUM(CASE WHEN stat_type = 'turnover' THEN stat_value ELSE 0 END) as turnovers,
        SUM(CASE WHEN stat_type = 'minutes_played' THEN stat_value ELSE 0 END) as minutes
    FROM player_stat_events e
    GROUP BY e.player_id, e.game_id
)
SELECT 
    p.name,
    p.player_id,
    COUNT(DISTINCT gt.game_id) as games_played,
    ROUND(AVG(gt.points), 1) as ppg,
    ROUND(AVG(gt.assists), 1) as apg,
    ROUND(AVG(gt.rebounds), 1) as rpg,
    ROUND(AVG(gt.steals), 1) as spg,
    ROUND(AVG(gt.blocks), 1) as bpg,
    ROUND(AVG(gt.turnovers), 1) as topg,
    ROUND(AVG(gt.minutes), 1) as mpg
FROM players p
LEFT JOIN game_totals gt ON p.player_id = gt.player_id
GROUP BY p.name, p.player_id;

-- Create team_stats view for overall team averages
CREATE VIEW team_stats AS
WITH game_totals AS (
    SELECT 
        e.team_id,
        e.game_id,
        SUM(CASE WHEN stat_type = 'point' THEN stat_value ELSE 0 END) as points,
        SUM(CASE WHEN stat_type = 'assist' THEN stat_value ELSE 0 END) as assists,
        SUM(CASE WHEN stat_type = 'rebound' THEN stat_value ELSE 0 END) as rebounds,
        SUM(CASE WHEN stat_type = 'steal' THEN stat_value ELSE 0 END) as steals,
        SUM(CASE WHEN stat_type = 'block' THEN stat_value ELSE 0 END) as blocks,
        SUM(CASE WHEN stat_type = 'turnover' THEN stat_value ELSE 0 END) as turnovers,
        SUM(CASE WHEN stat_type = 'minutes_played' THEN stat_value ELSE 0 END) as minutes
    FROM player_stat_events e
    GROUP BY e.team_id, e.game_id
)
SELECT 
    t.name,
    t.team_id,
    COUNT(DISTINCT gt.game_id) as games_played,
    ROUND(AVG(gt.points), 1) as ppg,
    ROUND(AVG(gt.assists), 1) as apg,
    ROUND(AVG(gt.rebounds), 1) as rpg,
    ROUND(AVG(gt.steals), 1) as spg,
    ROUND(AVG(gt.blocks), 1) as bpg,
    ROUND(AVG(gt.turnovers), 1) as topg,
    ROUND(AVG(gt.minutes), 1) as mpg
FROM teams t
LEFT JOIN game_totals gt ON t.team_id = gt.team_id
GROUP BY t.name, t.team_id;

-- Create indexes for performance
CREATE INDEX idx_player_stats_game ON player_stat_events (player_id, game_id, stat_type);
CREATE INDEX idx_game_state ON games (game_id, state, season_id);
CREATE INDEX idx_team_stats_game ON player_stat_events (team_id, game_id, stat_type);

-- Create view for live team stats (current/in-progress games)
CREATE VIEW team_live_stats AS
WITH live_stats AS (
    SELECT 
        e.team_id,
        e.game_id,
        g.season_id,
        SUM(CASE WHEN LOWER(e.stat_type) = 'point' THEN e.stat_value ELSE 0 END) AS points,
        SUM(CASE WHEN LOWER(e.stat_type) = 'assist' THEN e.stat_value ELSE 0 END) AS assists,
        SUM(CASE WHEN LOWER(e.stat_type) = 'rebound' THEN e.stat_value ELSE 0 END) AS rebounds,
        SUM(CASE WHEN LOWER(e.stat_type) = 'steal' THEN e.stat_value ELSE 0 END) AS steals,
        SUM(CASE WHEN LOWER(e.stat_type) = 'block' THEN e.stat_value ELSE 0 END) AS blocks,
        SUM(CASE WHEN LOWER(e.stat_type) = 'turnover' THEN e.stat_value ELSE 0 END) AS turnovers,
        SUM(CASE WHEN LOWER(e.stat_type) = 'minutes_played' THEN e.stat_value ELSE 0 END) AS minutes_played,
        MAX(e.created_at) as last_event_time
    FROM player_stat_events e
    JOIN games g ON e.game_id = g.game_id
    WHERE g.state = 'IN_PROGRESS'
    GROUP BY e.team_id, e.game_id, g.season_id
)
SELECT 
    ls.team_id,
    ls.season_id,
    t.name as team_name,
    t.league_id,
    l.name as league_name,
    ls.points AS ppg,
    ls.assists AS apg,
    ls.rebounds AS rpg,
    ls.steals AS spg,
    ls.blocks AS bpg,
    ls.turnovers AS topg,
    ls.minutes_played AS mpg,
    1 AS games_played,
    ls.last_event_time as last_updated
FROM live_stats ls
JOIN games g ON ls.game_id = g.game_id
JOIN teams t ON ls.team_id = t.team_id
JOIN leagues l ON t.league_id = l.league_id;

-- Create view for combined team stats (both completed and live games)
CREATE VIEW team_combined_stats AS
WITH combined_stats AS (
    -- Get completed game stats
    SELECT 
        team_id,
        season_id,
        team_name,
        league_id,
        league_name,
        ppg * games_played as total_points,
        apg * games_played as total_assists,
        rpg * games_played as total_rebounds,
        spg * games_played as total_steals,
        bpg * games_played as total_blocks,
        topg * games_played as total_turnovers,
        mpg * games_played as total_minutes,
        games_played,
        last_updated
    FROM team_session_avg
    
    UNION ALL
    
    -- Get live game stats
    SELECT 
        team_id,
        season_id,
        team_name,
        league_id,
        league_name,
        ppg as total_points,
        apg as total_assists,
        rpg as total_rebounds,
        spg as total_steals,
        bpg as total_blocks,
        topg as total_turnovers,
        mpg as total_minutes,
        games_played,
        last_updated
    FROM team_live_stats
)
SELECT 
    team_id,
    season_id,
    team_name,
    league_id,
    league_name,
    SUM(total_points) / SUM(games_played) as ppg,
    SUM(total_assists) / SUM(games_played) as apg,
    SUM(total_rebounds) / SUM(games_played) as rpg,
    SUM(total_steals) / SUM(games_played) as spg,
    SUM(total_blocks) / SUM(games_played) as bpg,
    SUM(total_turnovers) / SUM(games_played) as topg,
    SUM(total_minutes) / SUM(games_played) as mpg,
    SUM(games_played) as games_played,
    MAX(last_updated) as last_updated
FROM combined_stats
GROUP BY team_id, season_id, team_name, league_id, league_name;

-- Create trigger to refresh views when stats are updated
CREATE TRIGGER refresh_stats_trigger
    AFTER INSERT OR UPDATE OR DELETE ON player_stat_events
    FOR EACH STATEMENT
    EXECUTE FUNCTION refresh_stat_views();

-- Create trigger to refresh views when game state changes
CREATE TRIGGER refresh_stats_on_game_state_change
    AFTER UPDATE OF state ON games
    FOR EACH ROW
    WHEN (OLD.state != NEW.state)
    EXECUTE FUNCTION refresh_stat_views();

-- Create indexes for the underlying tables to help view performance
CREATE INDEX idx_stat_player_season ON player_stat_events (player_id, game_id);
CREATE INDEX idx_stat_team_season ON player_stat_events (team_id, game_id);
CREATE INDEX idx_game_season ON games (game_id, season_id);

-- Create continuous aggregate for player stats
CREATE MATERIALIZED VIEW player_stats_cagg
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 hour', timestamp) as bucket,
    player_id,
    game_id,
    stat_type,
    SUM(stat_value) as total_value,
    COUNT(*) as event_count
FROM player_stat_events
GROUP BY bucket, player_id, game_id, stat_type
WITH NO DATA;

-- Create continuous aggregate for team stats
CREATE MATERIALIZED VIEW team_stats_cagg
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 hour', timestamp) as bucket,
    team_id,
    game_id,
    stat_type,
    SUM(stat_value) as total_value,
    COUNT(*) as event_count
FROM player_stat_events
GROUP BY bucket, team_id, game_id, stat_type
WITH NO DATA;

-- Create indexes
CREATE INDEX idx_game_state ON games (state, game_id);
CREATE INDEX idx_game_season_state ON games (season_id, state);
CREATE INDEX idx_game_team ON games (home_team_id, away_team_id);

-- Composite indexes for stat queries
CREATE INDEX idx_player_stats_game ON player_stat_events (player_id, game_id, stat_type);
CREATE INDEX idx_team_stats_game ON player_stat_events (team_id, game_id, stat_type);
CREATE INDEX idx_stats_timestamp ON player_stat_events (timestamp, game_id);

-- Partial indexes for live games (smaller, faster)
CREATE INDEX idx_live_game_stats ON player_stat_events (game_id, stat_type, stat_value) 
WHERE game_id IN (SELECT game_id FROM games WHERE state = 'IN_PROGRESS');

CREATE INDEX idx_live_game_events ON player_stat_events (game_id, player_id, stat_type) 
WHERE game_id IN (SELECT game_id FROM games WHERE state = 'IN_PROGRESS');

-- Explain index benefits
COMMENT ON INDEX idx_game_state IS 'Fast filtering of live vs completed games';
COMMENT ON INDEX idx_game_season_state IS 'Efficient season-based queries with game state';
COMMENT ON INDEX idx_game_team IS 'Quick team game lookups';
COMMENT ON INDEX idx_player_stats_game IS 'Optimizes player game stat retrieval';
COMMENT ON INDEX idx_team_stats_game IS 'Optimizes team game stat retrieval';
COMMENT ON INDEX idx_stats_timestamp IS 'Helps TimescaleDB chunk navigation';
COMMENT ON INDEX idx_live_game_stats IS 'Small index only for active games';
COMMENT ON INDEX idx_live_game_events IS 'Fast live game stat queries';

-- Create player_stats view for overall player averages
CREATE VIEW player_stats AS
WITH game_totals AS (
    SELECT 
        e.player_id,
        e.game_id,
        SUM(CASE WHEN stat_type = 'point' THEN stat_value ELSE 0 END) as points,
        SUM(CASE WHEN stat_type = 'assist' THEN stat_value ELSE 0 END) as assists,
        SUM(CASE WHEN stat_type = 'rebound' THEN stat_value ELSE 0 END) as rebounds,
        SUM(CASE WHEN stat_type = 'steal' THEN stat_value ELSE 0 END) as steals,
        SUM(CASE WHEN stat_type = 'block' THEN stat_value ELSE 0 END) as blocks,
        SUM(CASE WHEN stat_type = 'turnover' THEN stat_value ELSE 0 END) as turnovers,
        SUM(CASE WHEN stat_type = 'minutes_played' THEN stat_value ELSE 0 END) as minutes
    FROM player_stat_events e
    GROUP BY e.player_id, e.game_id
)
SELECT 
    p.name,
    p.player_id,
    COUNT(DISTINCT gt.game_id) as games_played,
    ROUND(AVG(gt.points), 1) as ppg,
    ROUND(AVG(gt.assists), 1) as apg,
    ROUND(AVG(gt.rebounds), 1) as rpg,
    ROUND(AVG(gt.steals), 1) as spg,
    ROUND(AVG(gt.blocks), 1) as bpg,
    ROUND(AVG(gt.turnovers), 1) as topg,
    ROUND(AVG(gt.minutes), 1) as mpg
FROM players p
LEFT JOIN game_totals gt ON p.player_id = gt.player_id
GROUP BY p.name, p.player_id;

-- Create team_stats view for overall team averages
CREATE VIEW team_stats AS
WITH game_totals AS (
    SELECT 
        e.team_id,
        e.game_id,
        SUM(CASE WHEN stat_type = 'point' THEN stat_value ELSE 0 END) as points,
        SUM(CASE WHEN stat_type = 'assist' THEN stat_value ELSE 0 END) as assists,
        SUM(CASE WHEN stat_type = 'rebound' THEN stat_value ELSE 0 END) as rebounds,
        SUM(CASE WHEN stat_type = 'steal' THEN stat_value ELSE 0 END) as steals,
        SUM(CASE WHEN stat_type = 'block' THEN stat_value ELSE 0 END) as blocks,
        SUM(CASE WHEN stat_type = 'turnover' THEN stat_value ELSE 0 END) as turnovers,
        SUM(CASE WHEN stat_type = 'minutes_played' THEN stat_value ELSE 0 END) as minutes
    FROM player_stat_events e
    GROUP BY e.team_id, e.game_id
)
SELECT 
    t.name,
    t.team_id,
    COUNT(DISTINCT gt.game_id) as games_played,
    ROUND(AVG(gt.points), 1) as ppg,
    ROUND(AVG(gt.assists), 1) as apg,
    ROUND(AVG(gt.rebounds), 1) as rpg,
    ROUND(AVG(gt.steals), 1) as spg,
    ROUND(AVG(gt.blocks), 1) as bpg,
    ROUND(AVG(gt.turnovers), 1) as topg,
    ROUND(AVG(gt.minutes), 1) as mpg
FROM teams t
LEFT JOIN game_totals gt ON t.team_id = gt.team_id
GROUP BY t.name, t.team_id;

-- Create indexes for performance
CREATE INDEX idx_player_stats_game ON player_stat_events (player_id, game_id, stat_type);
CREATE INDEX idx_game_state ON games (game_id, state, season_id);
CREATE INDEX idx_team_stats_game ON player_stat_events (team_id, game_id, stat_type);

-- Create view for live team stats (current/in-progress games)
CREATE VIEW team_live_stats AS
WITH live_stats AS (
    SELECT 
        e.team_id,
        e.game_id,
        g.season_id,
        SUM(CASE WHEN LOWER(e.stat_type) = 'point' THEN e.stat_value ELSE 0 END) AS points,
        SUM(CASE WHEN LOWER(e.stat_type) = 'assist' THEN e.stat_value ELSE 0 END) AS assists,
        SUM(CASE WHEN LOWER(e.stat_type) = 'rebound' THEN e.stat_value ELSE 0 END) AS rebounds,
        SUM(CASE WHEN LOWER(e.stat_type) = 'steal' THEN e.stat_value ELSE 0 END) AS steals,
        SUM(CASE WHEN LOWER(e.stat_type) = 'block' THEN e.stat_value ELSE 0 END) AS blocks,
        SUM(CASE WHEN LOWER(e.stat_type) = 'turnover' THEN e.stat_value ELSE 0 END) AS turnovers,
        SUM(CASE WHEN LOWER(e.stat_type) = 'minutes_played' THEN e.stat_value ELSE 0 END) AS minutes_played,
        MAX(e.created_at) as last_event_time
    FROM player_stat_events e
    JOIN games g ON e.game_id = g.game_id
    WHERE g.state = 'IN_PROGRESS'
    GROUP BY e.team_id, e.game_id, g.season_id
)
SELECT 
    ls.team_id,
    ls.season_id,
    t.name as team_name,
    t.league_id,
    l.name as league_name,
    ls.points AS ppg,
    ls.assists AS apg,
    ls.rebounds AS rpg,
    ls.steals AS spg,
    ls.blocks AS bpg,
    ls.turnovers AS topg,
    ls.minutes_played AS mpg,
    1 AS games_played,
    ls.last_event_time as last_updated
FROM live_stats ls
JOIN games g ON ls.game_id = g.game_id
JOIN teams t ON ls.team_id = t.team_id
JOIN leagues l ON t.league_id = l.league_id;

-- Create view for combined team stats (both completed and live games)
CREATE VIEW team_combined_stats AS
WITH combined_stats AS (
    -- Get completed game stats
    SELECT 
        team_id,
        season_id,
        team_name,
        league_id,
        league_name,
        ppg * games_played as total_points,
        apg * games_played as total_assists,
        rpg * games_played as total_rebounds,
        spg * games_played as total_steals,
        bpg * games_played as total_blocks,
        topg * games_played as total_turnovers,
        mpg * games_played as total_minutes,
        games_played,
        last_updated
    FROM team_session_avg
    
    UNION ALL
    
    -- Get live game stats
    SELECT 
        team_id,
        season_id,
        team_name,
        league_id,
        league_name,
        ppg as total_points,
        apg as total_assists,
        rpg as total_rebounds,
        spg as total_steals,
        bpg as total_blocks,
        topg as total_turnovers,
        mpg as total_minutes,
        games_played,
        last_updated
    FROM team_live_stats
)
SELECT 
    team_id,
    season_id,
    team_name,
    league_id,
    league_name,
    SUM(total_points) / SUM(games_played) as ppg,
    SUM(total_assists) / SUM(games_played) as apg,
    SUM(total_rebounds) / SUM(games_played) as rpg,
    SUM(total_steals) / SUM(games_played) as spg,
    SUM(total_blocks) / SUM(games_played) as bpg,
    SUM(total_turnovers) / SUM(games_played) as topg,
    SUM(total_minutes) / SUM(games_played) as mpg,
    SUM(games_played) as games_played,
    MAX(last_updated) as last_updated
FROM combined_stats
GROUP BY team_id, season_id, team_name, league_id, league_name;

-- Create trigger to refresh views when stats are updated
CREATE TRIGGER refresh_stats_trigger
    AFTER INSERT OR UPDATE OR DELETE ON player_stat_events
    FOR EACH STATEMENT
    EXECUTE FUNCTION refresh_stat_views();

-- Create trigger to refresh views when game state changes
CREATE TRIGGER refresh_stats_on_game_state_change
    AFTER UPDATE OF state ON games
    FOR EACH ROW
    WHEN (OLD.state != NEW.state)
    EXECUTE FUNCTION refresh_stat_views();

-- Create indexes for the underlying tables to help view performance
CREATE INDEX idx_stat_player_season ON player_stat_events (player_id, game_id);
CREATE INDEX idx_stat_team_season ON player_stat_events (team_id, game_id);
CREATE INDEX idx_game_season ON games (game_id, season_id);

-- Create continuous aggregate for player stats
CREATE MATERIALIZED VIEW player_stats_cagg
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 hour', timestamp) as bucket,
    player_id,
    game_id,
    stat_type,
    SUM(stat_value) as total_value,
    COUNT(*) as event_count
FROM player_stat_events
GROUP BY bucket, player_id, game_id, stat_type
WITH NO DATA;

-- Create continuous aggregate for team stats
CREATE MATERIALIZED VIEW team_stats_cagg
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 hour', timestamp) as bucket,
    team_id,
    game_id,
    stat_type,
    SUM(stat_value) as total_value,
    COUNT(*) as event_count
FROM player_stat_events
GROUP BY bucket, team_id, game_id, stat_type
WITH NO DATA;

-- Create indexes
CREATE INDEX idx_game_state ON games (state, game_id);
CREATE INDEX idx_game_season_state ON games (season_id, state);
CREATE INDEX idx_game_team ON games (home_team_id, away_team_id);

-- Composite indexes for stat queries
CREATE INDEX idx_player_stats_game ON player_stat_events (player_id, game_id, stat_type);
CREATE INDEX idx_team_stats_game ON player_stat_events (team_id, game_id, stat_type);
CREATE INDEX idx_stats_timestamp ON player_stat_events (timestamp, game_id);

-- Partial indexes for live games (smaller, faster)
CREATE INDEX idx_live_game_stats ON player_stat_events (game_id, stat_type, stat_value) 
WHERE game_id IN (SELECT game_id FROM games WHERE state = 'IN_PROGRESS');

CREATE INDEX idx_live_game_events ON player_stat_events (game_id, player_id, stat_type) 
WHERE game_id IN (SELECT game_id FROM games WHERE state = 'IN_PROGRESS');

-- Explain index benefits
COMMENT ON INDEX idx_game_state IS 'Fast filtering of live vs completed games';
COMMENT ON INDEX idx_game_season_state IS 'Efficient season-based queries with game state';
COMMENT ON INDEX idx_game_team IS 'Quick team game lookups';
COMMENT ON INDEX idx_player_stats_game IS 'Optimizes player game stat retrieval';
COMMENT ON INDEX idx_team_stats_game IS 'Optimizes team game stat retrieval';
COMMENT ON INDEX idx_stats_timestamp IS 'Helps TimescaleDB chunk navigation';
COMMENT ON INDEX idx_live_game_stats IS 'Small index only for active games';
COMMENT ON INDEX idx_live_game_events IS 'Fast live game stat queries';

-- Create player_stats view for overall player averages
CREATE VIEW player_stats AS
WITH game_totals AS (
    SELECT 
        e.player_id,
        e.game_id,
        SUM(CASE WHEN stat_type = 'point' THEN stat_value ELSE 0 END) as points,
        SUM(CASE WHEN stat_type = 'assist' THEN stat_value ELSE 0 END) as assists,
        SUM(CASE WHEN stat_type = 'rebound' THEN stat_value ELSE 0 END) as rebounds,
        SUM(CASE WHEN stat_type = 'steal' THEN stat_value ELSE 0 END) as steals,
        SUM(CASE WHEN stat_type = 'block' THEN stat_value ELSE 0 END) as blocks,
        SUM(CASE WHEN stat_type = 'turnover' THEN stat_value ELSE 0 END) as turnovers,
        SUM(CASE WHEN stat_type = 'minutes_played' THEN stat_value ELSE 0 END) as minutes
    FROM player_stat_events e
    GROUP BY e.player_id, e.game_id
)
SELECT 
    p.name,
    p.player_id,
    COUNT(DISTINCT gt.game_id) as games_played,
    ROUND(AVG(gt.points), 1) as ppg,
    ROUND(AVG(gt.assists), 1) as apg,
    ROUND(AVG(gt.rebounds), 1) as rpg,
    ROUND(AVG(gt.steals), 1) as spg,
    ROUND(AVG(gt.blocks), 1) as bpg,
    ROUND(AVG(gt.turnovers), 1) as topg,
    ROUND(AVG(gt.minutes), 1) as mpg
FROM players p
LEFT JOIN game_totals gt ON p.player_id = gt.player_id
GROUP BY p.name, p.player_id;

-- Create team_stats view for overall team averages
CREATE VIEW team_stats AS
WITH game_totals AS (
    SELECT 
        e.team_id,
        e.game_id,
        SUM(CASE WHEN stat_type = 'point' THEN stat_value ELSE 0 END) as points,
        SUM(CASE WHEN stat_type = 'assist' THEN stat_value ELSE 0 END) as assists,
        SUM(CASE WHEN stat_type = 'rebound' THEN stat_value ELSE 0 END) as rebounds,
        SUM(CASE WHEN stat_type = 'steal' THEN stat_value ELSE 0 END) as steals,
        SUM(CASE WHEN stat_type = 'block' THEN stat_value ELSE 0 END) as blocks,
        SUM(CASE WHEN stat_type = 'turnover' THEN stat_value ELSE 0 END) as turnovers,
        SUM(CASE WHEN stat_type = 'minutes_played' THEN stat_value ELSE 0 END) as minutes
    FROM player_stat_events e
    GROUP BY e.team_id, e.game_id
)
SELECT 
    t.name,
    t.team_id,
    COUNT(DISTINCT gt.game_id) as games_played,
    ROUND(AVG(gt.points), 1) as ppg,
    ROUND(AVG(gt.assists), 1) as apg,
    ROUND(AVG(gt.rebounds), 1) as rpg,
    ROUND(AVG(gt.steals), 1) as spg,
    ROUND(AVG(gt.blocks), 1) as bpg,
    ROUND(AVG(gt.turnovers), 1) as topg,
    ROUND(AVG(gt.minutes), 1) as mpg
FROM teams t
LEFT JOIN game_totals gt ON t.team_id = gt.team_id
GROUP BY t.name, t.team_id;

-- Create indexes for performance
CREATE INDEX idx_player_stats_game ON player_stat_events (player_id, game_id, stat_type);
CREATE INDEX idx_game_state ON games (game_id, state, season_id);
CREATE INDEX idx_team_stats_game ON player_stat_events (team_id, game_id, stat_type);

-- Create view for live team stats (current/in-progress games)
CREATE VIEW team_live_stats AS
WITH live_stats AS (
    SELECT 
        e.team_id,
        e.game_id,
        g.season_id,
        SUM(CASE WHEN LOWER(e.stat_type) = 'point' THEN e.stat_value ELSE 0 END) AS points,
        SUM(CASE WHEN LOWER(e.stat_type) = 'assist' THEN e.stat_value ELSE 0 END) AS assists,
        SUM(CASE WHEN LOWER(e.stat_type) = 'rebound' THEN e.stat_value ELSE 0 END) AS rebounds,
        SUM(CASE WHEN LOWER(e.stat_type) = 'steal' THEN e.stat_value ELSE 0 END) AS steals,
        SUM(CASE WHEN LOWER(e.stat_type) = 'block' THEN e.stat_value ELSE 0 END) AS blocks,
        SUM(CASE WHEN LOWER(e.stat_type) = 'turnover' THEN e.stat_value ELSE 0 END) AS turnovers,
        SUM(CASE WHEN LOWER(e.stat_type) = 'minutes_played' THEN e.stat_value ELSE 0 END) AS minutes_played,
        MAX(e.created_at) as last_event_time
    FROM player_stat_events e
    JOIN games g ON e.game_id = g.game_id
    WHERE g.state = 'IN_PROGRESS'
    GROUP BY e.team_id, e.game_id, g.season_id
)
SELECT 
    ls.team_id,
    ls.season_id,
    t.name as team_name,
    t.league_id,
    l.name as league_name,
    ls.points AS ppg,
    ls.assists AS apg,
    ls.rebounds AS rpg,
    ls.steals AS spg,
    ls.blocks AS bpg,
    ls.turnovers AS topg,
    ls.minutes_played AS mpg,
    1 AS games_played,
    ls.last_event_time as last_updated
FROM live_stats ls
JOIN games g ON ls.game_id = g.game_id
JOIN teams t ON ls.team_id = t.team_id
JOIN leagues l ON t.league_id = l.league_id;

-- Create view for combined team stats (both completed and live games)
CREATE VIEW team_combined_stats AS
WITH combined_stats AS (
    -- Get completed game stats
    SELECT 
        team_id,
        season_id,
        team_name,
        league_id,
        league_name,
        ppg * games_played as total_points,
        apg * games_played as total_assists,
        rpg * games_played as total_rebounds,
        spg * games_played as total_steals,
        bpg * games_played as total_blocks,
        topg * games_played as total_turnovers,
        mpg * games_played as total_minutes,
        games_played,
        last_updated
    FROM team_session_avg
    
    UNION ALL
    
    -- Get live game stats
    SELECT 
        team_id,
        season_id,
        team_name,
        league_id,
        league_name,
        ppg as total_points,
        apg as total_assists,
        rpg as total_rebounds,
        spg as total_steals,
        bpg as total_blocks,
        topg as total_turnovers,
        mpg as total_minutes,
        games_played,
        last_updated
    FROM team_live_stats
)
SELECT 
    team_id,
    season_id,
    team_name,
    league_id,
    league_name,
    SUM(total_points) / SUM(games_played) as ppg,
    SUM(total_assists) / SUM(games_played) as apg,
    SUM(total_rebounds) / SUM(games_played) as rpg,
    SUM(total_steals) / SUM(games_played) as spg,
    SUM(total_blocks) / SUM(games_played) as bpg,
    SUM(total_turnovers) / SUM(games_played) as topg,
    SUM(total_minutes) / SUM(games_played) as mpg,
    SUM(games_played) as games_played,
    MAX(last_updated) as last_updated
FROM combined_stats
GROUP BY team_id, season_id, team_name, league_id, league_name;

-- Create trigger to refresh views when stats are updated
CREATE TRIGGER refresh_stats_trigger
    AFTER INSERT OR UPDATE OR DELETE ON player_stat_events
    FOR EACH STATEMENT
    EXECUTE FUNCTION refresh_stat_views();

-- Create trigger to refresh views when game state changes
CREATE TRIGGER refresh_stats_on_game_state_change
    AFTER UPDATE OF state ON games
    FOR EACH ROW
    WHEN (OLD.state != NEW.state)
    EXECUTE FUNCTION refresh_stat_views();

-- Create indexes for the underlying tables to help view performance
CREATE INDEX idx_stat_player_season ON player_stat_events (player_id, game_id);
CREATE INDEX idx_stat_team_season ON player_stat_events (team_id, game_id);
CREATE INDEX idx_game_season ON games (game_id, season_id);

-- Create continuous aggregate for player stats
CREATE MATERIALIZED VIEW player_stats_cagg
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 hour', timestamp) as bucket,
    player_id,
    game_id,
    stat_type,
    SUM(stat_value) as total_value,
    COUNT(*) as event_count
FROM player_stat_events
GROUP BY bucket, player_id, game_id, stat_type
WITH NO DATA;

-- Create continuous aggregate for team stats
CREATE MATERIALIZED VIEW team_stats_cagg
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 hour', timestamp) as bucket,
    team_id,
    game_id,
    stat_type,
    SUM(stat_value) as total_value,
    COUNT(*) as event_count
FROM player_stat_events
GROUP BY bucket, team_id, game_id, stat_type
WITH NO DATA;

-- Create indexes
CREATE INDEX idx_game_state ON games (state, game_id);
CREATE INDEX idx_game_season_state ON games (season_id, state);
CREATE INDEX idx_game_team ON games (home_team_id, away_team_id);

-- Composite indexes for stat queries
CREATE INDEX idx_player_stats_game ON player_stat_events (player_id, game_id, stat_type);
CREATE INDEX idx_team_stats_game ON player_stat_events (team_id, game_id, stat_type);
CREATE INDEX idx_stats_timestamp ON player_stat_events (timestamp, game_id);

-- Partial indexes for live games (smaller, faster)
CREATE INDEX idx_live_game_stats ON player_stat_events (game_id, stat_type, stat_value) 
WHERE game_id IN (SELECT game_id FROM games WHERE state = 'IN_PROGRESS');

CREATE INDEX idx_live_game_events ON player_stat_events (game_id, player_id, stat_type) 
WHERE game_id IN (SELECT game_id FROM games WHERE state = 'IN_PROGRESS');

-- Explain index benefits
COMMENT ON INDEX idx_game_state IS 'Fast filtering of live vs completed games';
COMMENT ON INDEX idx_game_season_state IS 'Efficient season-based queries with game state';
COMMENT ON INDEX idx_game_team IS 'Quick team game lookups';
COMMENT ON INDEX idx_player_stats_game IS 'Optimizes player game stat retrieval';
COMMENT ON INDEX idx_team_stats_game IS 'Optimizes team game stat retrieval';
COMMENT ON INDEX idx_stats_timestamp IS 'Helps TimescaleDB chunk navigation';
COMMENT ON INDEX idx_live_game_stats IS 'Small index only for active games';
COMMENT ON INDEX idx_live_game_events IS 'Fast live game stat queries';

-- Create player_stats view for overall player averages
CREATE VIEW player_stats AS
WITH game_totals AS (
    SELECT 
        e.player_id,
        e.game_id,
        SUM(CASE WHEN stat_type = 'point' THEN stat_value ELSE 0 END) as points,
        SUM(CASE WHEN stat_type = 'assist' THEN stat_value ELSE 0 END) as assists,
        SUM(CASE WHEN stat_type = 'rebound' THEN stat_value ELSE 0 END) as rebounds,
        SUM(CASE WHEN stat_type = 'steal' THEN stat_value ELSE 0 END) as steals,
        SUM(CASE WHEN stat_type = 'block' THEN stat_value ELSE 0 END) as blocks,
        SUM(CASE WHEN stat_type = 'turnover' THEN stat_value ELSE 0 END) as turnovers,
        SUM(CASE WHEN stat_type = 'minutes_played' THEN stat_value ELSE 0 END) as minutes
    FROM player_stat_events e
    GROUP BY e.player_id, e.game_id
)
SELECT 
    p.name,
    p.player_id,
    COUNT(DISTINCT gt.game_id) as games_played,
    ROUND(AVG(gt.points), 1) as ppg,
    ROUND(AVG(gt.assists), 1) as apg,
    ROUND(AVG(gt.rebounds), 1) as rpg,
    ROUND(AVG(gt.steals), 1) as spg,
    ROUND(AVG(gt.blocks), 1) as bpg,
    ROUND(AVG(gt.turnovers), 1) as topg,
    ROUND(AVG(gt.minutes), 1) as mpg
FROM players p
LEFT JOIN game_totals gt ON p.player_id = gt.player_id
GROUP BY p.name, p.player_id;

-- Create team_stats view for overall team averages
CREATE VIEW team_stats AS
WITH game_totals AS (
    SELECT 
        e.team_id,
        e.game_id,
        SUM(CASE WHEN stat_type = 'point' THEN stat_value ELSE 0 END) as points,
        SUM(CASE WHEN stat_type = 'assist' THEN stat_value ELSE 0 END) as assists,
        SUM(CASE WHEN stat_type = 'rebound' THEN stat_value ELSE 0 END) as rebounds,
        SUM(CASE WHEN stat_type = 'steal' THEN stat_value ELSE 0 END) as steals,
        SUM(CASE WHEN stat_type = 'block' THEN stat_value ELSE 0 END) as blocks,
        SUM(CASE WHEN stat_type = 'turnover' THEN stat_value ELSE 0 END) as turnovers,
        SUM(CASE WHEN stat_type = 'minutes_played' THEN stat_value ELSE 0 END) as minutes
    FROM player_stat_events e
    GROUP BY e.team_id, e.game_id
)
SELECT 
    t.name,
    t.team_id,
    COUNT(DISTINCT gt.game_id) as games_played,
    ROUND(AVG(gt.points), 1) as ppg,
    ROUND(AVG(gt.assists), 1) as apg,
    ROUND(AVG(gt.rebounds), 1) as rpg,
    ROUND(AVG(gt.steals), 1) as spg,
    ROUND(AVG(gt.blocks), 1) as bpg,
    ROUND(AVG(gt.turnovers), 1) as topg,
    ROUND(AVG(gt.minutes), 1) as mpg
FROM teams t
LEFT JOIN game_totals gt ON t.team_id = gt.team_id
GROUP BY t.name, t.team_id;

-- Create indexes for performance
CREATE INDEX idx_player_stats_game ON player_stat_events (player_id, game_id, stat_type);
CREATE INDEX idx_game_state ON games (game_id, state, season_id);
CREATE INDEX idx_team_stats_game ON player_stat_events (team_id, game_id, stat_type);

-- Create view for live team stats (current/in-progress games)
CREATE VIEW team_live_stats AS
WITH live_stats AS (
    SELECT 
        e.team_id,
        e.game_id,
        g.season_id,
        SUM(CASE WHEN LOWER(e.stat_type) = 'point' THEN e.stat_value ELSE 0 END) AS points,
        SUM(CASE WHEN LOWER(e.stat_type) = 'assist' THEN e.stat_value ELSE 0 END) AS assists,
        SUM(CASE WHEN LOWER(e.stat_type) = 'rebound' THEN e.stat_value ELSE 0 END) AS rebounds,
        SUM(CASE WHEN LOWER(e.stat_type) = 'steal' THEN e.stat_value ELSE 0 END) AS steals,
        SUM(CASE WHEN LOWER(e.stat_type) = 'block' THEN e.stat_value ELSE 0 END) AS blocks,
        SUM(CASE WHEN LOWER(e.stat_type) = 'turnover' THEN e.stat_value ELSE 0 END) AS turnovers,
        SUM(CASE WHEN LOWER(e.stat_type) = 'minutes_played' THEN e.stat_value ELSE 0 END) AS minutes_played,
        MAX(e.created_at) as last_event_time
    FROM player_stat_events e
    JOIN games g ON e.game_id = g.game_id
    WHERE g.state = 'IN_PROGRESS'
    GROUP BY e.team_id, e.game_id, g.season_id
)
SELECT 
    ls.team_id,
    ls.season_id,
    t.name as team_name,
    t.league_id,
    l.name as league_name,
    ls.points AS ppg,
    ls.assists AS apg,
    ls.rebounds AS rpg,
    ls.steals AS spg,
    ls.blocks AS bpg,
    ls.turnovers AS topg,
    ls.minutes_played AS mpg,
    1 AS games_played,
    ls.last_event_time as last_updated
FROM live_stats ls
JOIN games g ON ls.game_id = g.game_id
JOIN teams t ON ls.team_id = t.team_id
JOIN leagues l ON t.league_id = l.league_id;

-- Create view for combined team stats (both completed and live games)
CREATE VIEW team_combined_stats AS
WITH combined_stats AS (
    -- Get completed game stats
    SELECT 
        team_id,
        season_id,
        team_name,
        league_id,
        league_name,
        ppg * games_played as total_points,
        apg * games_played as total_assists,
        rpg * games_played as total_rebounds,
        spg * games_played as total_steals,
        bpg * games_played as total_blocks,
        topg * games_played as total_turnovers,
        mpg * games_played as total_minutes,
        games_played,
        last_updated
    FROM team_session_avg
    
    UNION ALL
    
    -- Get live game stats
    SELECT 
        team_id,
        season_id,
        team_name,
        league_id,
        league_name,
        ppg as total_points,
        apg as total_assists,
        rpg as total_rebounds,
        spg as total_steals,
        bpg as total_blocks,
        topg as total_turnovers,
        mpg as total_minutes,
        games_played,
        last_updated
    FROM team_live_stats
)
SELECT 
    team_id,
    season_id,
    team_name,
    league_id,
    league_name,
    SUM(total_points) / SUM(games_played) as ppg,
    SUM(total_assists) / SUM(games_played) as apg,
    SUM(total_rebounds) / SUM(games_played) as rpg,
    SUM(total_steals) / SUM(games_played) as spg,
    SUM(total_blocks) / SUM(games_played) as bpg,
    SUM(total_turnovers) / SUM(games_played) as topg,
    SUM(total_minutes) / SUM(games_played) as mpg,
    SUM(games_played) as games_played,
    MAX(last_updated) as last_updated
FROM combined_stats
GROUP BY team_id, season_id, team_name, league_id, league_name;

-- Create trigger to refresh views when stats are updated
CREATE TRIGGER refresh_stats_trigger
    AFTER INSERT OR UPDATE OR DELETE ON player_stat_events
    FOR EACH STATEMENT
    EXECUTE FUNCTION refresh_stat_views();

-- Create trigger to refresh views when game state changes
CREATE TRIGGER refresh_stats_on_game_state_change
    AFTER UPDATE OF state ON games
    FOR EACH ROW
    WHEN (OLD.state != NEW.state)
    EXECUTE FUNCTION refresh_stat_views();

-- Create indexes for the underlying tables to help view performance
CREATE INDEX idx_stat_player_season ON player_stat_events (player_id, game_id);
CREATE INDEX idx_stat_team_season ON player_stat_events (team_id, game_id);
CREATE INDEX idx_game_season ON games (game_id, season_id);

-- Create continuous aggregate for player stats
CREATE MATERIALIZED VIEW player_stats_cagg
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 hour', timestamp) as bucket,
    player_id,
    game_id,
    stat_type,
    SUM(stat_value) as total_value,
    COUNT(*) as event_count
FROM player_stat_events
GROUP BY bucket, player_id, game_id, stat_type
WITH NO DATA;

-- Create continuous aggregate for team stats
CREATE MATERIALIZED VIEW team_stats_cagg
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 hour', timestamp) as bucket,
    team_id,
    game_id,
    stat_type,
    SUM(stat_value) as total_value,
    COUNT(*) as event_count
FROM player_stat_events
GROUP BY bucket, team_id, game_id, stat_type
WITH NO DATA;

-- Create indexes
CREATE INDEX idx_game_state ON games (state, game_id);
CREATE INDEX idx_game_season_state ON games (season_id, state);
CREATE INDEX idx_game_team ON games (home_team_id, away_team_id);

-- Composite indexes for stat queries
CREATE INDEX idx_player_stats_game ON player_stat_events (player_id, game_id, stat_type);
CREATE INDEX idx_team_stats_game ON player_stat_events (team_id, game_id, stat_type);
CREATE INDEX idx_stats_timestamp ON player_stat_events (timestamp, game_id);

-- Partial indexes for live games (smaller, faster)
CREATE INDEX idx_live_game_stats ON player_stat_events (game_id, stat_type, stat_value) 
WHERE game_id IN (SELECT game_id FROM games WHERE state = 'IN_PROGRESS');

CREATE INDEX idx_live_game_events ON player_stat_events (game_id, player_id, stat_type) 
WHERE game_id IN (SELECT game_id FROM games WHERE state = 'IN_PROGRESS');

-- Explain index benefits
COMMENT ON INDEX idx_game_state IS 'Fast filtering of live vs completed games';
COMMENT ON INDEX idx_game_season_state IS 'Efficient season-based queries with game state';
COMMENT ON INDEX idx_game_team IS 'Quick team game lookups';
COMMENT ON INDEX idx_player_stats_game IS 'Optimizes player game stat retrieval';
COMMENT ON INDEX idx_team_stats_game IS 'Optimizes team game stat retrieval';
COMMENT ON INDEX idx_stats_timestamp IS 'Helps TimescaleDB chunk navigation';
COMMENT ON INDEX idx_live_game_stats IS 'Small index only for active games';
COMMENT ON INDEX idx_live_game_events IS 'Fast live game stat queries';

-- Create player_stats view for overall player averages
CREATE VIEW player_stats AS
WITH game_totals AS (
    SELECT 
        e.player_id,
        e.game_id,
        SUM(CASE WHEN stat_type = 'point' THEN stat_value ELSE 0 END) as points,
        SUM(CASE WHEN stat_type = 'assist' THEN stat_value ELSE 0 END) as assists,
        SUM(CASE WHEN stat_type = 'rebound' THEN stat_value ELSE 0 END) as rebounds,
        SUM(CASE WHEN stat_type = 'steal' THEN stat_value ELSE 0 END) as steals,
        SUM(CASE WHEN stat_type = 'block' THEN stat_value ELSE 0 END) as blocks,
        SUM(CASE WHEN stat_type = 'turnover' THEN stat_value ELSE 0 END) as turnovers,
        SUM(CASE WHEN stat_type = 'minutes_played' THEN stat_value ELSE 0 END) as minutes
    FROM player_stat_events e
    GROUP BY e.player_id, e.game_id
)
SELECT 
    p.name,
    p.player_id,
    COUNT(DISTINCT gt.game_id) as games_played,
    ROUND(AVG(gt.points), 1) as ppg,
    ROUND(AVG(gt.assists), 1) as apg,
    ROUND(AVG(gt.rebounds), 1) as rpg,
    ROUND(AVG(gt.steals), 1) as spg,
    ROUND(AVG(gt.blocks), 1) as bpg,
    ROUND(AVG(gt.turnovers), 1) as topg,
    ROUND(AVG(gt.minutes), 1) as mpg
FROM players p
LEFT JOIN game_totals gt ON p.player_id = gt.player_id
GROUP BY p.name, p.player_id;

-- Create team_stats view for overall team averages
CREATE VIEW team_stats AS
WITH game_totals AS (
    SELECT 
        e.team_id,
        e.game_id,
        SUM(CASE WHEN stat_type = 'point' THEN stat_value ELSE 0 END) as points,
        SUM(CASE WHEN stat_type = 'assist' THEN stat_value ELSE 0 END) as assists,
        SUM(CASE WHEN stat_type = 'rebound' THEN stat_value ELSE 0 END) as rebounds,
        SUM(CASE WHEN stat_type = 'steal' THEN stat_value ELSE 0 END) as steals,
        SUM(CASE WHEN stat_type = 'block' THEN stat_value ELSE 0 END) as blocks,
        SUM(CASE WHEN stat_type = 'turnover' THEN stat_value ELSE 0 END) as turnovers,
        SUM(CASE WHEN stat_type = 'minutes_played' THEN stat_value ELSE 0 END) as minutes
    FROM player_stat_events e
    GROUP BY e.team_id, e.game_id
)
SELECT 
    t.name,
    t.team_id,
    COUNT(DISTINCT gt.game_id) as games_played,
    ROUND(AVG(gt.points), 1) as ppg,
    ROUND(AVG(gt.assists), 1) as apg,
    ROUND(AVG(gt.rebounds), 1) as rpg,
    ROUND(AVG(gt.steals), 1) as spg,
    ROUND(AVG(gt.blocks), 1) as bpg,
    ROUND(AVG(gt.turnovers), 1) as topg,
    ROUND(AVG(gt.minutes), 1) as mpg
FROM teams t
LEFT JOIN game_totals gt ON t.team_id = gt.team_id
GROUP BY t.name, t.team_id;

-- Create indexes for performance
CREATE INDEX idx_player_stats_game ON player_stat_events (player_id, game_id, stat_type);
CREATE INDEX idx_game_state ON games (game_id, state, season_id);
CREATE INDEX idx_team_stats_game ON player_stat_events (team_id, game_id, stat_type);

-- Create view for live team stats (current/in-progress games)
CREATE VIEW team_live_stats AS
WITH live_stats AS (
    SELECT 
        e.team_id,
        e.game_id,
        g.season_id,
        SUM(CASE WHEN LOWER(e.stat_type) = 'point' THEN e.stat_value ELSE 0 END) AS points,
        SUM(CASE WHEN LOWER(e.stat_type) = 'assist' THEN e.stat_value ELSE 0 END) AS assists,
        SUM(CASE WHEN LOWER(e.stat_type) = 'rebound' THEN e.stat_value ELSE 0 END) AS rebounds,
        SUM(CASE WHEN LOWER(e.stat_type) = 'steal' THEN e.stat_value ELSE 0 END) AS steals,
        SUM(CASE WHEN LOWER(e.stat_type) = 'block' THEN e.stat_value ELSE 0 END) AS blocks,
        SUM(CASE WHEN LOWER(e.stat_type) = 'turnover' THEN e.stat_value ELSE 0 END) AS turnovers,
        SUM(CASE WHEN LOWER(e.stat_type) = 'minutes_played' THEN e.stat_value ELSE 0 END) AS minutes_played,
        MAX(e.created_at) as last_event_time
    FROM player_stat_events e
    JOIN games g ON e.game_id = g.game_id
    WHERE g.state = 'IN_PROGRESS'
    GROUP BY e.team_id, e.game_id, g.season_id
)
SELECT 
    ls.team_id,
    ls.season_id,
    t.name as team_name,
    t.league_id,
    l.name as league_name,
    ls.points AS ppg,
    ls.assists AS apg,
    ls.rebounds AS rpg,
    ls.steals AS spg,
    ls.blocks AS bpg,
    ls.turnovers AS topg,
    ls.minutes_played AS mpg,
    1 AS games_played,
    ls.last_event_time as last_updated
FROM live_stats ls
JOIN games g ON ls.game_id = g.game_id
JOIN teams t ON ls.team_id = t.team_id
JOIN leagues l ON t.league_id = l.league_id;

-- Create view for combined team stats (both completed and live games)
CREATE VIEW team_combined_stats AS
WITH combined_stats AS (
    -- Get completed game stats
    SELECT 
        team_id,
        season_id,
        team_name,
        league_id,
        league_name,
        ppg * games_played as total_points,
        apg * games_played as total_assists,
        rpg * games_played as total_rebounds,
        spg * games_played as total_steals,
        bpg * games_played as total_blocks,
        topg * games_played as total_turnovers,
        mpg * games_played as total_minutes,
        games_played,
        last_updated
    FROM team_session_avg
    
    UNION ALL
    
    -- Get live game stats
    SELECT 
        team_id,
        season_id,
        team_name,
        league_id,
        league_name,
        ppg as total_points,
        apg as total_assists,
        rpg as total_rebounds,
        spg as total_steals,
        bpg as total_blocks,
        topg as total_turnovers,
        mpg as total_minutes,
        games_played,
        last_updated
    FROM team_live_stats
)
SELECT 
    team_id,
    season_id,
    team_name,
    league_id,
    league_name,
    SUM(total_points) / SUM(games_played) as ppg,
    SUM(total_assists) / SUM(games_played) as apg,
    SUM(total_rebounds) / SUM(games_played) as rpg,
    SUM(total_steals) / SUM(games_played) as spg,
    SUM(total_blocks) / SUM(games_played) as bpg,
    SUM(total_turnovers) / SUM(games_played) as topg,
    SUM(total_minutes) / SUM(games_played) as mpg,
    SUM(games_played) as games_played,
    MAX(last_updated) as last_updated
FROM combined_stats
GROUP BY team_id, season_id, team_name, league_id, league_name;

-- Create trigger to refresh views when stats are updated
CREATE TRIGGER refresh_stats_trigger
    AFTER INSERT OR UPDATE OR DELETE ON player_stat_events
    FOR EACH STATEMENT
    EXECUTE FUNCTION refresh_stat_views();

-- Create trigger to refresh views when game state changes
CREATE TRIGGER refresh_stats_on_game_state_change
    AFTER UPDATE OF state ON games
    FOR EACH ROW
    WHEN (OLD.state != NEW.state)
    EXECUTE FUNCTION refresh_stat_views();

-- Create indexes for the underlying tables to help view performance
CREATE INDEX idx_stat_player_season ON player_stat_events (player_id, game_id);
CREATE INDEX idx_stat_team_season ON player_stat_events (team_id, game_id);
CREATE INDEX idx_game_season ON games (game_id, season_id);

-- Create continuous aggregate for player stats
CREATE MATERIALIZED VIEW player_stats_cagg
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 hour', timestamp) as bucket,
    player_id,
    game_id,
    stat_type,
    SUM(stat_value) as total_value,
    COUNT(*) as event_count
FROM player_stat_events
GROUP BY bucket, player_id, game_id, stat_type
WITH NO DATA;

-- Create continuous aggregate for team stats
CREATE MATERIALIZED VIEW team_stats_cagg
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 hour', timestamp) as bucket,
    team_id,
    game_id,
    stat_type,
    SUM(stat_value) as total_value,
    COUNT(*) as event_count
FROM player_stat_events
GROUP BY bucket, team_id, game_id, stat_type
WITH NO DATA;

-- Create indexes
CREATE INDEX idx_game_state ON games (state, game_id);
CREATE INDEX idx_game_season_state ON games (season_id, state);
CREATE INDEX idx_game_team ON games (home_team_id, away_team_id);

-- Composite indexes for stat queries
CREATE INDEX idx_player_stats_game ON player_stat_events (player_id, game_id, stat_type);
CREATE INDEX idx_team_stats_game ON player_stat_events (team_id, game_id, stat_type);
CREATE INDEX idx_stats_timestamp ON player_stat_events (timestamp, game_id);

-- Partial indexes for live games (smaller, faster)
CREATE INDEX idx_live_game_stats ON player_stat_events (game_id, stat_type, stat_value) 
WHERE game_id IN (SELECT game_id FROM games WHERE state = 'IN_PROGRESS');

CREATE INDEX idx_live_game_events ON player_stat_events (game_id, player_id, stat_type) 
WHERE game_id IN (SELECT game_id FROM games WHERE state = 'IN_PROGRESS');

-- Explain index benefits
COMMENT ON INDEX idx_game_state IS 'Fast filtering of live vs completed games';
COMMENT ON INDEX idx_game_season_state IS 'Efficient season-based queries with game state';
COMMENT ON INDEX idx_game_team IS 'Quick team game lookups';
COMMENT ON INDEX idx_player_stats_game IS 'Optimizes player game stat retrieval';
COMMENT ON INDEX idx_team_stats_game IS 'Optimizes team game stat retrieval';
COMMENT ON INDEX idx_stats_timestamp IS 'Helps TimescaleDB chunk navigation';
COMMENT ON INDEX idx_live_game_stats IS 'Small index only for active games';
COMMENT ON INDEX idx_live_game_events IS 'Fast live game stat queries';

-- Create player_stats view for overall player averages
CREATE VIEW player_stats AS
WITH game_totals AS (
    SELECT 
        e.player_id,
        e.game_id,
        SUM(CASE WHEN stat_type = 'point' THEN stat_value ELSE 0 END) as points,
        SUM(CASE WHEN stat_type = 'assist' THEN stat_value ELSE 0 END) as assists,
        SUM(CASE WHEN stat_type = 'rebound' THEN stat_value ELSE 0 END) as rebounds,
        SUM(CASE WHEN stat_type = 'steal' THEN stat_value ELSE 0 END) as steals,
        SUM(CASE WHEN stat_type = 'block' THEN stat_value ELSE 0 END) as blocks,
        SUM(CASE WHEN stat_type = 'turnover' THEN stat_value ELSE 0 END) as turnovers,
        SUM(CASE WHEN stat_type = 'minutes_played' THEN stat_value ELSE 0 END) as minutes
    FROM player_stat_events e
    GROUP BY e.player_id, e.game_id
)
SELECT 
    p.name,
    p.player_id,
    COUNT(DISTINCT gt.game_id) as games_played,
    ROUND(AVG(gt.points), 1) as ppg,
    ROUND(AVG(gt.assists), 1) as apg,
    ROUND(AVG(gt.rebounds), 1) as rpg,
    ROUND(AVG(gt.steals), 1) as spg,
    ROUND(AVG(gt.blocks), 1) as bpg,
    ROUND(AVG(gt.turnovers), 1) as topg,
    ROUND(AVG(gt.minutes), 1) as mpg
FROM players p
LEFT JOIN game_totals gt ON p.player_id = gt.player_id
GROUP BY p.name, p.player_id;

-- Create team_stats view for overall team averages
CREATE VIEW team_stats AS
WITH game_totals AS (
    SELECT 
        e.team_id,
        e.game_id,
        SUM(CASE WHEN stat_type = 'point' THEN stat_value ELSE 0 END) as points,
        SUM(CASE WHEN stat_type = 'assist' THEN stat_value ELSE 0 END) as assists,
        SUM(CASE WHEN stat_type = 'rebound' THEN stat_value ELSE 0 END) as rebounds,
        SUM(CASE WHEN stat_type = 'steal' THEN stat_value ELSE 0 END) as steals,
        SUM(CASE WHEN stat_type = 'block' THEN stat_value ELSE 0 END) as blocks,
        SUM(CASE WHEN stat_type = 'turnover' THEN stat_value ELSE 0 END) as turnovers,
        SUM(CASE WHEN stat_type = 'minutes_played' THEN stat_value ELSE 0 END) as minutes
    FROM player_stat_events e
    GROUP BY e.team_id, e.game_id
)
SELECT 
    t.name,
    t.team_id,
    COUNT(DISTINCT gt.game_id) as games_played,
    ROUND(AVG(gt.points), 1) as ppg,
    ROUND(AVG(gt.assists), 1) as apg,
    ROUND(AVG(gt.rebounds), 1) as rpg,
    ROUND(AVG(gt.steals), 1) as spg,
    ROUND(AVG(gt.blocks), 1) as bpg,
    ROUND(AVG(gt.turnovers), 1) as topg,
    ROUND(AVG(gt.minutes), 1) as mpg
FROM teams t
LEFT JOIN game_totals gt ON t.team_id = gt.team_id
GROUP BY t.name, t.team_id;

-- Create indexes for performance
CREATE INDEX idx_player_stats_game ON player_stat_events (player_id, game_id, stat_type);
CREATE INDEX idx_game_state ON games (game_id, state, season_id);
CREATE INDEX idx_team_stats_game ON player_stat_events (team_id, game_id, stat_type);

-- Create view for live team stats (current/in-progress games)
CREATE VIEW team_live_stats AS
WITH live_stats AS (
    SELECT 
        e.team_id,
        e.game_id,
        g.season_id,
        SUM(CASE WHEN LOWER(e.stat_type) = 'point' THEN e.stat_value ELSE 0 END) AS points,
        SUM(CASE WHEN LOWER(e.stat_type) = 'assist' THEN e.stat_value ELSE 0 END) AS assists,
        SUM(CASE WHEN LOWER(e.stat_type) = 'rebound' THEN e.stat_value ELSE 0 END) AS rebounds,
        SUM(CASE WHEN LOWER(e.stat_type) = 'steal' THEN e.stat_value ELSE 0 END) AS steals,
        SUM(CASE WHEN LOWER(e.stat_type) = 'block' THEN e.stat_value ELSE 0 END) AS blocks,
        SUM(CASE WHEN LOWER(e.stat_type) = 'turnover' THEN e.stat_value ELSE 0 END) AS turnovers,
        SUM(CASE WHEN LOWER(e.stat_type) = 'minutes_played' THEN e.stat_value ELSE 0 END) AS minutes_played,
        MAX(e.created_at) as last_event_time
    FROM player_stat_events e
    JOIN games g ON e.game_id = g.game_id
    WHERE g.state = 'IN_PROGRESS'
    GROUP BY e.team_id, e.game_id, g.season_id
)
SELECT 
    ls.team_id,
    ls.season_id,
    t.name as team_name,
    t.league_id,
    l.name as league_name,
    ls.points AS ppg,
    ls.assists AS apg,
    ls.rebounds AS rpg,
    ls.steals AS spg,
    ls.blocks AS bpg,
    ls.turnovers AS topg,
    ls.minutes_played AS mpg,
    1 AS games_played,
    ls.last_event_time as last_updated
FROM live_stats ls
JOIN games g ON ls.game_id = g.game_id
JOIN teams t ON ls.team_id = t.team_id
JOIN leagues l ON t.league_id = l.league_id;

-- Create view for combined team stats (both completed and live games)
CREATE VIEW team_combined_stats AS
WITH combined_stats AS (
    -- Get completed game stats
    SELECT 
        team_id,
        season_id,
        team_name,
        league_id,
        league_name,
        ppg * games_played as total_points,
        apg * games_played as total_assists,
        rpg * games_played as total_rebounds,
        spg * games_played as total_steals,
        bpg * games_played as total_blocks,
        topg * games_played as total_turnovers,
        mpg * games_played as total_minutes,
        games_played,
        last_updated
    FROM team_session_avg
    
    UNION ALL
    
    -- Get live game stats
    SELECT 
        team_id,
        season_id,
        team_name,
        league_id,
        league_name,
        ppg as total_points,
        apg as total_assists,
        rpg as total_rebounds,
        spg as total_steals,
        bpg as total_blocks,
        topg as total_turnovers,
        mpg as total_minutes,
        games_played,
        last_updated
    FROM team_live_stats
)
SELECT 
    team_id,
    season_id,
    team_name,
    league_id,
    league_name,
    SUM(total_points) / SUM(games_played) as ppg,
    SUM(total_assists) / SUM(games_played) as apg,
    SUM(total_rebounds) / SUM(games_played) as rpg,
    SUM(total_steals) / SUM(games_played) as spg,
    SUM(total_blocks) / SUM(games_played) as bpg,
    SUM(total_turnovers) / SUM(games_played) as topg,
    SUM(total_minutes) / SUM(games_played) as mpg,
    SUM(games_played) as games_played,
    MAX(last_updated) as last_updated
FROM combined_stats
GROUP BY team_id, season_id, team_name, league_id, league_name;

-- Create trigger to refresh views when stats are updated
CREATE TRIGGER refresh_stats_trigger
    AFTER INSERT OR UPDATE OR DELETE ON player_stat_events
    FOR EACH STATEMENT
    EXECUTE FUNCTION refresh_stat_views();

-- Create trigger to refresh views when game state changes
CREATE TRIGGER refresh_stats_on_game_state_change
    AFTER UPDATE OF state ON games
    FOR EACH ROW
    WHEN (OLD.state != NEW.state)
    EXECUTE FUNCTION refresh_stat_views();

-- Create indexes for the underlying tables to help view performance
CREATE INDEX idx_stat_player_season ON player_stat_events (player_id, game_id);
CREATE INDEX idx_stat_team_season ON player_stat_events (team_id, game_id);
CREATE INDEX idx_game_season ON games (game_id, season_id);

-- Create continuous aggregate for player stats
CREATE MATERIALIZED VIEW player_stats_cagg
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 hour', timestamp) as bucket,
    player_id,
    game_id,
    stat_type,
    SUM(stat_value) as total_value,
    COUNT(*) as event_count
FROM player_stat_events
GROUP BY bucket, player_id, game_id, stat_type
WITH NO DATA;

-- Create continuous aggregate for team stats
CREATE MATERIALIZED VIEW team_stats_cagg
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 hour', timestamp) as bucket,
    team_id,
    game_id,
    stat_type,
    SUM(stat_value) as total_value,
    COUNT(*) as event_count
FROM player_stat_events
GROUP BY bucket, team_id, game_id, stat_type
WITH NO DATA;

-- Create indexes
CREATE INDEX idx_game_state ON games (state, game_id);
CREATE INDEX idx_game_season_state ON games (season_id, state);
CREATE INDEX idx_game_team ON games (home_team_id, away_team_id);

-- Composite indexes for stat queries
CREATE INDEX idx_player_stats_game ON player_stat_events (player_id, game_id, stat_type);
CREATE INDEX idx_team_stats_game ON player_stat_events (team_id, game_id, stat_type);
CREATE INDEX idx_stats_timestamp ON player_stat_events (timestamp, game_id);

-- Partial indexes for live games (smaller, faster)
CREATE INDEX idx_live_game_stats ON player_stat_events (game_id, stat_type, stat_value) 
WHERE game_id IN (SELECT game_id FROM games WHERE state = 'IN_PROGRESS');

CREATE INDEX idx_live_game_events ON player_stat_events (game_id, player_id, stat_type) 
WHERE game_id IN (SELECT game_id FROM games WHERE state = 'IN_PROGRESS');

-- Explain index benefits
COMMENT ON INDEX idx_game_state IS 'Fast filtering of live vs completed games';
COMMENT ON INDEX idx_game_season_state IS 'Efficient season-based queries with game state';
COMMENT ON INDEX idx_game_team IS 'Quick team game lookups';
COMMENT ON INDEX idx_player_stats_game IS 'Optimizes player game stat retrieval';
COMMENT ON INDEX idx_team_stats_game IS 'Optimizes team game stat retrieval';
COMMENT ON INDEX idx_stats_timestamp IS 'Helps TimescaleDB chunk navigation';
COMMENT ON INDEX idx_live_game_stats IS 'Small index only for active games';
COMMENT ON INDEX idx_live_game_events IS 'Fast live game stat queries';

-- Create player_stats view for overall player averages
CREATE VIEW player_stats AS
WITH game_totals AS (
    SELECT 
        e.player_id,
        e.game_id,
        SUM(CASE WHEN stat_type = 'point' THEN stat_value ELSE 0 END) as points,
        SUM(CASE WHEN stat_type = 'assist' THEN stat_value ELSE 0 END) as assists,
        SUM(CASE WHEN stat_type = 'rebound' THEN stat_value ELSE 0 END) as rebounds,
        SUM(CASE WHEN stat_type = 'steal' THEN stat_value ELSE 0 END) as steals,
        SUM(CASE WHEN stat_type = 'block' THEN stat_value ELSE 0 END) as blocks,
        SUM(CASE WHEN stat_type = 'turnover' THEN stat_value ELSE 0 END) as turnovers,
        SUM(CASE WHEN stat_type = 'minutes_played' THEN stat_value ELSE 0 END) as minutes
    FROM player_stat_events e
    GROUP BY e.player_id, e.game_id
)
SELECT 
    p.name,
    p.player_id,
    COUNT(DISTINCT gt.game_id) as games_played,
    ROUND(AVG(gt.points), 1) as ppg,
    ROUND(AVG(gt.assists), 1) as apg,
    ROUND(AVG(gt.rebounds), 1) as rpg,
    ROUND(AVG(gt.steals), 1) as spg,
    ROUND(AVG(gt.blocks), 1) as bpg,
    ROUND(AVG(gt.turnovers), 1) as topg,
    ROUND(AVG(gt.minutes), 1) as mpg
FROM players p
LEFT JOIN game_totals gt ON p.player_id = gt.player_id
GROUP BY p.name, p.player_id;

-- Create team_stats view for overall team averages
CREATE VIEW team_stats AS
WITH game_totals AS (
    SELECT 
        e.team_id,
        e.game_id,
        SUM(CASE WHEN stat_type = 'point' THEN stat_value ELSE 0 END) as points,
        SUM(CASE WHEN stat_type = 'assist' THEN stat_value ELSE 0 END) as assists,
        SUM(CASE WHEN stat_type = 'rebound' THEN stat_value ELSE 0 END) as rebounds,
        SUM(CASE WHEN stat_type = 'steal' THEN stat_value ELSE 0 END) as steals,
        SUM(CASE WHEN stat_type = 'block' THEN stat_value ELSE 0 END) as blocks,
        SUM(CASE WHEN stat_type = 'turnover' THEN stat_value ELSE 0 END) as turnovers,
        SUM(CASE WHEN stat_type = 'minutes_played' THEN stat_value ELSE 0 END) as minutes
    FROM player_stat_events e
    GROUP BY e.team_id, e.game_id
)
SELECT 
    t.name,
    t.team_id,
    COUNT(DISTINCT gt.game_id) as games_played,
    ROUND(AVG(gt.points), 1) as ppg,
    ROUND(AVG(gt.assists), 1) as apg,
    ROUND(AVG(gt.rebounds), 1) as rpg,
    ROUND(AVG(gt.steals), 1) as spg,
    ROUND(AVG(gt.blocks), 1) as bpg,
    ROUND(AVG(gt.turnovers), 1) as topg,
    ROUND(AVG(gt.minutes), 1) as mpg
FROM teams t
LEFT JOIN game_totals gt ON t.team_id = gt.team_id
GROUP BY t.name, t.team_id;

-- Create indexes for performance
CREATE INDEX idx_player_stats_game ON player_stat_events (player_id, game_id, stat_type);
CREATE INDEX idx_game_state ON games (game_id, state, season_id);
CREATE INDEX idx_team_stats_game ON player_stat_events (team_id, game_id, stat_type);

-- Create view for live team stats (current/in-progress games)
CREATE VIEW team_live_stats AS
WITH live_stats AS (
    SELECT 
        e.team_id,
        e.game_id,
        g.season_id,
        SUM(CASE WHEN LOWER(e.stat_type) = 'point' THEN e.stat_value ELSE 0 END) AS points,
        SUM(CASE WHEN LOWER(e.stat_type) = 'assist' THEN e.stat_value ELSE 0 END) AS assists,
        SUM(CASE WHEN LOWER(e.stat_type) = 'rebound' THEN e.stat_value ELSE 0 END) AS rebounds,
        SUM(CASE WHEN LOWER(e.stat_type) = 'steal' THEN e.stat_value ELSE 0 END) AS steals,
        SUM(CASE WHEN LOWER(e.stat_type) = 'block' THEN e.stat_value ELSE 0 END) AS blocks,
        SUM(CASE WHEN LOWER(e.stat_type) = 'turnover' THEN e.stat_value ELSE 0 END) AS turnovers,
        SUM(CASE WHEN LOWER(e.stat_type) = 'minutes_played' THEN e.stat_value ELSE 0 END) AS minutes_played,
        MAX(e.created_at) as last_event_time
    FROM player_stat_events e
    JOIN games g ON e.game_id = g.game_id
    WHERE g.state = 'IN_PROGRESS'
    GROUP BY e.team_id, e.game_id, g.season_id
)
SELECT 
    ls.team_id,
    ls.season_id,
    t.name as team_name,
    t.league_id,
    l.name as league_name,
    ls.points AS ppg,
    ls.assists AS apg,
    ls.rebounds AS rpg,
    ls.steals AS spg,
    ls.blocks AS bpg,
    ls.turnovers AS topg,
    ls.minutes_played AS mpg,
    1 AS games_played,
    ls.last_event_time as last_updated
FROM live_stats ls
JOIN games g ON ls.game_id = g.game_id
JOIN teams t ON ls.team_id = t.team_id
JOIN leagues l ON t.league_id = l.league_id;

-- Create view for combined team stats (both completed and live games)
CREATE VIEW team_combined_stats AS
WITH combined_stats AS (
    -- Get completed game stats
    SELECT 
        team_id,
        season_id,
        team_name,
        league_id,
        league_name,
        ppg * games_played as total_points,
        apg * games_played as total_assists,
        rpg * games_played as total_rebounds,
        spg * games_played as total_steals,
        bpg * games_played as total_blocks,
        topg * games_played as total_turnovers,
        mpg * games_played as total_minutes,
        games_played,
        last_updated
    FROM team_session_avg
    
    UNION ALL
    
    -- Get live game stats
    SELECT 
        team_id,
        season_id,
        team_name,
        league_id,
        league_name,
        ppg as total_points,
        apg as total_assists,
        rpg as total_rebounds,
        spg as total_steals,
        bpg as total_blocks,
        topg as total_turnovers,
        mpg as total_minutes,
        games_played,
        last_updated
    FROM team_live_stats
)
SELECT 
    team_id,
    season_id,
    team_name,
    league_id,
    league_name,
    SUM(total_points) / SUM(games_played) as ppg,
    SUM(total_assists) / SUM(games_played) as apg,
    SUM(total_rebounds) / SUM(games_played) as rpg,
    SUM(total_steals) / SUM(games_played) as spg,
    SUM(total_blocks) / SUM(games_played) as bpg,
    SUM(total_turnovers) / SUM(games_played) as topg,
    SUM(total_minutes) / SUM(games_played) as mpg,
    SUM(games_played) as games_played,
    MAX(last_updated) as last_updated
FROM combined_stats
GROUP BY team_id, season_id, team_name, league_id, league_name;

-- Create trigger to refresh views when stats are updated
CREATE TRIGGER refresh_stats_trigger
    AFTER INSERT OR UPDATE OR DELETE ON player_stat_events
    FOR EACH STATEMENT
    EXECUTE FUNCTION refresh_stat_views();

-- Create trigger to refresh views when game state changes
CREATE TRIGGER refresh_stats_on_game_state_change
    AFTER UPDATE OF state ON games
    FOR EACH ROW
    WHEN (OLD.state != NEW.state)
    EXECUTE FUNCTION refresh_stat_views();

-- Create indexes for the underlying tables to help view performance
CREATE INDEX idx_stat_player_season ON player_stat_events (player_id, game_id);
CREATE INDEX idx_stat_team_season ON player_stat_events (team_id, game_id);
CREATE INDEX idx_game_season ON games (game_id, season_id);

-- Create continuous aggregate for player stats
CREATE MATERIALIZED VIEW player_stats_cagg
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 hour', timestamp) as bucket,
    player_id,
    game_id,
    stat_type,
    SUM(stat_value) as total_value,
    COUNT(*) as event_count
FROM player_stat_events
GROUP BY bucket, player_id, game_id, stat_type
WITH NO DATA;

-- Create continuous aggregate for team stats
CREATE MATERIALIZED VIEW team_stats_cagg
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 hour', timestamp) as bucket,
    team_id,
    game_id,
    stat_type,
    SUM(stat_value) as total_value,
    COUNT(*) as event_count
FROM player_stat_events
GROUP BY bucket, team_id, game_id, stat_type
WITH NO DATA;

-- Create indexes
CREATE INDEX idx_game_state ON games (state, game_id);
CREATE INDEX idx_game_season_state ON games (season_id, state);
CREATE INDEX idx_game_team ON games (home_team_id, away_team_id);

-- Composite indexes for stat queries
CREATE INDEX idx_player_stats_game ON player_stat_events (player_id, game_id, stat_type);
CREATE INDEX idx_team_stats_game ON player_stat_events (team_id, game_id, stat_type);
CREATE INDEX idx_stats_timestamp ON player_stat_events (timestamp, game_id);

-- Partial indexes for live games (smaller, faster)
CREATE INDEX idx_live_game_stats ON player_stat_events (game_id, stat_type, stat_value) 
WHERE game_id IN (SELECT game_id FROM games WHERE state = 'IN_PROGRESS');

CREATE INDEX idx_live_game_events ON player_stat_events (game_id, player_id, stat_type) 
WHERE game_id IN (SELECT game_id FROM games WHERE state = 'IN_PROGRESS');

-- Explain index benefits
COMMENT ON INDEX idx_game_state IS 'Fast filtering of live vs completed games';
COMMENT ON INDEX idx_game_season_state IS 'Efficient season-based queries with game state';
COMMENT ON INDEX idx_game_team IS 'Quick team game lookups';
COMMENT ON INDEX idx_player_stats_game IS 'Optimizes player game stat retrieval';
COMMENT ON INDEX idx_team_stats_game IS 'Optimizes team game stat retrieval';
COMMENT ON INDEX idx_stats_timestamp IS 'Helps TimescaleDB chunk navigation';
COMMENT ON INDEX idx_live_game_stats IS 'Small index only for active games';
COMMENT ON INDEX idx_live_game_events IS 'Fast live game stat queries';

-- Create player_stats view for overall player averages
CREATE VIEW player_stats AS
WITH game_totals AS (
    SELECT 
        e.player_id,
        e.game_id,
        SUM(CASE WHEN stat_type = 'point' THEN stat_value ELSE 0 END) as points,
        SUM(CASE WHEN stat_type = 'assist' THEN stat_value ELSE 0 END) as assists,
        SUM(CASE WHEN stat_type = 'rebound' THEN stat_value ELSE 0 END) as rebounds,
        SUM(CASE WHEN stat_type = 'steal' THEN stat_value ELSE 0 END) as steals,
        SUM(CASE WHEN stat_type = 'block' THEN stat_value ELSE 0 END) as blocks,
        SUM(CASE WHEN stat_type = 'turnover' THEN stat_value ELSE 0 END) as turnovers,
        SUM(CASE WHEN stat_type = 'minutes_played' THEN stat_value ELSE 0 END) as minutes
    FROM player_stat_events e
    GROUP BY e.player_id, e.game_id
)
SELECT 
    p.name,
    p.player_id,
    COUNT(DISTINCT gt.game_id) as games_played,
    ROUND(AVG(gt.points), 1) as ppg,
    ROUND(AVG(gt.assists), 1) as apg,
    ROUND(AVG(gt.rebounds), 1) as rpg,
    ROUND(AVG(gt.steals), 1) as spg,
    ROUND(AVG(gt.blocks), 1) as bpg,
    ROUND(AVG(gt.turnovers), 1) as topg,
    ROUND(AVG(gt.minutes), 1) as mpg
FROM players p
LEFT JOIN game_totals gt ON p.player_id = gt.player_id
GROUP BY p.name, p.player_id;

-- Create team_stats view for overall team averages
CREATE VIEW team_stats AS
WITH game_totals AS (
    SELECT 
        e.team_id,
        e.game_id,
        SUM(CASE WHEN stat_type = 'point' THEN stat_value ELSE 0 END) as points,
        SUM(CASE WHEN stat_type = 'assist' THEN stat_value ELSE 0 END) as assists,
        SUM(CASE WHEN stat_type = 'rebound' THEN stat_value ELSE 0 END) as rebounds,
        SUM(CASE WHEN stat_type = 'steal' THEN stat_value ELSE 0 END) as steals,
        SUM(CASE WHEN stat_type = 'block' THEN stat_value ELSE 0 END) as blocks,
        SUM(CASE WHEN stat_type = 'turnover' THEN stat_value ELSE 0 END) as turnovers,
        SUM(CASE WHEN stat_type = 'minutes_played' THEN stat_value ELSE 0 END) as minutes
    FROM player_stat_events e
    GROUP BY e.team_id, e.game_id
)
SELECT 
    t.name,
    t.team_id,
    COUNT(DISTINCT gt.game_id) as games_played,
    ROUND(AVG(gt.points), 1) as ppg,
    ROUND(AVG(gt.assists), 1) as apg,
    ROUND(AVG(gt.rebounds), 1) as rpg,
    ROUND(AVG(gt.steals), 1) as spg,
    ROUND(AVG(gt.blocks), 1) as bpg,
    ROUND(AVG(gt.turnovers), 1) as topg,
    ROUND(AVG(gt.minutes), 1) as mpg
FROM teams t
LEFT JOIN game_totals gt ON t.team_id = gt.team_id
GROUP BY t.name, t.team_id;

-- Create indexes for performance
CREATE INDEX idx_player_stats_game ON player_stat_events (player_id, game_id, stat_type);
CREATE INDEX idx_game_state ON games (game_id, state, season_id);
CREATE INDEX idx_team_stats_game ON player_stat_events (team_id, game_id, stat_type);

-- Create view for live team stats (current/in-progress games)
CREATE VIEW team_live_stats AS
WITH live_stats AS (
    SELECT 
        e.team_id,
        e.game_id,
        g.season_id,
        SUM(CASE WHEN LOWER(e.stat_type) = 'point' THEN e.stat_value ELSE 0 END) AS points,
        SUM(CASE WHEN LOWER(e.stat_type) = 'assist' THEN e.stat_value ELSE 0 END) AS assists,
        SUM(CASE WHEN LOWER(e.stat_type) = 'rebound' THEN e.stat_value ELSE 0 END) AS rebounds,
        SUM(CASE WHEN LOWER(e.stat_type) = 'steal' THEN e.stat_value ELSE 0 END) AS steals,
        SUM(CASE WHEN LOWER(e.stat_type) = 'block' THEN e.stat_value ELSE 0 END) AS blocks,
        SUM(CASE WHEN LOWER(e.stat_type) = 'turnover' THEN e.stat_value ELSE 0 END) AS turnovers,
        SUM(CASE WHEN LOWER(e.stat_type) = 'minutes_played' THEN e.stat_value ELSE 0 END) AS minutes_played,
        MAX(e.created_at) as last_event_time
    FROM player_stat_events e
    JOIN games g ON e.game_id = g.game_id
    WHERE g.state = 'IN_PROGRESS'
    GROUP BY e.team_id, e.game_id, g.season_id
)
SELECT 
    ls.team_id,
    ls.season_id,
    t.name as team_name,
    t.league_id,
    l.name as league_name,
    ls.points AS ppg,
    ls.assists AS apg,
    ls.rebounds AS rpg,
    ls.steals AS spg,
    ls.blocks AS bpg,
    ls.turnovers AS topg,
    ls.minutes_played AS mpg,
    1 AS games_played,
    ls.last_event_time as last_updated
FROM live_stats ls
JOIN games g ON ls.game_id = g.game_id
JOIN teams t ON ls.team_id = t.team_id
JOIN leagues l ON t.league_id = l.league_id;

-- Create view for combined team stats (both completed and live games)
CREATE VIEW team_combined_stats AS
WITH combined_stats AS (
    -- Get completed game stats
    SELECT 
        team_id,
        season_id,
        team_name,
        league_id,
        league_name,
        ppg * games_played as total_points,
        apg * games_played as total_assists,
        rpg * games_played as total_rebounds,
        spg * games_played as total_steals,
        bpg * games_played as total_blocks,
        topg * games_played as total_turnovers,
        mpg * games_played as total_minutes,
        games_played,
        last_updated
    FROM team_session_avg
    
    UNION ALL
    
    -- Get live game stats
    SELECT 
        team_id,
        season_id,
        team_name,
        league_id,
        league_name,
        ppg as total_points,
        apg as total_assists,
        rpg as total_rebounds,
        spg as total_steals,
        bpg as total_blocks,
        topg as total_turnovers,
        mpg as total_minutes,
        games_played,
        last_updated
    FROM team_live_stats
)
SELECT 
    team_id,
    season_id,
    team_name,
    league_id,
    league_name,
    SUM(total_points) / SUM(games_played) as ppg,
    SUM(total_assists) / SUM(games_played) as apg,
    SUM(total_rebounds) / SUM(games_played) as rpg,
    SUM(total_steals) / SUM(games_played) as spg,
    SUM(total_blocks) / SUM(games_played) as bpg,
    SUM(total_turnovers) / SUM(games_played) as topg,
    SUM(total_minutes) / SUM(games_played) as mpg,
    SUM(games_played) as games_played,
    MAX(last_updated) as last_updated
FROM combined_stats
GROUP BY team_id, season_id, team_name, league_id, league_name;

-- Create trigger to refresh views when stats are updated
CREATE TRIGGER refresh_stats_trigger
    AFTER INSERT OR UPDATE OR DELETE ON player_stat_events
    FOR EACH STATEMENT
    EXECUTE FUNCTION refresh_stat_views();

-- Create trigger to refresh views when game state changes
CREATE TRIGGER refresh_stats_on_game_state_change
    AFTER UPDATE OF state ON games
    FOR EACH ROW
    WHEN (OLD.state != NEW.state)
    EXECUTE FUNCTION refresh_stat_views();

-- Create indexes for the underlying tables to help view performance
CREATE INDEX idx_stat_player_season ON player_stat_events (player_id, game_id);
CREATE INDEX idx_stat_team_season ON player_stat_events (team_id, game_id);
CREATE INDEX idx_game_season ON games (game_id, season_id);

-- Create continuous aggregate for player stats
CREATE MATERIALIZED VIEW player_stats_cagg
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 hour', timestamp) as bucket,
    player_id,
    game_id,
    stat_type,
    SUM(stat_value) as total_value,
    COUNT(*) as event_count
FROM player_stat_events
GROUP BY bucket, player_id, game_id, stat_type
WITH NO DATA;

-- Create continuous aggregate for team stats
CREATE MATERIALIZED VIEW team_stats_cagg
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 hour', timestamp) as bucket,
    team_id,
    game_id,
    stat_type,
    SUM(stat_value) as total_value,
    COUNT(*) as event_count
FROM player_stat_events
GROUP BY bucket, team_id, game_id, stat_type
WITH NO DATA;

-- Create indexes
CREATE INDEX idx_game_state ON games (state, game_id);
CREATE INDEX idx_game_season_state ON games (season_id, state);
CREATE INDEX idx_game_team ON games (home_team_id, away_team_id);

-- Composite indexes for stat queries
CREATE INDEX idx_player_stats_game ON player_stat_events (player_id, game_id, stat_type);
CREATE INDEX idx_team_stats_game ON player_stat_events (team_id, game_id, stat_type);
CREATE INDEX idx_stats_timestamp ON player_stat_events (timestamp, game_id);

-- Partial indexes for live games (smaller, faster)
CREATE INDEX idx_live_game_stats ON player_stat_events (game_id, stat_type, stat_value) 
WHERE game_id IN (SELECT game_id FROM games WHERE state = 'IN_PROGRESS');

CREATE INDEX idx_live_game_events ON player_stat_events (game_id, player_id, stat_type) 
WHERE game_id IN (SELECT game_id FROM games WHERE state = 'IN_PROGRESS');

-- Explain index benefits
COMMENT ON INDEX idx_game_state IS 'Fast filtering of live vs completed games';
COMMENT ON INDEX idx_game_season_state IS 'Efficient season-based queries with game state';
COMMENT ON INDEX idx_game_team IS 'Quick team game lookups';
COMMENT ON INDEX idx_player_stats_game IS 'Optimizes player game stat retrieval';
COMMENT ON INDEX idx_team_stats_game IS 'Optimizes team game stat retrieval';
COMMENT ON INDEX idx_stats_timestamp IS 'Helps TimescaleDB chunk navigation';
COMMENT ON INDEX idx_live_game_stats IS 'Small index only for active games';
COMMENT ON INDEX idx_live_game_events IS 'Fast live game stat queries';

-- Create player_stats view for overall player averages
CREATE VIEW player_stats AS
WITH game_totals AS (
    SELECT 
        e.player_id,
        e.game_id,
        SUM(CASE WHEN stat_type = 'point' THEN stat_value ELSE 0 END) as points,
        SUM(CASE WHEN stat_type = 'assist' THEN stat_value ELSE 0 END) as assists,
        SUM(CASE WHEN stat_type = 'rebound' THEN stat_value ELSE 0 END) as rebounds,
        SUM(CASE WHEN stat_type = 'steal' THEN stat_value ELSE 0 END) as steals,
        SUM(CASE WHEN stat_type = 'block' THEN stat_value ELSE 0 END) as blocks,
        SUM(CASE WHEN stat_type = 'turnover' THEN stat_value ELSE 0 END) as turnovers,
        SUM(CASE WHEN stat_type = 'minutes_played' THEN stat_value ELSE 0 END) as minutes
    FROM player_stat_events e
    GROUP BY e.player_id, e.game_id
)
SELECT 
    p.name,
    p.player_id,
    COUNT(DISTINCT gt.game_id) as games_played,
    ROUND(AVG(gt.points), 1) as ppg,
    ROUND(AVG(gt.assists), 1) as apg,
    ROUND(AVG(gt.rebounds), 1) as rpg,
    ROUND(AVG(gt.steals), 1) as spg,
    ROUND(AVG(gt.blocks), 1) as bpg,
    ROUND(AVG(gt.turnovers), 1) as topg,
    ROUND(AVG(gt.minutes), 1) as mpg
FROM players p
LEFT JOIN game_totals gt ON p.player_id = gt.player_id
GROUP BY p.name, p.player_id;

-- Create team_stats view for overall team averages
CREATE VIEW team_stats AS
WITH game_totals AS (
    SELECT 
        e.team_id,
        e.game_id,
        SUM(CASE WHEN stat_type = 'point' THEN stat_value ELSE 0 END) as points,
        SUM(CASE WHEN stat_type = 'assist' THEN stat_value ELSE 0 END) as assists,
        SUM(CASE WHEN stat_type = 'rebound' THEN stat_value ELSE 0 END) as rebounds,
        SUM(CASE WHEN stat_type = 'steal' THEN stat_value ELSE 0 END) as steals,
        SUM(CASE WHEN stat_type = 'block' THEN stat_value ELSE 0 END) as blocks,
        SUM(CASE WHEN stat_type = 'turnover' THEN stat_value ELSE 0 END) as turnovers,
        SUM(CASE WHEN stat_type = 'minutes_played' THEN stat_value ELSE 0 END) as minutes
    FROM player_stat_events e
    GROUP BY e.team_id, e.game_id
)
SELECT 
    t.name,
    t.team_id,
    COUNT(DISTINCT gt.game_id) as games_played,
    ROUND(AVG(gt.points), 1) as ppg,
    ROUND(AVG(gt.assists), 1) as apg,
    ROUND(AVG(gt.rebounds), 1) as rpg,
    ROUND(AVG(gt.steals), 1) as spg,
    ROUND(AVG(gt.blocks), 1) as bpg,
    ROUND(AVG(gt.turnovers), 1) as topg,
    ROUND(AVG(gt.minutes), 1) as mpg
FROM teams t
LEFT JOIN game_totals gt ON t.team_id = gt.team_id
GROUP BY t.name, t.team_id;

-- Create indexes for performance
CREATE INDEX idx_player_stats_game ON player_stat_events (player_id, game_id, stat_type);
CREATE INDEX idx_game_state ON games (game_id, state, season_id);
CREATE INDEX idx_team_stats_game ON player_stat_events (team_id, game_id, stat_type);

-- Create view for live team stats (current/in-progress games)
CREATE VIEW team_live_stats AS
WITH live_stats AS (
    SELECT 
        e.team_id,
        e.game_id,
        g.season_id,
        SUM(CASE WHEN LOWER(e.stat_type) = 'point' THEN e.stat_value ELSE 0 END) AS points,
        SUM(CASE WHEN LOWER(e.stat_type) = 'assist' THEN e.stat_value ELSE 0 END) AS assists,
        SUM(CASE WHEN LOWER(e.stat_type) = 'rebound' THEN e.stat_value ELSE 0 END) AS rebounds,
        SUM(CASE WHEN LOWER(e.stat_type) = 'steal' THEN e.stat_value ELSE 0 END) AS steals,
        SUM(CASE WHEN LOWER(e.stat_type) = 'block' THEN e.stat_value ELSE 0 END) AS blocks,
        SUM(CASE WHEN LOWER(e.stat_type) = 'turnover' THEN e.stat_value ELSE 0 END) AS turnovers,
        SUM(CASE WHEN LOWER(e.stat_type) = 'minutes_played' THEN e.stat_value ELSE 0 END) AS minutes_played,
        MAX(e.created_at) as last_event_time
    FROM player_stat_events e
    JOIN games g ON e.game_id = g.game_id
    WHERE g.state = 'IN_PROGRESS'
    GROUP BY e.team_id, e.game_id, g.season_id
)
SELECT 
    ls.team_id,
    ls.season_id,
    t.name as team_name,
    t.league_id,
    l.name as league_name,
    ls.points AS ppg,
    ls.assists AS apg,
    ls.rebounds AS rpg,
    ls.steals AS spg,
    ls.blocks AS bpg,
    ls.turnovers AS topg,
    ls.minutes_played AS mpg,
    1 AS games_played,
    ls.last_event_time as last_updated
FROM live_stats ls
JOIN games g ON ls.game_id = g.game_id
JOIN teams t ON ls.team_id = t.team_id
JOIN leagues l ON t.league_id = l.league_id;

-- Create view for combined team stats (both completed and live games)
CREATE VIEW team_combined_stats AS
WITH combined_stats AS (
    -- Get completed game stats
    SELECT 
        team_id,
        season_id,
        team_name,
        league_id,
        league_name,
        ppg * games_played as total_points,
        apg * games_played as total_assists,
        rpg * games_played as total_rebounds,
        spg * games_played as total_steals,
        bpg * games_played as total_blocks,
        topg * games_played as total_turnovers,
        mpg * games_played as total_minutes,
        games_played,
        last_updated
    FROM team_session_avg
    
    UNION ALL
    
    -- Get live game stats
    SELECT 
        team_id,
        season_id,
        team_name,
        league_id,
        league_name,
        ppg as total_points,
        apg as total_assists,
        rpg as total_rebounds,
        spg as total_steals,
        bpg as total_blocks,
        topg as total_turnovers,
        mpg as total_minutes,
        games_played,
        last_updated
    FROM team_live_stats
)
SELECT 
    team_id,
    season_id,
    team_name,
    league_id,
    league_name,
    SUM(total_points) / SUM(games_played) as ppg,
    SUM(total_assists) / SUM(games_played) as apg,
    SUM(total_rebounds) / SUM(games_played) as rpg,
    SUM(total_steals) / SUM(games_played) as spg,
    SUM(total_blocks) / SUM(games_played) as bpg,
    SUM(total_turnovers) / SUM(games_played) as topg,
    SUM(total_minutes) / SUM(games_played) as mpg,
    SUM(games_played) as games_played,
    MAX(last_updated) as last_updated
FROM combined_stats
GROUP BY team_id, season_id, team_name, league_id, league_name;

-- Create trigger to refresh views when stats are updated
CREATE TRIGGER refresh_stats_trigger
    AFTER INSERT OR UPDATE OR DELETE ON player_stat_events
    FOR EACH STATEMENT
    EXECUTE FUNCTION refresh_stat_views();

-- Create trigger to refresh views when game state changes
CREATE TRIGGER refresh_stats_on_game_state_change
    AFTER UPDATE OF state ON games
    FOR EACH ROW
    WHEN (OLD.state != NEW.state)
    EXECUTE FUNCTION refresh_stat_views();

-- Create indexes for the underlying tables to help view performance
CREATE INDEX idx_stat_player_season ON player_stat_events (player_id, game_id);
CREATE INDEX idx_stat_team_season ON player_stat_events (team_id, game_id);
CREATE INDEX idx_game_season ON games (game_id, season_id);

-- Create continuous aggregate for player stats
CREATE MATERIALIZED VIEW player_stats_cagg
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 hour', timestamp) as bucket,
    player_id,
    game_id,
    stat_type,
    SUM(stat_value) as total_value,
    COUNT(*) as event_count
FROM player_stat_events
GROUP BY bucket, player_id, game_id, stat_type
WITH NO DATA;

-- Create continuous aggregate for team stats
CREATE MATERIALIZED VIEW team_stats_cagg
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 hour', timestamp) as bucket,
    team_id,
    game_id,
    stat_type,
    SUM(stat_value) as total_value,
    COUNT(*) as event_count
FROM player_stat_events
GROUP BY bucket, team_id, game_id, stat_type
WITH NO DATA;

-- Create indexes
CREATE INDEX idx_game_state ON games (state, game_id);
CREATE INDEX idx_game_season_state ON games (season_id, state);
CREATE INDEX idx_game_team ON games (home_team_id, away_team_id);

-- Composite indexes for stat queries
CREATE INDEX idx_player_stats_game ON player_stat_events (player_id, game_id, stat_type);
CREATE INDEX idx_team_stats_game ON player_stat_events (team_id, game_id, stat_type);
CREATE INDEX idx_stats_timestamp ON player_stat_events (timestamp, game_id);

-- Partial indexes for live games (smaller, faster)
CREATE INDEX idx_live_game_stats ON player_stat_events (game_id, stat_type, stat_value) 
WHERE game_id IN (SELECT game_id FROM games WHERE state = 'IN_PROGRESS');

CREATE INDEX idx_live_game_events ON player_stat_events (game_id, player_id, stat_type) 
WHERE game_id IN (SELECT game_id FROM games WHERE state = 'IN_PROGRESS');

-- Explain index benefits
COMMENT ON INDEX idx_game_state IS 'Fast filtering of live vs completed games';
COMMENT ON INDEX idx_game_season_state IS 'Efficient season-based queries with game state';
COMMENT ON INDEX idx_game_team IS 'Quick team game lookups';
COMMENT ON INDEX idx_player_stats_game IS 'Optimizes player game stat retrieval';
COMMENT ON INDEX idx_team_stats_game IS 'Optimizes team game stat retrieval';
COMMENT ON INDEX idx_stats_timestamp IS 'Helps TimescaleDB chunk navigation';
COMMENT ON INDEX idx_live_game_stats IS 'Small index only for active games';
COMMENT ON INDEX idx_live_game_events IS 'Fast live game stat queries';

-- Create player_stats view for overall player averages
CREATE VIEW player_stats AS
WITH game_totals AS (
    SELECT 
        e.player_id,
        e.game_id,
        SUM(CASE WHEN stat_type = 'point' THEN stat_value ELSE 0 END) as points,
        SUM(CASE WHEN stat_type = 'assist' THEN stat_value ELSE 0 END) as assists,
        SUM(CASE WHEN stat_type = 'rebound' THEN stat_value ELSE 0 END) as rebounds,
        SUM(CASE WHEN stat_type = 'steal' THEN stat_value ELSE 0 END) as steals,
        SUM(CASE WHEN stat_type = 'block' THEN stat_value ELSE 0 END) as blocks,
        SUM(CASE WHEN stat_type = 'turnover' THEN stat_value ELSE 0 END) as turnovers,
        SUM(CASE WHEN stat_type = 'minutes_played' THEN stat_value ELSE 0 END) as minutes
    FROM player_stat_events e
    GROUP BY e.player_id, e.game_id
)
SELECT 
    p.name,
    p.player_id,
    COUNT(DISTINCT gt.game_id) as games_played,
    ROUND(AVG(gt.points), 1) as ppg,
    ROUND(AVG(gt.assists), 1) as apg,
    ROUND(AVG(gt.rebounds), 1) as rpg,
    ROUND(AVG(gt.steals), 1) as spg,
    ROUND(AVG(gt.blocks), 1) as bpg,
    ROUND(AVG(gt.turnovers), 1) as topg,
    ROUND(AVG(gt.minutes), 1) as mpg
FROM players p
LEFT JOIN game_totals gt ON p.player_id = gt.player_id
GROUP BY p.name, p.player_id;

-- Create team_stats view for overall team averages
CREATE VIEW team_stats AS
WITH game_totals AS (
    SELECT 
        e.team_id,
        e.game_id,
        SUM(CASE WHEN stat_type = 'point' THEN stat_value ELSE 0 END) as points,
        SUM(CASE WHEN stat_type = 'assist' THEN stat_value ELSE 0 END) as assists,
        SUM(CASE WHEN stat_type = 'rebound' THEN stat_value ELSE 0 END) as rebounds,
        SUM(CASE WHEN stat_type = 'steal' THEN stat_value ELSE 0 END) as steals,
        SUM(CASE WHEN stat_type = 'block' THEN stat_value ELSE 0 END) as blocks,
        SUM(CASE WHEN stat_type = 'turnover' THEN stat_value ELSE 0 END) as turnovers,
        SUM(CASE WHEN stat_type = 'minutes_played' THEN stat_value ELSE 0 END) as minutes
    FROM player_stat_events e
    GROUP BY e.team_id, e.game_id
)
SELECT 
    t.name,
    t.team_id,
    COUNT(DISTINCT gt.game_id) as games_played,
    ROUND(AVG(gt.points), 1) as ppg,
    ROUND(AVG(gt.assists), 1) as apg,
    ROUND(AVG(gt.rebounds), 1) as rpg,
    ROUND(AVG(gt.steals), 1) as spg,
    ROUND(AVG(gt.blocks), 1) as bpg,
    ROUND(AVG(gt.turnovers), 1) as topg,
    ROUND(AVG(gt.minutes), 1) as mpg
FROM teams t
LEFT JOIN game_totals gt ON t.team_id = gt.team_id
GROUP BY t.name, t.team_id;

-- Create indexes for performance
CREATE INDEX idx_player_stats_game ON player_stat_events (player_id, game_id, stat_type);
CREATE INDEX idx_game_state ON games (game_id, state, season_id);
CREATE INDEX idx_team_stats_game ON player_stat_events (team_id, game_id, stat_type);

-- Create view for live team stats (current/in-progress games)
CREATE VIEW team_live_stats AS
WITH live_stats AS (
    SELECT 
        e.team_id,
        e.game_id,
        g.season_id,
        SUM(CASE WHEN LOWER(e.stat_type) = 'point' THEN e.stat_value ELSE 0 END) AS points,
        SUM(CASE WHEN LOWER(e.stat_type) = 'assist' THEN e.stat_value ELSE 0 END) AS assists,
        SUM(CASE WHEN LOWER(e.stat_type) = 'rebound' THEN e.stat_value ELSE 0 END) AS rebounds,
        SUM(CASE WHEN LOWER(e.stat_type) = 'steal' THEN e.stat_value ELSE 0 END) AS steals,
        SUM(CASE WHEN LOWER(e.stat_type) = 'block' THEN e.stat_value ELSE 0 END) AS blocks,
        SUM(CASE WHEN LOWER(e.stat_type) = 'turnover' THEN e.stat_value ELSE 0 END) AS turnovers,
        SUM(CASE WHEN LOWER(e.stat_type) = 'minutes_played' THEN e.stat_value ELSE 0 END) AS minutes_played,
        MAX(e.created_at) as last_event_time
    FROM player_stat_events e
    JOIN games g ON e.game_id = g.game_id
    WHERE g.state = 'IN_PROGRESS'
    GROUP BY e.team_id, e.game_id, g.season_id
)
SELECT 
    ls.team_id,
    ls.season_id,
    t.name as team_name,
    t.league_id,
    l.name as league_name,
    ls.points AS ppg,
    ls.assists AS apg,
    ls.rebounds AS rpg,
    ls.steals AS spg,
    ls.blocks AS bpg,
    ls.turnovers AS topg,
    ls.minutes_played AS mpg,
    1 AS games_played,
    ls.last_event_time as last_updated
FROM live_stats ls
JOIN games g ON ls.game_id = g.game_id
JOIN teams t ON ls.team_id = t.team_id
JOIN leagues l ON t.league_id = l.league_id;

-- Create view for combined team stats (both completed and live games)
CREATE VIEW team_combined_stats AS
WITH combined_stats AS (
    -- Get completed game stats
    SELECT 
        team_id,
        season_id,
        team_name,
        league_id,
        league_name,
        ppg * games_played as total_points,
        apg * games_played as total_assists,
        rpg * games_played as total_rebounds,
        spg * games_played as total_steals,
        bpg * games_played as total_blocks,
        topg * games_played as total_turnovers,
        mpg * games_played as total_minutes,
        games_played,
        last_updated
    FROM team_session_avg
    
    UNION ALL
    
    -- Get live game stats
    SELECT 
        team_id,
        season_id,
        team_name,
        league_id,
        league_name,
        ppg as total_points,
        apg as total_assists,
        rpg as total_rebounds,
        spg as total_steals,
        bpg as total_blocks,
        topg as total_turnovers,
        mpg as total_minutes,
        games_played,
        last_updated
    FROM team_live_stats
)
SELECT 
    team_id,
    season_id,
    team_name,
    league_id,
    league_name,
    SUM(total_points) / SUM(games_played) as ppg,
    SUM(total_assists) / SUM(games_played) as apg,
    SUM(total_rebounds) / SUM(games_played) as rpg,
    SUM(total_steals) / SUM(games_played) as spg,
    SUM(total_blocks) / SUM(games_played) as bpg,
    SUM(total_turnovers) / SUM(games_played) as topg,
    SUM(total_minutes) / SUM(games_played) as mpg,
    SUM(games_played) as games_played,
    MAX(last_updated) as last_updated
FROM combined_stats
GROUP BY team_id, season_id, team_name, league_id, league_name;

-- Create trigger to refresh views when stats are updated
CREATE TRIGGER refresh_stats_trigger
    AFTER INSERT OR UPDATE OR DELETE ON player_stat_events
    FOR EACH STATEMENT
    EXECUTE FUNCTION refresh_stat_views();

-- Create trigger to refresh views when game state changes
CREATE TRIGGER refresh_stats_on_game_state_change
    AFTER UPDATE OF state ON games
    FOR EACH ROW
    WHEN (OLD.state != NEW.state)
    EXECUTE FUNCTION refresh_stat_views();

-- Create indexes for the underlying tables to help view performance
CREATE INDEX idx_stat_player_season ON player_stat_events (player_id, game_id);
CREATE INDEX idx_stat_team_season ON player_stat_events (team_id, game_id);
CREATE INDEX idx_game_season ON games (game_id, season_id);

-- Create continuous aggregate for player stats
CREATE MATERIALIZED VIEW player_stats_cagg
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 hour', timestamp) as bucket,
    player_id,
    game_id,
    stat_type,
    SUM(stat_value) as total_value,
    COUNT(*) as event_count
FROM player_stat_events
GROUP BY bucket, player_id, game_id, stat_type
WITH NO DATA;

-- Create continuous aggregate for team stats
CREATE MATERIALIZED VIEW team_stats_cagg
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 hour', timestamp) as bucket,
    team_id,
    game_id,
    stat_type,
    SUM(stat_value) as total_value,
    COUNT(*) as event_count
FROM player_stat_events
GROUP BY bucket, team_id, game_id, stat_type
WITH NO DATA;

-- Create indexes
CREATE INDEX idx_game_state ON games (state, game_id);
CREATE INDEX idx_game_season_state ON games (season_id, state);
CREATE INDEX idx_game_team ON games (home_team_id, away_team_id);

-- Composite indexes for stat queries
CREATE INDEX idx_player_stats_game ON player_stat_events (player_id, game_id, stat_type);
CREATE INDEX idx_team_stats_game ON player_stat_events (team_id, game_id, stat_type);
CREATE INDEX idx_stats_timestamp ON player_stat_events (timestamp, game_id);

-- Partial indexes for live games (smaller, faster)
CREATE INDEX idx_live_game_stats ON player_stat_events (game_id, stat_type, stat_value) 
WHERE game_id IN (SELECT game_id FROM games WHERE state = 'IN_PROGRESS');

CREATE INDEX idx_live_game_events ON player_stat_events (game_id, player_id, stat_type) 
WHERE game_id IN (SELECT game_id FROM games WHERE state = 'IN_PROGRESS');

-- Explain index benefits
COMMENT ON INDEX idx_game_state IS 'Fast filtering of live vs completed games';
COMMENT ON INDEX idx_game_season_state IS 'Efficient season-based queries with game state';
COMMENT ON INDEX idx_game_team IS 'Quick team game lookups';
COMMENT ON INDEX idx_player_stats_game IS 'Optimizes player game stat retrieval';
COMMENT ON INDEX idx_team_stats_game IS 'Optimizes team game stat retrieval';
COMMENT ON INDEX idx_stats_timestamp IS 'Helps TimescaleDB chunk navigation';
COMMENT ON INDEX idx_live_game_stats IS 'Small index only for active games';
COMMENT ON INDEX idx_live_game_events IS 'Fast live game stat queries';

-- Create player_stats view for overall player averages
CREATE VIEW player_stats AS
WITH game_totals AS (
    SELECT 
        e.player_id,
        e.game_id,
        SUM(CASE WHEN stat_type = 'point' THEN stat_value ELSE 0 END) as points,
        SUM(CASE WHEN stat_type = 'assist' THEN stat_value ELSE 0 END) as assists,
        SUM(CASE WHEN stat_type = 'rebound' THEN stat_value ELSE 0 END) as rebounds,
        SUM(CASE WHEN stat_type = 'steal' THEN stat_value ELSE 0 END) as steals,
        SUM(CASE WHEN stat_type = 'block' THEN stat_value ELSE 0 END) as blocks,
        SUM(CASE WHEN stat_type = 'turnover' THEN stat_value ELSE 0 END) as turnovers,
        SUM(CASE WHEN stat_type = 'minutes_played' THEN stat_value ELSE 0 END) as minutes
    FROM player_stat_events e
    GROUP BY e.player_id, e.game_id
)
SELECT 
    p.name,
    p.player_id,
    COUNT(DISTINCT gt.game_id) as games_played,
    ROUND(AVG(gt.points), 1) as ppg,
    ROUND(AVG(gt.assists), 1) as apg,
    ROUND(AVG(gt.rebounds), 1) as rpg,
    ROUND(AVG(gt.steals), 1) as spg,
    ROUND(AVG(gt.blocks), 1) as bpg,
    ROUND(AVG(gt.turnovers), 1) as topg,
    ROUND(AVG(gt.minutes), 1) as mpg
FROM players p
LEFT JOIN game_totals gt ON p.player_id = gt.player_id
GROUP BY p.name, p.player_id;

-- Create team_stats view for overall team averages
CREATE VIEW team_stats AS
WITH game_totals AS (
    SELECT 
        e.team_id,
        e.game_id,
        SUM(CASE WHEN stat_type = 'point' THEN stat_value ELSE 0 END) as points,
        SUM(CASE WHEN stat_type = 'assist' THEN stat_value ELSE 0 END) as assists,
        SUM(CASE WHEN stat_type = 'rebound' THEN stat_value ELSE 0 END) as rebounds,
        SUM(CASE WHEN stat_type = 'steal' THEN stat_value ELSE 0 END) as steals,
        SUM(CASE WHEN stat_type = 'block' THEN stat_value ELSE 0 END) as blocks,
        SUM(CASE WHEN stat_type = 'turnover' THEN stat_value ELSE 0 END) as turnovers,
        SUM(CASE WHEN stat_type = 'minutes_played' THEN stat_value ELSE 0 END) as minutes
    FROM player_stat_events e
    GROUP BY e.team_id, e.game_id
)
SELECT 
    t.name,
    t.team_id,
    COUNT(DISTINCT gt.game_id) as games_played,
    ROUND(AVG(gt.points), 1) as ppg,
    ROUND(AVG(gt.assists), 1) as apg,
    ROUND(AVG(gt.rebounds), 1) as rpg,
    ROUND(AVG(gt.steals), 1) as spg,
    ROUND(AVG(gt.blocks), 1) as bpg,
    ROUND(AVG(gt.turnovers), 1) as topg,
    ROUND(AVG(gt.minutes), 1) as mpg
FROM teams t
LEFT JOIN game_totals gt ON t.team_id = gt.team_id
GROUP BY t.name, t.team_id;

-- Create indexes for performance
CREATE INDEX idx_player_stats_game ON player_stat_events (player_id, game_id, stat_type);
CREATE INDEX idx_game_state ON games (game_id, state, season_id);
CREATE INDEX idx_team_stats_game ON player_stat_events (team_id, game_id, stat_type);

-- Create view for live team stats (current/in-progress games)
CREATE VIEW team_live_stats AS
WITH live_stats AS (
    SELECT 
        e.team_id,
        e.game_id,
        g.season_id,
        SUM(CASE WHEN LOWER(e.stat_type) = 'point' THEN e.stat_value ELSE 0 END) AS points,
        SUM(CASE WHEN LOWER(e.stat_type) = 'assist' THEN e.stat_value ELSE 0 END) AS assists,
        SUM(CASE WHEN LOWER(e.stat_type) = 'rebound' THEN e.stat_value ELSE 0 END) AS rebounds,
        SUM(CASE WHEN LOWER(e.stat_type) = 'steal' THEN e.stat_value ELSE 0 END) AS steals,
        SUM(CASE WHEN LOWER(e.stat_type) = 'block' THEN e.stat_value ELSE 0 END) AS blocks,
        SUM(CASE WHEN LOWER(e.stat_type) = 'turnover' THEN e.stat_value ELSE 0 END) AS turnovers,
        SUM(CASE WHEN LOWER(e.stat_type) = 'minutes_played' THEN e.stat_value ELSE 0 END) AS minutes_played,
        MAX(e.created_at) as last_event_time
    FROM player_stat_events e
    JOIN games g ON e.game_id = g.game_id
    WHERE g.state = 'IN_PROGRESS'
    GROUP BY e.team_id, e.game_id, g.season_id
)
SELECT 
    ls.team_id,
    ls.season_id,
    t.name as team_name,
    t.league_id,
    l.name as league_name,
    ls.points AS ppg,
    ls.assists AS apg,
    ls.rebounds AS rpg,
    ls.steals AS spg,
    ls.blocks AS bpg,
    ls.turnovers AS topg,
    ls.minutes_played AS mpg,
    1 AS games_played,
    ls.last_event_time as last_updated
FROM live_stats ls
JOIN games g ON ls.game_id = g.game_id
JOIN teams t ON ls.team_id = t.team_id
JOIN leagues l ON t.league_id = l.league_id;

-- Create view for combined team stats (both completed and live games)
CREATE VIEW team_combined_stats AS
WITH combined_stats AS (
    -- Get completed game stats
    SELECT 
        team_id,
        season_id,
        team_name,
        league_id,
        league_name,
        ppg * games_played as total_points,
        apg * games_played as total_assists,
        rpg * games_played as total_rebounds,
        spg * games_played as total_steals,
        bpg * games_played as total_blocks,
        topg * games_played as total_turnovers,
        mpg * games_played as total_minutes,
        games_played,
        last_updated
    FROM team_session_avg
    
    UNION ALL
    
    -- Get live game stats
    SELECT 
        team_id,
        season_id,
        team_name,
        league_id,
        league_name,
        ppg as total_points,
        apg as total_assists,
        rpg as total_rebounds,
        spg as total_steals,
        bpg as total_blocks,
        topg as total_turnovers,
        mpg as total_minutes,
        games_played,
        last_updated
    FROM team_live_stats
)
SELECT 
    team_id,
    season_id,
    team_name,
    league_id,
    league_name,
    SUM(total_points) / SUM(games_played) as ppg,
    SUM(total_assists) / SUM(games_played) as apg,
    SUM(total_rebounds) / SUM(games_played) as rpg,
    SUM(total_steals) / SUM(games_played) as spg,
    SUM(total_blocks) / SUM(games_played) as bpg,
    SUM(total_turnovers) / SUM(games_played) as topg,
    SUM(total_minutes) / SUM(games_played) as mpg,
    SUM(games_played) as games_played,
    MAX(last_updated) as last_updated
FROM combined_stats
GROUP BY team_id, season_id, team_name, league_id, league_name;

-- Create trigger to refresh views when stats are updated
CREATE TRIGGER refresh_stats_trigger
    AFTER INSERT OR UPDATE OR DELETE ON player_stat_events
    FOR EACH STATEMENT
    EXECUTE FUNCTION refresh_stat_views();

-- Create trigger to refresh views when game state changes
CREATE TRIGGER refresh_stats_on_game_state_change
    AFTER UPDATE OF state ON games
    FOR EACH ROW
    WHEN (OLD.state != NEW.state)
    EXECUTE FUNCTION refresh_stat_views();

-- Create indexes for the underlying tables to help view performance
CREATE INDEX idx_stat_player_season ON player_stat_events (player_id, game_id);
CREATE INDEX idx_stat_team_season ON player_stat_events (team_id, game_id);
CREATE INDEX idx_game_season ON games (game_id, season_id);

-- Create continuous aggregate for player stats
CREATE MATERIALIZED VIEW player_stats_cagg
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 hour', timestamp) as bucket,
    player_id,
    game_id,
    stat_type,
    SUM(stat_value) as total_value,
    COUNT(*) as event_count
FROM player_stat_events
GROUP BY bucket, player_id, game_id, stat_type
WITH NO DATA;

-- Create continuous aggregate for team stats
CREATE MATERIALIZED VIEW team_stats_cagg
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 hour', timestamp) as bucket,
    team_id,
    game_id,
    stat_type,
    SUM(stat_value) as total_value,
    COUNT(*) as event_count
FROM player_stat_events
GROUP BY bucket, team_id, game_id, stat_type
WITH NO DATA;

-- Create indexes
CREATE INDEX idx_game_state ON games (state, game_id);
CREATE INDEX idx_game_season_state ON games (season_id, state);
CREATE INDEX idx_game_team ON games (home_team_id, away_team_id);

-- Composite indexes for stat queries
CREATE INDEX idx_player_stats_game ON player_stat_events (player_id, game_id, stat_type);
CREATE INDEX idx_team_stats_game ON player_stat_events (team_id, game_id, stat_type);
CREATE INDEX idx_stats_timestamp ON player_stat_events (timestamp, game_id);

-- Partial indexes for live games (smaller, faster)
CREATE INDEX idx_live_game_stats ON player_stat_events (game_id, stat_type, stat_value) 
WHERE game_id IN (SELECT game_id FROM games WHERE state = 'IN_PROGRESS');

CREATE INDEX idx_live_game_events ON player_stat_events (game_id, player_id, stat_type) 
WHERE game_id IN (SELECT game_id FROM games WHERE state = 'IN_PROGRESS');

-- Explain index benefits
COMMENT ON INDEX idx_game_state IS 'Fast filtering of live vs completed games';
COMMENT ON INDEX idx_game_season_state IS 'Efficient season-based queries with game state';
COMMENT ON INDEX idx_game_team IS 'Quick team game lookups';
COMMENT ON INDEX idx_player_stats_game IS 'Optimizes player game stat retrieval';
COMMENT ON INDEX idx_team_stats_game IS 'Optimizes team game stat retrieval';
COMMENT ON INDEX idx_stats_timestamp IS 'Helps TimescaleDB chunk navigation';
COMMENT ON INDEX idx_live_game_stats IS 'Small index only for active games';
COMMENT ON INDEX idx_live_game_events IS 'Fast live game stat queries';

-- Create player_stats view for overall player averages
CREATE VIEW player_stats AS
WITH game_totals AS (
    SELECT 
        e.player_id,
        e.game_id,
        SUM(CASE WHEN stat_type = 'point' THEN stat_value ELSE 0 END) as points,
        SUM(CASE WHEN stat_type = 'assist' THEN stat_value ELSE 0 END) as assists,
        SUM(CASE WHEN stat_type = 'rebound' THEN stat_value ELSE 0 END)
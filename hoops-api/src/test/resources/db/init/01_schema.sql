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
    city TEXT,
    division TEXT,
    conference TEXT,
    arena TEXT,
    founded_year INTEGER,
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
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
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
    last_updated TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
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
    game_state TEXT NOT NULL DEFAULT 'IN_PROGRESS',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (event_id, timestamp),
    FOREIGN KEY (player_id) REFERENCES players(player_id),
    FOREIGN KEY (game_id) REFERENCES games(game_id),
    FOREIGN KEY (team_id) REFERENCES teams(team_id),
    CONSTRAINT valid_stat_type CHECK (LOWER(stat_type) IN ('point', 'assist', 'rebound', 'steal', 'block', 'foul', 'turnover', 'minutes_played'))
);

-- Convert to hypertable
SELECT create_hypertable('player_stat_events', 'timestamp');

-- Create function to maintain game state
CREATE OR REPLACE FUNCTION update_stat_game_state()
RETURNS TRIGGER AS $$
BEGIN
    NEW.game_state := (SELECT state FROM games WHERE game_id = NEW.game_id);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to maintain game state
CREATE TRIGGER maintain_stat_game_state
    BEFORE INSERT OR UPDATE ON player_stat_events
    FOR EACH ROW
    EXECUTE FUNCTION update_stat_game_state();

-- Create refresh function for materialized views
CREATE OR REPLACE FUNCTION refresh_session_stats()
RETURNS TRIGGER AS $$
BEGIN
    IF (NEW.state = 'COMPLETED' AND OLD.state = 'IN_PROGRESS') THEN
        REFRESH MATERIALIZED VIEW CONCURRENTLY player_session_avg;
        REFRESH MATERIALIZED VIEW CONCURRENTLY team_session_avg;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Create all indexes in one place
-- Basic indexes
CREATE INDEX idx_stat_player_game ON player_stat_events (player_id, game_id);
CREATE INDEX idx_stat_team_game ON player_stat_events (team_id, game_id);
CREATE INDEX idx_stat_type ON player_stat_events (LOWER(stat_type));
CREATE INDEX idx_stats_timestamp ON player_stat_events (timestamp, game_id);

-- Game state based indexes for live games
CREATE INDEX idx_live_player_stats ON player_stat_events (player_id, game_id, stat_type, stat_value) 
WHERE game_state = 'IN_PROGRESS';

-- Game state based indexes for completed games
CREATE INDEX idx_completed_player_stats ON player_stat_events (player_id, game_id, stat_type, stat_value) 
WHERE game_state = 'COMPLETED';

CREATE INDEX idx_completed_team_stats ON player_stat_events (team_id, game_id, stat_type, stat_value) 
WHERE game_state = 'COMPLETED';

-- Game lookup indexes
CREATE INDEX idx_game_state ON games (state, game_id);
CREATE INDEX idx_game_season_state ON games (season_id, state);
CREATE INDEX idx_game_team ON games (home_team_id, away_team_id);
CREATE INDEX idx_game_team_season ON games (home_team_id, away_team_id, season_id);
CREATE INDEX idx_game_season ON games (game_id, season_id);

-- Team relationship indexes
CREATE INDEX idx_team_league ON teams (league_id);
CREATE INDEX idx_player_team ON players (team_id);

-- Create team_stats table
CREATE TABLE team_stats (
    team_id UUID NOT NULL,
    season_id UUID NOT NULL,
    time TIMESTAMPTZ NOT NULL,
    games INTEGER NOT NULL DEFAULT 0,
    ppg DOUBLE PRECISION NOT NULL DEFAULT 0,
    apg DOUBLE PRECISION NOT NULL DEFAULT 0,
    rpg DOUBLE PRECISION NOT NULL DEFAULT 0,
    spg DOUBLE PRECISION NOT NULL DEFAULT 0,
    bpg DOUBLE PRECISION NOT NULL DEFAULT 0,
    topg DOUBLE PRECISION NOT NULL DEFAULT 0,
    mpg DOUBLE PRECISION NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (team_id, season_id, time),
    FOREIGN KEY (team_id) REFERENCES teams(team_id)
);

-- Convert team_stats to hypertable
SELECT create_hypertable('team_stats', 'time');

-- Team stats indexes (moved after table creation)
CREATE INDEX idx_team_stats_season ON team_stats (team_id, season_id);

-- Document index purposes
COMMENT ON INDEX idx_live_player_stats IS 'Optimizes live game player stat queries';
COMMENT ON INDEX idx_completed_player_stats IS 'Optimizes completed game player stat queries';
COMMENT ON INDEX idx_completed_team_stats IS 'Optimizes completed game team stat queries';
COMMENT ON INDEX idx_game_team_season IS 'Optimizes team game lookups by season';
COMMENT ON INDEX idx_team_league IS 'Optimizes team-league relationship queries';
COMMENT ON INDEX idx_team_stats_season IS 'Optimizes team stats by season lookups';

-- Create materialized views
CREATE MATERIALIZED VIEW player_session_avg AS
WITH game_stats AS (
    -- First aggregate stats per game
    SELECT 
        e.player_id,
        e.game_id,
        SUM(CASE WHEN LOWER(e.stat_type) = 'point' THEN e.stat_value ELSE 0 END) AS game_points,
        SUM(CASE WHEN LOWER(e.stat_type) = 'assist' THEN e.stat_value ELSE 0 END) AS game_assists,
        SUM(CASE WHEN LOWER(e.stat_type) = 'rebound' THEN e.stat_value ELSE 0 END) AS game_rebounds,
        SUM(CASE WHEN LOWER(e.stat_type) = 'steal' THEN e.stat_value ELSE 0 END) AS game_steals,
        SUM(CASE WHEN LOWER(e.stat_type) = 'block' THEN e.stat_value ELSE 0 END) AS game_blocks,
        SUM(CASE WHEN LOWER(e.stat_type) = 'foul' THEN e.stat_value ELSE 0 END) AS game_fouls,
        SUM(CASE WHEN LOWER(e.stat_type) = 'turnover' THEN e.stat_value ELSE 0 END) AS game_turnovers,
        SUM(CASE WHEN LOWER(e.stat_type) = 'minutes_played' THEN e.stat_value ELSE 0 END) AS game_minutes_played
    FROM player_stat_events e
    GROUP BY e.player_id, e.game_id
)
SELECT 
    gs.player_id,
    g.season_id,
    AVG(gs.game_points) AS avg_points_per_game,
    AVG(gs.game_assists) AS avg_assists_per_game,
    AVG(gs.game_rebounds) AS avg_rebounds_per_game,
    AVG(gs.game_steals) AS avg_steals_per_game,
    AVG(gs.game_blocks) AS avg_blocks_per_game,
    AVG(gs.game_fouls) AS avg_fouls_per_game,
    AVG(gs.game_turnovers) AS avg_turnovers_per_game,
    AVG(gs.game_minutes_played) AS avg_minutes_per_game,
    COUNT(DISTINCT gs.game_id) AS games_played
FROM game_stats gs
JOIN games g ON gs.game_id = g.game_id
WHERE g.state = 'COMPLETED'
GROUP BY gs.player_id, g.season_id;

CREATE UNIQUE INDEX idx_player_session_avg ON player_session_avg (player_id, season_id);

-- Initial refresh of player materialized view
REFRESH MATERIALIZED VIEW player_session_avg;

-- Create materialized view for team stats from completed games
CREATE MATERIALIZED VIEW team_session_avg AS
WITH game_stats AS (
    -- First aggregate stats per team per game
    SELECT 
        e.team_id,
        e.game_id,
        g.season_id,
        SUM(CASE WHEN LOWER(e.stat_type) = 'point' THEN e.stat_value ELSE 0 END) AS game_points,
        SUM(CASE WHEN LOWER(e.stat_type) = 'assist' THEN e.stat_value ELSE 0 END) AS game_assists,
        SUM(CASE WHEN LOWER(e.stat_type) = 'rebound' THEN e.stat_value ELSE 0 END) AS game_rebounds,
        SUM(CASE WHEN LOWER(e.stat_type) = 'steal' THEN e.stat_value ELSE 0 END) AS game_steals,
        SUM(CASE WHEN LOWER(e.stat_type) = 'block' THEN e.stat_value ELSE 0 END) AS game_blocks,
        SUM(CASE WHEN LOWER(e.stat_type) = 'turnover' THEN e.stat_value ELSE 0 END) AS game_turnovers,
        SUM(CASE WHEN LOWER(e.stat_type) = 'minutes_played' THEN e.stat_value ELSE 0 END) AS game_minutes_played
    FROM player_stat_events e
    JOIN games g ON e.game_id = g.game_id
    WHERE g.state = 'COMPLETED'
    GROUP BY e.team_id, e.game_id, g.season_id
)
SELECT 
    gs.team_id,
    gs.season_id,
    AVG(gs.game_points) AS ppg,
    AVG(gs.game_assists) AS apg,
    AVG(gs.game_rebounds) AS rpg,
    AVG(gs.game_steals) AS spg,
    AVG(gs.game_blocks) AS bpg,
    AVG(gs.game_turnovers) AS topg,
    AVG(gs.game_minutes_played) AS mpg,
    COUNT(DISTINCT gs.game_id) AS games_played,
    MAX(g.last_updated) AS last_updated
FROM game_stats gs
JOIN games g ON gs.game_id = g.game_id
GROUP BY gs.team_id, gs.season_id;

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
        SUM(CASE WHEN LOWER(e.stat_type) = 'minutes_played' THEN e.stat_value ELSE 0 END) AS minutes_played
    FROM player_stat_events e
    JOIN games g ON e.game_id = g.game_id
    WHERE g.state = 'IN_PROGRESS'
    GROUP BY e.team_id, e.game_id, g.season_id
)
SELECT 
    ls.team_id,
    ls.game_id,
    ls.season_id,
    ls.points AS ppg,
    ls.assists AS apg,
    ls.rebounds AS rpg,
    ls.steals AS spg,
    ls.blocks AS bpg,
    ls.turnovers AS topg,
    ls.minutes_played AS mpg,
    1 AS games_played,
    g.last_updated
FROM live_stats ls
JOIN games g ON ls.game_id = g.game_id;

CREATE UNIQUE INDEX idx_team_session_avg ON team_session_avg (team_id, season_id);

-- Create trigger to refresh views when game completes
CREATE TRIGGER refresh_session_stats_trigger
    AFTER UPDATE OF state ON games
    FOR EACH ROW
    WHEN (NEW.state = 'COMPLETED' AND OLD.state = 'IN_PROGRESS')
    EXECUTE FUNCTION refresh_session_stats();

-- Create view for combined team stats (both completed and live games)
DROP MATERIALIZED VIEW IF EXISTS team_combined_stats;
CREATE VIEW team_combined_stats AS
WITH combined_stats AS (
    -- Get completed game stats
    SELECT 
        ts.team_id,
        ts.season_id,
        ts.ppg * ts.games_played as total_points,
        ts.apg * ts.games_played as total_assists,
        ts.rpg * ts.games_played as total_rebounds,
        ts.spg * ts.games_played as total_steals,
        ts.bpg * ts.games_played as total_blocks,
        ts.topg * ts.games_played as total_turnovers,
        ts.mpg * ts.games_played as total_minutes,
        ts.games_played,
        ts.last_updated,
        t.name as team_name,
        t.league_id,
        l.name as league_name
    FROM team_session_avg ts
    JOIN teams t ON ts.team_id = t.team_id
    JOIN leagues l ON t.league_id = l.league_id
    
    UNION ALL
    
    -- Get live game stats
    SELECT 
        tls.team_id,
        tls.season_id,
        tls.ppg as total_points,
        tls.apg as total_assists,
        tls.rpg as total_rebounds,
        tls.spg as total_steals,
        tls.bpg as total_blocks,
        tls.topg as total_turnovers,
        tls.mpg as total_minutes,
        tls.games_played,
        tls.last_updated,
        t.name as team_name,
        t.league_id,
        l.name as league_name
    FROM team_live_stats tls
    JOIN teams t ON tls.team_id = t.team_id
    JOIN leagues l ON t.league_id = l.league_id
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

-- Create view for live player stats (current/in-progress games)
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
        SUM(CASE WHEN LOWER(e.stat_type) = 'foul' THEN e.stat_value ELSE 0 END) AS fouls,
        SUM(CASE WHEN LOWER(e.stat_type) = 'turnover' THEN e.stat_value ELSE 0 END) AS turnovers,
        SUM(CASE WHEN LOWER(e.stat_type) = 'minutes_played' THEN e.stat_value ELSE 0 END) AS minutes_played
    FROM player_stat_events e
    JOIN games g ON e.game_id = g.game_id
    WHERE g.state = 'IN_PROGRESS'
    GROUP BY e.player_id, e.game_id, g.season_id
)
SELECT 
    ls.player_id,
    ls.game_id,
    ls.season_id,
    ls.points AS avg_points_per_game,
    ls.assists AS avg_assists_per_game,
    ls.rebounds AS avg_rebounds_per_game,
    ls.steals AS avg_steals_per_game,
    ls.blocks AS avg_blocks_per_game,
    ls.fouls AS avg_fouls_per_game,
    ls.turnovers AS avg_turnovers_per_game,
    ls.minutes_played AS avg_minutes_per_game,
    1 AS games_played,
    g.last_updated
FROM live_stats ls
JOIN games g ON ls.game_id = g.game_id;

-- Create view for combined player stats (both completed and live games)
CREATE VIEW player_combined_stats AS
WITH combined_stats AS (
    -- Get completed game stats
    SELECT 
        player_id,
        season_id,
        avg_points_per_game * games_played as total_points,
        avg_assists_per_game * games_played as total_assists,
        avg_rebounds_per_game * games_played as total_rebounds,
        avg_steals_per_game * games_played as total_steals,
        avg_blocks_per_game * games_played as total_blocks,
        avg_fouls_per_game * games_played as total_fouls,
        avg_turnovers_per_game * games_played as total_turnovers,
        avg_minutes_per_game * games_played as total_minutes,
        games_played,
        NULL::uuid as game_id
    FROM player_session_avg
    
    UNION ALL
    
    -- Get live game stats
    SELECT 
        player_id,
        season_id,
        avg_points_per_game as total_points,
        avg_assists_per_game as total_assists,
        avg_rebounds_per_game as total_rebounds,
        avg_steals_per_game as total_steals,
        avg_blocks_per_game as total_blocks,
        avg_fouls_per_game as total_fouls,
        avg_turnovers_per_game as total_turnovers,
        avg_minutes_per_game as total_minutes,
        games_played,
        game_id
    FROM player_live_stats
)
SELECT 
    cs.player_id,
    cs.season_id,
    cs.game_id,
    SUM(total_points) / SUM(games_played) as avg_points_per_game,
    SUM(total_assists) / SUM(games_played) as avg_assists_per_game,
    SUM(total_rebounds) / SUM(games_played) as avg_rebounds_per_game,
    SUM(total_steals) / SUM(games_played) as avg_steals_per_game,
    SUM(total_blocks) / SUM(games_played) as avg_blocks_per_game,
    SUM(total_fouls) / SUM(games_played) as avg_fouls_per_game,
    SUM(total_turnovers) / SUM(games_played) as avg_turnovers_per_game,
    SUM(total_minutes) / SUM(games_played) as avg_minutes_per_game,
    SUM(games_played) as games_played,
    p.name as player_name,
    p.team_id,
    t.name as team_name,
    t.league_id,
    l.name as league_name
FROM combined_stats cs
JOIN players p ON cs.player_id = p.player_id
JOIN teams t ON p.team_id = t.team_id
JOIN leagues l ON t.league_id = l.league_id
GROUP BY cs.player_id, cs.season_id, cs.game_id, p.name, p.team_id, t.name, t.league_id, l.name;

CREATE INDEX idx_historical_player_stats ON player_stat_events (player_id, game_id, stat_type, stat_value) 
WHERE game_state = 'COMPLETED';

-- Create indexes for team queries
CREATE INDEX idx_team_game_stats ON player_stat_events (team_id, game_id, stat_type, stat_value) 
WHERE game_state = 'IN_PROGRESS';

-- Create indexes for player queries
CREATE INDEX idx_player_stats_game ON player_stat_events (player_id, game_id, stat_type, stat_value) 
WHERE game_state = 'IN_PROGRESS';

CREATE INDEX idx_player_stats_team ON player_stat_events (player_id, team_id, stat_type, stat_value) 
WHERE game_state = 'IN_PROGRESS';

-- Create index for live game events (simplified)
CREATE INDEX idx_live_game_events ON player_stat_events (game_id, player_id, stat_type)
WHERE game_state = 'IN_PROGRESS'; 
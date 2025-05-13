-- Enable TimescaleDB extension
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- Create base tables
CREATE TABLE league (
    league_id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    country TEXT NOT NULL
);

CREATE TABLE season (
    season_id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    active BOOLEAN DEFAULT false
);

CREATE TABLE team (
    team_id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    league_id UUID REFERENCES league(league_id),
    country TEXT NOT NULL
);

CREATE TABLE player (
    player_id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    team_id UUID REFERENCES team(team_id),
    jersey_number TEXT,
    position TEXT,
    active BOOLEAN DEFAULT true
);

CREATE TABLE game (
    game_id UUID PRIMARY KEY,
    game_date DATE NOT NULL,
    season_id UUID REFERENCES season(season_id),
    league_id UUID REFERENCES league(league_id),
    home_team_id UUID REFERENCES team(team_id),
    away_team_id UUID REFERENCES team(team_id),
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
    PRIMARY KEY (event_id, timestamp),
    FOREIGN KEY (player_id) REFERENCES player(player_id),
    FOREIGN KEY (game_id) REFERENCES game(game_id),
    FOREIGN KEY (team_id) REFERENCES team(team_id),
    CONSTRAINT valid_stat_type CHECK (LOWER(stat_type) IN ('point', 'assist', 'rebound', 'steal', 'block', 'foul', 'turnover', 'minutes_played'))
);

-- Convert to hypertable
SELECT create_hypertable('player_stat_events', 'timestamp');

-- Create indexes
CREATE INDEX idx_stat_player_game ON player_stat_events (player_id, game_id);
CREATE INDEX idx_stat_team_game ON player_stat_events (team_id, game_id);
CREATE INDEX idx_stat_type ON player_stat_events (LOWER(stat_type));

-- Create materialized views
CREATE MATERIALIZED VIEW player_session_avg AS
SELECT 
    e.player_id,
    s.season_id,
    AVG(CASE WHEN LOWER(e.stat_type) = 'point' THEN e.stat_value ELSE NULL END) AS avg_points,
    AVG(CASE WHEN LOWER(e.stat_type) = 'assist' THEN e.stat_value ELSE NULL END) AS avg_assists,
    AVG(CASE WHEN LOWER(e.stat_type) = 'rebound' THEN e.stat_value ELSE NULL END) AS avg_rebounds,
    AVG(CASE WHEN LOWER(e.stat_type) = 'steal' THEN e.stat_value ELSE NULL END) AS avg_steals,
    AVG(CASE WHEN LOWER(e.stat_type) = 'block' THEN e.stat_value ELSE NULL END) AS avg_blocks,
    AVG(CASE WHEN LOWER(e.stat_type) = 'foul' THEN e.stat_value ELSE NULL END) AS avg_fouls,
    AVG(CASE WHEN LOWER(e.stat_type) = 'turnover' THEN e.stat_value ELSE NULL END) AS avg_turnovers,
    AVG(CASE WHEN LOWER(e.stat_type) = 'minutes_played' THEN e.stat_value ELSE NULL END) AS avg_minutes_played
FROM player_stat_events e
JOIN game g ON e.game_id = g.game_id
JOIN season s ON g.season_id = s.season_id
GROUP BY e.player_id, s.season_id;

CREATE INDEX idx_player_session_avg ON player_session_avg (player_id, season_id);

-- Create team statistics materialized view
CREATE MATERIALIZED VIEW team_session_avg AS
SELECT 
    e.team_id,
    s.season_id,
    AVG(CASE WHEN LOWER(e.stat_type) = 'point' THEN e.stat_value ELSE NULL END) AS avg_points,
    AVG(CASE WHEN LOWER(e.stat_type) = 'assist' THEN e.stat_value ELSE NULL END) AS avg_assists,
    AVG(CASE WHEN LOWER(e.stat_type) = 'rebound' THEN e.stat_value ELSE NULL END) AS avg_rebounds,
    AVG(CASE WHEN LOWER(e.stat_type) = 'steal' THEN e.stat_value ELSE NULL END) AS avg_steals,
    AVG(CASE WHEN LOWER(e.stat_type) = 'block' THEN e.stat_value ELSE NULL END) AS avg_blocks,
    AVG(CASE WHEN LOWER(e.stat_type) = 'foul' THEN e.stat_value ELSE NULL END) AS avg_fouls,
    AVG(CASE WHEN LOWER(e.stat_type) = 'turnover' THEN e.stat_value ELSE NULL END) AS avg_turnovers,
    AVG(CASE WHEN LOWER(e.stat_type) = 'minutes_played' THEN e.stat_value ELSE NULL END) AS avg_minutes_played
FROM player_stat_events e
JOIN game g ON e.game_id = g.game_id
JOIN season s ON g.season_id = s.season_id
GROUP BY e.team_id, s.season_id;

CREATE INDEX idx_team_session_avg ON team_session_avg (team_id, season_id); 
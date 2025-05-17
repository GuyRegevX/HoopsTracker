-- Enable TimescaleDB extension
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- Create base tables
CREATE TABLE leagues (
    league_id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    country TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE seasons (
    season_id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    active BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE teams (
    team_id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    league_id TEXT REFERENCES leagues(league_id),
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
    player_id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    team_id TEXT REFERENCES teams(team_id),
    jersey_number TEXT,
    position TEXT,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE games (
    game_id TEXT PRIMARY KEY,
    game_date DATE NOT NULL,
    season_id TEXT REFERENCES seasons(season_id),
    league_id TEXT REFERENCES leagues(league_id),
    home_team_id TEXT REFERENCES teams(team_id),
    away_team_id TEXT REFERENCES teams(team_id),
    start_time TIME NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    state TEXT NOT NULL
);

CREATE TABLE player_stat_events (
    event_id SERIAL,
    player_id TEXT NOT NULL,
    game_id TEXT NOT NULL,
    team_id TEXT NOT NULL,
    season_id TEXT NOT NULL,
    stat_type TEXT NOT NULL,
    stat_value NUMERIC NOT NULL,
    version BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (event_id, created_at),
    FOREIGN KEY (player_id) REFERENCES players(player_id),
    FOREIGN KEY (game_id) REFERENCES games(game_id),
    FOREIGN KEY (team_id) REFERENCES teams(team_id),
    CONSTRAINT valid_stat_type CHECK (LOWER(stat_type) IN ('point', 'assist', 'rebound', 'steal', 'block', 'foul', 'turnover', 'minutes_played'))
);

-- Convert to hypertable
SELECT create_hypertable('player_stat_events', 'created_at');

-- Create indexes
CREATE INDEX idx_player_stat_events_game_id ON player_stat_events(game_id);
CREATE INDEX idx_player_stat_events_player_id ON player_stat_events(player_id);
CREATE INDEX idx_player_stat_events_stat_type ON player_stat_events(stat_type);
CREATE INDEX idx_games_season_id ON games(season_id);
CREATE INDEX idx_games_league_id ON games(league_id);
CREATE INDEX idx_players_team_id ON players(team_id);
CREATE INDEX idx_teams_league_id ON teams(league_id);


-- Create the team_avg_stats_view materialized view
CREATE MATERIALIZED VIEW team_avg_stats_view
WITH (timescaledb.continuous, timescaledb.materialized_only=false) AS
SELECT
    team_id,
    season_id,
    time_bucket('1 day', created_at) AS bucket_time,
    COUNT(DISTINCT game_id) AS games,
    AVG(CASE WHEN stat_type = 'point' THEN stat_value ELSE 0 END) AS ppg,
    AVG(CASE WHEN stat_type = 'assist' THEN stat_value ELSE 0 END) AS apg,
    AVG(CASE WHEN stat_type = 'rebound' THEN stat_value ELSE 0 END) AS rpg,
    AVG(CASE WHEN stat_type = 'steal' THEN stat_value ELSE 0 END) AS spg,
    AVG(CASE WHEN stat_type = 'block' THEN stat_value ELSE 0 END) AS bpg,
    AVG(CASE WHEN stat_type = 'turnover' THEN stat_value ELSE 0 END) AS topg,
    AVG(CASE WHEN stat_type = 'minutes_played' THEN stat_value ELSE 0 END) AS mpg,
    MAX(created_at) AS last_updated
FROM player_stat_events
GROUP BY team_id, season_id, time_bucket('1 day', created_at);


-- Set the refresh policy with a 1-second delay (real-time aggregation)
SELECT add_continuous_aggregate_policy('team_avg_stats_view',
    start_offset => INTERVAL '1 month',
    end_offset => INTERVAL '1 second',
    schedule_interval => INTERVAL '10 minute');

-- Create the player_avg_stats_view materialized view
CREATE MATERIALIZED VIEW player_avg_stats_view
WITH (timescaledb.continuous, timescaledb.materialized_only=false) AS
SELECT
    player_id,
    season_id,
    time_bucket('1 day', created_at) AS bucket_time,
    COUNT(DISTINCT game_id) AS games,
    AVG(CASE WHEN stat_type = 'point' THEN stat_value ELSE 0 END) AS ppg,
    AVG(CASE WHEN stat_type = 'assist' THEN stat_value ELSE 0 END) AS apg,
    AVG(CASE WHEN stat_type = 'rebound' THEN stat_value ELSE 0 END) AS rpg,
    AVG(CASE WHEN stat_type = 'steal' THEN stat_value ELSE 0 END) AS spg,
    AVG(CASE WHEN stat_type = 'block' THEN stat_value ELSE 0 END) AS bpg,
    AVG(CASE WHEN stat_type = 'turnover' THEN stat_value ELSE 0 END) AS topg,
    AVG(CASE WHEN stat_type = 'minutes_played' THEN stat_value ELSE 0 END) AS mpg,
    MAX(created_at) AS last_updated
FROM player_stat_events
GROUP BY player_id, season_id, time_bucket('1 day', created_at);

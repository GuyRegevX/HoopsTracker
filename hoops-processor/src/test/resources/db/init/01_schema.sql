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
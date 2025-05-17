-- Insert test leagues
INSERT INTO leagues (league_id, name, country) VALUES
(1, 'NBA', 'USA');

-- Insert test seasons
INSERT INTO seasons (season_id, name, start_date, end_date, active) VALUES
(1, '2023/2024', '2023-10-01', '2024-04-30', TRUE);

-- Insert test teams
INSERT INTO teams (team_id, name, league_id, country, city, division, conference, arena, founded_year) VALUES
(1, 'Los Angeles Lakers', 1, 'USA', 'Los Angeles', 'Pacific', 'Western', 'Crypto.com Arena', 1947);

-- Insert test players
INSERT INTO players (player_id, name, team_id, jersey_number, position) VALUES
(1, 'LeBron James', 1, '23', 'SF');

-- Insert test games
INSERT INTO games (game_id, game_date, season_id, league_id, home_team_id, away_team_id, start_time, state) VALUES
(1, '2024-01-15', 1, 1, 1, 1, '19:30:00', 'COMPLETED');

-- Insert test player stats for completed game
INSERT INTO player_stat_events (player_id, game_id, team_id, stat_type, stat_value)
VALUES
('1', '1', '1', 'point', 3, '2024-01-01 19:35:00'),
('2', '1', '1', 'point', 2, '2024-01-01 19:36:00'),
('3', '1', '2', 'point', 3, '2024-01-01 19:37:00'),
('4', '1', '2', 'point', 2, '2024-01-01 19:38:00'),
('1', '2', '1', 'point', 25, '2024-01-15 20:15:00'),
('2', '2', '1', 'point', 18, '2024-01-15 20:20:00');

-- Insert test game for live stats
INSERT INTO games (game_id, game_date, season_id, league_id, home_team_id, away_team_id, start_time, state) VALUES
(2, '2024-01-16', 1, 1, 1, 1, '19:30:00', 'IN_PROGRESS');

-- Insert test player stats for live game
INSERT INTO player_stat_events (player_id, game_id, team_id, stat_type, stat_value, version) VALUES
('1', '2', '1', 'point', 25, 1),
('1', '2', '1', 'assist', 8, 1),
('1', '2', '1', 'rebound', 6, 1),
('1', '2', '1', 'steal', 1, 1),
('1', '2', '1', 'block', 2, 1),
('1', '2', '1', 'turnover', 2, 1),
('1', '2', '1', 'minutes_played', 28, 1);


-- Refresh materialized views
REFRESH MATERIALIZED VIEW team_session_avg;
REFRESH MATERIALIZED VIEW player_session_avg; 
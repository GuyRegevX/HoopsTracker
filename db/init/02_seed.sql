
INSERT INTO leagues (league_id, name, country)
VALUES ('1', 'NBA', 'USA');

-- Add seasons (required for games)
INSERT INTO seasons (season_id, name, start_date, end_date, active)
VALUES 
('1', '2023-2024 Regular Season', '2023-10-24', '2024-04-14', true),
('2', '2023-2024 Playoffs', '2024-04-16', '2024-06-20', false),
('3', '2024-2025 Regular Season', '2024-10-22', '2025-04-13', false);

-- Add teams
INSERT INTO teams (team_id, name, league_id, country, city, conference, division, arena, founded_year)
VALUES 
('1', 'Lakers', '1', 'USA', 'Los Angeles', 'Western', 'Pacific', 'Crypto.com Arena', 1947),
('2', 'Celtics', '1', 'USA', 'Boston', 'Eastern', 'Atlantic', 'TD Garden', 1946),
('3', 'Pacers', '1', 'USA', 'Indiana', 'Eastern', 'Central', 'Gainbridge Fieldhouse', 1967),
('4', 'Warriors', '1', 'USA', 'Golden State', 'Western', 'Pacific', 'Chase Center', 1946),
('5', 'Heat', '1', 'USA', 'Miami', 'Eastern', 'Southeast', 'Kaseya Center', 1988);

-- Add players (5 players per team)
-- Lakers players
INSERT INTO players (player_id, name, team_id, jersey_number, position)
VALUES 
('1', 'LeBron James', '1', '23', 'Forward'),
('2', 'Anthony Davis', '1', '3', 'Forward-Center'),
('20', 'Austin Reaves', '1', '15', 'Guard'),
('21', 'DAngelo Russell', '1', '1', 'Guard'),
('22', 'Rui Hachimura', '1', '28', 'Forward');

-- Celtics players
INSERT INTO players (player_id, name, team_id, jersey_number, position)
VALUES 
('3', 'Jayson Tatum', '2', '0', 'Forward'),
('4', 'Jaylen Brown', '2', '7', 'Guard-Forward'),
('23', 'Kristaps Porzingis', '2', '8', 'Center'),
('24', 'Jrue Holiday', '2', '4', 'Guard'),
('25', 'Derrick White', '2', '9', 'Guard');

-- Pacers players
INSERT INTO players (player_id, name, team_id, jersey_number, position)
VALUES 
('5', 'Tyrese Haliburton', '3', '0', 'Guard'),
('6', 'Myles Turner', '3', '33', 'Center'),
('7', 'Pascal Siakam', '3', '43', 'Forward'),
('8', 'Aaron Nesmith', '3', '23', 'Forward'),
('9', 'T.J. McConnell', '3', '9', 'Guard');

-- Warriors players
INSERT INTO players (player_id, name, team_id, jersey_number, position)
VALUES 
('10', 'Stephen Curry', '4', '30', 'Guard'),
('11', 'Klay Thompson', '4', '11', 'Guard'),
('12', 'Draymond Green', '4', '23', 'Forward'),
('13', 'Andrew Wiggins', '4', '22', 'Forward'),
('14', 'Jonathan Kuminga', '4', '00', 'Forward');

-- Heat players
INSERT INTO players (player_id, name, team_id, jersey_number, position)
VALUES 
('15', 'Jimmy Butler', '5', '22', 'Forward'),
('16', 'Bam Adebayo', '5', '13', 'Center'),
('17', 'Tyler Herro', '5', '14', 'Guard'),
('18', 'Terry Rozier', '5', '2', 'Guard'),
('19', 'Caleb Martin', '5', '16', 'Forward');

-- Now add games (now with valid season references)
-- Game 1: Lakers vs Celtics
INSERT INTO games (game_id, game_date, season_id, league_id, home_team_id, away_team_id, start_time, state)
VALUES ('1', '2024-01-15', '1', '1', '1', '2', '19:30:00', 'COMPLETED');

-- Game 2: Celtics vs Lakers
INSERT INTO games (game_id, game_date, season_id, league_id, home_team_id, away_team_id, start_time, state)
VALUES ('2', '2024-02-01', '1', '1', '2', '1', '20:00:00', 'IN_PROGRESS');

-- Game 3: Lakers vs Pacers
INSERT INTO games (game_id, game_date, season_id, league_id, home_team_id, away_team_id, start_time, state)
VALUES ('3', '2024-02-15', '1', '1', '1', '3', '19:00:00', 'COMPLETED');

-- Game 4: Pacers vs Lakers
INSERT INTO games (game_id, game_date, season_id, league_id, home_team_id, away_team_id, start_time, state)
VALUES ('4', '2024-02-28', '1', '1', '3', '1', '19:30:00', 'COMPLETED');

-- Additional games to ensure all teams have at least one game
-- Warriors vs Heat (Game 5)
INSERT INTO games (game_id, game_date, season_id, league_id, home_team_id, away_team_id, start_time, state)
VALUES ('5', '2024-03-01', '1', '1', '4', '5', '19:30:00', 'COMPLETED');

-- Celtics vs Warriors (Game 6)
INSERT INTO games (game_id, game_date, season_id, league_id, home_team_id, away_team_id, start_time, state)
VALUES ('6', '2024-03-15', '1', '1', '2', '4', '18:00:00', 'COMPLETED');

-- Heat vs Pacers (Game 7)
INSERT INTO games (game_id, game_date, season_id, league_id, home_team_id, away_team_id, start_time, state)
VALUES ('7', '2024-03-22', '1', '1', '5', '3', '20:00:00', 'COMPLETED');

-- Add upcoming games in future seasons
-- Playoff games (season 2)
INSERT INTO games (game_id, game_date, season_id, league_id, home_team_id, away_team_id, start_time, state)
VALUES ('8', '2024-05-01', '2', '1', '1', '4', '19:00:00', 'SCHEDULED');

INSERT INTO games (game_id, game_date, season_id, league_id, home_team_id, away_team_id, start_time, state)
VALUES ('9', '2024-05-05', '2', '1', '5', '2', '20:30:00', 'SCHEDULED');

-- Next season game (season 3)
INSERT INTO games (game_id, game_date, season_id, league_id, home_team_id, away_team_id, start_time, state)
VALUES ('10', '2024-10-25', '3', '1', '4', '3', '18:30:00', 'SCHEDULED');

-- Add game stats for completed games
-- Stats for Game 1 (Lakers vs Celtics)
INSERT INTO player_stat_events (
    event_id,
    game_id,
    player_id,
    team_id,
    season_id,
    stat_type,
    stat_value,
    version,
    created_at
) VALUES
-- Lakers stats
('1', '1', '1', '1', '1', 'point', 30, '1', '2024-01-15 19:35:00'),
('2', '1', '2', '1', '1', 'point', 22, '1', '2024-01-15 19:36:00'),
('3', '1', '1', '1', '1', 'assist', 11, '1', '2024-01-15 19:37:00'),
('4', '1', '2', '1', '1', 'rebound', 12, '1', '2024-01-15 19:38:00'),
-- Celtics stats
('5', '1', '3', '2', '1', 'point', 28, '1', '2024-01-15 19:39:00'),
('6', '1', '4', '2', '1', 'point', 25, '1', '2024-01-15 19:40:00'),
('7', '1', '3', '2', '1', 'assist', 7, '1', '2024-01-15 19:41:00'),
('8', '1', '4', '2', '1', 'rebound', 8, '1', '2024-01-15 19:42:00');

-- Stats for Game 3 (Lakers vs Pacers)
INSERT INTO player_stat_events (
    event_id,
    game_id,
    player_id,
    team_id,
    season_id,
    stat_type,
    stat_value,
    version,
    created_at
) VALUES
-- Lakers stats
('9', '3', '1', '1', '1', 'point', 32, '1', '2024-02-15 19:05:00'),
('10', '3', '2', '1', '1', 'point', 24, '1', '2024-02-15 19:06:00'),
('11', '3', '1', '1', '1', 'assist', 9, '1', '2024-02-15 19:07:00'),
('12', '3', '2', '1', '1', 'rebound', 13, '1', '2024-02-15 19:08:00'),
-- Pacers stats
('13', '3', '5', '3', '1', 'point', 26, '1', '2024-02-15 19:09:00'),
('14', '3', '6', '3', '1', 'point', 16, '1', '2024-02-15 19:10:00'),
('15', '3', '5', '3', '1', 'assist', 11, '1', '2024-02-15 19:11:00'),
('16', '3', '6', '3', '1', 'rebound', 10, '1', '2024-02-15 19:12:00');

-- Stats for Game 4 (Pacers vs Lakers)
INSERT INTO player_stat_events (
    event_id,
    game_id,
    player_id,
    team_id,
    season_id,
    stat_type,
    stat_value,
    version,
    created_at
) VALUES
-- Pacers stats
('17', '4', '5', '3', '1', 'point', 29, '1', '2024-02-28 19:35:00'),
('18', '4', '6', '3', '1', 'point', 18, '1', '2024-02-28 19:36:00'),
('19', '4', '5', '3', '1', 'assist', 10, '1', '2024-02-28 19:37:00'),
('20', '4', '6', '3', '1', 'rebound', 11, '1', '2024-02-28 19:38:00'),
-- Lakers stats
('21', '4', '1', '1', '1', 'point', 27, '1', '2024-02-28 19:39:00'),
('22', '4', '2', '1', '1', 'point', 21, '1', '2024-02-28 19:40:00'),
('23', '4', '1', '1', '1', 'assist', 8, '1', '2024-02-28 19:41:00'),
('24', '4', '2', '1', '1', 'rebound', 9, '1', '2024-02-28 19:42:00');

-- Stats for Game 5 (Warriors vs Heat)
INSERT INTO player_stat_events (
    event_id,
    game_id,
    player_id,
    team_id,
    season_id,
    stat_type,
    stat_value,
    version,
    created_at
) VALUES
-- Warriors stats
('35', '5', '10', '4', '1', 'point', 32, '1', '2024-03-01 19:35:00'),
('36', '5', '11', '4', '1', 'point', 25, '1', '2024-03-01 19:36:00'),
('37', '5', '12', '4', '1', 'point', 8, '1', '2024-03-01 19:37:00'),
('38', '5', '13', '4', '1', 'point', 16, '1', '2024-03-01 19:38:00'),
('39', '5', '14', '4', '1', 'point', 12, '1', '2024-03-01 19:39:00'),
('40', '5', '10', '4', '1', 'assist', 8, '1', '2024-03-01 19:40:00'),
('41', '5', '11', '4', '1', 'assist', 4, '1', '2024-03-01 19:41:00'),
('42', '5', '12', '4', '1', 'assist', 7, '1', '2024-03-01 19:42:00'),
('43', '5', '10', '4', '1', 'rebound', 5, '1', '2024-03-01 19:43:00'),
('44', '5', '12', '4', '1', 'rebound', 11, '1', '2024-03-01 19:44:00'),
('45', '5', '13', '4', '1', 'rebound', 6, '1', '2024-03-01 19:45:00');

-- Heat stats
INSERT INTO player_stat_events (
    event_id,
    game_id,
    player_id,
    team_id,
    season_id,
    stat_type,
    stat_value,
    version,
    created_at
) VALUES
('46', '5', '15', '5', '1', 'point', 28, '1', '2024-03-01 19:50:00'),
('47', '5', '16', '5', '1', 'point', 22, '1', '2024-03-01 19:51:00'),
('48', '5', '17', '5', '1', 'point', 18, '1', '2024-03-01 19:52:00'),
('49', '5', '18', '5', '1', 'point', 10, '1', '2024-03-01 19:53:00'),
('50', '5', '19', '5', '1', 'point', 14, '1', '2024-03-01 19:54:00'),
('51', '5', '15', '5', '1', 'assist', 6, '1', '2024-03-01 19:55:00'),
('52', '5', '17', '5', '1', 'assist', 5, '1', '2024-03-01 19:56:00'),
('53', '5', '16', '5', '1', 'rebound', 15, '1', '2024-03-01 19:57:00'),
('54', '5', '15', '5', '1', 'rebound', 7, '1', '2024-03-01 19:58:00'),
('55', '5', '19', '5', '1', 'rebound', 5, '1', '2024-03-01 19:59:00');

-- Stats for Game 6 (Celtics vs Warriors)
INSERT INTO player_stat_events (
    event_id,
    game_id,
    player_id,
    team_id,
    season_id,
    stat_type,
    stat_value,
    version,
    created_at
) VALUES
-- Celtics stats
('56', '6', '3', '2', '1', 'point', 29, '1', '2024-03-15 18:05:00'),
('57', '6', '4', '2', '1', 'point', 24, '1', '2024-03-15 18:06:00'),
('58', '6', '23', '2', '1', 'point', 19, '1', '2024-03-15 18:07:00'),
('59', '6', '24', '2', '1', 'point', 16, '1', '2024-03-15 18:08:00'),
('60', '6', '25', '2', '1', 'point', 7, '1', '2024-03-15 18:09:00'),
('61', '6', '24', '2', '1', 'assist', 9, '1', '2024-03-15 18:10:00'),
('62', '6', '3', '2', '1', 'assist', 5, '1', '2024-03-15 18:11:00'),
('63', '6', '23', '2', '1', 'rebound', 12, '1', '2024-03-15 18:12:00'),
('64', '6', '25', '2', '1', 'rebound', 8, '1', '2024-03-15 18:13:00'),
('65', '6', '4', '2', '1', 'rebound', 6, '1', '2024-03-15 18:14:00');

-- Warriors stats
INSERT INTO player_stat_events (
    event_id,
    game_id,
    player_id,
    team_id,
    season_id,
    stat_type,
    stat_value,
    version,
    created_at
) VALUES
('66', '6', '10', '4', '1', 'point', 34, '1', '2024-03-15 18:20:00'),
('67', '6', '11', '4', '1', 'point', 22, '1', '2024-03-15 18:21:00'),
('68', '6', '12', '4', '1', 'point', 11, '1', '2024-03-15 18:22:00'),
('69', '6', '13', '4', '1', 'point', 14, '1', '2024-03-15 18:23:00'),
('70', '6', '14', '4', '1', 'point', 9, '1', '2024-03-15 18:24:00'),
('71', '6', '10', '4', '1', 'assist', 9, '1', '2024-03-15 18:25:00'),
('72', '6', '12', '4', '1', 'assist', 7, '1', '2024-03-15 18:26:00'),
('73', '6', '12', '4', '1', 'rebound', 10, '1', '2024-03-15 18:27:00'),
('74', '6', '13', '4', '1', 'rebound', 7, '1', '2024-03-15 18:28:00'),
('75', '6', '14', '4', '1', 'rebound', 4, '1', '2024-03-15 18:29:00');

-- Stats for Game 7 (Heat vs Pacers)
INSERT INTO player_stat_events (
    event_id,
    game_id,
    player_id,
    team_id,
    season_id,
    stat_type,
    stat_value,
    version,
    created_at
) VALUES
-- Heat stats
('76', '7', '15', '5', '1', 'point', 31, '1', '2024-03-22 20:05:00'),
('77', '7', '16', '5', '1', 'point', 24, '1', '2024-03-22 20:06:00'),
('78', '7', '17', '5', '1', 'point', 17, '1', '2024-03-22 20:07:00'),
('79', '7', '18', '5', '1', 'point', 12, '1', '2024-03-22 20:08:00'),
('80', '7', '19', '5', '1', 'point', 10, '1', '2024-03-22 20:09:00'),
('81', '7', '15', '5', '1', 'assist', 7, '1', '2024-03-22 20:10:00'),
('82', '7', '17', '5', '1', 'assist', 6, '1', '2024-03-22 20:11:00'),
('83', '7', '16', '5', '1', 'rebound', 14, '1', '2024-03-22 20:12:00'),
('84', '7', '15', '5', '1', 'rebound', 8, '1', '2024-03-22 20:13:00'),
('85', '7', '19', '5', '1', 'rebound', 6, '1', '2024-03-22 20:14:00'),
('86', '7', '15', '5', '1', 'steal', 3, '1', '2024-03-22 20:15:00'),
('87', '7', '16', '5', '1', 'block', 2, '1', '2024-03-22 20:16:00');

-- Pacers stats
INSERT INTO player_stat_events (
    event_id,
    game_id,
    player_id,
    team_id,
    season_id,
    stat_type,
    stat_value,
    version,
    created_at
) VALUES
('88', '7', '5', '3', '1', 'point', 28, '1', '2024-03-22 20:20:00'),
('89', '7', '6', '3', '1', 'point', 18, '1', '2024-03-22 20:21:00'),
('90', '7', '7', '3', '1', 'point', 21, '1', '2024-03-22 20:22:00'),
('91', '7', '8', '3', '1', 'point', 12, '1', '2024-03-22 20:23:00'),
('92', '7', '9', '3', '1', 'point', 15, '1', '2024-03-22 20:24:00'),
('93', '7', '5', '3', '1', 'assist', 12, '1', '2024-03-22 20:25:00'),
('94', '7', '8', '3', '1', 'assist', 6, '1', '2024-03-22 20:26:00'),
('95', '7', '6', '3', '1', 'rebound', 11, '1', '2024-03-22 20:27:00'),
('96', '7', '7', '3', '1', 'rebound', 9, '1', '2024-03-22 20:28:00'),
('97', '7', '9', '3', '1', 'rebound', 5, '1', '2024-03-22 20:29:00'),
('98', '7', '5', '3', '1', 'steal', 2, '1', '2024-03-22 20:30:00'),
('99', '7', '6', '3', '1', 'block', 3, '1', '2024-03-22 20:31:00');

-- Add minutes played stats to completed games
-- For Game 5
INSERT INTO player_stat_events (
    event_id,
    game_id,
    player_id,
    team_id,
    season_id,
    stat_type,
    stat_value,
    version,
    created_at
) VALUES
-- Warriors minutes
('100', '5', '10', '4', '1', 'minutes_played', 36, '1', '2024-03-01 22:00:00'),
('101', '5', '11', '4', '1', 'minutes_played', 34, '1', '2024-03-01 22:00:00'),
('102', '5', '12', '4', '1', 'minutes_played', 32, '1', '2024-03-01 22:00:00'),
('103', '5', '13', '4', '1', 'minutes_played', 28, '1', '2024-03-01 22:00:00'),
('104', '5', '14', '4', '1', 'minutes_played', 20, '1', '2024-03-01 22:00:00'),
-- Heat minutes
('105', '5', '15', '5', '1', 'minutes_played', 38, '1', '2024-03-01 22:00:00'),
('106', '5', '16', '5', '1', 'minutes_played', 36, '1', '2024-03-01 22:00:00'),
('107', '5', '17', '5', '1', 'minutes_played', 30, '1', '2024-03-01 22:00:00'),
('108', '5', '18', '5', '1', 'minutes_played', 25, '1', '2024-03-01 22:00:00'),
('109', '5', '19', '5', '1', 'minutes_played', 22, '1', '2024-03-01 22:00:00');

-- Add minutes for remaining games
INSERT INTO player_stat_events (
    event_id,
    game_id,
    player_id,
    team_id,
    season_id,
    stat_type,
    stat_value,
    version,
    created_at
) VALUES
-- Game 1: Lakers vs Celtics
('130', '1', '1', '1', '1', 'minutes_played', 38, '1', '2024-01-15 22:00:00'),
('131', '1', '2', '1', '1', 'minutes_played', 35, '1', '2024-01-15 22:00:00'),
('132', '1', '3', '2', '1', 'minutes_played', 37, '1', '2024-01-15 22:00:00'),
('133', '1', '4', '2', '1', 'minutes_played', 36, '1', '2024-01-15 22:00:00'),

-- Game 3: Lakers vs Pacers
('134', '3', '1', '1', '1', 'minutes_played', 36, '1', '2024-02-15 22:00:00'),
('135', '3', '2', '1', '1', 'minutes_played', 34, '1', '2024-02-15 22:00:00'),
('136', '3', '5', '3', '1', 'minutes_played', 35, '1', '2024-02-15 22:00:00'),
('137', '3', '6', '3', '1', 'minutes_played', 33, '1', '2024-02-15 22:00:00'),

-- Game 4: Pacers vs Lakers
('138', '4', '5', '3', '1', 'minutes_played', 37, '1', '2024-02-28 22:00:00'),
('139', '4', '6', '3', '1', 'minutes_played', 33, '1', '2024-02-28 22:00:00'),
('140', '4', '1', '1', '1', 'minutes_played', 36, '1', '2024-02-28 22:00:00'),
('141', '4', '2', '1', '1', 'minutes_played', 34, '1', '2024-02-28 22:00:00'),

-- Game 6: Celtics vs Warriors
('142', '6', '3', '2', '1', 'minutes_played', 38, '1', '2024-03-15 21:00:00'),
('143', '6', '4', '2', '1', 'minutes_played', 36, '1', '2024-03-15 21:00:00'),
('144', '6', '10', '4', '1', 'minutes_played', 37, '1', '2024-03-15 21:00:00'),
('145', '6', '11', '4', '1', 'minutes_played', 35, '1', '2024-03-15 21:00:00'),

-- Game 7: Heat vs Pacers
('146', '7', '15', '5', '1', 'minutes_played', 39, '1', '2024-03-22 23:00:00'),
('147', '7', '16', '5', '1', 'minutes_played', 35, '1', '2024-03-22 23:00:00'),
('148', '7', '5', '3', '1', 'minutes_played', 36, '1', '2024-03-22 23:00:00'),
('149', '7', '6', '3', '1', 'minutes_played', 34, '1', '2024-03-22 23:00:00');

-- After inserting all your seed data, add these commands:

-- Refresh the player stats continuous aggregate
CALL refresh_continuous_aggregate('player_avg_stats_view_per_bucket', NULL, NULL);

-- Refresh the team stats continuous aggregate
CALL refresh_continuous_aggregate('team_avg_stats_view_per_bucket', NULL, NULL);
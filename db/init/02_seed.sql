-- Insert test league
INSERT INTO leagues (league_id, name, country)
VALUES ('1', 'NBA', 'USA');

-- Insert test season
INSERT INTO seasons (season_id, name, start_date, end_date, active)
VALUES ('1', '2023-24', '2023-10-24', '2024-06-16', true);

-- Insert test teams
INSERT INTO teams (team_id, name, league_id, country, city, division, conference)
VALUES
('1', 'Los Angeles Lakers', '1', 'USA', 'Los Angeles', 'Pacific', 'Western'),
('2', 'Boston Celtics', '1', 'USA', 'Boston', 'Atlantic', 'Eastern');

-- Insert additional teams (3 more to make 5 total)
INSERT INTO teams (team_id, name, league_id, country, city, division, conference)
VALUES 
('3', 'Indiana Pacers', '1', 'USA', 'Indianapolis', 'Central', 'Eastern'),
('4', 'Golden State Warriors', '1', 'USA', 'San Francisco', 'Pacific', 'Western'),
('5', 'Miami Heat', '1', 'USA', 'Miami', 'Southeast', 'Eastern');

-- Insert test players
INSERT INTO players (player_id, name, team_id, jersey_number, position)
VALUES
('1', 'LeBron James', '1', '23', 'F'),
('2', 'Anthony Davis', '1', '3', 'F-C'),
('3', 'Jayson Tatum', '2', '0', 'F'),
('4', 'Jaylen Brown', '2', '7', 'G-F');

-- Insert 5 players for each team (adding to existing players)
-- Pacers players
INSERT INTO players (player_id, name, team_id, jersey_number, position)
VALUES 
('5', 'Tyrese Haliburton', '3', '0', 'G'),
('6', 'Myles Turner', '3', '33', 'C'),
('7', 'Pascal Siakam', '3', '43', 'F'),
('8', 'T.J. McConnell', '3', '9', 'G'),
('9', 'Bennedict Mathurin', '3', '00', 'G-F');

-- Warriors players
INSERT INTO players (player_id, name, team_id, jersey_number, position)
VALUES 
('10', 'Stephen Curry', '4', '30', 'G'),
('11', 'Klay Thompson', '4', '11', 'G'),
('12', 'Draymond Green', '4', '23', 'F'),
('13', 'Andrew Wiggins', '4', '22', 'F'),
('14', 'Jonathan Kuminga', '4', '00', 'F');

-- Heat players
INSERT INTO players (player_id, name, team_id, jersey_number, position)
VALUES 
('15', 'Jimmy Butler', '5', '22', 'F'),
('16', 'Bam Adebayo', '5', '13', 'C'),
('17', 'Tyler Herro', '5', '14', 'G'),
('18', 'Duncan Robinson', '5', '55', 'F'),
('19', 'Caleb Martin', '5', '16', 'F');

-- Add additional Lakers players (already have LeBron and AD)
INSERT INTO players (player_id, name, team_id, jersey_number, position)
VALUES 
('20', 'Austin Reaves', '1', '15', 'G'),
('21', 'D''Angelo Russell', '1', '1', 'G'),
('22', 'Rui Hachimura', '1', '28', 'F');

-- Add additional Celtics players (already have Tatum and Brown)
INSERT INTO players (player_id, name, team_id, jersey_number, position)
VALUES 
('23', 'Kristaps Porzingis', '2', '8', 'C'),
('24', 'Derrick White', '2', '9', 'G'),
('25', 'Al Horford', '2', '42', 'F-C');

-- Insert test games
INSERT INTO games (game_id, game_date, season_id, league_id, home_team_id, away_team_id, start_time, state)
VALUES
('1', '2024-01-01', '1', '1', '1', '2', '19:30:00', 'COMPLETED'),
('2', '2024-01-15', '1', '1', '2', '1', '20:00:00', 'IN_PROGRESS');

-- Add games between teams (just 2 more simple games)
INSERT INTO games (game_id, game_date, season_id, league_id, home_team_id, away_team_id, start_time, state)
VALUES 
-- Lakers vs Pacers
('3', '2024-02-01', '1', '1', '1', '3', '19:30:00', 'COMPLETED'),
('4', '2024-02-15', '1', '1', '3', '1', '19:00:00', 'COMPLETED');

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
('1', '1', '1', '1', '1', 'point', 3, '1', '2024-01-01 19:35:00'),
('1', '1', '1', '1', '1', 'point', 2, '1', '2024-01-01 19:36:00'),
('2', '1', '2', '1', '1', 'point', 2, '2', '2024-01-01 19:36:00'),
('3', '1', '3', '2', '1', 'point', 3, '3', '2024-01-01 19:37:00'),
('4', '1', '4', '2', '1', 'point', 2, '4', '2024-01-01 19:38:00'),
('5', '2', '1', '1', '1', 'point', 25, '5', '2024-01-15 20:15:00'),
('6', '2', '2', '1', '1', 'point', 18, '6', '2024-01-15 20:20:00');

-- Add simple stats for these games
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
-- Game 3: Lakers (home) vs Pacers
('7', '3', '1', '1', '1', 'point', 28, '1', '2024-02-01 19:35:00'),
('8', '3', '2', '1', '1', 'point', 22, '1', '2024-02-01 19:36:00'),
('9', '3', '20', '1', '1', 'point', 15, '1', '2024-02-01 19:37:00'),
('10', '3', '21', '1', '1', 'point', 12, '1', '2024-02-01 19:38:00'),
('11', '3', '22', '1', '1', 'point', 10, '1', '2024-02-01 19:39:00'),

('12', '3', '5', '3', '1', 'point', 24, '1', '2024-02-01 19:40:00'),
('13', '3', '6', '3', '1', 'point', 16, '1', '2024-02-01 19:41:00'),
('14', '3', '7', '3', '1', 'point', 18, '1', '2024-02-01 19:42:00'),
('15', '3', '8', '3', '1', 'point', 8, '1', '2024-02-01 19:43:00'),
('16', '3', '9', '3', '1', 'point', 14, '1', '2024-02-01 19:44:00'),

-- Game 4: Pacers (home) vs Lakers  
('17', '4', '5', '3', '1', 'point', 26, '1', '2024-02-15 19:05:00'),
('18', '4', '6', '3', '1', 'point', 18, '1', '2024-02-15 19:06:00'),
('19', '4', '7', '3', '1', 'point', 20, '1', '2024-02-15 19:07:00'),
('20', '4', '8', '3', '1', 'point', 10, '1', '2024-02-15 19:08:00'),
('21', '4', '9', '3', '1', 'point', 16, '1', '2024-02-15 19:09:00'),

('22', '4', '1', '1', '1', 'point', 24, '1', '2024-02-15 19:10:00'),
('23', '4', '2', '1', '1', 'point', 20, '1', '2024-02-15 19:11:00'),
('24', '4', '20', '1', '1', 'point', 13, '1', '2024-02-15 19:12:00'),
('25', '4', '21', '1', '1', 'point', 14, '1', '2024-02-15 19:13:00'),
('26', '4', '22', '1', '1', 'point', 12, '1', '2024-02-15 19:14:00');

-- Add some basic rebound stats too
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
-- Game 3: Some rebounds
('27', '3', '1', '1', '1', 'rebound', 7, '1', '2024-02-01 19:45:00'),
('28', '3', '2', '1', '1', 'rebound', 12, '1', '2024-02-01 19:46:00'),
('29', '3', '5', '3', '1', 'rebound', 3, '1', '2024-02-01 19:47:00'),
('30', '3', '6', '3', '1', 'rebound', 8, '1', '2024-02-01 19:48:00'),

-- Game 4: Some rebounds
('31', '4', '1', '1', '1', 'rebound', 9, '1', '2024-02-15 19:15:00'),
('32', '4', '2', '1', '1', 'rebound', 10, '1', '2024-02-15 19:16:00'),
('33', '4', '5', '3', '1', 'rebound', 4, '1', '2024-02-15 19:17:00'),
('34', '4', '7', '3', '1', 'rebound', 7, '1', '2024-02-15 19:18:00');
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

-- Insert test players
INSERT INTO players (player_id, name, team_id, jersey_number, position)
VALUES 
('1', 'LeBron James', '1', '23', 'F'),
('2', 'Anthony Davis', '1', '3', 'F-C'),
('3', 'Jayson Tatum', '2', '0', 'F'),
('4', 'Jaylen Brown', '2', '7', 'G-F');

-- Insert test games
INSERT INTO games (game_id, game_date, season_id, league_id, home_team_id, away_team_id, start_time, state)
VALUES 
('1', '2024-01-01', '1', '1', '1', '2', '19:30:00', 'COMPLETED'),
('2', '2024-01-15', '1', '1', '2', '1', '20:00:00', 'IN_PROGRESS');

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
('2', '1', '2', '1', '1', 'point', 2, '2', '2024-01-01 19:36:00'),
('3', '1', '3', '2', '1', 'point', 3, '3', '2024-01-01 19:37:00'),
('4', '1', '4', '2', '1', 'point', 2, '4', '2024-01-01 19:38:00'),
('5', '2', '1', '1', '1', 'point', 25, '5', '2024-01-15 20:15:00'),
('6', '2', '2', '1', '1', 'point', 18, '6', '2024-01-15 20:20:00');

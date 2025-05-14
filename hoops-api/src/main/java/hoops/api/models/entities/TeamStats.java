package hoops.api.models.entities;

import java.time.OffsetDateTime;

public class TeamStats {
    // Identifiers
    private String teamId;
    private String seasonId;
    private String gameId; // For live games

    // Team Metadata
    private String teamName;
    private String leagueId;
    private String leagueName;

    // Stats
    private int games;
    private double ppg;
    private double apg;
    private double rpg;
    private double spg;
    private double bpg;
    private double topg;
    private double mpg;

    // Timestamps
    private OffsetDateTime time;
    private OffsetDateTime lastUpdated;

    // Getters and Setters
    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public String getSeasonId() {
        return seasonId;
    }

    public void setSeasonId(String seasonId) {
        this.seasonId = seasonId;
    }

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getLeagueId() {
        return leagueId;
    }

    public void setLeagueId(String leagueId) {
        this.leagueId = leagueId;
    }

    public String getLeagueName() {
        return leagueName;
    }

    public void setLeagueName(String leagueName) {
        this.leagueName = leagueName;
    }

    public OffsetDateTime getTime() {
        return time;
    }

    public void setTime(OffsetDateTime time) {
        this.time = time;
    }

    public int getGames() {
        return games;
    }

    public void setGames(int games) {
        this.games = games;
    }

    public double getPpg() {
        return ppg;
    }

    public void setPpg(double ppg) {
        this.ppg = ppg;
    }

    public double getApg() {
        return apg;
    }

    public void setApg(double apg) {
        this.apg = apg;
    }

    public double getRpg() {
        return rpg;
    }

    public void setRpg(double rpg) {
        this.rpg = rpg;
    }

    public double getSpg() {
        return spg;
    }

    public void setSpg(double spg) {
        this.spg = spg;
    }

    public double getBpg() {
        return bpg;
    }

    public void setBpg(double bpg) {
        this.bpg = bpg;
    }

    public double getTopg() {
        return topg;
    }

    public void setTopg(double topg) {
        this.topg = topg;
    }

    public double getMpg() {
        return mpg;
    }

    public void setMpg(double mpg) {
        this.mpg = mpg;
    }

    public OffsetDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(OffsetDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
} 
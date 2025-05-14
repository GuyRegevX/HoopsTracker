package hoops.api.services.teams;

import hoops.api.models.dtos.teams.TeamMetaDTO;
import hoops.api.models.dtos.teams.TeamStatsDTO;
import hoops.api.mappers.TeamMapper;
import hoops.api.models.entities.teams.Team;
import hoops.api.models.entities.teams.TeamStats;
import hoops.api.repositories.teams.TeamsRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TeamsServiceImpl implements TeamsService {
    private static final Logger log = LoggerFactory.getLogger(TeamsServiceImpl.class);
    
    private final TeamsRepository teamsRepository;
    private final TeamMapper teamMapper;

    public TeamsServiceImpl(TeamsRepository teamsRepository, TeamMapper teamMapper) {
        this.teamsRepository = teamsRepository;
        this.teamMapper = teamMapper;
    }

    @Override
    public List<TeamMetaDTO> getAllTeams() {
        log.info("Fetching all teams metadata");
        List<Team> teams = teamsRepository.getAllTeams();
        return teams.stream()
            .map(teamMapper::toTeamMetaDTO)
            .collect(Collectors.toList());
    }

    @Override
    public TeamStatsDTO getTeamStats(String teamId, String seasonId) {
        log.info("Fetching stats for team {} in season {}", teamId, seasonId);
        TeamStats stats = teamsRepository.getTeamStats(teamId, seasonId);
        return stats != null ? teamMapper.toTeamStatsDTO(stats) : null;
    }
} 
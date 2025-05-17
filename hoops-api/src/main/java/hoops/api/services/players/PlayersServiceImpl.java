package hoops.api.services.players;

import hoops.api.models.dtos.players.PlayerMetaDTO;
import hoops.api.models.dtos.players.PlayerStatsDTO;
import hoops.api.mappers.PlayerMapper;
import hoops.api.models.entities.players.Player;
import hoops.api.models.entities.players.PlayerStats;
import hoops.api.repositories.players.PlayersRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlayersServiceImpl implements PlayersService {
    private static final Logger log = LoggerFactory.getLogger(PlayersServiceImpl.class);
    
    private final PlayersRepository playersRepository;
    private final PlayerMapper playerMapper;

    public PlayersServiceImpl(PlayersRepository playersRepository, PlayerMapper playerMapper) {
        this.playersRepository = playersRepository;
        this.playerMapper = playerMapper;
    }

    @Override
    public List<PlayerMetaDTO> getAllPlayers() {
        log.info("Fetching all players metadata");
        List<Player> players = playersRepository.getAllPlayers();
        return players.stream()
            .map(playerMapper::toPlayerMetaDTO)
            .collect(Collectors.toList());
    }

    @Override
    public PlayerStatsDTO getPlayerStats(String playerId, String seasonId) {
        log.info("Fetching stats for player {} in season {}", playerId, seasonId);
        PlayerStats stats = playersRepository.getPlayerStats(playerId, seasonId);
        return stats != null ? playerMapper.toPlayerStatsDTO(stats) : null;
    }
} 
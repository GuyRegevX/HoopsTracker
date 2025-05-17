package hoops.api.controllers;

import hoops.api.models.dtos.players.PlayerMetaDTO;
import hoops.api.models.dtos.players.PlayerStatsDTO;
import hoops.api.services.players.PlayerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/players")
public class PlayersController {
    private final PlayerService playerService;

    public PlayersController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping
    public List<PlayerMetaDTO> getAllPlayers() {
        return playerService.getAllPlayers();
    }

    @GetMapping("/{playerId}/stats")
    public ResponseEntity<PlayerStatsDTO> getPlayerStats(
            @PathVariable String playerId,
            @RequestParam String seasonId) {
        PlayerStatsDTO stats = playerService.getPlayerStats(playerId, seasonId);
        if (stats == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(stats);
    }
} 
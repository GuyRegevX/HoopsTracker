package hoops.api.controllers;

import hoops.api.models.dtos.players.PlayerMetaDTO;
import hoops.api.models.dtos.players.PlayerStatsDTO;
import hoops.api.services.players.PlayersService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/players")
@Tag(name = "Players", description = "Player management APIs")
public class PlayersController {
    private static final Logger log = LoggerFactory.getLogger(PlayersController.class);
    private final PlayersService playersService;

    @Autowired
    public PlayersController(PlayersService playersService) {
        this.playersService = playersService;
    }

    @Operation(summary = "Get all players", description = "Retrieves metadata for all players")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved players list",
                    content = @Content(schema = @Schema(implementation = PlayerMetaDTO.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<List<PlayerMetaDTO>> getAllPlayers() {
        log.info("GET /api/v1/players");
        List<PlayerMetaDTO> players = playersService.getAllPlayers();
        return ResponseEntity.ok(players);
    }

    @Operation(summary = "Get player statistics")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved player stats",
                    content = @Content(schema = @Schema(implementation = PlayerStatsDTO.class))),
        @ApiResponse(responseCode = "404", description = "Player not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{playerId}/stats")
    public ResponseEntity<PlayerStatsDTO> getPlayerStats(
            @Parameter(description = "Player ID") @PathVariable("playerId") String playerId,
            @Parameter(description = "Season ID") @RequestParam(name = "seasonId", required = true) String seasonId) {
        log.info("GET /api/v1/players/{}/stats?seasonId={}", playerId, seasonId);
        PlayerStatsDTO stats = playersService.getPlayerStats(playerId, seasonId);
        return stats != null ? ResponseEntity.ok(stats) : ResponseEntity.notFound().build();
    }
} 
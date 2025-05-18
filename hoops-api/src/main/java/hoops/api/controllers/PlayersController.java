package hoops.api.controllers;

import hoops.api.models.dtos.players.PlayerMetaDTO;
import hoops.api.models.dtos.players.PlayerStatsDTO;
import hoops.api.services.players.PlayerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/v1/players")
@Tag(name = "Players", description = "Player management and statistics API")
public class PlayersController {
    private final PlayerService playerService;

    public PlayersController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @Operation(
        summary = "Get all players",
        description = "Retrieves a list of all players with their metadata information"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved list of players",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PlayerMetaDTO.class)
            )
        )
    })
    @GetMapping
    public List<PlayerMetaDTO> getAllPlayers() {
        return playerService.getAllPlayers();
    }

    @Operation(
        summary = "Get player statistics",
        description = "Retrieves statistics for a specific player in a given season"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved player statistics",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PlayerStatsDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Player or season not found",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid player ID or season ID",
            content = @Content
        )
    })
    @GetMapping("/{playerId}/stats")
    public ResponseEntity<PlayerStatsDTO> getPlayerStats(
            @Parameter(description = "ID of the player to retrieve stats for", required = true)
            @PathVariable String playerId,
            @Parameter(description = "ID of the season to retrieve stats from", required = true)
            @RequestParam String seasonId) {
        PlayerStatsDTO stats = playerService.getPlayerStats(playerId, seasonId);
        if (stats == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(stats);
    }
} 
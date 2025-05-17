package hoops.api.controllers;

import hoops.api.models.dtos.teams.TeamMetaDTO;
import hoops.api.models.dtos.teams.TeamStatsDTO;
import hoops.api.services.teams.TeamsService;
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
@RequestMapping("/api/v1/teams")
@Tag(name = "Teams", description = "Team management APIs")
public class TeamsController {
    private static final Logger log = LoggerFactory.getLogger(TeamsController.class);
    private final TeamsService teamsService;

    @Autowired
    public TeamsController(TeamsService teamsService) {
        this.teamsService = teamsService;
    }

    @Operation(summary = "Get all teams", description = "Retrieves metadata for all teams")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved teams list",
                    content = @Content(schema = @Schema(implementation = TeamMetaDTO.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<List<TeamMetaDTO>> getAllTeams() {
        log.info("GET /api/v1/teams");
        List<TeamMetaDTO> teams = teamsService.getAllTeams();
        return ResponseEntity.ok(teams);
    }

    @Operation(summary = "Get team statistics")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved team stats",
                    content = @Content(schema = @Schema(implementation = TeamStatsDTO.class))),
        @ApiResponse(responseCode = "404", description = "Team not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{teamId}/stats")
    public ResponseEntity<TeamStatsDTO> getTeamStats(
            @Parameter(description = "Team ID") @PathVariable("teamId") String teamId,
            @Parameter(description = "Season ID") @RequestParam(required = true) String seasonId) {
        log.info("GET /api/v1/teams/{}/stats?seasonId={}", teamId, seasonId);
        TeamStatsDTO stats = teamsService.getTeamStats(teamId, seasonId);
        return stats != null ? ResponseEntity.ok(stats) : ResponseEntity.notFound().build();
    }
} 
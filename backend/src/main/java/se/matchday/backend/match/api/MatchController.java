package se.matchday.backend.match.api;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.matchday.backend.match.application.MatchQueryService;

@RestController
@RequestMapping("/api/v1/matches")
class MatchController {

  private final MatchQueryService matchQueryService;

  MatchController(MatchQueryService matchQueryService) {
    this.matchQueryService = matchQueryService;
  }

  @GetMapping
  ResponseEntity<List<MatchResponse>> listMatches() {
    List<MatchResponse> matches =
        matchQueryService.listMatches().stream().map(MatchResponse::from).toList();
    return ResponseEntity.ok(matches);
  }
}

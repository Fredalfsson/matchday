package se.matchday.backend.circle.api;

import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.matchday.backend.circle.application.CircleCreationResult;
import se.matchday.backend.circle.application.CircleCreationService;
import se.matchday.backend.circle.application.CircleMembershipService;
import se.matchday.backend.circle.application.CircleMembershipStatus;
import se.matchday.backend.circle.application.CircleQueryService;

@RestController
@RequestMapping("/api/v1/matches/{matchId}/circle")
class CircleController {

  private final CircleCreationService circleCreationService;
  private final CircleQueryService circleQueryService;
  private final CircleMembershipService circleMembershipService;

  CircleController(
      CircleCreationService circleCreationService,
      CircleQueryService circleQueryService,
      CircleMembershipService circleMembershipService) {
    this.circleCreationService = circleCreationService;
    this.circleQueryService = circleQueryService;
    this.circleMembershipService = circleMembershipService;
  }

  @GetMapping
  ResponseEntity<CircleResponse> getCircle(@PathVariable UUID matchId) {
    CircleMembershipStatus status = circleQueryService.findForMatch(matchId);
    return ResponseEntity.ok(CircleResponse.from(status));
  }

  @PostMapping
  ResponseEntity<CircleResponse> createCircle(@PathVariable UUID matchId) {
    CircleCreationResult result = circleCreationService.createForMatch(matchId);
    URI location = URI.create("/api/v1/matches/" + matchId + "/circle");
    return ResponseEntity.created(location).body(CircleResponse.from(result));
  }

  @PutMapping("/membership")
  ResponseEntity<Void> joinCircle(@PathVariable UUID matchId) {
    circleMembershipService.joinForMatch(matchId);
    return ResponseEntity.noContent().build();
  }
}

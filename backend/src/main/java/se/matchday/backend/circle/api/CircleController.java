package se.matchday.backend.circle.api;

import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.matchday.backend.circle.application.CircleCreationService;
import se.matchday.backend.circle.domain.Circle;

@RestController
@RequestMapping("/api/v1/matches/{matchId}/circle")
class CircleController {

  private final CircleCreationService circleCreationService;

  CircleController(CircleCreationService circleCreationService) {
    this.circleCreationService = circleCreationService;
  }

  @PostMapping
  ResponseEntity<CircleResponse> createCircle(@PathVariable UUID matchId) {
    Circle circle = circleCreationService.createForMatch(matchId);
    URI location = URI.create("/api/v1/matches/" + matchId + "/circle");
    return ResponseEntity.created(location).body(CircleResponse.fromCreatedCircle(circle));
  }
}

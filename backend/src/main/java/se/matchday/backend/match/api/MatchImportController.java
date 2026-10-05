package se.matchday.backend.match.api;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.matchday.backend.match.application.SeasonMatchImportResult;
import se.matchday.backend.match.application.SeasonMatchImporter;

@RestController
@RequestMapping("/api/v1/admin/match-imports")
class MatchImportController {

  private final SeasonMatchImporter seasonMatchImporter;

  MatchImportController(SeasonMatchImporter seasonMatchImporter) {
    this.seasonMatchImporter = seasonMatchImporter;
  }

  @PostMapping
  ResponseEntity<MatchImportResponse> importSeason(@Valid @RequestBody MatchImportRequest request) {
    SeasonMatchImportResult result = seasonMatchImporter.importSeason(request.season());
    return ResponseEntity.ok(MatchImportResponse.from(result));
  }
}

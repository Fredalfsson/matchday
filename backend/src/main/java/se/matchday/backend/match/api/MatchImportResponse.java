package se.matchday.backend.match.api;

import se.matchday.backend.match.application.SeasonMatchImportResult;

record MatchImportResponse(int season, int processedMatches) {

  static MatchImportResponse from(SeasonMatchImportResult result) {
    return new MatchImportResponse(result.season(), result.processedMatches());
  }
}

package se.matchday.backend.match.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import se.matchday.backend.match.application.ProviderMatch;
import se.matchday.backend.match.domain.Match;
import se.matchday.backend.match.domain.MatchStatus;

@Entity
@Table(name = "matches")
class MatchJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private @Nullable UUID id;

  @Column(name = "external_id", nullable = false, updatable = false, unique = true, length = 255)
  private String externalMatchId = "";

  @Column(nullable = false)
  private int season;

  @Column(nullable = false)
  private int round;

  @Column(name = "home_team_id", nullable = false, length = 255)
  private String homeTeamExternalId = "";

  @Column(name = "home_team_name", nullable = false, length = 255)
  private String homeTeamName = "";

  @Column(name = "away_team_id", nullable = false, length = 255)
  private String awayTeamExternalId = "";

  @Column(name = "away_team_name", nullable = false, length = 255)
  private String awayTeamName = "";

  @Column(name = "scheduled_date", nullable = false)
  private LocalDate scheduledDate = LocalDate.MIN;

  @Column(name = "kickoff_at")
  private @Nullable Instant kickoffAt;

  @Column(name = "home_score")
  private @Nullable Integer homeScore;

  @Column(name = "away_score")
  private @Nullable Integer awayScore;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private MatchStatus status = MatchStatus.UNKNOWN;

  @Column(name = "venue_name", length = 255)
  private @Nullable String venueName;

  protected MatchJpaEntity() {}

  MatchJpaEntity(ProviderMatch match) {
    this.externalMatchId = match.externalMatchId();
    updateFrom(match);
  }

  String externalMatchId() {
    return externalMatchId;
  }

  void updateFrom(ProviderMatch match) {
    if (!externalMatchId.equals(match.externalMatchId())) {
      throw new IllegalArgumentException("externalMatchId cannot be changed");
    }
    season = match.season();
    round = match.round();
    homeTeamExternalId = match.homeTeamExternalId();
    homeTeamName = match.homeTeamName();
    awayTeamExternalId = match.awayTeamExternalId();
    awayTeamName = match.awayTeamName();
    scheduledDate = match.scheduledDate();
    kickoffAt = match.kickoffAt();
    homeScore = match.homeScore();
    awayScore = match.awayScore();
    status = match.status();
    venueName = match.venueName();
  }

  Match toDomain() {
    if (id == null) {
      throw new IllegalStateException("A persisted match must have an id");
    }
    return new Match(
        id,
        season,
        round,
        homeTeamName,
        awayTeamName,
        scheduledDate,
        kickoffAt,
        homeScore,
        awayScore,
        status,
        venueName);
  }
}

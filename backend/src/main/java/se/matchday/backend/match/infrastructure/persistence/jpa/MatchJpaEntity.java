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
import se.matchday.backend.match.domain.Match;
import se.matchday.backend.match.domain.MatchStatus;

@Entity
@Table(name = "matches")
class MatchJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private @Nullable UUID id;

  @Column(name = "external_id", nullable = false, updatable = false, unique = true, length = 255)
  private String externalId = "";

  @Column(nullable = false)
  private int season;

  @Column(nullable = false)
  private int round;

  @Column(name = "home_team_id", nullable = false, length = 255)
  private String homeTeamId = "";

  @Column(name = "home_team_name", nullable = false, length = 255)
  private String homeTeamName = "";

  @Column(name = "away_team_id", nullable = false, length = 255)
  private String awayTeamId = "";

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

  MatchJpaEntity(Match match) {
    this.externalId = match.externalId();
    updateFrom(match);
  }

  String externalId() {
    return externalId;
  }

  void updateFrom(Match match) {
    if (!externalId.equals(match.externalId())) {
      throw new IllegalArgumentException("externalId cannot be changed");
    }
    season = match.season();
    round = match.round();
    homeTeamId = match.homeTeamId();
    homeTeamName = match.homeTeamName();
    awayTeamId = match.awayTeamId();
    awayTeamName = match.awayTeamName();
    scheduledDate = match.scheduledDate();
    kickoffAt = match.kickoffAt();
    homeScore = match.homeScore();
    awayScore = match.awayScore();
    status = match.status();
    venueName = match.venueName();
  }

  Match toDomain() {
    return new Match(
        externalId,
        season,
        round,
        homeTeamId,
        homeTeamName,
        awayTeamId,
        awayTeamName,
        scheduledDate,
        kickoffAt,
        homeScore,
        awayScore,
        status,
        venueName);
  }
}

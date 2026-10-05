CREATE TABLE matches (
    id UUID PRIMARY KEY,
    external_id VARCHAR(255) NOT NULL UNIQUE CHECK (BTRIM(external_id) <> ''),
    season INTEGER NOT NULL CHECK (season > 0),
    round INTEGER NOT NULL CHECK (round > 0),
    home_team_id VARCHAR(255) NOT NULL CHECK (BTRIM(home_team_id) <> ''),
    home_team_name VARCHAR(255) NOT NULL CHECK (BTRIM(home_team_name) <> ''),
    away_team_id VARCHAR(255) NOT NULL CHECK (BTRIM(away_team_id) <> ''),
    away_team_name VARCHAR(255) NOT NULL CHECK (BTRIM(away_team_name) <> ''),
    scheduled_date DATE NOT NULL,
    kickoff_at TIMESTAMP WITH TIME ZONE,
    home_score INTEGER,
    away_score INTEGER,
    status VARCHAR(32) NOT NULL CHECK (
        status IN ('SCHEDULED', 'FINISHED', 'POSTPONED', 'UNKNOWN')
    ),
    venue_name VARCHAR(255) CHECK (venue_name IS NULL OR BTRIM(venue_name) <> ''),
    CONSTRAINT matches_scores_paired CHECK (
        (home_score IS NULL AND away_score IS NULL)
        OR (home_score IS NOT NULL AND away_score IS NOT NULL)
    ),
    CONSTRAINT matches_scores_non_negative CHECK (
        (home_score IS NULL OR home_score >= 0)
        AND (away_score IS NULL OR away_score >= 0)
    ),
    CONSTRAINT matches_status_result CHECK (
        (status = 'FINISHED' AND home_score IS NOT NULL)
        OR (status = 'SCHEDULED' AND home_score IS NULL)
        OR status IN ('POSTPONED', 'UNKNOWN')
    )
);

CREATE INDEX matches_scheduled_date_idx ON matches (scheduled_date);

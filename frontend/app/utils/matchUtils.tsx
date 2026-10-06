import { Match, MatchStatus } from '../lib/interfaces/matchInterface';

export function isMatch(value: unknown): value is Match {
  if (typeof value !== 'object' || value === null) {
    return false;
  }

  const match = value as Record<string, unknown>;
  return (
    typeof match.id === 'string' &&
    typeof match.season === 'number' &&
    typeof match.round === 'number' &&
    typeof match.homeTeamName === 'string' &&
    typeof match.awayTeamName === 'string' &&
    typeof match.scheduledDate === 'string' &&
    (typeof match.kickoffAt === 'string' || match.kickoffAt === null) &&
    Object.values(MatchStatus).includes(match.status as MatchStatus) &&
    (typeof match.homeScore === 'number' || match.homeScore === null) &&
    (typeof match.awayScore === 'number' || match.awayScore === null) &&
    (typeof match.venueName === 'string' || match.venueName === null)
  );
}

export enum MatchStatus {
  SCHEDULED = 'SCHEDULED',
  FINISHED = 'FINISHED',
  POSTPONED = 'POSTPONED',
  UNKNOWN = 'UNKNOWN',
}
export type Match = {
  id: string;
  season: number;
  round: number;
  homeTeamName: string;
  awayTeamName: string;
  scheduledDate: string;
  kickoffAt: string | null;
  status: MatchStatus;
  homeScore: number | null;
  awayScore: number | null;
  venueName: string | null;
};

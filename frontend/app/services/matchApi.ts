import { Match } from '../lib/interfaces/matchInterface';
import { isMatch } from '@/app/utils/matchUtils';

export async function getMatches(): Promise<Match[]> {
  const apiUrl = (process.env.API_URL ?? 'http://localhost:8080').replace(
    /\/$/,
    '',
  );
  const response = await fetch(`${apiUrl}/api/v1/matches`, {
    // next: { revalidate: 86_400 },
    cache: 'no-store',
  });

  if (!response.ok) {
    throw new Error(`Error, ${response.status}.`);
  }

  const data = await response.json();
  if (!Array.isArray(data) || !data.every(isMatch)) {
    throw new Error('Matchnings-API:t returnerade en ogiltig matchlista.');
  }
  return data;
}

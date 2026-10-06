import GamesComponent from '@/app/components/GamesComponent';
import { getMatches } from '@/app/services/matchApi';
import { Match } from '../lib/interfaces/matchInterface';

export default async function Dashboard() {
  let matches: Match[];

  try {
    const currentSeason = new Date().getFullYear();
    matches = (await getMatches()).filter(
      (match) => match.season === currentSeason,
    );
  } catch (error) {
    const message =
      error instanceof Error ? error.message : 'An unexpected error occurred.';

    return (
      <p role='alert' className='mx-auto mt-12 max-w-xl text-center text-error'>
        Matcherna kunde inte hämtas. {message}
      </p>
    );
  }

  const today = new Date().toISOString().slice(0, 10);
  const previousMatches = matches.filter(
    (match) => match.status === 'FINISHED' || match.scheduledDate < today,
  );
  const upcomingMatches = matches.filter(
    (match) => match.status !== 'FINISHED' && match.scheduledDate >= today,
  );

  return (
    <div className='flex items-start justify-around gap-4 px-4'>
      <GamesComponent heading='Tidigare Matcher' matches={previousMatches} />
      <GamesComponent heading='Kommande Matcher' matches={upcomingMatches} />
    </div>
  );
}

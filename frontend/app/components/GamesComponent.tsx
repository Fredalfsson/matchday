'use client';

import { useState } from 'react';
import { Match } from '../lib/interfaces/matchInterface';
import Card from './Card';

const MATCHES_PER_PAGE = 20;

interface GameProps {
  heading: string;
  matches: Match[];
}

export default function GamesComponent({ heading, matches }: GameProps) {
  const [visibleCount, setVisibleCount] = useState(MATCHES_PER_PAGE);
  const visibleMatches = matches.slice(0, visibleCount);

  return (
    <section className='flex min-w-0 flex-1 flex-col items-center'>
      <h2 className='flex justify-center py-5 border-b border-primary-dark/20'>
        {heading}
      </h2>
      <div className='mt-6 grid grid-cols-1 '>
        {matches.length === 0 ? (
          <p className='m-4 text-center text-sm text-muted'>
            Inga matcher att visa.
          </p>
        ) : (
          visibleMatches.map((match) => (
            <Card
              key={match.id}
              id={match.id}
              team1={match.homeTeamName}
              team2={match.awayTeamName}
              round={match.round}
              date={match.scheduledDate}
            />
          ))
        )}
        {visibleCount < matches.length && (
          <button
            type='button'
            className='m-4 justify-self-center rounded-full bg-primary px-5 py-2 text-sm font-semibold text-white hover:bg-primary-dark'
            onClick={() => setVisibleCount((count) => count + MATCHES_PER_PAGE)}
          >
            Fler matcher
          </button>
        )}
      </div>
    </section>
  );
}

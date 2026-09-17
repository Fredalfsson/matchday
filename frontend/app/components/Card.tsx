import Link from 'next/link';
import Wrapper from '@/app/components/Wrapper';

interface CardProps {
  round: string;
  team1: string;
  team2: string;
  id: number;
  date: string;
  isAuthenticated: boolean;
  hasConversation: boolean;
}
export default function Card({
  round,
  team1,
  team2,
  id,
  date,
  isAuthenticated = false,
  hasConversation = false,
}: CardProps) {
  return (
    <div className='flex flex-row'>
      <Wrapper>
        <div className='text-sm flex flex-row justify-between'>
          <p>Omgång {round}</p>
          <p>{date}</p>
        </div>
        <p>
          {team1} vs {team2}
        </p>
      </Wrapper>
      <Link
        href={isAuthenticated ? `/messages/${id}` : '/login'}
        className='dark:bg-primary-dark bg-primary/50 rounded-full p-3 text-center mb-auto mt-auto text-sm font-semibold'
      >
        {hasConversation ? 'Gå med i konversation' : 'Skapa konversation'}
      </Link>
    </div>
  );
}

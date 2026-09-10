import Wrapper from './Wrapper';

interface CardProps {
  round: string;
  team1: string;
  team2: string;
  date: string;
}
export default function Card({ round, team1, team2, date }: CardProps) {
  return (
    <Wrapper>
      <div className='text-sm flex flex-row justify-between'>
        <p>Omgång {round}</p>
        <p>{date}</p>
      </div>
      <p>
        {team1} vs {team2}
      </p>
    </Wrapper>
  );
}

import GamesComponent from '@/app/components/GamesComponent';

export default function Dashboard() {
  return (
    <div className='flex justify-around'>
      <GamesComponent heading='Tidigare Matcher' />
      <GamesComponent heading='Kommande Matcher' />
    </div>
  );
}

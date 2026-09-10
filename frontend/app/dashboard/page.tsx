import GamesComponent from '@/app/components/GamesComponent';

export default function Dashboard() {
  return (
    <div className='flex justify-around'>
      <GamesComponent name='Tidigare Matcher' />
      <GamesComponent name='Kommande Matcher' />
    </div>
  );
}

import GamesComponent from '../components/GamesComponent';

export default function Dashboard() {
  return (
    <div className='flex justify-around'>
      <GamesComponent name='Tidigare Matcher' link='previous' />
      <GamesComponent name='Kommande Matcher' link='upcoming' />
    </div>
  );
}

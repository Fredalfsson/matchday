import Card from './Card';

interface GameProps {
  name: string;
  data?: object;
}

export default function GamesComponent({ name }: GameProps) {
  return (
    <div className='text-4xl mt-20 flex flex-col justify-center align-middle'>
      <h2 className='flex justify-center py-5 border-b border-primary-dark/20'>
        {name}
      </h2>
      <div className='text-4xl mt-10 grid grid-cols-1 m-auto'>
        <Card team1='Hammarby' team2='AIK' round='5' date='2026-03-03' />
        <Card team1='Hammarby' team2='AIK' round='5' date='2026-03-03' />
        <Card team1='Hammarby' team2='AIK' round='5' date='2026-03-03' />
        <Card team1='Hammarby' team2='AIK' round='5' date='2026-03-03' />
        <Card team1='Hammarby' team2='AIK' round='5' date='2026-03-03' />
      </div>
    </div>
  );
}

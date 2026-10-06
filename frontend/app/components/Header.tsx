import Link from 'next/link';
import Wrapper from './Wrapper';

export default function Header() {
  return (
    <header className='flex justify-around m-5'>
      <Link href='/' className='border-b border-primary-dark/20 px-6 py-6'>
        <h1 className='text-center text-5xl font-bold tracking-tight text-primary-dark sm:text-6xl'>
          Matchday
        </h1>
      </Link>
      <div className='max-w-25 text-center opacity-50'>
        <Wrapper>
          <Link href='/login'>Logga in</Link>
        </Wrapper>
      </div>
    </header>
  );
}

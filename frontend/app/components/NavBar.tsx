import Link from 'next/link';

const NavBar = () => {
  return (
    <div className='flex'>
      <div className='w-full text-4xl mt-20 flex justify-around flex-row'>
        <Link href='/'>Tidigare Matcher</Link>
        <Link href='/'>Kommande Matcher</Link>
      </div>
      <Link
        href='/login'
        className='border flex justify-center pt-5 pb-auto rounded-full w-20 m-5 h-20'
      >
        Logga in
      </Link>
    </div>
  );
};

export default NavBar;

import Link from 'next/link';

const NavBar = () => {
  return (
    <div className='flex'>
      <div className='w-full text-4xl underline mt-20 dark:text-white flex justify-around flex-row'>
        <Link href='/'>Tidigare Matcher</Link>
        <Link href='/'>Kommande Matcher</Link>
      </div>
      <Link
        href='/login'
        className='border dark:border-gray-700 rounded-full w-20 m-5 h-20'
      >
        Logga in
      </Link>
    </div>
  );
};

export default NavBar;

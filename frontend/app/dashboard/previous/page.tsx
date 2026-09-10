// import Link from 'next/link';
// import Card from '../components/Card';

// export default function PreviousGames() {
//   return (
//     <div className='w-full text-4xl m-20 flex flex-col'>
//       <Link href='/dashboard/previous-games' className='ml-auto mr-50'>
//         Tidigare Matcher
//       </Link>
//       <div className='text-4xl mt-20 '>
//         <Card />
//         <Card />
//         <Card />
//       </div>
//     </div>
//   );
// }

import GamesComponent from '@/app/components/GamesComponent';

export default function PreviousGames() {
  return (
    <div className='flex flex-col justify-center align-middle'>
      <GamesComponent name='Tidigare Matcher' link='previous' />
    </div>
  );
}

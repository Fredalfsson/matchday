import Wrapper from '@/app/components/Wrapper';
import MOCK_MESSAGES from '@/app/_MOCKDATA/MOCK_MESSAGES.json';
import DisplayMessage from '@/app/components/DisplayMessage';
import { Message } from '@/app/lib/interfaces/messageInterface';

export default async function Conversation({
  params,
}: {
  params: Promise<{ id: number }>;
}) {
  const { id } = await params;
  const team_1 = 'Hammarby';
  const team_2 = 'AIK';

  //   Fetch chatData from Id in params
  const data: Message[] = MOCK_MESSAGES;

  return (
    <div className='flex flex-col items-center'>
      <h2 className='text-xl'>
        Konversation kring matchen mellan {team_1} och {team_2}
      </h2>
      <Wrapper>
        <ol className='md:min-h-150'>
          {data.map((m, i) => (
            <DisplayMessage
              key={i}
              username={m.username}
              date={m.date}
              message={m.message}
            />
          ))}
        </ol>

        <input className='bg-white'></input>
        <button className='rounded-full bg-primary'>Skicka</button>
      </Wrapper>
    </div>
  );
}

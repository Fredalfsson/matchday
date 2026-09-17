interface MessageProps {
  username: string;
  date: string;
  message: string;
}

export default function DisplayMessage({
  username,
  date,
  message,
}: MessageProps) {
  return (
    <div className='text-sm mt-3 border-b border-muted/30 p-2'>
      <div className='flex justify-between mb-1'>
        <li>{username}</li>
        <li className='text-muted'>{date}</li>
      </div>
      <li>{message}</li>
    </div>
  );
}

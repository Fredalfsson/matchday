type Props = {
  children: React.ReactNode;
  footer: React.ReactNode;
};

export default function AuthWrapper({ children, footer }: Props) {
  return (
    <div className='max-w-300 min-w-100 grid gap-5 mt-20 text-md dark:text-white'>
      {children}
      {footer}
    </div>
  );
}

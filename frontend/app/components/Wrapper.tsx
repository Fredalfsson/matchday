type Props = {
  children: React.ReactNode;
  footer?: React.ReactNode;
};

export default function Wrapper({ children, footer }: Props) {
  return (
    <div
      className={
        'm-4 grid w-full max-w-sm gap-4 rounded-2xl border border-primary-dark/15 bg-surface p-4 shadow-md shadow-primary-dark/10'
      }
    >
      {children}
      <div className='border-t border-muted/30 pt-4 text-center text-sm text-muted'>
        {footer}
      </div>
    </div>
  );
}

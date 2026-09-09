import type { Metadata } from 'next';
import { Geist, Geist_Mono } from 'next/font/google';
import './globals.css';
import Link from 'next/link';

const geistSans = Geist({
  variable: '--font-geist-sans',
  subsets: ['latin'],
});

const geistMono = Geist_Mono({
  variable: '--font-geist-mono',
  subsets: ['latin'],
});

export const metadata: Metadata = {
  title: 'Matchday',
  description:
    'Kalendarium för fotbollsmatcher där du även kan diskutera matcherna',
};

export default function RootLayout({ children }: LayoutProps<'/'>) {
  return (
    <html
      lang='sv'
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
    >
      <body className='min-h-full flex flex-col bg-background text-foreground'>
        <Link href='/' className='border-b border-primary-dark/20 px-6 py-6'>
          <h1 className='text-center text-5xl font-bold tracking-tight text-primary-dark sm:text-6xl'>
            Matchday
          </h1>
        </Link>

        {children}
      </body>
    </html>
  );
}

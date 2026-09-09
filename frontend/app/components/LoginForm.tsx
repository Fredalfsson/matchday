'use client';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import Link from 'next/link';
import { loginSchema, LoginFormValues } from '../lib/schemas/loginSchema';
import InputField from './InputField';
import AuthWrapper from './AuthWrapper';

export default function LoginForm() {
  const form = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    mode: 'onChange',
    defaultValues: { email: '', password: '' },
  });

  const submit = form.handleSubmit(async (data) => {
    // await login(data)
  });

  return (
    <AuthWrapper
      footer={
        <Link href='/register' className='hover:text-gray-700 flex flex-row'>
          Har du inget konto ännu?<p className='underline ml-2'> Klicka här</p>
        </Link>
      }
    >
      <InputField
        control={form.control}
        name='email'
        label='Enter email'
        placeholder='Email'
        type='email'
        autoComplete='email'
      />
      <InputField
        control={form.control}
        name='password'
        label='Enter password'
        placeholder='Lösenord'
        type='password'
        autoComplete='current-password'
      />
      <button
        type='button'
        disabled={!form.formState.isValid}
        onClick={submit}
        className='rounded-3xl border p-2 border-gray-400 bg-green-800 disabled:bg-gray-800'
      >
        Skicka
      </button>
    </AuthWrapper>
  );
}

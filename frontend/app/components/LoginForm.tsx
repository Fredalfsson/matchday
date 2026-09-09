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
        <Link
          href='/register'
          className='flex flex-row justify-center gap-1 hover:text-primary'
        >
          Har du inget konto ännu?
          <span className='font-semibold underline'>Klicka här</span>
        </Link>
      }
    >
      <InputField
        control={form.control}
        name='email'
        label='E-post'
        placeholder='E-post'
        type='email'
        autoComplete='email'
      />
      <InputField
        control={form.control}
        name='password'
        label='Lösenord'
        placeholder='Lösenord'
        type='password'
        autoComplete='current-password'
      />
      <button
        type='button'
        disabled={!form.formState.isValid}
        onClick={submit}
        className='rounded-lg max-h-15 bg-primary p-2 font-medium text-white dark:text-black transition-colors hover:bg-primary-dark disabled:cursor-not-allowed disabled:bg-muted'
      >
        Logga in
      </button>
    </AuthWrapper>
  );
}

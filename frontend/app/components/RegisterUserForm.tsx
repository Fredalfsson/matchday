'use client';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import Link from 'next/link';
import {
  registerUserSchema,
  RegisterUserFormValues,
} from '../lib/schemas/registerUserSchema';
import AuthWrapper from './AuthWrapper';
import InputField from './InputField';

export default function RegisterUserForm() {
  const form = useForm<RegisterUserFormValues>({
    resolver: zodResolver(registerUserSchema),
    mode: 'onChange',
    defaultValues: { email: '', password: '', confirmPassword: '' },
  });

  const submit = form.handleSubmit(async (data) => {
    // await register(data)
  });

  return (
    <AuthWrapper
      footer={
        <Link
          href='/login'
          className='flex flex-row justify-center gap-1 hover:text-primary'
        >
          Logga in
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
      />
      <InputField
        control={form.control}
        name='confirmPassword'
        label='Bekräfta lösenord'
        placeholder='Bekräfta lösenord'
        type='password'
      />
      <button
        type='button'
        disabled={!form.formState.isValid}
        onClick={submit}
        className='rounded-lg max-h-15 bg-primary p-2 font-medium text-white transition-colors hover:bg-primary-dark disabled:cursor-not-allowed disabled:bg-muted'
      >
        Skapa konto
      </button>
    </AuthWrapper>
  );
}

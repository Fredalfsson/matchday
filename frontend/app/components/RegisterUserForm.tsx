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
        <Link href='/login' className='hover:text-gray-700 underline'>
          Logga in
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
        autoComplete='new-password'
      />
      <InputField
        control={form.control}
        name='confirmPassword'
        label='Confirm password'
        placeholder='Bekräfta lösenord'
        type='password'
        autoComplete='new-password'
      />
      <button
        type='button'
        disabled={!form.formState.isValid}
        onClick={submit}
        className='rounded-3xl border p-2 border-white bg-blue-200 disabled:bg-blue-950'
      >
        Skicka
      </button>
    </AuthWrapper>
  );
}

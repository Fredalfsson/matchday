import { z } from 'zod';

export const emailField = z
  .email({ message: 'Ange en giltig mailadress' })
  .min(1, { message: 'Ange email' })
  .refine(
    (e) =>
      !e.includes('privaterelay.appleid.com') && !e.includes('@passinbox.com'),
    {
      message: 'Denna mailadress är inte godkänd',
    },
  );

export const passwordField = z
  .string()
  .min(1, { message: 'Ange lösenord' })
  .min(8, { message: 'Lösenordet måste vara minst 8 tecken' });

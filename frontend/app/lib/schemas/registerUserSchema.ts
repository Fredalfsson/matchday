import { z } from 'zod';
import { emailField, passwordField } from './authSchema';

export const registerUserSchema = z
  .object({
    email: emailField,
    password: passwordField,
    confirmPassword: z.string(),
  })
  .refine((d) => d.password === d.confirmPassword, {
    message: 'Lösenorden matchar inte',
    path: ['confirmPassword'],
  });
export type RegisterUserFormValues = z.infer<typeof registerUserSchema>;

import { z } from 'zod';
import { emailField, passwordField } from './authSchema';

export const loginSchema = z.object({
  email: emailField,
  password: passwordField,
});
export type LoginFormValues = z.infer<typeof loginSchema>;

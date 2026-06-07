import { z } from 'zod';

export const passwordChangeSchema = z.object({
  currentPassword: z.string().min(1, 'Senha atual obrigatória.'),
  nextPassword: z
    .string()
    .min(8, 'A nova senha deve conter no mínimo 8 caracteres.')
    .max(64, 'A nova senha deve conter no máximo 64 caracteres.'),
  confirmPassword: z.string().min(1, 'Confirmação obrigatória.'),
}).refine((data) => data.nextPassword === data.confirmPassword, {
  path: ['confirmPassword'],
  message: 'As senhas não conferem.',
});

export type PasswordChangeInput = z.infer<typeof passwordChangeSchema>;

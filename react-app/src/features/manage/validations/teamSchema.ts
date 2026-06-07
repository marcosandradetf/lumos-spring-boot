import { z } from 'zod';

export const teamSchema = z.object({
  teamName: z.string().min(1, 'Nome da equipe obrigatório.'),
  memberIds: z.array(z.string()).min(1, 'Selecione ao menos um membro.'),
  UFName: z.string().optional(),
  cityName: z.string().optional(),
  regionName: z.string().optional(),
  plate: z.string().optional(),
});

export type TeamInput = z.infer<typeof teamSchema>;

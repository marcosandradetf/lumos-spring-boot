import { z } from 'zod';

export const contractFormSchema = z.object({
  number: z.string().min(1, 'Número do contrato obrigatório.'),
  contractor: z.string().min(1, 'Contratante obrigatório.'),

  address: z
    .string()
    .min(1, 'Endereço obrigatório.')
    .refine((value) => {
      const hasStreet = /rua|avenida|travessa|alameda/i.test(value);
      const hasNumber = /\d/.test(value);
      return hasStreet && hasNumber;
    }, 'Endereço deve conter nome da rua e número.'),

  phone: z.string().min(1, 'Telefone obrigatório.'),
  cnpj: z.string().min(1, 'CNPJ obrigatório.'),

  companyId: z
    .number({ message: 'Empresa contratada obrigatória.' })
    .min(1, 'Empresa contratada obrigatória.'),

  ibgeCode: z.string().min(1, 'Código IBGE obrigatório.'),

  contractionDate: z
    .date()
    .nullable()
    .refine((date) => date !== null, {
      message: 'Data de contratação obrigatória.',
    }),

  dueDate: z
    .date()
    .nullable()
    .refine((date) => date !== null, {
      message: 'Data de vencimento obrigatória.',
    }),

  contractType: z.string().min(1, 'Tipo de contrato obrigatório.'),
});

export type ContractFormInput = z.infer<typeof contractFormSchema>;

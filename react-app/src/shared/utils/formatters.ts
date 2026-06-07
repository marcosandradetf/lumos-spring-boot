// src/shared/utils/formatters.ts

export const formatCurrency = (value: string): string => {
    if (!value) return '';
    return new Intl.NumberFormat('pt-BR', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
    }).format(parseFloat(value) / 100);
};

export const formatCPF = (value: string): string => {
    const v = value.replace(/\D/g, '');
    return v
        .replace(/(\d{3})(\d)/, '$1.$2')
        .replace(/(\d{3})(\d)/, '$1.$2')
        .replace(/(\d{3})(\d{1,2})$/, '$1-$2');
};

export const showPhoneFormatted = (value: string): string => {
    const v = value.replace(/\D/g, '');
    if (v.length === 11) {
        return v.replace(/(\d{2})(\d{5})(\d{4})/, '($1) $2-$3');
    } else if (v.length === 10) {
        return v.replace(/(\d{2})(\d{4})(\d{4})/, '($1) $2-$3');
    }
    return value; // Retorna o valor original se não for um telefone válido
};

export const capitalize = (value: string): string => {
    if (!value) return '';
    const lower = value.toLowerCase();
    return lower.charAt(0).toUpperCase() + lower.slice(1);
}

export const fmtDateToBrazilian = (s: string | undefined) => s ? new Date(s).toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', year: '2-digit', hour: '2-digit', minute: '2-digit' }) : '—';


export const normalizeFloatInput = (value: string) => {
  const normalizedSeparators = value.replace(/,/g, '.');
  const sanitized = normalizedSeparators.replace(/[^0-9.]/g, '');
  const [rawInt = '', ...rest] = sanitized.split('.');
  const rawDec = rest.join('');

  const intPart = rawInt.replace(/^0+(?=\d)/, '');

  if (sanitized.startsWith('.')) {
    return `0.${rawDec}`;
  }

  if (sanitized.includes('.')) {
    return `${intPart || '0'}.${rawDec}`;
  }

  return intPart;
};

export const formatDecimalForDisplay = (value: string | number | null | undefined) => {
  if (value === null || value === undefined || value === '') return '';

  const numericValue = typeof value === 'number'
    ? value
    : Number(String(value).replace(',', '.'));

  if (Number.isNaN(numericValue)) return String(value);

  return new Intl.NumberFormat('pt-BR', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 3,
  }).format(numericValue);
};

export const normalizePhoneDigits = (value: string) => value.replace(/\D/g, '');

export const formatPhone = (value: string) => {
  const digits = normalizePhoneDigits(value);
  if (digits.length <= 10) {
    return digits.replace(/(\d{2})(\d{4})(\d{0,4})/, (_, ddd, part1, part2) =>
      part2 ? `(${ddd}) ${part1}-${part2}` : `(${ddd}) ${part1}`,
    );
  }

  return digits.replace(/(\d{2})(\d{5})(\d{0,4})/, (_, ddd, part1, part2) =>
    part2 ? `(${ddd}) ${part1}-${part2}` : `(${ddd}) ${part1}`,
  );
};

// ... repita para CNPJ, Phone, Plate, etc.

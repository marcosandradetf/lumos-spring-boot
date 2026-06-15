import { useEffect, useMemo, useState } from 'react';
import { Tooltip, TooltipContent, TooltipTrigger } from './ui/tooltip';

interface AppNumberInputProps {
  value: number;
  onChange: (value: number) => void;
  min?: number;
  max?: number;
  minFractionDigits?: number;
  maxFractionDigits?: number;
  mode?: 'decimal' | 'currency';
  currency?: string;
  locale?: string;
  disabled?: boolean;
  placeholder?: string;
  className?: string;
  inputClassName?: string;
  showButtons?: boolean;
  step?: number;
  disabledTooltip?: string;
  onKeyDown?: (event: React.KeyboardEvent<HTMLInputElement>) => void;
  ref?: React.Ref<HTMLInputElement>;
}

function clamp(value: number, min?: number, max?: number): number {
  let next = value;

  if (typeof min === 'number' && next < min) {
    next = min;
  }

  if (typeof max === 'number' && next > max) {
    next = max;
  }

  return next;
}

function parsePtValue(raw: string): number {
  if (!raw.trim()) {
    return 0;
  }

  const cleaned = raw
    .replace(/\s/g, '')
    .replace(/R\$/gi, '')
    .replace(/\./g, '')
    .replace(/,/g, '.')
    .replace(/[^0-9.-]/g, '');

  const parsed = Number(cleaned);
  return Number.isFinite(parsed) ? parsed : 0;
}

function parseCurrencyLikeBank(
  raw: string,
  fractionDigits: number,
): number {
  const trimmed = raw.trim();
  if (!trimmed) {
    return 0;
  }

  const negative = trimmed.startsWith('-');
  const digits = trimmed.replace(/\D/g, '');
  if (!digits) {
    return 0;
  }

  const base = Number(digits) / (10 ** fractionDigits);
  if (!Number.isFinite(base)) {
    return 0;
  }

  return negative ? -base : base;
}

function toEditableValue(value: number): string {
  if (!Number.isFinite(value)) {
    return '0';
  }
  return String(value).replace('.', ',');
}

function sanitizeEditableValue(raw: string): string {
  return raw
    .replace(/[^\d,.-]/g, '')
    .replace(/\.(?=.*\.)/g, '')
    .replace(/,(?=.*,)/g, '');
}

export function AppNumberInput({
  value,
  onChange,
  min,
  max,
  minFractionDigits = 0,
  maxFractionDigits = 2,
  mode = 'decimal',
  currency = 'BRL',
  locale = 'pt-BR',
  disabled = false,
  placeholder,
  className = '',
  inputClassName = '',
  showButtons = false,
  step,
  disabledTooltip,
  onKeyDown,
  ref,
}: AppNumberInputProps) {
  const [focused, setFocused] = useState(false);
  const [text, setText] = useState('0');

  const formatter = useMemo(() => new Intl.NumberFormat(locale, {
    style: mode === 'currency' ? 'currency' : 'decimal',
    currency,
    minimumFractionDigits: minFractionDigits,
    maximumFractionDigits: maxFractionDigits,
  }), [currency, locale, maxFractionDigits, minFractionDigits, mode]);

  useEffect(() => {
    if (!focused) {
      setText(formatter.format(value || 0));
    }
  }, [focused, formatter, value]);

  const commit = (raw: string) => {
    const parsed = mode === 'currency'
      ? parseCurrencyLikeBank(raw, maxFractionDigits)
      : parsePtValue(raw);
    const rounded = Number(parsed.toFixed(maxFractionDigits));
    const next = clamp(rounded, min, max);

    onChange(next);
    setText(formatter.format(next));
  };

  const resolvedStep = step ?? (maxFractionDigits > 0 ? 0.01 : 1);

  const increment = (direction: 1 | -1) => {
    const base = Number.isFinite(value) ? value : 0;
    const next = clamp(Number((base + (resolvedStep * direction)).toFixed(maxFractionDigits)), min, max);
    onChange(next);
    setText(focused ? toEditableValue(next) : formatter.format(next));
  };

  const showDisabledTooltip = disabled && disabledTooltip !== undefined && disabledTooltip !== null;

  const inputElement = (
    <div className={`relative ${className}`}>
      <input
        ref={ref}
        type="text"
        inputMode={mode === 'currency' ? 'numeric' : 'decimal'}
        value={text}
        disabled={disabled}
        placeholder={placeholder}
        onFocus={() => {
          setFocused(true);
          setText(mode === 'currency' ? formatter.format(value || 0) : toEditableValue(value || 0));
        }}
        onBlur={(event) => {
          setFocused(false);
          commit(event.target.value);
        }}
        onKeyDown={(event) => {
          if (onKeyDown) {
            onKeyDown(event);
            return;
          }

          if (event.key === 'Enter') {
            commit((event.target as HTMLInputElement).value);
          }
          if (event.key === 'ArrowUp') {
            event.preventDefault();
            increment(1);
          }
          if (event.key === 'ArrowDown') {
            event.preventDefault();
            increment(-1);
          }
          
        }}
        onChange={(event) => {
          if (mode === 'currency') {
            const parsed = parseCurrencyLikeBank(event.target.value, maxFractionDigits);
            const rounded = Number(parsed.toFixed(maxFractionDigits));
            const next = clamp(rounded, min, max);
            onChange(next);
            setText(formatter.format(next));
            return;
          }

          console.log(event.target.value   );
          const editable = sanitizeEditableValue(event.target.value);
          setText(editable);
          onChange(clamp(Number(parsePtValue(editable).toFixed(maxFractionDigits)), min, max));
        }}
        className={[
          'w-full rounded-full border border-slate-200 dark:border-zinc-700 bg-white dark:bg-zinc-900 px-2 py-1 text-sm text-right outline-none focus:border-indigo-400',
          'disabled:opacity-60 disabled:cursor-not-allowed',
          mode === 'currency' ? 'font-semibold tabular-nums bg-emerald-50/40 dark:bg-emerald-950/20 border-emerald-200 dark:border-emerald-800 focus:border-emerald-500' : '',
          showButtons || onKeyDown ? 'pr-7' : '',
          inputClassName,
        ].join(' ')}
      />

      {onKeyDown && (
        <b
          onClick={() =>
            onKeyDown(
              new KeyboardEvent('keydown', {
                key: 'Enter',
                code: 'Enter',
                bubbles: true,
                cancelable: true,
              }) as unknown as React.KeyboardEvent<HTMLInputElement>
            )
          }
          className="text-xs text-zinc-700 dark:text-zinc-300 absolute right-0 mr-2 top-1/2 transform -translate-y-1/2">↵</b>
      )}

      {showButtons && (
        <div className="absolute inset-y-0 right-1 my-1 flex w-5 flex-col overflow-hidden rounded-md border border-slate-200 dark:border-zinc-700">
          <button
            type="button"
            tabIndex={-1}
            onMouseDown={(event) => event.preventDefault()}
            onClick={() => increment(1)}
            className="flex h-1/2 items-center justify-center bg-slate-50 text-[9px] text-slate-600 transition hover:bg-slate-100 dark:bg-zinc-800 dark:text-zinc-300 dark:hover:bg-zinc-700"
          >
            <i className="pi pi-chevron-up text-[8px]" />
          </button>
          <button
            type="button"
            tabIndex={-1}
            onMouseDown={(event) => event.preventDefault()}
            onClick={() => increment(-1)}
            className="flex h-1/2 items-center justify-center border-t border-slate-200 bg-slate-50 text-[9px] text-slate-600 transition hover:bg-slate-100 dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-300 dark:hover:bg-zinc-700"
          >
            <i className="pi pi-chevron-down text-[8px]" />
          </button>
        </div>
      )}
    </div>
  );

  return (
    <>
      {showDisabledTooltip ? (
        <Tooltip>
          <TooltipTrigger asChild>
            <span className="block">{inputElement}</span>
          </TooltipTrigger>
          <TooltipContent side="top" align="start" className="max-w-xs break-words">
            {disabledTooltip}
          </TooltipContent>
        </Tooltip>
      ) : inputElement }
      </>
  );


}

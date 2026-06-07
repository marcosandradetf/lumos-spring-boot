import { Popover, PopoverButton, PopoverPanel, Transition } from '@headlessui/react';
import { addMonths, addYears, format, isAfter, isBefore, isSameDay, startOfDay } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import { Fragment, useEffect, useMemo, useState } from 'react';
import { DayPicker, type Matcher } from 'react-day-picker';

interface AppDatePickerProps {
  value: Date | null;
  onChange: (date: Date | null) => void;
  minDate?: Date;
  maxDate?: Date;
  placeholder?: string;
  disabled?: boolean;
  buttonClassName?: string;
  panelClassName?: string;
}

export function AppDatePicker({
  value,
  onChange,
  minDate,
  maxDate,
  placeholder = 'Selecione uma data',
  disabled = false,
  buttonClassName = '',
  panelClassName = '',
}: AppDatePickerProps) {
  const [month, setMonth] = useState<Date>(value ?? new Date());

  useEffect(() => {
    if (value) setMonth(value);
  }, [value]);

  const normalizedMin = useMemo(() => (minDate ? startOfDay(minDate) : undefined), [minDate]);
  const normalizedMax = useMemo(() => (maxDate ? startOfDay(maxDate) : undefined), [maxDate]);

  const disabledMatcher = useMemo<Matcher | Matcher[] | undefined>(() => {
    if (normalizedMin && normalizedMax) {
      return [{ before: normalizedMin }, { after: normalizedMax }];
    }
    if (normalizedMin) return { before: normalizedMin };
    if (normalizedMax) return { after: normalizedMax };
    return undefined;
  }, [normalizedMax, normalizedMin]);

  const canGoPrev = normalizedMin ? !isBefore(addMonths(month, -1), addMonths(normalizedMin, -1)) : true;
  const canGoNext = normalizedMax ? !isAfter(addMonths(month, 1), addMonths(normalizedMax, 1)) : true;
  const canGoPrevYear = normalizedMin ? !isBefore(addYears(month, -1), addMonths(normalizedMin, -1)) : true;
  const canGoNextYear = normalizedMax ? !isAfter(addYears(month, 1), addMonths(normalizedMax, 1)) : true;

  const goMonth = (direction: -1 | 1) => {
    const next = addMonths(month, direction);
    if (direction < 0 && !canGoPrev) return;
    if (direction > 0 && !canGoNext) return;
    setMonth(next);
  };

  const goYear = (direction: -1 | 1) => {
    const next = addYears(month, direction);
    if (direction < 0 && !canGoPrevYear) return;
    if (direction > 0 && !canGoNextYear) return;
    setMonth(next);
  };

  return (
    <Popover className="relative">
      <PopoverButton
        disabled={disabled}
        className={[
          'flex w-full items-center justify-between gap-3 rounded-2xl border px-3 py-2 text-sm transition',
          'border-slate-200 bg-white text-slate-800 hover:bg-slate-50',
          'dark:border-white/20 dark:bg-zinc-900 dark:text-neutral-100 dark:hover:bg-black/40',
          'outline-none focus:border-indigo-400 transition-colors',
          'disabled:cursor-not-allowed disabled:opacity-60',
          buttonClassName,
        ].join(' ')}
      >
        <span className={value ? '' : 'text-slate-400 dark:text-zinc-500'}>
          {value ? format(value, 'dd/MM/yyyy', { locale: ptBR }) : placeholder}
        </span>
        <i className="pi pi-calendar text-xs text-slate-500 dark:text-zinc-400" />
      </PopoverButton>

      <Transition
        as={Fragment}
        enter="transition duration-150 ease-out"
        enterFrom="opacity-0 translate-y-1"
        enterTo="opacity-100 translate-y-0"
        leave="transition duration-120 ease-in"
        leaveFrom="opacity-100 translate-y-0"
        leaveTo="opacity-0 translate-y-1"
      >
        <PopoverPanel
          className={[
            'absolute left-0 z-[80] mt-2 w-[300px] rounded-2xl border p-3 shadow-2xl',
            'border-slate-200 bg-white',
            'dark:border-white/10 dark:bg-zinc-900',
            panelClassName,
          ].join(' ')}
        >
          <div className="mb-3 flex items-center justify-between">
            <button
              type="button"
              onClick={() => goMonth(-1)}
              disabled={!canGoPrev}
              className="inline-flex h-8 w-8 items-center justify-center rounded-lg text-slate-500 transition hover:bg-slate-100 hover:text-slate-700 disabled:opacity-30 dark:text-zinc-400 dark:hover:bg-slate-800 dark:hover:text-zinc-100"
            >
              <i className="pi pi-chevron-left text-xs" />
            </button>

            <div className="flex items-center gap-2 text-sm font-semibold text-slate-700 dark:text-zinc-200">
              <span className="capitalize">{format(month, 'MMMM', { locale: ptBR })}</span>
              <div className="flex items-center overflow-hidden rounded-lg">
                <span className="min-w-12 px-2 text-center tabular-nums">
                  {format(month, 'yyyy', { locale: ptBR })}
                </span>
                <div className="flex flex-col">
                  <button
                    type="button"
                    onClick={() => goYear(1)}
                    disabled={!canGoNextYear}
                    className="flex h-4 w-6 items-center justify-center text-slate-500 transition hover:bg-slate-100 hover:text-slate-700 disabled:opacity-30 dark:text-zinc-400 dark:hover:bg-slate-800 dark:hover:text-zinc-100"
                    aria-label="Avancar ano"
                  >
                    <i className="pi pi-chevron-up text-[9px]" />
                  </button>
                  <button
                    type="button"
                    onClick={() => goYear(-1)}
                    disabled={!canGoPrevYear}
                    className="flex h-4 w-6 items-center justify-center text-slate-500 transition hover:bg-slate-100 hover:text-slate-700 disabled:opacity-30 dark:text-zinc-400 dark:hover:bg-slate-800 dark:hover:text-zinc-100"
                    aria-label="Voltar ano"
                  >
                    <i className="pi pi-chevron-down text-[9px]" />
                  </button>
                </div>
              </div>
            </div>

            <button
              type="button"
              onClick={() => goMonth(1)}
              disabled={!canGoNext}
              className="inline-flex h-8 w-8 items-center justify-center rounded-lg text-slate-500 transition hover:bg-slate-100 hover:text-slate-700 disabled:opacity-30 dark:text-zinc-400 dark:hover:bg-slate-800 dark:hover:text-zinc-100"
            >
              <i className="pi pi-chevron-right text-xs" />
            </button>
          </div>

          <DayPicker
            mode="single"
            month={month}
            onMonthChange={setMonth}
            selected={value ?? undefined}
            onSelect={(selectedDate) => onChange(selectedDate ?? null)}
            locale={ptBR}
            showOutsideDays
            disabled={disabledMatcher}
            className="w-full dark:text-zinc-200"
            classNames={{
              chevron: 'hidden',
              month_caption: 'hidden',
              months: 'w-full',
              month: 'w-full',
              month_grid: 'w-full border-collapse',
              weekdays: 'grid grid-cols-7',
              weekday: 'text-center text-[11px] font-semibold uppercase text-slate-500 dark:text-zinc-500 py-1',
              week: 'grid grid-cols-7',
              day: 'flex items-center justify-center py-0.5',
              day_button:
                ['h-9 w-9 rounded-2xl text-sm text-slate-700',
                  'transition dark:text-zinc-200',
                  'dark:hover:bg-slate-800',
                  'hover:bg-gradient-to-r hover:from-blue-50 hover:to-indigo-50 hover:text-indigo-700 hover:ring-1 hover:ring-indigo-200',
                ].join(' '),
              selected:
                [
                  '[&>button]:!bg-gradient-to-r [&>button]:!from-blue-600 [&>button]:!to-indigo-500',
                  '[&>button]:!text-white [&>button]:shadow-md [&>button]:shadow-indigo-500/25',
                  '[&>button:hover]:!from-blue-600 [&>button:hover]:!to-indigo-500 [&>button:hover]:!text-white',
                  '[&>button:hover]:!ring-0 dark:[&>button]:!text-white',
                ].join(' '),
              outside: 'text-slate-300 dark:text-zinc-600',
              disabled: 'opacity-30 pointer-events-none',
              hidden: 'invisible',
            }}
            modifiersStyles={{
              selected: {
                borderRadius: '0.75rem',
              },
            }}
            modifiers={{
              selected: value ? (day) => isSameDay(day, value) : undefined,
            }}
          />
        </PopoverPanel>
      </Transition>
    </Popover>
  );
}

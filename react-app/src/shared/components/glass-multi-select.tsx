import {
  Listbox,
  ListboxButton,
  ListboxOption,
  ListboxOptions,
  Transition,
} from '@headlessui/react';
import { Fragment, useEffect, useMemo, useRef, useState, type RefObject } from 'react';

import type { GlassListboxOption } from './glass-list-box';

type Primitive = string | number | boolean | null;

type SummaryMode = 'chips' | 'count' | 'auto';

interface GlassMultiSelectProps<T extends Primitive> {
  value: T[];
  onChange: (value: T[]) => void;
  options: GlassListboxOption<T>[];
  placeholder?: string;
  disabled?: boolean;
  className?: string;
  buttonClassName?: string;
  optionsClassName?: string;
  emptyText?: string;
  ariaLabel?: string;
  summaryMode?: SummaryMode;
  maxChips?: number;
  search?: boolean;
  searchPlaceholder?: string;
  initialSearch?: string;
  inlineOptions?: boolean;
  autoScrollOnOpen?: boolean;
}

function normalize(value: string): string {
  return value
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .trim();
}

function AutoScrollOnOpen({
  open,
  enabled,
  targetRef,
}: {
  open: boolean;
  enabled: boolean;
  targetRef: RefObject<HTMLElement | null>;
}) {
  useEffect(() => {
    if (!open || !enabled) {
      return;
    }

    const scroll = () => {
      targetRef.current?.scrollIntoView({
        block: 'nearest',
        inline: 'nearest',
        behavior: 'smooth',
      });
    };

    const frame = requestAnimationFrame(scroll);
    const timer = window.setTimeout(scroll, 160);

    return () => {
      cancelAnimationFrame(frame);
      window.clearTimeout(timer);
    };
  }, [enabled, open, targetRef]);

  return null;
}

export function GlassMultiSelect<T extends Primitive>({
  value,
  onChange,
  options,
  placeholder = 'Selecione',
  disabled = false,
  className = '',
  buttonClassName = '',
  optionsClassName = '',
  emptyText = 'Nenhuma opção disponível',
  ariaLabel,
  summaryMode = 'auto',
  maxChips = 2,
  search = false,
  searchPlaceholder = 'Pesquisar...',
  initialSearch = '',
  inlineOptions = false,
  autoScrollOnOpen = true,
}: GlassMultiSelectProps<T>) {
  const [query, setQuery] = useState(initialSearch);
  const containerRef = useRef<HTMLDivElement>(null);
  const optionsRef = useRef<HTMLDivElement>(null);

  const selectedOptions = useMemo(
    () => options.filter((option) => value.some((selectedValue) => Object.is(selectedValue, option.value))),
    [options, value],
  );

  const filteredOptions = useMemo(() => {
    if (!search || !query.trim()) {
      return options;
    }

    const normalizedQueryTerms = normalize(query)
      .split(' ')
      .filter(Boolean);

    if (normalizedQueryTerms.length === 0) {
      return options;
    }

    return options.filter((option) => {
      const normalizedLabel = normalize(option.label);
      return normalizedQueryTerms.every((term) => normalizedLabel.includes(term));
    });
  }, [options, query, search]);

  const summaryText = useMemo(() => {
    if (selectedOptions.length === 0) {
      return placeholder;
    }

    if (summaryMode === 'count') {
      return `${selectedOptions.length} selecionado(s)`;
    }

    if (summaryMode === 'chips' || selectedOptions.length > maxChips) {
      if (summaryMode === 'chips' && selectedOptions.length <= maxChips) {
        return selectedOptions.map((option) => option.label).join(' | ');
      }

      return `${selectedOptions.length} selecionado(s)`;
    }

    return selectedOptions.map((option) => option.label).join(' | ');
  }, [maxChips, placeholder, selectedOptions, summaryMode]);

  return (
    <Listbox value={value} onChange={onChange} multiple disabled={disabled}>
      {({ open }) => (
        <div
          ref={containerRef}
          className={`relative ${className}`}
          onPointerDown={(event) => event.stopPropagation()}
          onWheel={(event) => event.stopPropagation()}
        >
          <AutoScrollOnOpen open={open} enabled={autoScrollOnOpen} targetRef={optionsRef} />
          <ListboxButton
            aria-label={ariaLabel}
            className={[
              'w-full rounded-full border border-neutral-200 dark:border-white/20 px-3 py-2 text-left text-sm text-slate-900 backdrop-blur-xl transition',
              'focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-400/70',
              'disabled:cursor-not-allowed disabled:opacity-60',
              'dark:text-zinc-100',
              buttonClassName,
            ].join(' ')}
          >
            {summaryMode !== 'count' && selectedOptions.length > 0 && selectedOptions.length <= maxChips ? (
              <span className="flex truncate items-center gap-1.5 pr-28">
                {selectedOptions.map((option) => (
                  <span
                    key={`${String(option.value)}-${option.label}`}
                    className="inline-flex max-w-full items-center rounded-full border border-cyan-500/30 bg-cyan-500/20 px-2 py-0.5 text-xs text-cyan-800 dark:text-cyan-100"
                  >
                    <span className="truncate">{option.label}</span>
                  </span>
                ))}
              </span>
            ) : (
              <span className={selectedOptions.length > 0 ? '' : 'text-slate-500 dark:text-zinc-400'}>
                {summaryText}
              </span>
            )}

            <span className="pointer-events-none absolute inset-y-0 right-3 flex items-center text-slate-500 dark:text-zinc-400">
              <i className={`pi ${open ? 'pi-chevron-up' : 'pi-chevron-down'} text-xs`} />
            </span>
          </ListboxButton>

          <Transition
            as={Fragment}
            leave="transition ease-in duration-100"
            leaveFrom="opacity-100"
            leaveTo="opacity-0"
          >
            <ListboxOptions
              ref={optionsRef}
              anchor={inlineOptions ? undefined : 'bottom start'}
              onPointerDown={(event) => event.stopPropagation()}
              onClick={(event) => event.stopPropagation()}
              onWheel={(event) => event.stopPropagation()}
              className={[
                inlineOptions
                  ? 'absolute left-0 right-0 top-full z-[9999] mt-1 w-full'
                  : 'z-[9999] mt-1 w-[var(--button-width)]',
                'overflow-hidden rounded-2xl border border-neutral-200 bg-white/50',
                'backdrop-blur-2xl shadow-xl dark:border-white/20 dark:bg-black/50',
                'focus:outline-none',
                optionsClassName,
              ].join(' ')}
            >
              {search && (
                <div className="px-2 py-2 backdrop-blur">
                  <div className="relative">
                    <input
                      type="text"
                      value={query}
                      onKeyDown={(event) => {
                        event.stopPropagation();
                      }}
                      onChange={(event) => setQuery(event.target.value)}
                      placeholder={searchPlaceholder}
                      className="w-full rounded-2xl border border-neutral-200 bg-transparent px-2 py-1.5 pr-8 text-sm text-slate-800 placeholder:text-slate-500 focus:outline-none dark:border-white/10 dark:text-zinc-100 dark:placeholder:text-zinc-400"
                    />
                    <i className="pi pi-search absolute right-0 top-1/2 mr-4 -translate-y-1/2 text-xs text-blue-500 dark:text-blue-400" />
                  </div>
                </div>
              )}

              <div className="max-h-52 overflow-y-auto p-1" onWheel={(event) => event.stopPropagation()}>
                {filteredOptions.length === 0 ? (
                  <div className="px-3 py-2 text-sm text-slate-500 dark:text-zinc-400">{emptyText}</div>
                ) : (
                  filteredOptions.map((option) => (
                    <ListboxOption
                      key={`${String(option.value)}-${option.label}`}
                      value={option.value}
                      disabled={option.disabled}
                      className={[
                        'group relative flex cursor-default select-none items-center justify-between rounded-2xl px-3 py-2 text-sm text-slate-800 transition ',
                        'data-[focus]:bg-black/10 data-[focus]:text-slate-900 ',
                        'dark:data-[focus]:bg-white/20 dark:data-[focus]:text-slate-900 data-[disabled]:opacity-40 ',
                        'dark:text-zinc-100 dark:data-[focus]:text-zinc-100 ',
                        ''
                      ].join(' ')}
                    >
                      {({ selected: isSelected }) => (
                        <>
                          <span className={isSelected ? 'font-semibold' : ''}>{option.label}</span>
                          <i className={`pi ${isSelected ? 'pi-check-square' : 'pi-square'} text-xs ${isSelected ? 'text-blue-600 dark:text-blue-300' : 'text-slate-400 dark:text-zinc-500'}`} />
                        </>
                      )}
                    </ListboxOption>
                  ))
                )}
              </div>
            </ListboxOptions>
          </Transition>
        </div>
      )}
    </Listbox>
  );
}

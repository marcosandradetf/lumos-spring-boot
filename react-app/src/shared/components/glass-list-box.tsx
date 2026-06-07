import {
  Listbox,
  ListboxButton,
  ListboxOption,
  ListboxOptions,
  Transition,
} from '@headlessui/react';
import { Fragment, useEffect, useRef, type ReactNode } from 'react';
import { Tooltip, TooltipContent, TooltipTrigger } from '@/shared/components/ui/tooltip';

type Primitive = string | number | boolean | null;

export interface GlassListboxOption<T extends Primitive> {
  value: T;
  label: string;
  disabled?: boolean;
}

interface GlassListboxProps<T extends Primitive> {
  value: T;
  onChange: (value: T) => void;
  options: GlassListboxOption<T>[];
  placeholder?: string;
  disabled?: boolean;
  disabledTooltip?: ReactNode;
  className?: string;
  buttonClassName?: string;
  optionsClassName?: string;
  emptyText?: string;
  ariaLabel?: string;
  footerActionLabel?: string;
  footerActionIcon?: string;
  onFooterAction?: () => void;
  footerActionClassName?: string;
  searchable?: boolean;
  isLoading?: boolean;
  onOpenChange?: (open: boolean) => void;
}

interface GlassListboxContentProps<T extends Primitive>
  extends Omit<GlassListboxProps<T>, 'value' | 'onChange' | 'disabled'> {
  currentValue: T;
  open: boolean;
  selected?: GlassListboxOption<T>;
  disabledTooltip?: ReactNode;
  showDisabledTooltip: boolean;
}

function GlassListboxContent<T extends Primitive>({
  currentValue,
  open,
  selected,
  options,
  placeholder = 'Selecione',
  disabledTooltip,
  showDisabledTooltip,
  className = '',
  buttonClassName = '',
  optionsClassName = '',
  emptyText = 'Nenhuma opção disponível',
  ariaLabel,
  footerActionLabel,
  footerActionIcon = 'pi-plus',
  onFooterAction,
  footerActionClassName = '',
  searchable = false,
  isLoading = false,
  onOpenChange,
}: GlassListboxContentProps<T>) {
  const searchInputRef = useRef<HTMLInputElement>(null);
  const previousOpenRef = useRef<boolean | null>(null);

  useEffect(() => {
    if (open && searchable) {
      // Um timeout de 50ms dá tempo para a animação da Transition começar
      // e impede que o Headless UI roube o foco de volta.
      const timer = setTimeout(() => {
        searchInputRef.current?.focus();
      }, 50);

      return () => clearTimeout(timer);
    }
  }, [open, searchable]);

  useEffect(() => {
    if (!onOpenChange) return;

    if (previousOpenRef.current === null) {
      previousOpenRef.current = open;
      return;
    }

    if (previousOpenRef.current !== open) {
      onOpenChange(open);
      previousOpenRef.current = open;
    }
  }, [open, onOpenChange]);

  const listboxButton = (
    //TODO: Mudar a classe para deixar branco ao inves de cinza
    <ListboxButton
      aria-label={ariaLabel}
      className={[
        'w-full rounded-2xl border border-neutral-200 dark:border-white/20 px-3 py-2 text-left text-sm text-slate-900 backdrop-blur-xl transition',
        'focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-400/70',
        'disabled:cursor-not-allowed disabled:opacity-60',
        'dark:text-zinc-100',
        buttonClassName,
      ].join(' ')}
    >
      <span className={selected ? '' : 'text-slate-500 dark:text-zinc-400'}>
        {selected?.label ?? placeholder}
      </span>
      <span className="pointer-events-none absolute inset-y-0 right-3 flex items-center text-slate-500 dark:text-zinc-400">
        <i className={`pi text-blue-500 ${open ? 'pi-chevron-up' : isLoading ? 'pi-spin pi-spinner' : 'pi-chevron-down'} text-xs`} />
      </span>
    </ListboxButton>
  );

  return (
    <div className={`relative ${className}`}>
      {showDisabledTooltip ? (
        <Tooltip>
          <TooltipTrigger asChild>
            <span className="block">{listboxButton}</span>
          </TooltipTrigger>
          <TooltipContent side="top" align="start" className="max-w-xs break-words">
            {disabledTooltip}
          </TooltipContent>
        </Tooltip>
      ) : (
        listboxButton
      )}

      <Transition
        as={Fragment}
        leave="transition ease-in duration-100"
        leaveFrom="opacity-100"
        leaveTo="opacity-0"
      >
        <ListboxOptions
          // Mantemos a string padrão que o TS aceita.
          anchor="bottom start"
          className={[
            // Trava o tamanho máximo físico (max-h-52 = 208px, cabe cerca de 5 a 6 itens antes do scroll).
            // Usamos o !important (prefixo !) no max-h se o Headless UI estiver tentando recalcular a altura dinamicamente.
            'z-[9999] mt-1 h-fit !max-h-52 w-[var(--button-width)] overflow-auto rounded-2xl border bg-white/50 border-neutral-200 ', 
            'dark:border-white/20 dark:bg-black/50 p-1 backdrop-blur-2xl shadow-xl ',
            'focus:outline-none',
            optionsClassName,
          ].join(' ')}
        >
          {options.length === 0 ? (
            <div className="px-3 py-2 text-sm text-slate-500 dark:text-zinc-400">{emptyText}</div>
          ) : (
            <>
              {searchable && (
                <div className="px-1 py-1 relative">
                  <input
                    ref={searchInputRef}
                    onInput={(event) => {
                      const input = event.currentTarget;
                      const filter = input.value.toLowerCase();
                      const optionsContainer = input.parentElement?.parentElement;
                      if (!optionsContainer) return;

                      const optionElements = optionsContainer.querySelectorAll<HTMLElement>('[role="option"]');
                      optionElements.forEach((optionEl) => {
                        const text = optionEl.textContent?.toLowerCase() || '';
                        optionEl.style.display = text.includes(filter) ? '' : 'none';
                      });
                    }}
                    type="text"
                    placeholder="Pesquisar..."
                    className="px-2 border border-neutral-200 dark:border-white/10 w-full py-1.5 rounded-2xl bg-transparent text-sm text-slate-800 placeholder:text-slate-500 focus:outline-none dark:text-zinc-100 dark:placeholder:text-zinc-400"
                  />
                  <i className="pi pi-search absolute right-0 mr-4 top-1/2 -translate-y-1/2 text-xs text-blue-500 dark:text-blue-400" />
                </div>
              )}

              {options.map((option) => (
                <ListboxOption
                  key={`${String(option.value)}-${option.label}`}
                  value={option.value}
                  disabled={option.disabled}
                  className={[
                    'group relative flex cursor-default select-none items-center justify-between rounded-lg px-3 py-2 text-sm text-slate-800 transition',
                    'data-[focus]:bg-white/30 data-[focus]:text-slate-900',
                    'dark:data-[focus]:bg-white/20 dark:data-[focus]:text-slate-900 data-[disabled]:opacity-40',
                    'dark:text-zinc-100 dark:data-[focus]:text-zinc-100',
                  ].join(' ')}
                >
                  {({ selected: isSelected }) => (
                    <>
                      <span className={isSelected ? 'font-semibold' : ''}>{option.label}</span>
                      {isSelected && (
                        <i className="pi pi-check text-xs text-blue-600 dark:text-blue-300" />
                      )}
                    </>
                  )}
                </ListboxOption>
              ))}
            </>
          )}

          {footerActionLabel && onFooterAction && (
            <div className="mt-1 border-t border-neutral-200 pt-1 dark:border-white/20">
              <ListboxOption
                value={currentValue}
                as="button"
                type="button"
                onClick={onFooterAction}
                className={[
                  'flex w-full items-center justify-center gap-2 rounded-lg border border-neutral-200 bg-white/60 px-3 py-2 text-sm font-medium text-slate-700 transition',
                  'hover:bg-white/80 dark:border-white/10 dark:bg-white/5 dark:text-zinc-200 dark:hover:bg-white/10',
                  footerActionClassName,
                ].join(' ')}
              >
                <i className={`pi ${footerActionIcon} text-xs`} />
                {footerActionLabel}
              </ListboxOption>
            </div>
          )}
        </ListboxOptions>
      </Transition>
    </div>
  );
}

export function GlassListbox<T extends Primitive>({
  value,
  onChange,
  options,
  placeholder = 'Selecione',
  disabled = false,
  disabledTooltip,
  className = '',
  buttonClassName = '',
  optionsClassName = '',
  emptyText = 'Nenhuma opção disponível',
  ariaLabel,
  footerActionLabel,
  footerActionIcon = 'pi-plus',
  onFooterAction,
  footerActionClassName = '',
  searchable = false,
  isLoading = false,
  onOpenChange,
}: GlassListboxProps<T>) {
  const selected = options.find((option) => Object.is(option.value, value));
  const isDisabled = disabled || isLoading;
  const showDisabledTooltip = disabled && !isLoading && disabledTooltip !== undefined && disabledTooltip !== null;

  return (
    <Listbox value={value} onChange={onChange} disabled={isDisabled} >
      {({ open }) => (
        <GlassListboxContent
          currentValue={value}
          open={open}
          selected={selected}
          options={options}
          placeholder={placeholder}
          disabledTooltip={disabledTooltip}
          showDisabledTooltip={showDisabledTooltip}
          className={className}
          buttonClassName={buttonClassName}
          optionsClassName={optionsClassName}
          emptyText={emptyText}
          ariaLabel={ariaLabel}
          footerActionLabel={footerActionLabel}
          footerActionIcon={footerActionIcon}
          onFooterAction={onFooterAction}
          footerActionClassName={footerActionClassName}
          searchable={searchable}
          isLoading={isLoading}
          onOpenChange={onOpenChange}
        />
      )}
    </Listbox>
  );
}

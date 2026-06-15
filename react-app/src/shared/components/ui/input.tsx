import * as React from "react"
import { Tooltip, TooltipContent, TooltipTrigger } from "@/shared/components/ui/tooltip"

import { cn } from "@/shared/lib/utils"

type InputProps = React.ComponentProps<"input"> & {
  disabledTooltip?: string;
}

function Input(
  { 
    className, 
    type, 
    disabledTooltip,
    ...props
  }: InputProps,
) {
  
  const showDisabledTooltip = props.disabled && disabledTooltip !== undefined && disabledTooltip !== null; 

  const inputElement = (
    <input
      type={type}
      data-slot="input"
      // className={cn(
      //   "w-full rounded-full border border-slate-200 dark:border-zinc-700 bg-white dark:bg-zinc-900 px-3 py-2.5 text-sm text-slate-800 dark:text-zinc-100 placeholder:text-slate-400 outline-none focus:border-indigo-400 transition-colors",
      //   className
      // )}
      className={[
          'flex w-full items-center justify-between gap-3 rounded-full border px-3 py-2 text-sm transition',
          'border-slate-200 bg-white text-slate-800',
          'dark:border-white/20 dark:bg-zinc-900 dark:text-neutral-100',
          'placeholder:text-slate-400 outline-none focus:border-indigo-400 transition-colors',
          'disabled:cursor-not-allowed disabled:opacity-60',
          className,
        ].join(' ')}
      {...props}
    />
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

export { Input }

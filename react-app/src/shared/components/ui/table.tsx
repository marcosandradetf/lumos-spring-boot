import * as React from "react"
import { cn } from "@/shared/lib/utils"

function Table({ className, ...props }: React.ComponentProps<"table">) {
  return (
    <div
      data-slot="table-container"
      /* Container Premium: Borda ultrafina, sombra suave e fundo com leve transparência (se o pai tiver cor) */
      className="relative w-full overflow-x-auto rounded-xl border border-neutral-200/80 bg-white shadow-[0_2px_8px_-3px_rgba(0,0,0,0.05),0_8px_24px_-12px_rgba(0,0,0,0.05)] dark:border-zinc-800/80 dark:bg-zinc-950/40 dark:backdrop-blur-md"
    >
      <table
        data-slot="table"
        /* Font-smoothing melhora drasticamente a renderização da tipografia */
        className={cn("w-full caption-bottom text-sm subpixel-antialiased select-none", className)}
        {...props}
      />
    </div>
  )
}

function TableHeader({ className, ...props }: React.ComponentProps<"thead">) {
  return (
    <thead
      data-slot="table-header"
      /* Cabeçalho com contraste sutil e letras ligeiramente mais espaçadas para melhor legibilidade */
      className={cn(
        "bg-neutral-50/70 text-neutral-500 border-b border-neutral-200/60 font-medium tracking-wide uppercase text-[11px] dark:bg-zinc-900/40 dark:text-zinc-400 dark:border-zinc-800/60",
        className
      )}
      {...props}
    />
  )
}

function TableBody({ className, ...props }: React.ComponentProps<"tbody">) {
  return (
    <tbody
      data-slot="table-body"
      /* Linhas divisórias ultrafinas entre os dados */
      className={cn("divide-y divide-neutral-100 dark:divide-zinc-900", className)}
      {...props}
    />
  )
}

function TableRow({ className, ...props }: React.ComponentProps<"tr">) {
  return (
    <tr
      data-slot="table-row"
      /* Transição suave de cor e hover com opacidade controlada para evitar flash visual pesado */
      className={cn(
        "group transition-all duration-200 hover:bg-neutral-50/40 dark:hover:bg-zinc-900/30 data-[state=selected]:bg-neutral-50 dark:data-[state=selected]:bg-zinc-900/60",
        className
      )}
      {...props}
    />
  )
}

function TableHead({ className, ...props }: React.ComponentProps<"th">) {
  return (
    <th
      data-slot="table-head"
      /* Altura elegante e alinhamento preciso */
      className={cn(
        "h-10 px-4 text-left align-middle font-semibold whitespace-nowrap [&:has([role=checkbox])]:pr-0",
        className
      )}
      {...props}
    />
  )
}

function TableCell({ className, ...props }: React.ComponentProps<"td">) {
  return (
    <td
      data-slot="table-cell"
      /* Padding equilibrado e cor de texto principal refinada */
      className={cn(
        "p-4 align-middle whitespace-nowrap text-neutral-800 dark:text-zinc-200 [&:has([role=checkbox])]:pr-0",
        className
      )}
      {...props}
    />
  )
}

function TableFooter({ className, ...props }: React.ComponentProps<"tfoot">) {
  return (
    <tfoot
      data-slot="table-footer"
      className={cn(
        "border-t border-neutral-200 bg-neutral-50/50 font-medium text-neutral-900 dark:border-zinc-800 dark:bg-zinc-900/30 dark:text-zinc-100",
        className
      )}
      {...props}
    />
  )
}

function TableCaption({ className, ...props }: React.ComponentProps<"caption">) {
  return (
    <caption
      data-slot="table-caption"
      className={cn("mt-4 text-xs text-neutral-400 dark:text-zinc-500", className)}
      {...props}
    />
  )
}

export {
  Table,
  TableHeader,
  TableBody,
  TableFooter,
  TableHead,
  TableRow,
  TableCell,
  TableCaption,
}
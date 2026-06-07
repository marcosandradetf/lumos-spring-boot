import React from 'react';

// 1. Tipagem exata das chaves para evitar erros de compilação
type SeverityType = 'success' | 'error' | 'warn' | 'info' | 'indigo';

// 2. Cores ajustadas: tons mais escuros de hover e bg para os botões fazerem sentido como ações (UX Premium)
const severityColors: Record<SeverityType, string> = {
    success: 'bg-green-600 hover:bg-green-500 text-white',
    error: 'bg-red-600 hover:bg-red-500 text-white',
    warn: 'bg-amber-500 hover:bg-amber-400 text-white', // Amber/Yellow escuro para contraste
    info: 'bg-blue-600 hover:bg-blue-500 text-white',
    indigo: 'bg-indigo-600 hover:bg-indigo-500 text-white',
};

// Cores dinâmicas para o Badge (Contador) baseado no botão
const badgeColors: Record<SeverityType, string> = {
    success: 'bg-green-800 text-green-100',
    error: 'bg-red-800 text-red-100',
    warn: 'bg-amber-700 text-amber-100',
    info: 'bg-blue-800 text-blue-100',
    indigo: 'bg-white text-indigo-700', // Mantém o seu padrão original para o indigo
};

interface ButtonProps {
    description: string;
    severity?: SeverityType;
    icon?: string;
    count?: number;
    type?: 'button' | 'submit' | 'reset';
    onClick?: (e: React.MouseEvent<HTMLButtonElement, MouseEvent>) => void;
}

export function Button({
    description,
    severity,
    icon,
    count,
    type = 'button', // Default parameter direto na desestruturação
    onClick,
}: ButtonProps) {
    
    // Fallback elegante caso nenhuma severity seja informada
    const activeSeverity = severity || 'indigo';

    return (
        <button 
            onClick={(e) => {
                e.stopPropagation();
                onClick?.(e);
            }}
            type={type}
            className={`
                flex items-center gap-2 rounded-full px-5 py-3 text-sm font-semibold 
                shadow-md hover:shadow-lg active:scale-[0.98]
                transition-all duration-200 select-none
                ${severityColors[activeSeverity]}
            `}>
            {icon && <i className={`${icon} text-base`} />}
            <span>{description}</span>
            
            {/* Correção do bug do count (0 não renderiza mais uma bola vazia) */}
            {typeof count === 'number' && count > 0 && (
                <span className={`
                    flex h-5 min-w-5 items-center justify-center rounded-full px-1
                    text-[10px] font-bold animate-fade-in shadow-sm
                    ${badgeColors[activeSeverity]}
                `}>
                    {count}
                </span>
            )}
        </button>
    );
}
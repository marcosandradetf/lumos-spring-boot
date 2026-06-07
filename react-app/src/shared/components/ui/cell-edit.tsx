export function CellEdit(
    {
        onClick, 
        children, 
        disabled
    }: {
        onClick: () => void; 
        children: React.ReactNode; 
        disabled?: boolean
    }
) {
    return disabled ? 
        <>{children}</>
    : (
        <div
            onClick={() => onClick()}
            className="relative cursor-pointer hover:border hover:border-dotted hover:border-blue-500 dark:hover:border-blue-700 hover:bg-slate-200 dark:hover:bg-gray-700 p-1 rounded group ">
            <i className="pi pi-pencil text-[10px] absolute -top-1 -right-1 text-blue-400 dark:text-blue-700 opacity-0 group-hover:opacity-100 transition-opacity" />
            {children}
        </div>
    );
}
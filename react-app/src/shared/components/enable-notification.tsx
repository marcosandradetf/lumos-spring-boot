
type EnableNotificationProps = {
    statusMeta: {
        icon: string;
        title: string;
        detail: string;
        action: string;
    };
    clickAction?: () => void;
    className?: string;
    detail?: string;
};

const EnableNotification = (
    { statusMeta, clickAction, className, detail }: EnableNotificationProps
) => {
    return (
        <div className={`rounded-2xl border border-slate-200 bg-white p-4 shadow-sm dark:border-zinc-800 dark:bg-zinc-900 ${className || ''}`}>
            <div className="flex items-center justify-center">
                <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-slate-100 dark:bg-zinc-800">
                    <i className={`${statusMeta.icon} text-base`} />
                </div>
            </div>
            <h3 className="mt-3 text-center text-sm font-semibold text-slate-800 dark:text-zinc-100">
                {statusMeta.title}
            </h3>
            <p className="mt-2 text-center text-xs leading-relaxed text-slate-500 dark:text-zinc-400">
                {detail || statusMeta.detail}
            </p>
            <div className="mt-4 flex items-center justify-end gap-2">
                <button
                    type="button"
                    className="rounded-full bg-gradient-to-r from-blue-600 to-cyan-500 px-3 py-2 text-xs font-semibold text-white shadow-sm hover:brightness-110"
                    onClick={clickAction}
                >
                    {statusMeta.action}
                </button>
            </div>
        </div>
    );
};

export default EnableNotification;
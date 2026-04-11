export type ShareRequest = {
    message: string;
    title?: string;
    subject?: string;
    whatsappPhone?: string;
    file?: File;
    fileName?: string;
};

export type ShareOptions = {
    title?: string;
    subject?: string;
    whatsappPhone?: string;
    files?: File[];
    fileName?: string;
    onDownload?: () => void;
};

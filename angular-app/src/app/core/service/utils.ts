export class Utils {
    static normalizeString(value: string): string {
        return value
            .normalize('NFD')                 // separa acentos
            .replace(/[\u0300-\u036f]/g, '')  // remove acentos
            .replace(/\s+/g, '_')             // espaço → underline
            .replace(/[^a-zA-Z0-9_]/g, '')    // remove caracteres especiais
            .toLowerCase();
    }

    static formatNowToDDMMYYHHmm(formatted: boolean = false): string {
        const now = new Date();

        const pad = (n: number) => n.toString().padStart(2, '0');

        const dd = pad(now.getDate());
        const mm = pad(now.getMonth() + 1);
        const yy = pad(now.getFullYear() % 100);
        const hh = pad(now.getHours());
        const min = pad(now.getMinutes());

        return formatted ? `${dd}/${mm}/${yy} ${hh}:${min}` : `${dd}${mm}${yy}${hh}${min}`;
    }

    static diffInDays(start: Date, end: Date): number {
        const msPerDay = 1000 * 60 * 60 * 24;
        const diff = end.getTime() - start.getTime();
        return Math.floor(diff / msPerDay);
    }


}

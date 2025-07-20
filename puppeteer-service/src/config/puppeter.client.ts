import puppeteer, { Browser } from 'puppeteer-core';

let browser: Browser | null = null;
let inactivityTimer: NodeJS.Timeout | null = null;
const INACTIVITY_TIMEOUT = 60 * 1000; // 1 minuto

export const getBrowser = async (): Promise<Browser> => {
  if (!browser) {
    browser = await puppeteer.launch({
      executablePath: '/usr/bin/google-chrome', // ou /usr/bin/chromium dependendo do SO
      headless: 'new' as any,
      args: [
        '--no-sandbox',
        '--disable-setuid-sandbox',
        '--disable-dev-shm-usage',
        '--disable-gpu',
      ]
    });
  }
  scheduleClose(); // Reinicia o timer
  return browser;
};

const scheduleClose = () => {
  if (inactivityTimer) clearTimeout(inactivityTimer);
  inactivityTimer = setTimeout(async () => {
    if (browser) {
      console.log('[Puppeteer] Encerrando por inatividade');
      await browser.close();
      browser = null;
    }
  }, INACTIVITY_TIMEOUT);
};

export const closeBrowser = async () => {
  if (inactivityTimer) clearTimeout(inactivityTimer);
  if (browser) {
    console.log('[Puppeteer] Fechando manualmente');
    await browser.close();
    browser = null;
  }
};

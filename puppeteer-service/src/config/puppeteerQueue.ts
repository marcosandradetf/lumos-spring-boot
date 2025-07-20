import PQueue from 'p-queue';
import { Browser } from 'puppeteer-core';
import { getBrowser } from './puppeter.client';

const queue = new PQueue({ concurrency: 2 }); // 👈  PDF por vez

export const enqueuePdfTask = async <T>(task: (browser: Browser) => Promise<T>): Promise<T> => {
  return queue.add(async () => {
    const browser = await getBrowser();
    return task(browser);
  }) as Promise<T>;
};

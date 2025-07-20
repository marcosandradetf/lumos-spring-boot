import { minioClient } from "../config/minio.client";
import { Readable } from 'stream';
import { DateTime } from 'luxon';

export const getFileAsBase64 = async (
  bucket: string,
  objectName: string
): Promise<string> => {
  try {
    const dataStream: Readable = await minioClient.getObject(bucket, objectName);

    const chunks: Buffer[] = [];

    for await (const chunk of dataStream) {
      chunks.push(chunk as Buffer);
    }

    const buffer = Buffer.concat(chunks);
    return buffer.toString('base64');
  } catch (err) {
    throw new Error(`Erro ao buscar objeto ${objectName}: ${(err as Error).message}`);
  }
};

export const toSaoPauloTime = (dateStr: string) =>
  DateTime.fromISO(dateStr, { zone: 'utc' }).setZone('America/Sao_Paulo');

  
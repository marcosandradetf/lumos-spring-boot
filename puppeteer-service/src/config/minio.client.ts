import { Client } from 'minio';

export const minioClient = new Client({
  endPoint: process.env.MINIO_ENDPOINT || 'minio',
  port: parseInt(process.env.MINIO_PORT || '9000'),
  useSSL: false,
  accessKey: process.env.MINIO_ACCESS_KEY || 'admin',
  secretKey: process.env.MINIO_SECRET_KEY || 'minioadmin'
});

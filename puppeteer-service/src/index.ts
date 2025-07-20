import express from 'express';
import { authenticateToken } from './middlewares/auth.middleware';
import { closeBrowser } from './config/puppeter.client'
import { maintenanceReport } from './services/maintenance.service';
import cors from 'cors';
import { enqueuePdfTask } from './config/puppeteerQueue';


const app = express();
app.use(cors({
    origin: ['https://lumos.thryon.com.br', 'http://localhost:4200'],
    methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'Authorization'],
    credentials: true // <-- ESSENCIAL
}));
app.post('/generate-maintenance-pdf/:maintenanceId/:reportFile', authenticateToken, async (req, res) => {
    const { maintenanceId } = req.params;
    const { reportFile } = req.params;
    const streetIds = req.query.streets?.toString().split(',') ?? [];

    try {
        const html = await maintenanceReport(reportFile, maintenanceId, streetIds);

        if (!html) {
            console.log(html);
            return res.status(500).send('Falha ao gerar HTML do relatório');
        }

        const pdfBuffer = await enqueuePdfTask(async (browser) => {
            const page = await browser.newPage();
            await page.setContent(html, { waitUntil: 'networkidle0' });
        
            const buffer = await page.pdf({
                format: 'A4',
                printBackground: true,
                landscape: true,
                margin: { top: '10mm', bottom: '10mm', left: '10mm', right: '10mm' },
            });
        
            await page.close();
            return buffer;
        });

        res.writeHead(200, {
            'Content-Type': 'application/pdf',
            'Content-Disposition': 'inline; filename="report.pdf"',
            'Content-Length': Buffer.byteLength(pdfBuffer),
        });
        res.end(pdfBuffer);

    } catch (err: any) {
        res.status(500).json({ error: err.message });
    }
});


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server is running on port ${PORT}`);
});

const shutdown = async () => {
    console.log('\n[Server] Encerrando...');
    await closeBrowser();
    process.exit(0);
  };

process.on('SIGINT', shutdown);   // Para Ctrl+C
process.on('SIGTERM', shutdown);  // Para Docker stop / down
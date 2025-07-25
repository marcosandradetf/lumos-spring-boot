import express from 'express';
import { closeBrowser } from './config/puppeter.client'
import { enqueuePdfTask } from './config/puppeteerQueue';


const app = express();
app.use(express.json());
app.post('/generate-pdf',  async (req, res) => {
    const { html } = req.body;

    try {
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
import express from 'express';
import { closeBrowser } from './config/puppeter.client'
import { enqueuePdfTask } from './config/puppeteerQueue';

const app = express();
app.use(express.json({ limit: '100mb' }));
app.use(express.urlencoded({ limit: '100mb', extended: true }));

app.post('/generate-pdf', async (req, res) => {
    const { html, orientation = 'landscape', title = 'Relatório Operacional Consolidado' } = req.body;

    try {
        if (!html) {
            console.log(html);
            return res.status(400).send('HTML do relatório não foi fornecido');
        }

        if (!['landscape', 'portrait'].includes(orientation)) {
            return res.status(400).send('Orientação inválida. Use "landscape" ou "portrait".');
        }

        const pdfBuffer = await enqueuePdfTask(async (browser) => {
            const page = await browser.newPage();
            await page.setContent(html, { waitUntil: 'networkidle0' });

            const buffer = await page.pdf({
                format: 'A4',
                printBackground: true,
                landscape: orientation === 'landscape',

                displayHeaderFooter: true,

                margin: {
                    top: '10mm',
                    bottom: '25mm', // precisa aumentar por causa do footer
                    left: '10mm',
                    right: '10mm'
                },
                headerTemplate: `<div></div>`,

                footerTemplate: `
                    <div style="
                        width:100%;
                        font-size:10px;
                        padding:0 20px;
                        display:flex;
                        justify-content:space-between;
                        font-family:Arial;
                        color:#475569;
                    ">
                        <span>
                            <b>${title}</b>
                        </span>

                        <span>
                            Página <span class="pageNumber"></span>
                            de <span class="totalPages"></span>
                        </span>
                    </div>
                `
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
        console.error(err);
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
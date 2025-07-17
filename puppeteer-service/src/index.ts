import express, {Request, Response} from 'express';
import puppeteer from 'puppeteer';

const app = express();
app.use(express.json());

app.post('/generate-pdf', async (req: Request, res: Response) => {
    const { url, html, options } = req.body;

    const browser = await puppeteer.launch({
        headless: 'new',
        args: ['--no-sandbox', '--disable-setuid-sandbox'],
    });
    const page = await browser.newPage();

    if (url) {
        await page.goto(url, { waitUntil: 'networkidle0' });
    } else if (html) {
        await page.setContent(html, { waitUntil: 'networkidle0' });
    } else {
        return res.status(400).json({ error: 'URL or HTML is required' });
    }

    const pdfBuffer = await page.pdf({
        format: 'A4',
        printBackground: true,
        ...options,
    });

    await browser.close();
    res.setHeader('Content-Type', 'application/pdf');
    res.send(pdfBuffer);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server is running on port ${PORT}`);
});
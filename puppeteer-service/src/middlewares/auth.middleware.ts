import jwt from 'jsonwebtoken';
import fs from 'fs'

function verifySign(token: string) {
    const publicKey = fs.readFileSync('/etc/keys/app.pub', 'utf8');
    return jwt.verify(token, publicKey, { algorithms: ['RS256'] });
}


export function authenticateToken(req: any, res: any, next: any) {
    const authHeader = req.headers['authorization'];
    const token = authHeader && authHeader.split(' ')[1]; // Bearer TOKEN
    
    if (!token) {
        return res.status(401).json({ error: 'Token não fornecido' });
    }

    try {
        const decoded = verifySign(token);
        req.user = decoded;
        next();
    } catch (error: any) {
        return res.status(401).json({ error: error.message });
    }
    
}
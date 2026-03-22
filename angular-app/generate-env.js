require('dotenv').config();
const fs = require('fs');

const isProd = process.argv.includes('--prod');

// 🔒 Função que valida e quebra o build
function getEnvOrThrow(name) {
  const value = process.env[name];

  if (!value || value.trim() === '') {
    throw new Error(`❌ Variável obrigatória não definida: ${name}`);
  }

  return value;
}

// 🔐 Aqui você garante que nunca builda sem chave
const googleMapsKey = isProd
  ? getEnvOrThrow('GOOGLE_MAPS_KEY_PROD')
  : getEnvOrThrow('GOOGLE_MAPS_KEY_DEV');

const localhost = getEnvOrThrow('LOCALHOST');
const api = getEnvOrThrow('API_HOST');

const content = `
export const environment = {
    production: ${isProd},
    springboot: ${isProd ? "'" + api + "'" : "'" + localhost + "'"},

    firebase: {
        apiKey: "AIzaSyAxDhw4uOmEoq-Yew4G-Zbe6K-5GDMzsCE",
        authDomain: "lumos-push.firebaseapp.com",
        projectId: "lumos-push",
        storageBucket: "lumos-push.firebasestorage.app",
        messagingSenderId: "37243759038",
        appId: "1:37243759038:web:47343c71f1c322ef7a31ef",
        measurementId: "G-WVZZ5PRZKY"
    },

    googleMapsApiKey: '${googleMapsKey}'
};
`;

fs.writeFileSync(
  `src/environments/environment${isProd ? '.prod' : ''}.ts`,
  content
);

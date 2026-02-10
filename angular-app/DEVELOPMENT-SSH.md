# Use proxy config
ng serve --host 0.0.0.0 --proxy-config proxy.conf.json

# Create tunnel to localhost:4200
cloudflared tunnel --url http://localhost:4200

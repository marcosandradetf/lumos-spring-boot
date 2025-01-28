#!/bin/sh

# Iniciar o primeiro aplicativo (app1) na porta 8081
nohup /myapp &

# Iniciar o segundo aplicativo (app2) na porta 8082
nohup /myapp2 &

# Manter o contêiner rodando
wait -n

package main

import (
	"fmt"
	"github.com/gin-contrib/cors"
	"github.com/gin-gonic/gin"
	"lumos-golang/api/middleware"
	"net/http"
	"os"
)

func main() {
	// Inicializando o router Gin
	router := gin.Default()

	// Configuração do middleware CORS
	router.Use(cors.New(cors.Config{
		AllowOrigins: []string{
			"http://192.168.3.100:4200", // Frontend local
			"http://localhost:4200",     // Frontend local
			"http://frontend:4200",      // Frontend dentro de Docker (se aplicável)
		},
		AllowMethods: []string{
			"GET", "POST", "OPTIONS", "PUT", "PATCH", "DELETE",
		}, // Métodos permitidos
		AllowHeaders: []string{
			"Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin", "X-Custom-Header",
		}, // Permite cabeçalhos necessários, incluindo 'Authorization'
		AllowCredentials: true, // Permite cookies e credenciais
		MaxAge:           3600, // Cache da preflight request
	}))

	// Usando o middleware de autenticação JWT em todas as rotas que precisam
	router.Use(middleware.AuthMiddleware())

	// Rota de download
	router.GET("/download/:filename", func(c *gin.Context) {
		// Recupera o nome do arquivo a ser baixado da URL
		filename := c.Param("filename")
		// Define o caminho do arquivo no volume compartilhado (NFS)
		filepath := fmt.Sprintf("/mnt/nfs/%s", filename)

		// Verifica se o arquivo existe
		_, err := os.Stat(filepath)
		if os.IsNotExist(err) {
			// Arquivo não encontrado
			c.JSON(http.StatusNotFound, gin.H{"error": "Arquivo não encontrado"})
			return
		} else if err != nil {
			// Caso ocorra algum erro inesperado
			c.JSON(http.StatusInternalServerError, gin.H{"error": fmt.Sprintf("Erro ao acessar o arquivo: %v", err)})
			return
		}

		// Inicia o download do arquivo
		c.File(filepath)
	})

	// Iniciar o servidor após todas as rotas estarem definidas
	if err := router.Run(":8081"); err != nil {
		fmt.Printf("Erro ao iniciar o servidor: %v\n", err)
	}
}

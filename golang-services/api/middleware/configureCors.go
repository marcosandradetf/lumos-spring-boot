package middleware

import (
	"github.com/gin-contrib/cors"
	"github.com/gin-gonic/gin"
)

func ConfigureCORS(router *gin.Engine) {
	// Configuração do middleware CORS
	router.Use(cors.New(cors.Config{
		AllowOrigins: []string{
			"http://192.168.3.100:4200", // Permite o frontend rodando no IP local da VM
			"http://localhost:4200",     // Permite o frontend local
			"http://frontend:4200",      // Permite o frontend dentro do container (caso esteja rodando dentro do Docker)
		},
		AllowMethods:     []string{"GET", "POST", "OPTIONS", "PUT", "PATCH", "DELETE"}, // Métodos permitidos
		AllowHeaders:     []string{"*"},                                                // Permite todos os cabeçalhos (cuidado, pois em produção pode ser mais restritivo)
		AllowCredentials: true,                                                         // Permite cookies e credenciais
		MaxAge:           3600,                                                         // Cache da preflight request
	}))

}

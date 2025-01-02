package middleware

import (
	"github.com/gin-contrib/cors"
	"github.com/gin-gonic/gin"
)

func ConfigureCORS(router *gin.Engine) {
	// Configuração do middleware CORS
	router.Use(cors.New(cors.Config{
		AllowOrigins: []string{
			"http://localhost:4200", // Frontend local
			"http://frontend:4200",  // Frontend dentro de Docker (se aplicável)
		},
		AllowMethods: []string{
			"GET", "POST", "OPTIONS", "PUT", "PATCH", "DELETE",
		}, // Métodos permitidos
		AllowHeaders: []string{
			"Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin", "X-Custom-Header",
		}, // Permite cabeçalhos necessários, incluindo 'Authorization'
		AllowCredentials: true, // Permite cookies e credenciais
		MaxAge:           3600, // Cache da preflight request                                                         // Cache da preflight request
	}))

}

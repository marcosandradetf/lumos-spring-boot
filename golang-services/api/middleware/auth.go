package middleware

import (
	"errors"
	"fmt"
	"github.com/gin-gonic/gin"
	"github.com/golang-jwt/jwt/v5"
	"io/ioutil"
	"net/http"
	"os"
	"path/filepath"
	"strings"
)

var publicKey []byte

func init() {
	// Obter o diretório home do usuário
	homeDir, err := os.UserHomeDir()
	if err != nil {
		fmt.Println("Erro ao obter o diretório home:", err)
		return
	}

	// Construir o caminho completo para o arquivo da chave pública
	publicKeyPath := filepath.Join(homeDir, ".ssh", "app.pub")

	// Ler o arquivo da chave pública
	publicKey, err = ioutil.ReadFile(publicKeyPath)
	if err != nil {
		fmt.Println("Erro ao ler a chave pública:", err)
		return
	}

	// Usar a chave pública (exemplo)
	fmt.Println("Chave pública:", string(publicKey))
}

func ValidateToken(tokenString string) (*jwt.Token, error) {
	key, err := jwt.ParseRSAPublicKeyFromPEM(publicKey)
	if err != nil {
		return nil, err
	}

	token, err := jwt.Parse(tokenString, func(token *jwt.Token) (interface{}, error) {
		if _, ok := token.Method.(*jwt.SigningMethodRSA); !ok {
			return nil, errors.New("método de assinatura inválido")
		}
		return key, nil
	})

	if err != nil {
		return nil, errors.New("token inválido")
	}

	return token, nil
}

func AuthMiddleware() gin.HandlerFunc {
	return func(c *gin.Context) {
		// Extrai o token do header Authorization
		authHeader := c.GetHeader("Authorization")
		if authHeader == "" || !strings.HasPrefix(authHeader, "Bearer ") {
			c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{"error": "Token ausente ou inválido"})
			return
		}

		tokenString := strings.TrimPrefix(authHeader, "Bearer ")

		// Valida o token
		_, err := ValidateToken(tokenString)
		if err != nil {
			fmt.Println("err Chave pública:", string(publicKey))
			c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{"error": "Token inválido"})
			return
		}

		// Continua se o token for válido
		c.Next()
	}
}

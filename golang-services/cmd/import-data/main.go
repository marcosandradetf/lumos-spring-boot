package main

import (
	"database/sql"
	"fmt"
	"log"
	"lumos-golang/api/middleware"
	"lumos-golang/handlers"
	"lumos-golang/internal/db"
	"os"

	"github.com/gin-gonic/gin"
	_ "github.com/lib/pq"
)

func main() {
	router := gin.Default()

	middleware.ConfigureCORS(router)

	router.Use(middleware.AuthMiddleware())
	// Pega as variáveis de ambiente
	dbUser := os.Getenv("DB_USER")
	dbPassword := os.Getenv("DB_PASSWORD")
	dbHost := os.Getenv("DB_HOST")
	dbPort := os.Getenv("DB_PORT")
	dbName := os.Getenv("DB_NAME")

	// Verifica se alguma variável de ambiente está vazia
	if dbUser == "" || dbPassword == "" || dbHost == "" || dbPort == "" || dbName == "" {
		log.Fatal("ERRO: Alguma variável de ambiente do banco de dados está vazia")
	}

	// Monta a string de conexão
	connStr := fmt.Sprintf("postgresql://%s:%s@%s:%s/%s?sslmode=disable",
		dbUser, dbPassword, dbHost, dbPort, dbName)

	// Conecta ao banco de dados
	connDb, err := sql.Open("postgres", connStr)
	if err != nil {
		log.Fatal("Cannot connect to database:", err)
	}

	queries := db.New(connDb)
	handler := &handlers.Handler{Queries: queries, DB: connDb}

	//handler := &handlers.Handler{}

	router.POST("/api/stock/import", handler.CreateMaterials)

	// Inicializa o servidor na porta 8082
	log.Fatal(router.Run(":8082"))
}

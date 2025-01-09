package main

import (
	"database/sql"
	"github.com/gin-gonic/gin"
	_ "github.com/lib/pq"
	"log"
	"lumos-golang/api/middleware"
	"lumos-golang/handlers"
	"lumos-golang/internal/db"
)

func main() {
	router := gin.Default()

	middleware.ConfigureCORS(router)

	router.Use(middleware.AuthMiddleware())

	// Conecta ao banco de dados
	connDb, err := sql.Open("postgres", "postgresql://postgres:4dejulho_@localhost:5432/001SCLCONST?sslmode=disable")
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

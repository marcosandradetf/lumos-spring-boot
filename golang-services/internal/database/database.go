package database

import (
	"fmt"
	"gorm.io/driver/postgres"
	"gorm.io/gorm"
	"lumos-golang/internal/models"
)

var DB *gorm.DB

func ConnectDatabase() {
	dsn := "host=192.168.3.100 user=postgres password=4dejulho_ dbname=001SCLCONST port=5432 sslmode=disable"
	database, err := gorm.Open(postgres.Open(dsn), &gorm.Config{})
	if err != nil {
		panic("failed to connect database")
	}

	err = database.AutoMigrate(&models.User{})
	if err != nil {
		panic("failed to connect database")
	}

	DB = database
	fmt.Println("Successfully connected to database")
}

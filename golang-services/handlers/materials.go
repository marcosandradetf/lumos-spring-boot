package handlers

import (
	"database/sql"
	"errors"
	"fmt"
	"github.com/gin-gonic/gin"
	"golang.org/x/text/cases"
	"golang.org/x/text/language"
	"lumos-golang/internal/db"
	"lumos-golang/internal/models"
	"net/http"
	"strings"
)

type Handler struct {
	Queries *db.Queries
	DB      *sql.DB
}

func (h *Handler) CreateMaterials(c *gin.Context) {
	var materials []models.MaterialInput

	// Decodificar JSON diretamente do contexto
	if err := c.ShouldBindJSON(&materials); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid JSON"})
		return
	}

	tx, err := h.DB.Begin()
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Transaction failed"})
		return
	}
	defer func() {
		if p := recover(); p != nil { // Caso haja um panic, realiza o rollback e repropaga o panic
			_ = tx.Rollback()
			panic(p)
		} else if err != nil { // Caso ocorra erro durante o fluxo, realiza o rollback
			_ = tx.Rollback()
		}
	}()

	qtx := h.Queries.WithTx(tx)

	caser := cases.Title(language.BrazilianPortuguese)

	// Iterar pelos materiais
	for _, material := range materials {
		// Verificar se o material já existe
		_, err := qtx.ExistsMaterialByName(c.Request.Context(), db.ExistsMaterialByNameParams{
			MaterialName:   strings.ToLower(material.MaterialName),
			MaterialBrand:  sql.NullString{String: strings.ToLower(material.MaterialBrand), Valid: material.MaterialBrand != ""},
			MaterialPower:  sql.NullString{String: strings.ToLower(material.MaterialPower), Valid: material.MaterialPower != ""},
			MaterialLength: sql.NullString{String: strings.ToLower(material.MaterialLength), Valid: material.MaterialLength != ""},
			DepositName:    strings.ToLower(material.DepositName),
		})
		if err != nil {
			if errors.Is(err, sql.ErrNoRows) {
				fmt.Println("marca", material.MaterialBrand)
			} else {
				c.JSON(http.StatusInternalServerError, gin.H{"error": "Database Error"})
				return
			}
		} else {
			fmt.Println("Pulando material encontrado: ", material.MaterialName)
			continue
		}

		companyId, err := qtx.ExistsCompanyByName(c.Request.Context(),
			strings.ToLower(material.CompanyName))
		if err != nil {
			if errors.Is(err, sql.ErrNoRows) {
				companyId, err = qtx.CreateCompany(c.Request.Context(), material.CompanyName)
				if err != nil {
					c.JSON(http.StatusInternalServerError, gin.H{"error": "Database Error"})
					return
				}
				fmt.Println("Empresa criada ID: ", companyId)
			} else {
				c.JSON(http.StatusInternalServerError, gin.H{"error": "Database Error"})
				return
			}
		} else {
			fmt.Println("Empresa já existe ID: ", companyId)
		}

		depositId, err := qtx.ExistsDepositByName(c.Request.Context(),
			strings.ToLower(material.DepositName))
		if err != nil {
			if errors.Is(err, sql.ErrNoRows) {
				depositId, err = qtx.CreateDeposit(c.Request.Context(), db.CreateDepositParams{
					DepositName: material.DepositName,
					CompanyID:   companyId,
				})
				if err != nil {
					c.JSON(http.StatusInternalServerError, gin.H{"error": "Database Error1"})
					return
				}
				fmt.Println("Almoxarifado criado ID: ", depositId)
			} else {
				c.JSON(http.StatusInternalServerError, gin.H{"error": "Database Error2"})
				return
			}
		} else {
			fmt.Println("Almoxarifado já existe ID: ", depositId)
		}

		groupId, err := qtx.ExistsByGroupName(c.Request.Context(),
			strings.ToLower(material.MaterialGroupName))
		if err != nil {
			if errors.Is(err, sql.ErrNoRows) {
				groupId, err = qtx.CreateGroup(c.Request.Context(), material.MaterialGroupName)
				if err != nil {
					c.JSON(http.StatusInternalServerError, gin.H{"error": "Database Error"})
					return
				}
				fmt.Println("Grupo criado ID: ", groupId)
			} else {
				c.JSON(http.StatusInternalServerError, gin.H{"error": "Database Error"})
				return
			}
		} else {
			fmt.Println("Grupo já existe ID: ", groupId)
		}

		typeId, err := qtx.ExistsTypeByName(c.Request.Context(),
			strings.ToLower(material.MaterialTypeName))
		if err != nil {
			if errors.Is(err, sql.ErrNoRows) {
				typeId, err = qtx.CreateType(c.Request.Context(), db.CreateTypeParams{
					IDGroup:  groupId,
					TypeName: material.MaterialTypeName,
				})
				if err != nil {
					c.JSON(http.StatusInternalServerError, gin.H{"error": "Database Error"})
					return
				}
				fmt.Println("Tipo criado ID: ", typeId)
			} else {
				c.JSON(http.StatusInternalServerError, gin.H{"error": "Database Error"})
				return
			}
		} else {
			fmt.Println("Tipo já existe ID: ", typeId)
		}

		// Criar material no banco
		err = qtx.CreateMaterial(c.Request.Context(), db.CreateMaterialParams{
			MaterialName:   material.MaterialName,
			MaterialBrand:  sql.NullString{String: caser.String(strings.ToLower(material.MaterialBrand)), Valid: material.MaterialBrand != ""},
			MaterialPower:  sql.NullString{String: strings.ToUpper(material.MaterialPower), Valid: material.MaterialPower != ""},
			MaterialAmps:   sql.NullString{String: strings.ToUpper(material.MaterialAmps), Valid: material.MaterialAmps != ""},
			MaterialLength: sql.NullString{String: strings.ToLower(material.MaterialLength), Valid: material.MaterialLength != ""},
			BuyUnit:        strings.ToUpper(material.BuyUnit),
			RequestUnit:    strings.ToUpper(material.RequestUnit),
			IDMaterialType: typeId,
			IDCompany:      companyId,
			IDDeposit:      depositId,
		})
		if err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"error": "Database Error"})
			return
		}
	}

	err = tx.Commit()
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Transaction failed"})
		return
	}

	// Retornar resposta de sucesso
	c.JSON(http.StatusCreated, gin.H{
		"message": "Dados importados com sucesso!",
	})
}

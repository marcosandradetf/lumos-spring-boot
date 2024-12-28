package import_data

import (
	"github.com/gin-gonic/gin"
	"lumos-golang/api/middleware"
)

func main() {
	router := gin.Default()

	middleware.ConfigureCORS(router)
	mid
}

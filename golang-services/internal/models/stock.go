package models

type MaterialInput struct {
	MaterialName      string `json:"materialName"`
	MaterialBrand     string `json:"materialBrand"`
	MaterialPower     string `json:"materialPower"`
	MaterialAmps      string `json:"materialAmps"`
	MaterialLength    string `json:"materialLength"`
	BuyUnit           string `json:"buyUnit"`
	RequestUnit       string `json:"requestUnit"`
	MaterialGroupName string `json:"materialTypeName"`
	MaterialTypeName  string `json:"materialGroupName"`
	CompanyName       string `json:"companyName"`
	DepositName       string `json:"depositName"`
}

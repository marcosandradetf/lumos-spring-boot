package com.lumos.lumosspring.stock.materialstock.controller;

import com.lumos.lumosspring.stock.materialsku.dto.MaterialResponse;
import com.lumos.lumosspring.stock.materialstock.service.MaterialStockViewService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class MaterialStockViewController {
    private final MaterialStockViewService materialStockViewService;

    public MaterialStockViewController(MaterialStockViewService materialStockViewService) {
        this.materialStockViewService = materialStockViewService;
    }


    // Endpoint para retornar todos os materiais
    @GetMapping("/material/find-stock-by-deposit-id")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('SCOPE_ESTOQUISTA_CHEFE') or hasAuthority('SCOPE_ESTOQUISTA')")
    public ResponseEntity<MaterialStockViewService.PagedResponse<MaterialResponse>> getAllMaterials(
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "depositId", required = false, defaultValue = "-1") Long depositId) {

        return materialStockViewService.getAllMaterialsWithPagination(page, size, depositId);
    }

    @GetMapping("/material/find-stock-by-search")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('SCOPE_ESTOQUISTA_CHEFE') or hasAuthority('SCOPE_ESTOQUISTA')")
    public ResponseEntity<MaterialStockViewService.PagedResponse<MaterialResponse>> getMaterialByNameStartingWith(
            @RequestParam(value = "page", defaultValue = "0") Integer page,  // 'page' com valor padrão
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "name") String name,  // 'name' vindo da URL
            @RequestParam(value = "depositId", required = false, defaultValue = "-1") Long depositId) {  // 'size' com valor padrão

        return materialStockViewService.getMaterialsBySearchWithPagination(name, page, size, depositId);  // Retorna os materiais no formato de resposta
    }

}

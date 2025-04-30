package com.lumos.lumosspring.stock.controller.mobile;

import com.lumos.lumosspring.stock.controller.dto.mobile.DepositResponseMobile;
import com.lumos.lumosspring.stock.controller.dto.mobile.MaterialDTOMob;
import com.lumos.lumosspring.stock.service.DepositService;
import com.lumos.lumosspring.stock.service.MaterialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mobile/stock")
public class StockControllerMobile {
    @Autowired
    private DepositService almoxarifadoService;
    @Autowired
    private MaterialService materialService;

    @GetMapping("get-deposits")
    public ResponseEntity<List<DepositResponseMobile>> getAllMobile() {
        return almoxarifadoService.findAllForMobile();
    }

    @GetMapping("get-items")
    public ResponseEntity<List<MaterialDTOMob>> getItemsForMob() {
        return materialService.findAllForMobile();
    }


}

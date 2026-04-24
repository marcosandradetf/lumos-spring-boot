package com.lumos.lumosspring.stock.materialsku.service;

import com.lumos.lumosspring.contract.entities.MaterialContractReferenceItem;
import com.lumos.lumosspring.stock.deposit.repository.DepositRepository;
import com.lumos.lumosspring.stock.materialsku.dto.MaterialRequest;
import com.lumos.lumosspring.stock.materialsku.model.Material;
import com.lumos.lumosspring.stock.materialsku.repository.MaterialContractReferenceItemRepository;
import com.lumos.lumosspring.stock.materialsku.repository.MaterialReferenceRepository;
import com.lumos.lumosspring.stock.materialstock.model.MaterialStock;
import com.lumos.lumosspring.stock.materialstock.repository.MaterialStockRegisterRepository;
import com.lumos.lumosspring.system.entities.Log;
import com.lumos.lumosspring.system.repository.LogRepository;
import com.lumos.lumosspring.user.repository.UserRepository;
import com.lumos.lumosspring.util.Utils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class MaterialReferenceViewService {
    private final MaterialStockRegisterRepository materialStockRegisterRepository;
    private final UserRepository userRepository;
    private final LogRepository logRepository;
    private final MaterialReferenceRepository materialReferenceRepository;
    private final MaterialContractReferenceItemRepository materialContractReferenceItemRepository;
    private final DepositRepository depositRepository;

    public MaterialReferenceViewService(MaterialStockRegisterRepository materialStockRegisterRepository,
                                        UserRepository userRepository,
                                        LogRepository logRepository,
                                        MaterialReferenceRepository materialReferenceRepository,
                                        MaterialContractReferenceItemRepository materialContractReferenceItemRepository, DepositRepository depositRepository) {

        this.materialStockRegisterRepository = materialStockRegisterRepository;
        this.userRepository = userRepository;
        this.logRepository = logRepository;
        this.materialReferenceRepository = materialReferenceRepository;
        this.materialContractReferenceItemRepository = materialContractReferenceItemRepository;
        this.depositRepository = depositRepository;
    }


}

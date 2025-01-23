package com.lumos.lumosspring.execution.service;

import com.lumos.lumosspring.execution.controller.dto.ItemsResponse;
import com.lumos.lumosspring.execution.repository.ItemRepository;
import com.lumos.lumosspring.stock.controller.dto.MaterialResponse;
import com.lumos.lumosspring.stock.entities.Material;
import com.lumos.lumosspring.stock.repository.MaterialRepository;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ItemService {
    private final ItemRepository repository;
    private final MaterialRepository materialRepository;

    public ItemService(ItemRepository repository, MaterialRepository materialRepository) {
        this.repository = repository;
        this.materialRepository = materialRepository;
    }


    public ResponseEntity<?> getItems(Long depositId) {
        List<Material> materials = materialRepository.getByDeposit(depositId);
        List<ItemsResponse> items = new ArrayList<>();

        for (Material m : materials) {
            items.add(new ItemsResponse(
                    m.getIdMaterial(),
                    m.getMaterialName(),
                    m.getStockQuantity()
            ));
        }

        return ResponseEntity.ok(items);
    }
}

package com.lumos.lumosspring.execution.controller;

import com.lumos.lumosspring.execution.service.ItemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/execution")
public class ItemsController {
    private final ItemService itemService;

    public ItemsController(ItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping("/itens/{depositId}")
    public ResponseEntity<?> deposit(@PathVariable("depositId") Long depositId) {
        return itemService.getItems(depositId);
    }
}

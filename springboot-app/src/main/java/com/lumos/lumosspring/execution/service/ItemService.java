package com.lumos.lumosspring.execution.service;

import com.lumos.lumosspring.execution.repository.ItemRepository;
import org.springframework.stereotype.Service;

@Service
public class ItemService {
    private final ItemRepository repository;

    public ItemService(ItemRepository repository) {
        this.repository = repository;
    }


}

package com.lumos.lumosspring.execution.controller.dto;

import java.util.List;

public record StreetWithItemsDTO(String street, List<ItemsDTO> items
) {
}

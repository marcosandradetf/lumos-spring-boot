package com.lumos.lumosspring.stock.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table
public class Brand {
    @Id
    private Long brandId;

    private String brandName;

}

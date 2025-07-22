package com.lumos.lumosspring.contract.repository;

import com.lumos.lumosspring.contract.entities.ContractItem;
import org.springframework.data.repository.CrudRepository;

public interface ContractItemsQuantitativeRepository extends CrudRepository<ContractItem, Long> {
}

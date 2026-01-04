package com.lumos.lumosspring.stock.materialsku.repository;

import com.lumos.lumosspring.stock.materialsku.model.MaterialContractReferenceItem;
import com.lumos.lumosspring.stock.materialsku.model.MaterialGroup;
import kotlin.Pair;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialContractReferenceItemRepository extends CrudRepository<MaterialContractReferenceItem, Long> {

}

package com.lumos.lumosspring.stock.materialsku.repository;

import com.lumos.lumosspring.stock.materialsku.model.MaterialContractReferenceItem;
import com.lumos.lumosspring.stock.materialsku.model.MaterialGroup;
import kotlin.Pair;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialContractReferenceItemRepository extends CrudRepository<MaterialContractReferenceItem, Long> {

    @Modifying
    void deleteByMaterialId(Long materialId);

    @Query(
            """
                select contract_reference_item_id
                from material_contract_reference_item
                where material_id = :materialId
            """
    )
    List<Long> findAllByMaterialId(Long materialId);
}

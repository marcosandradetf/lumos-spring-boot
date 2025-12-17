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

    @Query("""
        select distinct ci.contract_item_id, ms.material_id_stock
        from material_stock ms
        join material m on m.id_material = ms.material_id
        join material_contract_reference_item mcri on mcri.material_id = m.id_material
        join contract_reference_item cri on cri.contract_reference_item_id = mcri.contract_reference_item_id
        join contract_item ci on ci.contract_item_reference_id = cri.contract_reference_item_id
        join direct_execution_item dei on dei.contract_item_id = ci.contract_item_id
        where ms.material_id_stock in (:materialIds)
            AND dei.direct_execution_id = :directExecutionId
    """)
    List<Pair<Long, Long>> findByContractReferenceItemId(List<Long> materialIds, Long directExecutionId);
}

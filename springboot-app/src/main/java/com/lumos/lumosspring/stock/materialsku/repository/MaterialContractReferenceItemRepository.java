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
        SELECT distinct ci.contract_item_id, ms.material_id_stock
        FROM material_stock ms
        JOIN material m ON m.id_material = ms.material_id
        JOIN material_contract_reference_item mcri ON mcri.material_id = m.id_material
        JOIN contract_reference_item cri ON cri.contract_reference_item_id = mcri.contract_reference_item_id
        JOIN contract_item ci ON ci.contract_item_reference_id = cri.contract_reference_item_id
        JOIN direct_execution_item dei ON dei.contract_item_id = ci.contract_item_id
        WHERE ms.material_id_stock IN (:materialIds)
            AND dei.direct_execution_id = :directExecutionId
    """)
    List<Pair<Long, Long>> findByContractReferenceItemId(List<Long> materialIds, Long directExecutionId);
}

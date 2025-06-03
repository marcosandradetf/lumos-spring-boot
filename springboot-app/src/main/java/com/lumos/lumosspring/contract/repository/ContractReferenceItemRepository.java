package com.lumos.lumosspring.contract.repository;

import com.lumos.lumosspring.contract.dto.ContractItemsDTO;
import com.lumos.lumosspring.contract.dto.ContractReferenceItemDTO;
import com.lumos.lumosspring.contract.dto.PContractReferenceItemDTO;
import com.lumos.lumosspring.contract.entities.ContractReferenceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ContractReferenceItemRepository extends JpaRepository<ContractReferenceItem, Long> {
    @Query("SELECT new com.lumos.lumosspring.contract.dto.PContractReferenceItemDTO(" +
            "i.contractReferenceItemId, i.description" +
            ",i.nameForImport, i.type," +
            "i.linking, i.itemDependency )" +
            "FROM ContractReferenceItem i " +
            "WHERE i.nameForImport is not null " +
            "and i.type not in ('SERVICE', 'PROJETO')")
    List<PContractReferenceItemDTO> findAllByPreMeasurement();

}

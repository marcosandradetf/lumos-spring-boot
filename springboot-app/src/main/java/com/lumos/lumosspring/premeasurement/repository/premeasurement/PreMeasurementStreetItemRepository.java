package com.lumos.lumosspring.premeasurement.repository.premeasurement;

import com.lumos.lumosspring.contract.dto.ItemResponseDTO;
import com.lumos.lumosspring.premeasurement.model.PreMeasurementStreetItem;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PreMeasurementStreetItemRepository extends CrudRepository<PreMeasurementStreetItem, Long> {
    List<PreMeasurementStreetItem> findAllByPreMeasurementStreetId(Long preMeasurementStreetId);

    List<PreMeasurementStreetItem> findAllByPreMeasurementStreetItemId(Long id);

    List<PreMeasurementStreetItem> findAllByPreMeasurementId(Long preMeasurementId);

    @Query("""
        select
            ci.contract_item_id,
            cri.description,
            sum(pmsi.measured_item_quantity) as quantity,
            sum(ci.contracted_quantity - ci.quantity_executed) as current_balance,
            cri.type,
            cri.linking,
            cri.contract_reference_item_id,
            cri.truck_stock_control
        from pre_measurement_street_item pmsi
        join contract_item ci on ci.contract_item_id = pmsi.contract_item_id
        join contract_reference_item cri on cri.contract_reference_item_id = ci.contract_item_reference_id
        where pmsi.pre_measurement_id = :preMeasurementID
            and pmsi.item_status = :itemStatus
            and cri.type not in ('SERVIÇO', 'PROJETO', 'MANUTENÇÃO','EXTENSÃO DE REDE', 'TERCEIROS', 'CEMIG')
        group by ci.contracted_quantity, ci.quantity_executed, cri.description, cri.contract_reference_item_id, ci.contract_item_id
        order by description
    """)
    List<ItemResponseDTO> getItemsByPreMeasurementId(long preMeasurementID, String itemStatus);
}

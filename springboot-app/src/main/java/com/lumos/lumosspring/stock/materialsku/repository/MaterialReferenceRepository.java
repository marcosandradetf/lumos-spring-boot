package com.lumos.lumosspring.stock.materialsku.repository;

import com.lumos.lumosspring.stock.materialsku.model.Material;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MaterialReferenceRepository extends CrudRepository<Material, Long> {
    @Query("""
            SELECT *
            FROM material
            WHERE (inactive IS NULL OR inactive = false)
              AND name_for_import IS NOT NULL
            """)
    List<Material> findAllForImportPreMeasurement();

    @Query("select id_material from material where material_name = :name and tenant_id = :tenantId")
    Long findBaseMaterialId(String name, UUID tenantId);

    Optional<Material> findByBarcodeAndTenantId(String barcode, UUID tenantId);

    Optional<Material> findFirstByBarcode(String barcode);

    @Query("""
                select
                    m.id_material as material_id,
                    m.material_name,
                    m.id_material_type as material_type,
                    m.subtype_id as material_subtype,
                    m.barcode,
                    m.buy_unit,
                    m.request_unit,
                    m.inactive
                from material m
                where m.tenant_id = :tenantId
                    and m.is_generic = false
                order by m.material_name
            """)
    List<MaterialResponse> getCatalogue(UUID tenantId);
    record MaterialResponse(
            Long materialId,
            String materialName,
            Long materialType,
            Long materialSubtype,
            String barcode,
            String buyUnit,
            String requestUnit,
            Boolean inactive) {
    }
}


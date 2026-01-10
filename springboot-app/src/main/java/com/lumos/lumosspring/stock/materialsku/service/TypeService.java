package com.lumos.lumosspring.stock.materialsku.service;

import com.lumos.lumosspring.stock.materialsku.dto.TypeDTO;
import com.lumos.lumosspring.stock.materialsku.model.MaterialType;
import com.lumos.lumosspring.stock.materialstock.repository.MaterialStockRegisterRepository;
import com.lumos.lumosspring.stock.materialsku.repository.TypeRepository;
import com.lumos.lumosspring.stock.materialstock.repository.MaterialStockViewRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TypeService {
    private final TypeRepository typeRepository;
    private final MaterialStockRegisterRepository materialStockRegisterRepository;
    private final MaterialStockViewRepository materialStockViewRepository;

    public TypeService(
            TypeRepository typeRepository,
            MaterialStockRegisterRepository materialStockRegisterRepository, MaterialStockViewRepository materialStockViewRepository) {
        this.typeRepository = typeRepository;
        this.materialStockRegisterRepository = materialStockRegisterRepository;
        this.materialStockViewRepository = materialStockViewRepository;
    }

    @Cacheable("getAllTypes")
    public List<MaterialType> findAll() {
        return typeRepository.findAllByOrderByIdTypeAsc();
    }

    record sypTypeResponse(Long subtypeId, String subtypeName) {}
    public record TypeResponse(Long typeId, String typeName, List<sypTypeResponse> subtypes) {}

    @Cacheable("getAllTypes")
    public List<TypeResponse> findAllTypeSubtype() {
        var types = typeRepository.findAllTypeSubtype();

        return types.stream()
                .collect(Collectors.groupingBy(
                        TypeRepository.TypeSubtypeResponse::typeId
                ))
                .values()
                .stream()
                .map(group -> {
                    var first = group.getFirst();

                    var subtypes = group.stream()
                            .filter(e -> e.subtypeId() != null)
                            .map(e -> new sypTypeResponse(e.subtypeId(), e.subtypeName()))
                            .distinct()
                            .toList();

                    return new TypeResponse(first.typeId(), first.typeName(), subtypes);

                })
                .sorted(Comparator.comparing(TypeResponse::typeName))
                .toList();
    }

    public ResponseEntity<?> findUnitsByTypeId(Long typeId) {
        record UnitResponse(List<TypeRepository.TypeUnitResponse> buyUnits, List<TypeRepository.TypeUnitResponse> requestUnits) {}

        var units = typeRepository.findUnitsByTypeId(typeId);
        var response = new UnitResponse(
                units.stream().filter(TypeRepository.TypeUnitResponse::buyUnit).toList(),
                units.stream().filter(u -> !u.buyUnit()).toList()
        );

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }


    public MaterialType findById(Long id) {
        return typeRepository.findById(id).orElse(null);
    }

    public ResponseEntity<?> save(TypeDTO typeDTO) {
        if (typeRepository.existsByTypeName(typeDTO.typeName())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Collections.singletonMap("message", "Este tipo já existe."));
        }

        MaterialType materialType = new MaterialType();
        materialType.setTypeName(typeDTO.typeName());
        materialType.setIdGroup(typeDTO.groupId());
        typeRepository.save(materialType);

        return ResponseEntity.ok(this.findAll());
    }

    public ResponseEntity<?> update(Long typeId, TypeDTO typeDTO) {
        var type = typeRepository.findById(typeId).orElse(null);
        if (type == null) {
            return ResponseEntity.notFound().build();
        }

        type.setTypeName(typeDTO.typeName());
        type.setIdGroup(typeDTO.groupId());
        typeRepository.save(type);

        return ResponseEntity.ok(this.findAll());
    }

    public ResponseEntity<?> delete(Long id) {
        var type = typeRepository.findById(id).orElse(null);
        if (type == null) {
            return ResponseEntity.notFound().build();
        }

        if (materialStockViewRepository.existsType(id).isPresent()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Não é possível excluir: há materiais associados a este tipo."));
        }

        typeRepository.delete(type);
        return ResponseEntity.ok(this.findAll());
    }
}
